package jomu.instrument;

import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.FrameCache;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class InstrumentTestData {

	public static ToneMap buildToneMap(String key, ParameterManager pm, FrameCache fc) {

		ToneMap toneMap = new ToneMap(key, pm, fc);

		double timeStart = 0;
		double timeEnd = 100;

		TimeSet timeSet = new TimeSet(timeStart, timeEnd, 44125, timeEnd - timeStart);
		PitchSet pitchSet = new PitchSet();
		ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
		toneMap.addTimeFrame(ttf);

		return toneMap;

	}

}
