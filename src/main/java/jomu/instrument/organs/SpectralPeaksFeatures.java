package jomu.instrument.organs;

import java.util.List;

public class SpectralPeaksFeatures {

	SpectralPeaksSource sps;
	List<SpectralInfo> spectralInfo;

	void initialise(SpectralPeaksSource sps) {
		this.sps = sps;
		spectralInfo = sps.getSpectralInfo();
		sps.clear();
	}

	public SpectralPeaksSource getSps() {
		return sps;
	}

	public List<SpectralInfo> getSpectralInfo() {
		return spectralInfo;
	}
}
