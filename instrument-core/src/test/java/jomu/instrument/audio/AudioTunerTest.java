package jomu.instrument.audio;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jomu.instrument.Instrument;
import jomu.instrument.workspace.Atlas;
import jomu.instrument.workspace.tonemap.ToneMap;

@EnableWeld
class AudioTunerTest {

	ToneMap toneMap;

	@WeldSetup // This tells weld to consider only Bar, nothing else
	WeldInitiator weld = WeldInitiator.performDefaultDiscovery();

	@Inject
	Instrument instrument;

	@Inject
	Atlas atlas;

	@BeforeEach
	public void init() {
		System.out.println(">>Weld test");
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

		assertFalse(result, "AudioTuner noteScan shodiul have failed");
	}

}
