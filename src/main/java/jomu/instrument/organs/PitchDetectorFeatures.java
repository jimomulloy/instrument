package jomu.instrument.organs;

import java.util.TreeMap;

import be.tarsos.dsp.pitch.PitchDetectionResult;

public class PitchDetectorFeatures {

	PitchDetectorSource pds;
	TreeMap<Double, PitchDetectionResult> features;

	void initialise(PitchDetectorSource pds) {
		this.pds = pds;
		this.features = pds.getFeatures();
		pds.clear();
	}

	public PitchDetectorSource getPds() {
		return pds;
	}

	public TreeMap<Double, PitchDetectionResult> getFeatures() {
		return features;
	}

}
