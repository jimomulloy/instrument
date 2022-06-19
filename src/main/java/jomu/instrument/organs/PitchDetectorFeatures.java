package jomu.instrument.organs;

import java.util.TreeMap;

public class PitchDetectorFeatures {

	PitchDetectorSource pds;
	TreeMap<Double, SpectrogramInfo> features;

	void initialise(PitchDetectorSource pds) {
		this.pds = pds;
		this.features = pds.getFeatures();
		pds.clear();
	}

	public PitchDetectorSource getPds() {
		return pds;
	}

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		return features;
	}

}
