package jomu.instrument.organs;

import java.util.TreeMap;

public class SpectrogramFeatures {

	TreeMap<Double, SpectrogramInfo> features;
	SpectrogramSource ss;

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
