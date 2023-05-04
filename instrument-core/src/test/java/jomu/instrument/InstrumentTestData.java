package jomu.instrument;

import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class InstrumentTestData {

	public static ToneMap buildToneMap(String key, ParameterManager pm) {

		ToneMap toneMap = new ToneMap(key, pm);

		double timeStart = 0;
		double timeEnd = 100;

		TimeSet timeSet = new TimeSet(timeStart, timeEnd, 44125, timeEnd - timeStart);
		PitchSet pitchSet = new PitchSet();
		ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
		toneMap.addTimeFrame(ttf);

		return toneMap;

	}

}
