package jomu.instrument.organs;

import java.util.TreeMap;

public class SpectrogramFeatures {

	SpectrogramSource ss;
	TreeMap<Double, SpectrogramInfo> features;

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
