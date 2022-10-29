package jomu.instrument.audio.features;

import java.util.List;
import java.util.TreeMap;

public class SpectralPeaksFeatures {

	private TreeMap<Double, SpectralInfo> features;
	List<SpectralInfo> spectralInfo;
	SpectralPeaksSource sps;

	public TreeMap<Double, SpectralInfo> getFeatures() {
		return features;
	}

	public List<SpectralInfo> getSpectralInfo() {
		return spectralInfo;
	}

	public SpectralPeaksSource getSps() {
		return sps;
	}

	void initialise(SpectralPeaksSource sps) {
		this.sps = sps;
		spectralInfo = sps.getSpectralInfo();
		features = sps.getFeatures();
		sps.clear();
	}
}
