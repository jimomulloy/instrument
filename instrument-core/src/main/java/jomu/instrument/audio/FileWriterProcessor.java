package jomu.instrument.audio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * This class writes the ongoing sound to an output specified by the programmer
 */
public class FileWriterProcessor implements AudioProcessor {

	TarsosDSPAudioFormat audioFormat;
	private int audioLen = 0;
	private File out;
	private static final int HEADER_LENGTH = 44;// byte

	/**
	 * @param audioFormat
	 *            which this processor is attached to
	 * @param output
	 *            randomaccessfile of the output file
	 */
	public FileWriterProcessor(TarsosDSPAudioFormat audioFormat, final File out) {
		this.out = out;
		this.audioFormat = audioFormat;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		audioLen += audioEvent.getByteBuffer().length;
		AudioFormat outFormat = new AudioFormat(audioFormat.getSampleRate(), 16, 1, true, false);
		try (
				ByteArrayInputStream bais = new ByteArrayInputStream(audioEvent.getByteBuffer());
				AudioInputStream outAudioStream = new AudioInputStream(bais, outFormat,
						audioLen / audioFormat.getFrameSize())) {
			audioLen += audioEvent.getByteBuffer().length;
			AudioSystem.write(outAudioStream, AudioFileFormat.Type.WAVE, out);
			outAudioStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

		}
		return true;
	}

	@Override
	public void processingFinished() {
	}
}
