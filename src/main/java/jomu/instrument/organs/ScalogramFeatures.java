package jomu.instrument.organs;

import java.util.TreeMap;

public class ScalogramFeatures {

	ScalogramSource scs;
	TreeMap<Double, ScalogramFrame> features;

	void initialise(ScalogramSource scs) {
		this.scs = scs;
		this.features = scs.getFeatures();
		scs.clear();
	}

	public ScalogramSource getScs() {
		return scs;
	}

	public TreeMap<Double, ScalogramFrame> getFeatures() {
		return features;
	}

}
