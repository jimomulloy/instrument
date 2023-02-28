package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.TreeMap;

import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;

public class ResynthFeatures implements ToneMapConstants {

	public boolean logSwitch = true;
	public int powerHigh = 100;
	public int powerLow = 0;
	private AudioFeatureFrame audioFeatureFrame;
	private PitchSet pitchSet;
	private TimeSet timeSet;
	private ToneMap toneMap;

	TreeMap<Double, float[]> features;
	ResynthSource rss;

	public TreeMap<Double, float[]> getFeatures() {
		return features;
	}

	public ResynthSource getRss() {
		return rss;
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		this.rss = audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getResynthSource();
		this.features = rss.getFeatures();
		TreeMap<Double, float[]> newFeatures = this.rss.getFeatures();
		for (Entry<Double, float[]> entry : newFeatures.entrySet()) {
			this.features.put(entry.getKey(), entry.getValue());
		}
		rss.clear();
	}
}
