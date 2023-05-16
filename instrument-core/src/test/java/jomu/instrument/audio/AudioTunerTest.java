package jomu.instrument.audio;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jomu.instrument.Instrument;
import jomu.instrument.workspace.Atlas;
import jomu.instrument.workspace.tonemap.ToneMap;

@QuarkusTest
class AudioTunerTest {

	ToneMap toneMap;

	@Inject
	Instrument instrument;

	@Inject
	Atlas atlas;

	@BeforeEach
	public void init() {
		Instrument.setInstance(instrument);
		instrument.initialise();
		instrument.start();
		toneMap = atlas.getToneMap("test-streamId");
	}

	@AfterEach
	public void close() {
		instrument.stop();
	}

	@Test
	void testNoteScan() {

		AudioTuner tuner = new AudioTuner();

		boolean result = tuner.noteScan(toneMap, 1, 100, 1000);

		assertTrue(result, "AudioTuner noteScan failed");
	}

}
