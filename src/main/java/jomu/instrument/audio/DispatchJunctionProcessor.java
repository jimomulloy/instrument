package jomu.instrument.audio;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * This class plays a file and sends float arrays to registered AudioProcessor
 * implementors. This class can be used to feed FFT's, pitch detectors, audio
 * players, ... Using a (blocking) audio player it is even possible to
 * synchronize execution of AudioProcessors and sound. This behavior can be used
 * for visualization.
 * 
 * @author Joren Six
 */
public class DispatchJunctionProcessor implements AudioProcessor {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(DispatchJunctionProcessor.class.getName());

	private final TarsosDSPAudioFormat format;

	/**
	 * This buffer is reused again and again to store audio data using the float
	 * data type.
	 */
	private float[] audioFloatBuffer;

	private float[] lastBuffer;

	private int processedLength;

	private String name;

	/**
	 * A list of registered audio processors. The audio processors are responsible
	 * for actually doing the digital signal processing
	 */
	private final List<AudioProcessor> audioProcessors;

	/**
	 * The floatOverlap: the number of elements that are copied in the buffer from
	 * the previous buffer. Overlap should be smaller (strict) than the buffer size
	 * and can be zero. Defined in number of samples.
	 */
	private int floatOverlap, floatStepSize, floatPosition;

	/**
	 * The overlap and stepsize defined not in samples but in bytes. So it depends
	 * on the bit depth. Since the int datatype is used only 8,16,24,... bits or
	 * 1,2,3,... bytes are supported.
	 */
	private int byteOverlap, byteStepSize;

	/**
	 * The number of bytes to skip before processing starts.
	 */
	private long bytesToSkip;

	/**
	 * Position in the stream in bytes. e.g. if 44100 bytes are processed and 16
	 * bits per frame are used then you are 0.5 seconds into the stream.
	 */
	private long bytesProcessed;

	/**
	 * The audio event that is send through the processing chain.
	 */
	private AudioEvent audioEvent;

	/**
	 * If true the dispatcher stops dispatching audio.
	 */
	private boolean stopped;

	/**
	 * If true then the first buffer is only filled up to buffer size - hop size
	 * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000 times
	 * zero 0 and 48 actual audio samples. During the next iteration you get mostly
	 * zeros and 96 samples.
	 */
	private boolean zeroPadFirstBuffer;

	/**
	 * If true then the last buffer is zero padded. Otherwise the buffer is
	 * shortened to the remaining number of samples. If false then the audio
	 * processors must be prepared to handle shorter audio buffers.
	 */
	private boolean zeroPadLastBuffer;

	private float[] leftoverBuffer = new float[0];

	private boolean firstProcessed = false;

	/**
	 * Create a new dispatcher junction processor
	 * 
	 * @param audioBufferSize The size of the buffer defines how much samples are
	 *                        processed in one step. Common values are 1024,2048.
	 * @param bufferOverlap   How much consecutive buffers overlap (in samples).
	 *                        Half of the AudioBufferSize is common (512, 1024) for
	 *                        an FFT.
	 */
	public DispatchJunctionProcessor(final TarsosDSPAudioFormat format, final int audioBufferSize,
			final int bufferOverlap) {
		// The copy on write list allows concurrent modification of the list while
		// it is iterated. A nice feature to have when adding AudioProcessors while
		// the AudioDispatcher is running.
		audioProcessors = new CopyOnWriteArrayList<AudioProcessor>();

		this.format = format;

		setStepSizeAndOverlap(audioBufferSize, bufferOverlap);

		audioEvent = new AudioEvent(format);
		audioEvent.setFloatBuffer(audioFloatBuffer);
		audioEvent.setOverlap(bufferOverlap);

		stopped = false;

		bytesToSkip = 0;

		zeroPadLastBuffer = true;
	}

	/**
	 * Adds an AudioProcessor to the chain of processors.
	 * 
	 * @param audioProcessor The AudioProcessor to add.
	 */
	public void addAudioProcessor(final AudioProcessor audioProcessor) {
		audioProcessors.add(audioProcessor);
		LOG.fine("Added an audioprocessor to the list of processors: " + audioProcessor.toString());
	}

	public TarsosDSPAudioFormat getFormat() {
		return format;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return True if the dispatcher is stopped or the end of stream has been
	 *         reached.
	 */
	public boolean isStopped() {
		return stopped;
	}

	@Override
	public boolean process(AudioEvent incomingAudioEvent) {
		// System.out.println(
		// ">>DJP audio: " + name + ", " + incomingAudioEvent.getTimeStamp() + ", " +
		// incomingAudioEvent.getSamplesProcessed());
		// Passthrough
		if (this.audioFloatBuffer.length == incomingAudioEvent.getBufferSize()
				&& this.floatOverlap == incomingAudioEvent.getOverlap()) {
			audioEvent = incomingAudioEvent; // TODO !!
			for (final AudioProcessor processor : audioProcessors) {
				if (!processor.process(audioEvent)) {
					// skip to the next audio processors if false is returned.
					break;
				}
			}
			return true;
		}

		boolean isFirstBuffer = (bytesProcessed == 0 || bytesProcessed == bytesToSkip);
		boolean isExpanding = this.audioFloatBuffer.length >= incomingAudioEvent.getBufferSize();
		boolean leftover = false;

		int incomingBufferSize = incomingAudioEvent.getBufferSize();
		int incomingOverlap = incomingAudioEvent.getOverlap();
		int incomingPosition = 0;
		float[] incomingBuffer = incomingAudioEvent.getFloatBuffer();
		if (!isFirstBuffer) {
			incomingBufferSize -= incomingOverlap;
			incomingPosition = incomingOverlap;
		}

		if (isExpanding) {
			if (lastBuffer == null) {
				lastBuffer = new float[this.audioFloatBuffer.length];
				transferAudioBuffer(incomingBufferSize, incomingPosition, incomingBuffer);
			} else {
				do {
					leftover = false;
					if (floatOverlap > 0 && processedLength == 0) {
						System.arraycopy(lastBuffer, floatStepSize, audioFloatBuffer, 0, floatOverlap);
						processedLength += floatOverlap;
					}
					if (processedLength + leftoverBuffer.length >= this.audioFloatBuffer.length) {
						leftover = true;
						transferAudioBuffer(this.audioFloatBuffer.length - processedLength, 0, leftoverBuffer);
					} else {
						if (leftoverBuffer.length > 0) {
							try {
								System.arraycopy(leftoverBuffer, 0, audioFloatBuffer, processedLength,
										leftoverBuffer.length);
							} catch (Throwable t) {
								System.out.println("..");
							}
							processedLength += leftoverBuffer.length;
							leftoverBuffer = new float[0];
						}
						transferAudioBuffer(incomingBufferSize, incomingPosition, incomingBuffer);
					}
				} while (leftover);
			}
		} else {
			if (isFirstBuffer) {
				do {
					int copyLength = this.audioFloatBuffer.length;
					if (isFirstBuffer) {
						System.arraycopy(incomingBuffer, incomingPosition, audioFloatBuffer, 0, copyLength);
						// isFirstBuffer = false;
					} else {

					}
					processedLength = copyLength;
					audioEvent.setBytesProcessed(
							(audioEvent.getSamplesProcessed() + processedLength) * format.getFrameSize());
					for (final AudioProcessor processor : audioProcessors) {
						// System.out.println(
						// ">>DJP 2 processor: " + name + ", " + audioEvent.getTimeStamp() + ", " +
						// processor.getClass().descriptorString());
						if (!processor.process(audioEvent)) {
							// skip to the next audio processors if false is returned.
							break;
						}
					}
					incomingPosition += copyLength;
				} while (incomingPosition < incomingBufferSize);
				processedLength = 0;
			} else {

			}
		}
		return true;
	}

	@Override
	public void processingFinished() {
		System.out.println(">>!!DJP FINISH!!");
		// TODO Auto-generated method stub
		for (final AudioProcessor processor : audioProcessors) {
			processor.processingFinished();
		}
	}

	/**
	 * Removes an AudioProcessor to the chain of processors and calls its
	 * <code>processingFinished</code> method.
	 * 
	 * @param audioProcessor The AudioProcessor to remove.
	 */
	public void removeAudioProcessor(final AudioProcessor audioProcessor) {
		audioProcessors.remove(audioProcessor);
		audioProcessor.processingFinished();
		LOG.fine("Remove an audioprocessor to the list of processors: " + audioProcessor.toString());
	}

	/**
	 * 
	 * @return The currently processed number of seconds.
	 */
	public float secondsProcessed() {
		return bytesProcessed / (format.getSampleSizeInBits() / 8) / format.getSampleRate() / format.getChannels();
	}

	public void setAudioFloatBuffer(float[] audioBuffer) {
		audioFloatBuffer = audioBuffer;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set a new step size and overlap size. Both in number of samples. Watch out
	 * with this method: it should be called after a batch of samples is processed,
	 * not during.
	 * 
	 * @param audioBufferSize The size of the buffer defines how much samples are
	 *                        processed in one step. Common values are 1024,2048.
	 * @param bufferOverlap   How much consecutive buffers overlap (in samples).
	 *                        Half of the AudioBufferSize is common (512, 1024) for
	 *                        an FFT.
	 */
	public void setStepSizeAndOverlap(final int audioBufferSize, final int bufferOverlap) {
		audioFloatBuffer = new float[audioBufferSize];
		processedLength = 0;
		floatPosition = 0;
		floatOverlap = bufferOverlap;
		floatStepSize = audioFloatBuffer.length - floatOverlap;
		byteOverlap = floatOverlap * format.getFrameSize();
		byteStepSize = floatStepSize * format.getFrameSize();
	}

	/**
	 * if zero pad is true then the first buffer is only filled up to buffer size -
	 * hop size E.g. if the buffer is 2048 and the hop size is 48 then you get
	 * 2000x0 and 48 filled audio samples
	 * 
	 * @param zeroPadFirstBuffer true if the buffer should be zeroPadFirstBuffer,
	 *                           false otherwise.
	 */
	public void setZeroPadFirstBuffer(boolean zeroPadFirstBuffer) {
		this.zeroPadFirstBuffer = zeroPadFirstBuffer;
	}

	/**
	 * If zero pad last buffer is true then the last buffer is filled with zeros
	 * until the normal amount of elements are present in the buffer. Otherwise the
	 * buffer only contains the last elements and no zeros. By default it is set to
	 * true.
	 * 
	 * @param zeroPadLastBuffer
	 */
	public void setZeroPadLastBuffer(boolean zeroPadLastBuffer) {
		this.zeroPadLastBuffer = zeroPadLastBuffer;
	}

	/**
	 * Skip a number of seconds before processing the stream.
	 * 
	 * @param seconds
	 */
	public void skip(double seconds) {
		bytesToSkip = Math.round(seconds * format.getSampleRate()) * format.getFrameSize();
	}

	/**
	 * Stops dispatching audio data.
	 */
	public void stop() {
		System.out.println(">>!!DJP STOP!!");
		stopped = true;
		for (final AudioProcessor processor : audioProcessors) {
			processor.processingFinished();
		}
	}

	private void transferAudioBuffer(int incomingBufferSize, int incomingPosition, float[] incomingBuffer) {
		int lengthRemaining = this.audioFloatBuffer.length - processedLength;
		int copyLength = incomingBufferSize <= lengthRemaining ? incomingBufferSize : lengthRemaining;
		System.arraycopy(incomingBuffer, incomingPosition, audioFloatBuffer, processedLength, copyLength);
		processedLength += copyLength;
		if (processedLength == this.audioFloatBuffer.length) {
			if (firstProcessed) {
				audioEvent.setBytesProcessed(
						(audioEvent.getSamplesProcessed() + processedLength) * format.getFrameSize());
			} else {
				audioEvent.setBytesProcessed(
						(audioEvent.getSamplesProcessed() + this.floatStepSize) * format.getFrameSize());
			}
			for (final AudioProcessor processor : audioProcessors) {
				// System.out.println(
				// ">>DJP processor: " + name + ", " + audioEvent.getTimeStamp() + ", " +
				// processor.getClass().descriptorString());
				if (!processor.process(audioEvent)) {
					// skip to the next audio processors if false is returned.
					break;
				}
			}
			System.arraycopy(audioFloatBuffer, 0, lastBuffer, 0, audioFloatBuffer.length);
			processedLength = 0;
			leftoverBuffer = new float[incomingBufferSize - copyLength];
			System.arraycopy(incomingBuffer, incomingPosition + copyLength, leftoverBuffer, 0,
					incomingBufferSize - copyLength);
			if (firstProcessed) {
				bytesProcessed += this.audioFloatBuffer.length * audioEvent.getFrameLength();
				firstProcessed = false;
			} else {
				bytesProcessed += this.floatStepSize * audioEvent.getFrameLength();
			}
		}
	}

}
