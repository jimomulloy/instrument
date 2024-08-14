package jomu.instrument.audio;

import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * This AudioProcessor can be used to sync events with sound. It uses a pattern
 * described in JavaFX Special Effects Taking Java RIA to the Extreme with
 * Animation, Multimedia, and Game Element Chapter 9 page 185: <blockquote><i>
 * The variable LineWavelet is the Java Sound object that actually makes the
 * sound. The
 * write method on LineWavelet is interesting because it blocks until it is
 * ready for
 * more data. </i></blockquote> If this AudioProcessor chained with other
 * AudioProcessors the others should be able to operate in real time or process
 * the signal on a separate thread.
 * 
 * @author Joren Six
 */
public final class AudioPlayer implements AudioProcessor {
	private final static Logger LOG = Logger.getLogger(AudioPlayer.class.getName());
	/**
	 * The LineWavelet to send sound to. Is also used to keep everything in sync.
	 */
	private SourceDataLine line;

	private final AudioFormat format;

	byte[] lastBuffer = null;

	boolean lastBufferEmpty = true;

	/**
	 * Creates a new audio player.
	 * 
	 * @param format
	 *            The AudioFormat of the buffer.
	 * @throws LineUnavailableException
	 *             If no output LineWavelet is available.
	 */
	public AudioPlayer(final AudioFormat format) throws LineUnavailableException {
		this(format, 1024);
	}

	public AudioPlayer(final AudioFormat format, int bufferSize) throws LineUnavailableException {
		final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, bufferSize);
		LOG.info("Opening data line" + info.toString());
		this.format = format;
		line = (SourceDataLine) AudioSystem.getLine(info);

		line.open(format, bufferSize * 2);
		line.start();
	}

	public AudioPlayer(final TarsosDSPAudioFormat format, int bufferSize) throws LineUnavailableException {
		this(JVMAudioInputStream.toAudioFormat(format), bufferSize);
	}

	public AudioPlayer(final TarsosDSPAudioFormat format) throws LineUnavailableException {
		this(JVMAudioInputStream.toAudioFormat(format));
	}

	public long getMicroSecondPosition() {
		return line.getMicrosecondPosition();
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		boolean thisBufferEmpty = true;
		for (byte entry : audioEvent.getByteBuffer()) {
			if (entry != 0) {
				thisBufferEmpty = false;
				break;
			}
		}
		if (lastBuffer != null) {
			if (lastBufferEmpty && !thisBufferEmpty) {
				for (int i = 0; i < audioEvent.getByteBuffer().length; i++) {
					if (audioEvent.getByteBuffer()[i] != 0) {
						audioEvent.getByteBuffer()[i] = 0;
					} else {
						break;
					}
				}
			} else if (!lastBufferEmpty && thisBufferEmpty) {
				// for (int i = 0; i < audioEvent.getByteBuffer().length; i++) {
				// if (audioEvent.getByteBuffer()[i] != 0) {
				// audioEvent.getByteBuffer()[i] = 0;
				// } else {
				// break;
				// }
				// }
			}
		}
		byte[] lastBuffer = audioEvent.getByteBuffer();
		int byteOverlap = audioEvent.getOverlap() * format.getFrameSize();
		int byteStepSize = audioEvent.getBufferSize() * format.getFrameSize() - byteOverlap;
		LOG.severe(">>>AO 1: " + byteOverlap + ", " + byteStepSize
				+ ", " + (System.currentTimeMillis() / 1000.0)
				+ ", " + audioEvent.getTimeStamp() + ", " + audioEvent.getSamplesProcessed());

		if (audioEvent.getTimeStamp() == 0) {
			byteOverlap = 0;
			byteStepSize = audioEvent.getBufferSize() * format.getFrameSize();
			LOG.severe(">>>AO 2: " + byteOverlap + ", " + byteStepSize
					+ ", " + (System.currentTimeMillis() / 1000.0)
					+ ", " + audioEvent.getTimeStamp() + ", " + audioEvent.getSamplesProcessed());
		}
		// overlap in samples * nr of bytes / sample = bytes overlap

		/*
		 * if(byteStepSize < line.available()){
		 * System.out.println(line.available() + " Will not block " +
		 * line.getMicrosecondPosition());
		 * }else {
		 * System.out.println("Will block " + line.getMicrosecondPosition());
		 * }
		 */

		int bytesWritten = line.write(audioEvent.getByteBuffer(), byteOverlap, byteStepSize);
		if (bytesWritten != byteStepSize) {
			System.err.println(
					String.format("Expected to write %d bytes but only wrote %d bytes", byteStepSize, bytesWritten));
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
	 * processingFinished()
	 */
	public void processingFinished() {
		// cleanup
		line.drain();// drain takes too long..
		line.stop();
		line.close();
	}
}
