/*
*      _______                       _____   _____ _____
*     |__   __|                     |  __ \ / ____|  __ \
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
*
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
*
*/

package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.List;

import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;

public class PeakInfo {
	private float[] frequencyEstimates;
	private float[] magnitudes;

	public PeakInfo(float[] magnitudes, float[] frequencyEstimates) {
		this.magnitudes = magnitudes;
		this.frequencyEstimates = frequencyEstimates;
	}

	@Override
	public PeakInfo clone() {
		PeakInfo sic = new PeakInfo(magnitudes.clone(), frequencyEstimates.clone());
		return sic;
	}

	public float[] getMagnitudes() {
		return magnitudes;
	}

	public float[] getNoiseFloor(int medianFilterLength, float noiseFloorFactor) {
		return SpectralPeakDetector.calculateNoiseFloor(magnitudes, medianFilterLength, noiseFloorFactor);
	}

	public List<SpectralPeak> getPeakList(int medianFilterLength, float noiseFloorFactor, int numberOfPeaks,
			int minPeakDistanceInCents) {
		float[] noiseFloor = getNoiseFloor(medianFilterLength, noiseFloorFactor);
		List<Integer> localMaxima = SpectralPeakDetector.findLocalMaxima(magnitudes, noiseFloor);
		if (localMaxima.size() > 0) {
			return SpectralPeakDetector.findPeaks(magnitudes, frequencyEstimates, localMaxima, numberOfPeaks,
					minPeakDistanceInCents);
		} else {
			return new ArrayList<>();
		}
	}
}
