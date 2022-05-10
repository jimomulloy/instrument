package jomu.instrument.organs;

import java.util.List;
import java.util.TreeMap;

public class SpectralPeaksFeatures {

	SpectralPeaksSource sps;
	List<SpectralInfo> spectralInfo;
	private TreeMap<Double, SpectralInfo> features;

	void initialise(SpectralPeaksSource sps) {
		this.sps = sps;
		spectralInfo = sps.getSpectralInfo();
		features = sps.getFeatures();
		sps.clear();
	}

	public SpectralPeaksSource getSps() {
		return sps;
	}

	public List<SpectralInfo> getSpectralInfo() {
		return spectralInfo;
	}

	public TreeMap<Double, SpectralInfo> getFeatures() {
		return features;
	}
}
