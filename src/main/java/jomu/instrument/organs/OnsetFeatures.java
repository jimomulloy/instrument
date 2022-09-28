package jomu.instrument.organs;

import java.util.TreeMap;

public class OnsetFeatures {

	OnsetSource os;
	TreeMap<Double, OnsetInfo[]> features;

	public TreeMap<Double, OnsetInfo[]> getFeatures() {
		return features;
	}

	public OnsetSource getOs() {
		return os;
	}

	void initialise(OnsetSource os) {
		this.os = os;
		this.features = os.getFeatures();
		os.clear();
	}
}
