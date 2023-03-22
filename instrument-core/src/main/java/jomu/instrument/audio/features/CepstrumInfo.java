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

import java.util.List;

public class CepstrumInfo {
	List<Integer> peaks;
	double[] correlations;
	double[] magnitudes;
	int length;
	private int minPeakIndex;
	private int maxACFIndex;

	public CepstrumInfo(List<Integer> peaks, double[] correlations, int maxACFIndex, int minPeakIndex, int length,
			double[] magnitudes) {
		super();
		this.peaks = peaks;
		this.correlations = correlations;
		this.length = length;
		this.maxACFIndex = maxACFIndex;
		this.minPeakIndex = minPeakIndex;
		this.magnitudes = magnitudes;
	}

	public List<Integer> getPeaks() {
		return peaks;
	}

	public double[] getCorrelations() {
		return correlations;
	}

	public int getLength() {
		return length;
	}

	public double[] getMagnitudes() {
		return magnitudes;
	}

	public int getMinPeakIndex() {
		return minPeakIndex;
	}

	public int getMaxACFIndex() {
		return maxACFIndex;
	}

	public CepstrumInfo clone() {
		return new CepstrumInfo(peaks, correlations, maxACFIndex, minPeakIndex, length, magnitudes);
	}
}
