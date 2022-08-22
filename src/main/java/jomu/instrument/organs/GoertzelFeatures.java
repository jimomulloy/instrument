package jomu.instrument.organs;

import java.util.TreeMap;

public class GoertzelFeatures {

	GoertzelSource gs;
	TreeMap<Double, GoertzelInfo> features;

	public TreeMap<Double, GoertzelInfo> getFeatures() {
		return features;
	}

	public GoertzelSource getGs() {
		return gs;
	}

	void initialise(GoertzelSource gs) {
		this.gs = gs;
		this.features = gs.getFeatures();
		gs.clear();
	}

}
