package jomu.instrument.organs;

import java.util.TreeMap;

import be.tarsos.dsp.AudioEvent;

public class AudioEventFeatures {
	AudioEventSource aes;
	TreeMap<Double, AudioEvent> features;

	void initialise(AudioEventSource aes) {
		this.aes = aes;
		this.features = aes.getFeatures();
		aes.clear();
	}

	public AudioEventSource getAes() {
		return aes;
	}

	public TreeMap<Double, AudioEvent> getFeatures() {
		return features;
	}

}
