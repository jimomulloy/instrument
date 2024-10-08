package jomu.instrument.workspace.tonemap;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import be.tarsos.dsp.onsets.OnsetHandler;

public class CalibrationMap implements OnsetHandler {

	private static final Logger LOG = Logger.getLogger(CalibrationMap.class.getName());

	ConcurrentSkipListMap<Double, Double> audioFilePowerMap = new ConcurrentSkipListMap<>();
	ConcurrentSkipListMap<Double, Double> beatMap = new ConcurrentSkipListMap<>();
	String key;

	private static double DEFAULT_MEAN_POWER = 1.0;

	public CalibrationMap(String key) {
		this.key = key;
	}

	public void put(double time, double power) {
		audioFilePowerMap.put(time, power);
	}

	public double get(double time) {
		Entry<Double, Double> entry = audioFilePowerMap.floorEntry(time);
		if (entry != null) {
			return entry.getValue();
		} else {
			return 0;
		}
	}

	public int getNumberOfBeats() {
		return beatMap.size();
	}

	public double getBeat(double time, double range) {
		double result = getBeatAfterTime(time, range);
		if (result == -1) {
			result = getBeatBeforeTime(time, range);
		}
		return result;
	}

	public double getStartTime() {
		try {
			return audioFilePowerMap.firstKey();
		} catch (Exception e) {
			return -1;
		}
	}

	public double getEndTime() {
		try {
			return audioFilePowerMap.lastKey();
		} catch (Exception e) {
			return -1;
		}
	}

	public double getMeanPower(double fromTime, double toTime) {
		double total = 0;
		int count = 0;
		ConcurrentNavigableMap<Double, Double> subMap = audioFilePowerMap.subMap(fromTime, toTime);
		if (subMap.isEmpty()) {
			total = get(fromTime);
			count = 1;
		} else {
			for (double power : subMap.values()) {
				total += power;
				count++;
			}
		}
		return count > 0 ? total / count : 0;
	}

	public double getMaxPower(double fromTime, double toTime) {
		double max = 0;
		ConcurrentNavigableMap<Double, Double> subMap = audioFilePowerMap.subMap(fromTime, toTime);
		if (subMap.isEmpty()) {
			max = get(fromTime);
		} else {
			for (double power : subMap.values()) {
				if (max < power) {
					max = power;
				}
			}
		}
		return max;
	}

	public double getMaxPowerTime(double fromTime, double toTime) {
		double max = 0;
		double maxTime = 0;
		ConcurrentNavigableMap<Double, Double> subMap = audioFilePowerMap.subMap(fromTime, toTime);
		if (subMap.isEmpty()) {
			maxTime = fromTime;
		} else {
			for (Entry<Double, Double> entry : subMap.entrySet()) {
				if (max < entry.getValue()) {
					max = entry.getValue();
					maxTime = entry.getKey();
				}
			}
		}
		return maxTime;
	}

	public double getPower(double time) {
		Double power = audioFilePowerMap.get(time);
		if (power != null) {
			return power.doubleValue();
		} else {
			Entry<Double, Double> floorEntry = audioFilePowerMap.floorEntry(time);
			if (floorEntry != null) {
				return floorEntry.getValue();
			}
			return 0;
		}
	}

	@Override
	public void handleOnset(double time, double salience) {
		beatMap.put(time, salience);
	}

	public double getBeatBeforeTime(double time, double range) {
		Entry<Double, Double> le = beatMap.floorEntry(time);
		if (le != null && (range / 1000.0) >= (time - le.getKey())) {
			return le.getKey();
		}
		return -1;
	}

	public double getBeatRange(double time) {
		Entry<Double, Double> startEntry = beatMap.ceilingEntry(time);
		if (startEntry != null) {
			Entry<Double, Double> endEntry = beatMap.ceilingEntry(startEntry.getKey() + 0.1);
			if (endEntry != null) {
				return endEntry.getKey() - startEntry.getKey();
			} else {
				endEntry = startEntry;
				startEntry = beatMap.floorEntry(time);
				if (startEntry != null) {
					return endEntry.getKey() - startEntry.getKey();
				}
			}
		}
		return -1;
	}

	public double getBeatAfterTime(double time, double range) {
		Entry<Double, Double> he = beatMap.ceilingEntry(time);
		if (he != null && (range / 1000.0) >= (he.getKey() - time)) {
			return he.getKey();
		} else {
			return -1;
		}
	}

	public void calibrateMetronome(double metronomeStart, double metronomeDistance) {
		double lastTime = audioFilePowerMap.lastEntry().getKey();
		LOG.severe(">>CM metro: " + lastTime);
		double time = metronomeStart;
		while (time <= lastTime) {
			beatMap.put(time, -1.0);
			time += metronomeDistance;
		}
	}

}
