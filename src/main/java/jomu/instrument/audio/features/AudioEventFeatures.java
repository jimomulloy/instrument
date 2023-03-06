package jomu.instrument.audio.features;

import java.util.TreeMap;

public abstract class AudioEventFeatures<T> {
	AudioEventSource<T> source;
	TreeMap<Double, T> features = new TreeMap<>();

	abstract public AudioEventSource<T> getSource();

	final public TreeMap<Double, T> getFeatures() {
		return features;
	}
	
	final public boolean hasFeatures() {
		return features != null && !features.isEmpty();
	}

	final public void initialise(AudioEventSource<T> source) {
		this.source = source;
	}

}
