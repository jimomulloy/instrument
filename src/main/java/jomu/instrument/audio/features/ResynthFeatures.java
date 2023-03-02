package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.TreeMap;

import jomu.instrument.workspace.tonemap.ToneMapConstants;

public class ResynthFeatures implements ToneMapConstants {

	private AudioFeatureFrame audioFeatureFrame;

	TreeMap<Double, ResynthInfo> features;
	ResynthSource rss;

	public TreeMap<Double, ResynthInfo> getFeatures() {
		return features;
	}

	public ResynthSource getRss() {
		return rss;
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		this.rss = audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getResynthSource();
		this.features = rss.getFeatures();
		TreeMap<Double, ResynthInfo> newFeatures = rss.getFeatures();
		for (Entry<Double, ResynthInfo> entry : newFeatures.entrySet()) {
			this.features.put(entry.getKey(), entry.getValue());
		}
		rss.clear();
	}
}
