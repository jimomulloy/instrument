package jomu.instrument.organs;

import java.util.TreeMap;

import be.tarsos.dsp.AudioEvent;
import jomu.instrument.audio.TarsosAudioIO;

public class AudioEventSource {

	private TreeMap<Double, AudioEvent> features = new TreeMap<>();
	private TarsosAudioIO tarsosIO;

	public AudioEventSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
	}

	public TreeMap<Double, AudioEvent> getFeatures() {
		features = tarsosIO.getFeatures();
		TreeMap<Double, AudioEvent> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, AudioEvent> entry : features
				.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue());
		}
		return clonedFeatures;
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	void clear() {
		tarsosIO.clearFeatures();
	}
}
