package jomu.instrument.audio.analysis;

import java.util.Map;

/**
 * This is a robust polyphonic multi-pitch detector. The algorithm is described
 * by Anssi Klapuri in Multiple Fundamental Frequency Estimation by Summing
 * Harmonic Amplitudes. Yuan Gao, 2011
 */

public class PitchDetect {
	/** array to hold fzeros info, 1 := positive, 0 := negative */
	public float[] fzeros;
	public float[] fzeroSaliences;
	public Map<Integer, Float> fzeroBins;

	private double[] pitches;

	/** FFT object for Fast-Fourier Transform */
	// private FFT fft;

	/** sample rate of the samples in the buffer */
	private float sampleRate;

	/** spectrum to be analyzed */
	// private float[] spec;

	/** spectrum "whitener" for pre-processing */
	public SpecWhitener sw;

	/** size of the buffer */
	private int timeSize;

	public PitchDetect(int timeSize, float sampleRate, double[] pitches) {
		this.timeSize = timeSize;
		this.sampleRate = sampleRate;
		this.pitches = pitches;
		sw = new SpecWhitener(timeSize, sampleRate);
		fzeros = new float[pitches.length];
		fzeroSaliences = new float[pitches.length];
	}

	public void whiten(float[] spec) {
		sw.whiten(spec);
	}

	/**
	 * This method takes an AudioBuffer object as argument. It detects all notes in
	 * presence in buffer.
	 */
	public void detect(float[] spectrum) {
		float[] spec = new float[spectrum.length];
		for (int i = 0; i < spectrum.length; i++) {
			spec[i] = spectrum[i];
		}
		// for (int i = 0; i < spec.length; i++)
		// spec[i] *= 1000;
		// sw.whiten(spec);

		// iteratively find all presented pitches
		float test = 0, lasttest = 0;
		int loopcount = 1;
		float[] fzeroInfo = new float[3]; // ind 0 is the pitch, ind 1 its salience, ind 2 its ind in PITCHES
		while (true) {
			detectfzero(spec, fzeroInfo);
			lasttest = test;
			test = (test + fzeroInfo[1]) / (float) Math.pow(loopcount, .7f);
			if (test <= lasttest)
				break;
			loopcount++;

			// subtract the information of the found pitch from the current spectrum
			for (int i = 1; (i * fzeroInfo[0] < sampleRate / 2) && i < 6; ++i) {
				int partialInd = (int) Math.floor((double) (i * fzeroInfo[0] * timeSize / sampleRate));
				if (i == 1) {
					spec[partialInd] = 0;
				} else {
					float weighting = ((float) fzeroInfo[0] + 52.0F) / ((float) i * (float) fzeroInfo[0] + 320.0F);
					if (partialInd < spec.length) {
						spec[partialInd] *= (1f - 0.89f * weighting);
					}
					// if (partialInd > 0) {
					// spec[partialInd - 1] *= (1 - 0.89f * weighting);
					// }
				}
			}

			// update fzeros
			if (fzeros[(int) fzeroInfo[2]] == 0) {
				fzeros[(int) fzeroInfo[2]] = fzeroInfo[0];
				fzeroSaliences[(int) fzeroInfo[2]] = fzeroInfo[1];
			}
		}
//		for (int i = 0; i < spec.length; i++) {
//			if (fzeroBins.containsKey(i)) {
//				spec[i] = fzeroBins.get(i);
//			} else {
//				spec[i] = 0;
//			}
//		}

	}

	// utility function for detecting a single pitch
	private void detectfzero(float[] spec, float[] fzeroInfo) {
		{
			float maxSalience = 0;
			for (int j = 0; j < pitches.length; ++j) {
				float cSalience = 0; // salience of the candidate pitch
				float val = 0;
				if (pitches[j] == 0)
					break;
				for (int i = 1; (i * pitches[j] < sampleRate / 2) && i < 6; ++i) {
					int bin = (int) ((double) i * pitches[j] * (double) timeSize / (double) sampleRate);
					val = 0;
					if (bin >= spec.length)
						break;
					val = spec[bin];
					// if (bin < timeSize / 2 - 1) {
					// val = spec[bin + 1];
					// }
					// if (bin < timeSize / 2 && val < spec[bin]) {
					// val = spec[bin];
					// }
					// if (bin > 0 && val < spec[bin - 1]) {
					// val = spec[bin - 1];
					// }
					// if (bin > 1 && val < spec[bin - 2]) {
					// val = spec[bin - 2];
					// }
					// if (bin > 2 && val < spec[bin - 3]) {
					// val = spec[bin - 3];
					// }

					// calculate the salience of the current candidate
					if (i > 1) {
						float weighting = (float) (((float) pitches[j] + 52.0F)
								/ ((float) i * (float) pitches[j] + 320.0F));
						cSalience += val * weighting;
					} else {
						cSalience = val;
					}
				}
				if (cSalience > maxSalience) {
					maxSalience = cSalience;
					fzeroInfo[0] = (float) pitches[j];
					fzeroInfo[1] = cSalience;
					fzeroInfo[2] = j;
				}
			}
		}
	}

	private float max(float f, float g, float h) {
		if (f > g && f > h) {
			return f;
		} else if (g > h) {
			return g;
		} else {
			return h;
		}
	}
}

final class SpecWhitener {
	private int[][] banksRanTable; // each row is a filter; cols are lower band
									// index and upper band index this
	// filter covers
	private int bufferSize; // the size of the Fourier Transform
	private float[] cenFreqs; // center frequencies of bandpass filter banks
	private float[] cenFreqsSteps; // steps of increment
	private float sr; // the sample rate of the samples in the buffer

	// public float[] wSpec; // the whitened specturm

	public SpecWhitener(int bufferSize, float sr) {
		this.bufferSize = bufferSize;
		this.sr = sr;

		// calculate center frequencies
		cenFreqs = new float[32];
		for (int i = 0; i < cenFreqs.length; ++i)
			cenFreqs[i] = (float) (229 * (Math.pow(10, (i + 1) / 21.4f) - 1));
		cenFreqsSteps = new float[32];
		for (int i = 1; i < cenFreqsSteps.length; ++i)
			cenFreqsSteps[i] = cenFreqs[i] - cenFreqs[i - 1];

		// calculate the filter banks range table
		banksRanTable = new int[32][2];
		float bandIndLow = 0, bandIndMid = 0, bandIndUp = 0;
		for (int i = 1; i < cenFreqs.length - 1; ++i) {
			if (i == 1) {
				bandIndLow = (cenFreqs[i - 1] * bufferSize) / sr;
				bandIndMid = (cenFreqs[i] * bufferSize) / sr;
				bandIndUp = (cenFreqs[i + 1] * bufferSize) / sr;
			} else {
				bandIndLow = bandIndMid;
				bandIndMid = bandIndUp;
				bandIndUp = (cenFreqs[i + 1] * bufferSize) / sr;
			}
			banksRanTable[i][0] = (int) Math.ceil(bandIndLow);
			banksRanTable[i][1] = (int) Math.floor(bandIndUp);
		}

		// wSpec = new float[bufferSize / 2 + 1];
	}

	public void whiten(float[] spec) {
		// calculate bandwise compression coefficients
		float[] bwCompCoef = new float[32];
		for (int j = 1; j < bwCompCoef.length - 1; ++j) {
			float sum = 0;
			for (int i = banksRanTable[j][0]; i <= banksRanTable[j][1]; ++i) {
				float bandFreq = i * sr / bufferSize;
				if (bandFreq < cenFreqs[j]) {
					sum += Math.pow(spec[i], 2) * (bandFreq - cenFreqs[j - 1]) / cenFreqsSteps[j];
				} else {//
					sum += Math.pow(spec[i], 2) * (cenFreqs[j + 1] - bandFreq) / cenFreqsSteps[j + 1];
				}
			}
			if (sum > 0) {
				bwCompCoef[j] = (float) Math.pow(Math.pow(sum / bufferSize, .5f), .33f - 1);
			}
		}

		// calculate steps of increment of bwCompCoef
		float[] bwCompCoefSteps = new float[32];
		for (int i = 1; i < bwCompCoef.length; ++i) {
			bwCompCoefSteps[i] = bwCompCoef[i] - bwCompCoef[i - 1];
		}

		// whitens
		float compCoef = 0;
		int bankCount = 1;
		for (int i = banksRanTable[1][0]; i <= banksRanTable[30][1]; ++i) {
			float bandFreq = i * sr / bufferSize;
			if (bandFreq > cenFreqs[bankCount])
				bankCount++;
			if (bwCompCoefSteps[bankCount] > 0) {
				compCoef = (bwCompCoefSteps[bankCount] * (bandFreq - cenFreqs[bankCount - 1])
						/ cenFreqsSteps[bankCount]) + bwCompCoef[bankCount - 1];
			} else {
				compCoef = (-bwCompCoefSteps[bankCount] * (cenFreqs[bankCount] - bandFreq) / cenFreqsSteps[bankCount])
						+ bwCompCoef[bankCount];
			}
			// wSpec[i] = spec[i] * compCoef;
			spec[i] = spec[i] * compCoef;
		}
	}
}