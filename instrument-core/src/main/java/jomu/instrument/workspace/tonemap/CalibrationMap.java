package jomu.instrument.workspace.tonemap;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class CalibrationMap {

	ConcurrentSkipListMap<Double, Double> audioFilePowerMap = new ConcurrentSkipListMap<>();
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

}
