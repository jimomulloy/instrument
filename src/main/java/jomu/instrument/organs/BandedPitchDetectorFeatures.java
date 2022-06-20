package jomu.instrument.organs;

import java.util.Map;
import java.util.TreeMap;

public class BandedPitchDetectorFeatures {

	BandedPitchDetectorSource bpds;
	Map<Integer, TreeMap<Double, SpectrogramInfo>> features;

	void initialise(BandedPitchDetectorSource bpds) {
		this.bpds = bpds;
		this.features = bpds.getFeatures();
		bpds.clear();
	}

	public BandedPitchDetectorSource getBpds() {
		return bpds;
	}

	public Map<Integer, TreeMap<Double, SpectrogramInfo>> getFeatures() {
		return features;
	}

}
