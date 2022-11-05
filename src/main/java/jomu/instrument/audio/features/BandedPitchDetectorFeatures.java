package jomu.instrument.audio.features;

import java.util.Map;
import java.util.TreeMap;

public class BandedPitchDetectorFeatures {

	BandedPitchDetectorSource bpds;
	Map<Integer, TreeMap<Double, SpectrogramInfo>> features;

	public BandedPitchDetectorSource getBpds() {
		return bpds;
	}

	public Map<Integer, TreeMap<Double, SpectrogramInfo>> getFeatures() {
		return features;
	}

	void initialise(BandedPitchDetectorSource bpds) {
		this.bpds = bpds;
		this.features = bpds.getFeatures();
		bpds.clear();
	}

}