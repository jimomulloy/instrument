package jomu.instrument.organs;

import java.util.TreeMap;

public class ConstantQFeatures {

	ConstantQSource cqs;
	TreeMap<Double, float[]> features;

	void initialise(ConstantQSource cqs) {
		this.cqs = cqs;
		this.features = cqs.getFeatures();
		cqs.clear();
	}

}
