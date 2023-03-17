package jomu.instrument.workspace.tonemap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class MicroTones {

	ConcurrentSkipListMap<Double, Map<Integer, Double>> microToneMap = new ConcurrentSkipListMap<>();

	int subPitchSize = 1;

	@Override
	public MicroTones clone() {
		MicroTones copy = new MicroTones();
		for (Entry<Double, Map<Integer, Double>> mtm : microToneMap.entrySet()) {
			for (Entry<Integer, Double> mtpm : mtm.getValue().entrySet()) {
				copy.putMicroTone(mtm.getKey(), mtpm.getKey(), mtpm.getValue());
			}
		}
		return copy;
	}

	public void putMicroTone(double time, int subPitchIndex, double amplitude) {
		Map<Integer, Double> microTonePitchMap = null;
		if (microToneMap.containsKey(time)) {
			microTonePitchMap = microToneMap.get(time);
		} else {
			microTonePitchMap = new HashMap<>();
			microToneMap.put(time, microTonePitchMap);

		}
		microTonePitchMap.put(subPitchIndex, amplitude);
		if (subPitchSize < subPitchIndex) {
			subPitchSize = subPitchIndex + 1;
		}
	}

	public void putMicroTone(double time, double amplitude) {
		putMicroTone(time, 0, amplitude);
	}

	public Map<Double, Double> getMicroTones(int subPitchIndex) {
		Map<Double, Double> microToneAmplitudeMap = new ConcurrentHashMap<>();
		for (Entry<Double, Map<Integer, Double>> mtm : microToneMap.entrySet()) {
			if (mtm.getValue().containsKey(subPitchIndex)) {
				for (Double amplitude : mtm.getValue().values()) {
					microToneAmplitudeMap.put(mtm.getKey(), amplitude);
				}
			}
		}
		return microToneAmplitudeMap;
	}

	public Map<Double, Double> getMicroTones() {
		return getMicroTones(0);
	}

	public double getPower() {
		double power = 0;
		for (Entry<Double, Map<Integer, Double>> mtm : microToneMap.entrySet()) {
			for (Entry<Integer, Double> mtpm : mtm.getValue().entrySet()) {
				power += mtpm.getValue();
			}
		}
		return power;
	}

}
