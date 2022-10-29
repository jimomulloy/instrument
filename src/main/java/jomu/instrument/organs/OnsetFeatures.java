package jomu.instrument.organs;

import java.util.TreeMap;

public class OnsetFeatures {

	TreeMap<Double, OnsetInfo[]> features;
	OnsetSource os;

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
