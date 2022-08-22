package jomu.instrument.organs;

import java.util.TreeMap;

import jomu.instrument.tonemap.ToneMap;

public class SpectrogramFeatures {

	SpectrogramSource ss;
	TreeMap<Double, SpectrogramInfo> features;
	private ToneMap toneMap;

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		return features;
	}

	public SpectrogramSource getSs() {
		return ss;
	}

	void initialise(SpectrogramSource ss) {
		this.ss = ss;
		this.features = ss.getFeatures();
		ss.clear();
	}

}
