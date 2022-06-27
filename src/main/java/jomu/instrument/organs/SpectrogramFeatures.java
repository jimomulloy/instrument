package jomu.instrument.organs;

import java.util.TreeMap;

import jomu.instrument.tonemap.ToneMap;

public class SpectrogramFeatures {

	SpectrogramSource ss;
	TreeMap<Double, SpectrogramInfo> features;
	private ToneMap toneMap;

	void initialise(SpectrogramSource ss) {
		this.ss = ss;
		this.features = ss.getFeatures();
		ss.clear();
	}

	public SpectrogramSource getSs() {
		return ss;
	}

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		return features;
	}

}
