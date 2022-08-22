package jomu.instrument.organs;

import java.util.TreeMap;

public class ScalogramFeatures {

	ScalogramSource scs;
	TreeMap<Double, ScalogramFrame> features;

	public TreeMap<Double, ScalogramFrame> getFeatures() {
		return features;
	}

	public ScalogramSource getScs() {
		return scs;
	}

	void initialise(ScalogramSource scs) {
		this.scs = scs;
		this.features = scs.getFeatures();
		scs.clear();
	}

}
