/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*  
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

package jomu.instrument.audio;

import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

public class AudioGenerator {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AudioGenerator.class.getName());

	/**
	 * This buffer is reused again and again to store audio data using the float
	 * data type.
	 */
	private float[] audioFloatBuffer;

	/**
	 * A list of registered audio processors. The audio processors are responsible
	 * for actually doing the digital signal processing
	 */
	private final List<AudioProcessor> audioProcessors;

	private final TarsosDSPAudioFormat format;

	/**
	 * The floatOverlap: the number of elements that are copied in the buffer from
	 * the previous buffer. Overlap should be smaller (strict) than the buffer size
	 * and can be zero. Defined in number of samples.
	 */
	private int floatOverlap, floatStepSize;

	private int samplesProcessed;

	/**
	 * The audio event that is send through the processing chain.
	 */
	private AudioEvent audioEvent;

	/**
	 * If true the dispatcher stops dispatching audio.
	 */
	private boolean stopped;

	private int audioBufferSize;

	/**
	 * Create a new generator.
	 * 
	 * @param audioBufferSize The size of the buffer defines how much samples are
	 *                        processed in one step. Common values are 1024,2048.
	 * @param bufferOverlap   How much consecutive buffers overlap (in samples).
	 *                        Half of the AudioBufferSize is common (512, 1024) for
	 *                        an FFT.
	 */
	public AudioGenerator(final int audioBufferSize, final int bufferOverlap) {

		this(audioBufferSize, bufferOverlap, 44100);
	}

	public AudioGenerator(final int audioBufferSize, final int bufferOverlap, final int samplerate) {

		audioProcessors = new CopyOnWriteArrayList<AudioProcessor>();

		format = getTargetAudioFormat(samplerate);

		setStepSizeAndOverlap(audioBufferSize, bufferOverlap);

		this.audioBufferSize = audioBufferSize;

		audioEvent = new AudioEvent(format);
		audioEvent.setFloatBuffer(audioFloatBuffer);

		stopped = false;

		samplesProcessed = 0;
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

	public AudioEvent getAudioEvent() {
		return audioEvent;
	}

	/**
	 * Removes an AudioProcessor to the chain of processors and calls
	 * processingFinished.
	 * 
	 * @param audioProcessor The AudioProcessor to remove.
	 */
	public void removeAudioProcessor(final AudioProcessor audioProcessor) {
		audioProcessors.remove(audioProcessor);
		audioProcessor.processingFinished();
		LOG.fine("Remove an audioprocessor to the list of processors: " + audioProcessor.toString());
	}

	public void resetTime() {
		samplesProcessed = 0;
	}

	public void process() {

		System.out.println(">>generateNextAudioBlock A: " + System.currentTimeMillis());
		// Read the first (and in some cases last) audio block.
		generateNextAudioBlock();
		System.out.println(">>generateNextAudioBlock B: " + System.currentTimeMillis());

		// As long as the stream has not ended
		if (!stopped) {

			// Makes sure the right buffers are processed, they can be changed
			// by audio processors.
			System.out.println(">>Generate AUDIO EVENT: " + this.samplesProcessed + ", " + audioEvent.getBufferSize()
					+ ", " + audioEvent.getFrameLength() + ", " + audioEvent.getSampleRate() + ", "
					+ audioEvent.getTimeStamp() + ", " + audioEvent.getEndTimeStamp() + ", "
					+ audioEvent.getSamplesProcessed() + ", " + audioEvent.getBufferSize());

			audioFloatBuffer = new float[audioBufferSize];
			audioEvent.clearFloatBuffer();
			// audioEvent.setFloatBuffer(audioFloatBuffer);
			for (final AudioProcessor processor : audioProcessors) {
				if (!processor.process(audioEvent)) {
					// skip to the next audio processors if false is returned.
					break;
				}
			}

			if (!stopped) {
				audioEvent.setBytesProcessed(samplesProcessed * format.getFrameSize());

				// Read, convert and process consecutive overlapping buffers.
				// Slide the buffer.
				System.out.println(">>generateNextAudioBlock C: " + System.currentTimeMillis());
				try {
					TimeUnit.MILLISECONDS.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// Notify all processors that no more data is available.
		// when stop() is called processingFinished is called explicitly, no
		// need to do this again.
		// The explicit call is to prevent timing issues.
		if (stopped) {
			stop();
		}
	}

	/**
	 * 
	 * @return The currently processed number of seconds.
	 */
	public float secondsProcessed() {
		return samplesProcessed / format.getSampleRate() / format.getChannels();
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
		floatOverlap = bufferOverlap;
		floatStepSize = audioFloatBuffer.length - floatOverlap;
	}

	/**
	 * Stops dispatching audio data.
	 */
	public void stop() {
		stopped = true;
		for (final AudioProcessor processor : audioProcessors) {
			processor.processingFinished();
		}
	}

	/**
	 * Reads the next audio block. It tries to read the number of bytes defined by
	 * the audio buffer size minus the overlap. If the expected number of bytes
	 * could not be read either the end of the stream is reached or something went
	 * wrong.
	 * 
	 * The behavior for the first and last buffer is defined by their corresponding
	 * the zero pad settings. The method also handles the case if the first buffer
	 * is also the last.
	 * 
	 */
	private void generateNextAudioBlock() {
		// assert floatOverlap < audioFloatBuffer.length;

		audioFloatBuffer = new float[audioBufferSize];

		// Shift the audio information using array copy since it is probably
		// faster than manually shifting it.
		// No need to do this on the first buffer
		// if (audioFloatBuffer.length == floatOverlap + floatStepSize) {
		// System.arraycopy(audioFloatBuffer, floatStepSize, audioFloatBuffer, 0,
		// floatOverlap);
		// }
		samplesProcessed += floatStepSize;
	}

	/**
	 * Constructs the target audio format. The audio format is one channel signed
	 * PCM of a given sample rate.
	 * 
	 * @param targetSampleRate The sample rate to convert to.
	 * @return The audio format after conversion.
	 */
	private TarsosDSPAudioFormat getTargetAudioFormat(int targetSampleRate) {
		TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
				targetSampleRate, 2 * 8, 1, 2 * 1, targetSampleRate,
				ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder()));
		return audioFormat;
	}

}
