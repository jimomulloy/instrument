package jomu.instrument.audio;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jomu.instrument.InstrumentTestBase;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.ToneMap;

class AudioTunerTest extends InstrumentTestBase {

	@Test
	void testNoteScan() {

		String streamId = "stream1";
		ToneMap toneMap = initToneMap(CellTypes.AUDIO_CQ, streamId);

		AudioTuner tuner = new AudioTuner();

		boolean result = tuner.noteScan(toneMap, 1, 100, 1000);

		assertTrue(result, "AudioTuner noteScan failed");
	}

}
