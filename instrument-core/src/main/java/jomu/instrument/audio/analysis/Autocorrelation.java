package jomu.instrument.audio.analysis;

import java.util.ArrayList;
import java.util.List;
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

	private FastFourierTransformer fftTran = new FastFourierTransformer(DftNormalization.STANDARD);
	private double ACF_THRESH = 0.2; // Minimum correlation threshold
	private int maxLag; // Maximum length of autocorrelation to calculate
	private Complex[] fft;

	public Autocorrelation(int maxLag) {
		this.maxLag = maxLag;
	}

	public void setMaxLag(int lag) {
		maxLag = lag;
	}

	public void setCorrelationThreshold(double thresh) {
		ACF_THRESH = thresh;
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
			fft[i] = fft[i].multiply(fft[i].conjugate());
		}
		// Inverse transform
		fft = fftTran.transform(fft, TransformType.INVERSE);

		correlations = new double[maxLag];
		for (int i = 1; i < maxLag; i++) {
			correlations[i] = fft[i].getReal() / fft[0].getReal();
		}

		sacfCorrelations();
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
						}
					}
					positive = !positive;
				}
			}
		}
		return peaks;
	}
}