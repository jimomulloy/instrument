package jomu.instrument.audio;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

public class TarsosAudioDispatcherFactory {

	public AudioDispatcher getAudioDispatcher(final TarsosDSPAudioInputStream audioStream, final int audioBufferSize,
			final int bufferOverlap) {
		return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);

	}
}
