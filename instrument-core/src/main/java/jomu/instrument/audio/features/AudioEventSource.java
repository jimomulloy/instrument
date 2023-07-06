package jomu.instrument.audio.features;

import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AudioEventSource<T> {

	final ReentrantLock lock = new ReentrantLock();

	final TreeMap<Double, T> features = new TreeMap<>();

	final public TreeMap<Double, T> getFeatures() {
		TreeMap<Double, T> clonedFeatures = null;
		lock.lock();
		try {
			clonedFeatures = new TreeMap<>();
			for (java.util.Map.Entry<Double, T> entry : features.entrySet()) {
				clonedFeatures.put(entry.getKey(), cloneFeatures(entry.getValue()));
			}
		} finally {
			lock.unlock();
		}

		return clonedFeatures;
	}

	final public TreeMap<Double, T> getAndClearFeatures() {
		TreeMap<Double, T> clonedFeatures = null;
		lock.lock();
		try {
			clonedFeatures = new TreeMap<>();
			for (java.util.Map.Entry<Double, T> entry : features.entrySet()) {
				clonedFeatures.put(entry.getKey(), cloneFeatures(entry.getValue()));
			}
			features.clear();
		} finally {
			lock.unlock();
		}

		return clonedFeatures;
	}

	abstract T cloneFeatures(T features);

	final void putFeature(double key, T feature) {
		lock.lock();
		try {
			features.put(key, feature);
		} finally {
			lock.unlock();
		}
	}

	void removeFeatures(double endTime) {
		lock.lock();
		try {
			features.keySet()
					.removeIf(key -> key <= endTime);
		} finally {
			lock.unlock();
		}
	}

	final void clear() {
		lock.lock();
		try {
			features.clear();
		} finally {
			lock.unlock();
		}
	}
}
