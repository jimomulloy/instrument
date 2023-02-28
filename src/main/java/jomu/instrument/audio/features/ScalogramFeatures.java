package jomu.instrument.audio.features;

import java.util.TreeMap;

public class ScalogramFeatures {

	TreeMap<Double, ScalogramFrame> features;
	ScalogramSource scs;

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
