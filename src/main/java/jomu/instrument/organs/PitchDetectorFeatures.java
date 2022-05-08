package jomu.instrument.organs;

import java.util.List;

import be.tarsos.dsp.pitch.PitchDetectionResult;

public class PitchDetectorFeatures {

	PitchDetectorSource pds;
	List<PitchDetectionResult> features;

	void initialise(PitchDetectorSource pds) {
		this.pds = pds;
		this.features = pds.getFeatures();
		pds.clear();
	}

	public PitchDetectorSource getPds() {
		return pds;
	}

	public List<PitchDetectionResult> getFeatures() {
		return features;
	}

}
