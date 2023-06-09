package jomu.instrument.workspace.tonemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jomu.instrument.InstrumentTestBase;
import jomu.instrument.cognition.cell.Cell.CellTypes;

class ToneTimeFrameTest extends InstrumentTestBase {

	@Test
	void testAttributes() {
		ToneMap toneMap = initToneMap();
		ToneTimeFrame ttf = toneMap.getTimeFrame();
		assertEquals(ttf.avgAmplitude, ToneTimeFrame.AMPLITUDE_FLOOR, "ToneTimeFrame average amplitude incorrect");
	}

	@Test
	void testNormalise() {
		ToneMap toneMap = initToneMap();
		ToneTimeFrame ttf = toneMap.getTimeFrame();
		ttf.normalise(1.0);
		assertEquals(ttf.avgAmplitude, ToneTimeFrame.AMPLITUDE_FLOOR, "ToneTimeFrame average amplitude incorrect");
	}

	protected ToneMap initToneMap() {
		String streamId = "stream1";
		return initToneMap(CellTypes.AUDIO_CQ, streamId);
	}
}
