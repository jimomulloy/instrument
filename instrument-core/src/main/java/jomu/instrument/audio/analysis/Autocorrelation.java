package jomu.instrument.audio.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/* Autocorrelation via FFT
 *    F_R(f) = FFT(X)
 *    S(f) = F_R(f)F_R*(f)
 *    R(t) = IFFT(S(f))
 * */
public class Autocorrelation {

	private static final Logger LOG = Logger.getLogger(Autocorrelation.class.getName());

	public double[] correlations; // Autocorrelation
	public double maxACF = 0; // Max autocorrelation peak
	public int length = 0;
	public int maxACFIndex = -1;
	public int minPeakIndex = Integer.MAX_VALUE;

	private FastFourierTransformer fftTran = new FastFourierTransformer(DftNormalization.STANDARD);
	private double ACF_THRESH = 0.2; // Minimum correlation threshold
	private int maxLag; // Maximum length of autocorrelation to calculate
	private Complex[] fft;

	private int undertoneRange = 4;

	private int undertoneThreshold = 10;

	private boolean isRemoveUndertones = true;

	private boolean isSacf = false;

	private boolean isCepstrum;

	public Autocorrelation(int maxLag) {
		this.maxLag = maxLag;
	}

	public void setUndertoneRange(int undertoneRange) {
		this.undertoneRange = undertoneRange;
	}

	public void setIsRemoveUndertones(boolean isRemoveUndertones) {
		this.isRemoveUndertones = isRemoveUndertones;
	}

	public void setIsSacf(boolean isSacf) {
		this.isSacf = isSacf;
	}

	public void setUndertoneThreshold(int undertoneThreshold) {
		this.undertoneThreshold = undertoneThreshold;
	}

	public void setMaxLag(int lag) {
		maxLag = lag;
	}

	public void setCorrelationThreshold(double thresh) {
		ACF_THRESH = thresh;
	}

	public double[] getMagnitudes() {
		double[] magnitudes = new double[fft.length];
		for (int i = 0; i < fft.length; i++) {
			magnitudes[i] = fft[i].getReal() * fft[i].getReal() + fft[i].getImaginary() * fft[i].getImaginary();
		}
		return magnitudes;
	}

	private double mean(double[] metrics) {
		int n = metrics.length;
		double m = 0;
		for (int i = 0; i < n; i++) {
			m += metrics[i];
		}
		return m / n;
	}

	private double[] formatData(double[] data) {
		int n = data.length;
		double m = mean(data);
		// Pad with 0
		Double padding = Math.pow(2, 32 - Integer.numberOfLeadingZeros(2 * n - 1));
		double[] values = new double[padding.intValue()];
		// zero mean data
		for (int i = 0; i < n; i++) {
			values[i] = data[i] - m;
		}
		return values;
	}

	/* Calculate autocorrelation for the given list of Datum */
	public void evaluate(double[] data) {
		double[] values = formatData(data);
		length = values.length;
		// FFT
		fft = fftTran.transform(values, TransformType.FORWARD);
		// Multiply by complex conjugate
		for (int i = 0; i < fft.length; i++) {
			if (isCepstrum) {
				fft[i] = new Complex(Math.log10(1 + (1000 * (fft[i].abs()))));
			} else {
				fft[i] = fft[i].multiply(fft[i].conjugate());
			}
		}
		// Inverse transform
		fft = fftTran.transform(fft, TransformType.INVERSE);

		correlations = new double[maxLag];
		for (int i = 1; i < maxLag && i < fft.length; i++) {
			correlations[i] = fft[i].getReal() / fft[0].getReal();
		}

		if (isSacf) {
			sacfCorrelations();
		}
	}

	private void sacfCorrelations() {
		double[] clippedCorrelations = new double[correlations.length];
		double[] enhancedCorrelations = new double[correlations.length * 2];
		double[] enhanced2Correlations = new double[correlations.length * 4];

		for (int i = 0; i < correlations.length; i++) {
			if (correlations[i] < 0) {
				clippedCorrelations[i] = 0;
			} else {
				clippedCorrelations[i] = correlations[i];
			}
		}

		for (int i = 0; i < clippedCorrelations.length; i++) {
			enhancedCorrelations[i * 2] = clippedCorrelations[i];
			if (i > 0) {
				enhancedCorrelations[(i * 2) - 1] = clippedCorrelations[i - 1]
						+ (clippedCorrelations[i] - clippedCorrelations[i - 1]) / 2;
			}
			// clippedCorrelations[i] -= enhancedCorrelations[i];
			// if (clippedCorrelations[i] < 0) {
			// clippedCorrelations[i] = 0;
			// }
		}

		for (int i = 0; i < clippedCorrelations.length; i++) {
			enhanced2Correlations[i * 4] = clippedCorrelations[i];
			if (i > 0) {
				enhanced2Correlations[(i * 4) - 1] = clippedCorrelations[i - 1]
						+ 3 * (clippedCorrelations[i] - clippedCorrelations[i - 1]) / 4;
				enhanced2Correlations[(i * 4) - 2] = clippedCorrelations[i - 1]
						+ 2 * (clippedCorrelations[i] - clippedCorrelations[i - 1]) / 4;
				enhanced2Correlations[(i * 4) - 3] = clippedCorrelations[i - 1]
						+ (clippedCorrelations[i] - clippedCorrelations[i - 1]) / 4;
			}
			// clippedCorrelations[i] -= enhanced2Correlations[i];
			// if (clippedCorrelations[i] < 0) {
			// clippedCorrelations[i] = 0;
			// }
		}

		for (int i = 0; i < correlations.length; i++) {
			correlations[i] = correlations[i] - enhancedCorrelations[i] - enhanced2Correlations[i];
			if (correlations[i] < 0) {
				correlations[i] = 0;
			}
		}
	}

	public int getLength() {
		return fft.length;
	}

	/* Find autocorrelation peaks */
	public List<Integer> findPeaks() {
		List<Integer> peaks = new ArrayList<>();
		int max = 1;
		maxACF = 0;
		if (correlations.length > 1) {
			boolean positive = (correlations[1] > correlations[0]);
			for (int i = 2; i < correlations.length; i++) {
				if (!positive && correlations[i] > correlations[i - 1]) {
					max = i;
					positive = !positive;
				} else if (positive && correlations[i] > correlations[max]) {
					max = i;
				} else if (positive && correlations[i] < correlations[i - 1]) {
					if (max > 1 && correlations[max] > ACF_THRESH) {
						peaks.add(max);
						if (correlations[max] > maxACF) {
							maxACF = correlations[max];
							maxACFIndex = max;
						}
						if (max < minPeakIndex) {
							minPeakIndex = max;
						}
					}
					positive = !positive;
				}
			}

			if (isRemoveUndertones) {
				HashSet<Integer> peaksProcessed = new HashSet<>();
				HashSet<Integer> peaksToRemove = new HashSet<>();
				removeUndertonePeaks(peaks, peaksToRemove, peaksProcessed);
				peaks.removeAll(peaksToRemove);
			}

		}
		return peaks;
	}

	private void removeUndertonePeaks(List<Integer> peaks, Set<Integer> peaksToRemove, Set<Integer> peaksProcessed) {
		int maxIndex = -1;
		double maxValue = 0;
		for (int peakIndex : peaks) {
			if (!peaksProcessed.contains(peakIndex)) {
				if (maxValue < correlations[peakIndex]) {
					maxIndex = peakIndex;
					maxValue = correlations[peakIndex];
				}
			}
		}
		if (maxIndex > -1 && peaksProcessed.size() < undertoneRange) {
			peaksProcessed.add(maxIndex);
			for (int peakIndex : peaks) {
				if (!peaksProcessed.contains(peakIndex) && peakIndex > maxIndex
						&& Math.abs(peakIndex % maxIndex) < undertoneThreshold) {
					peaksToRemove.add(peakIndex);
				}
			}
			removeUndertonePeaks(peaks, peaksToRemove, peaksProcessed);
		}
	}

	@Override
	public String toString() {
		return "Autocorrelation [maxACF=" + maxACF + ", length=" + length + ", ACF_THRESH=" + ACF_THRESH + ", maxLag="
				+ maxLag + ", undertoneRange=" + undertoneRange + ", undertoneThreshold=" + undertoneThreshold
				+ ", isRemoveUndertones=" + isRemoveUndertones + ", isSacf=" + isSacf + "]";
	}

	public void setIsCepstrum(boolean isCepstrum) {
		this.isCepstrum = isCepstrum;

	}

}