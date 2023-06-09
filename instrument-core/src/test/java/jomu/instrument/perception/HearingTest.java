package jomu.instrument.perception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import jakarta.inject.Inject;
import jomu.instrument.Instrument;
import jomu.instrument.InstrumentTestBase;
import jomu.instrument.audio.TarsosAudioDispatcherFactory;

@ExtendWith(MockitoExtension.class)
class HearingTest extends InstrumentTestBase {

	@Inject
	protected Hearing hearing;

	@Mock
	TarsosAudioDispatcherFactory audioDispatcherFactory;

	@Mock
	AudioDispatcher audioDispatcher;

	TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(0, 0, 0, false, false);

	@Test
	void testStreamException() {
		Exception thrown = Assertions.assertThrows(Exception.class, () -> {
			hearing.startAudioFileStream("");
		});
		Assertions.assertEquals("Stream closed", thrown.getMessage());
	}

	@Test
	void testFileException() {

		String fileName = "data/3notescale.wav";
		URL fileResource = getClass().getClassLoader().getResource(fileName);

		when(audioDispatcherFactory.getAudioDispatcher(any(TarsosDSPAudioInputStream.class), anyInt(), anyInt()))
				.thenReturn(audioDispatcher);
		when(audioDispatcher.getFormat()).thenReturn(format);

		Exception thrown = Assertions.assertThrows(Exception.class, () -> {
			hearing.startAudioFileStream(fileResource.getPath());
		});
		Assertions.assertEquals("n must be greater than 0", thrown.getMessage());
	}

	protected void mockInstrument(Instrument instrument) {
		hearing.setAudioDispatcherFactory(audioDispatcherFactory);
	}
}
