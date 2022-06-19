package jomu.instrument.organs;

import java.util.TreeMap;

import jomu.instrument.tonemap.ToneMapConstants;

public class OnsetFeatures implements ToneMapConstants {

	OnsetSource os;
	TreeMap<Double, OnsetInfo[]> features;

	void initialise(OnsetSource os) {
		this.os = os;
		this.features = os.getFeatures();
		os.clear();
	}

	public OnsetSource getOs() {
		return os;
	}

	public TreeMap<Double, OnsetInfo[]> getFeatures() {
		return features;
	}
}
