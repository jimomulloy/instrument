package jomu.instrument.workspace.tonemap;

/**
 * This is a robust polyphonic multi-pitch detector. The algorithm is described
 * by Anssi Klapuri in Multiple Fundamental Frequency Estimation by Summing
 * Harmonic Amplitudes. Yuan Gao, 2011
 */

public class PitchDetect {
	/** array to hold fzeros info, 1 := positive, 0 := negative */
	public float[] fzeros;

	private float[] pitches;

	/** FFT object for Fast-Fourier Transform */
	// private FFT fft;

	/** sample rate of the samples in the buffer */
	private float sampleRate;

	/** spectrum to be analyzed */
	private float[] spec;

	/** spectrum "whitener" for pre-processing */
	private SpecWhitener sw;

	/** size of the buffer */
	private int timeSize;

	public PitchDetect(int timeSize, float sampleRate, float[] pitches) {
		this.timeSize = timeSize;
		this.sampleRate = sampleRate;
		this.pitches = pitches;
		sw = new SpecWhitener(timeSize, sampleRate);
		spec = new float[timeSize / 2 + 1];
		fzeros = new float[pitches.length];
	}

	/**
	 * This method takes an AudioBuffer object as argument. It detects all notes in
	 * presence in buffer.
	 */
	public void detect(float[] spec) {
		for (int i = 0; i < spec.length; i++)
			spec[i] *= 1000;
		// spectrum pre-processing
		sw.whiten(spec);
		// spec = sw.wSpec;

		System.out.println(">>fzeros detecting");
		// iteratively find all presented pitches
		float test = 0, lasttest = 0;
		int loopcount = 1;
		float[] fzeroInfo = new float[3]; // ind 0 is the pitch, ind 1 its
											// salience, ind 2 its ind in
											// pitches
		while (true) {
			detectfzero(spec, fzeroInfo);
			lasttest = test;
			// System.out.println(">>fzeros BEFORE test: " + test + ", "+
			// fzeroInfo[1]+ ",
			// "+ Math.pow(loopcount, .1f));
			test = (float) ((test + fzeroInfo[1]) / Math.pow(loopcount, .2f));
			// System.out.println(">>fzeros test: " + test + ", "+ lasttest);
			// System.out.println(">>fzeros fzeroInfo[2] A: " + loopcount + ",
			// "+
			// fzeroInfo[2]);
			if (test <= lasttest)
				break;
			loopcount++;
			System.out.println(">>loop: " + loopcount);
			// System.out.println(">>fzeros fzeroInfo[2] B: " + loopcount + ",
			// "+
			// fzeroInfo[2]+ ", "+ fzeroInfo[0]);
			// System.out.println(">>fzeros fzeroInfo[2] C: " + (fzeroInfo[0] *
			// timeSize /
			// sampleRate));
			// subtract the information of the found pitch from the current
			// spectrum
			float fpitch = fzeroInfo[0];
			int highInd = (int) Math.floor(fzeroInfo[0] * timeSize / sampleRate);
			int lowInd = (int) Math.floor(fzeroInfo[0] * timeSize / sampleRate);
			for (int i = 1; i * fzeroInfo[0] < sampleRate / 2; ++i) {
				for (int j = 0; j < pitches.length; j++) {
					if (pitches[j] >= fpitch) {
						if (j > 0) {
							lowInd = (int) Math.floor(pitches[j - 1] * timeSize / sampleRate);
						} else {
							lowInd = (int) Math.floor(pitches[j] * timeSize / sampleRate);
						}
						if (j < pitches.length - 1) {
							highInd = (int) Math.floor(pitches[j] * timeSize / sampleRate);
						} else {
							highInd = (int) Math.floor(pitches[j - 1] * timeSize / sampleRate);
						}
						break;
					}
				}
				fpitch *= i;
				float weighting = (fzeroInfo[0] + 52) / (fpitch + 320);
				for (int j = lowInd; j <= highInd; j++) {
					spec[j] *= (1 - 0.89f * weighting);
				}
			}

			// update fzeros
			if (fzeros[(int) fzeroInfo[2]] == 0) {
				fzeros[(int) fzeroInfo[2]] = fzeroInfo[1];
			}
		}
		for (int i = 0; i < spec.length; i++) {
			float pitch = i * sampleRate / timeSize;
			spec[i] = 0F;
			for (int j = 0; j < pitches.length; j++) {
				if (fzeros[j] > 0 && pitches[j] <= pitch && pitches[j + 1] > pitch) {
					spec[i] = fzeros[j];
				}
			}
		}
	}

	public void whiten(float[] spec) {
		float max = 0;
		for (int i = 0; i < spec.length; i++) {
			if (spec[i] > max) {
				max = spec[i];
			}
			spec[i] *= 1000;
		}
		System.out.println(">>SPECMAX: " + max);
		// for (int i = 0; i < spec.length; i++) spec[i] *= 1000;
		// spectrum pre-processing
		sw.whiten(spec);
		// for (int i = 0; i < spec.length; i++) spec[i] /= 1000;
	}

	// utility function for detecting a single pitch
	private void detectfzero(float[] spec, float[] fzeroInfo) {
		float maxSalience = 0;
		for (int j = 0; j < pitches.length; ++j) {
			float cSalience = 0; // salience of the candidate pitch
			float val = 0;
			float fpitch = 0;
			int highInd = (int) Math.floor(pitches[j] * timeSize / sampleRate);
			int lowInd = (int) Math.floor(pitches[j] * timeSize / sampleRate);
			for (int i = 1; i * pitches[j] < sampleRate / 2; ++i) {
				fpitch = i * pitches[j];
				int bin = Math.round(i * pitches[j] * timeSize / sampleRate);
				for (int k = 0; k < pitches.length; k++) {
					if (pitches[k] >= fpitch) {
						if (k > 0) {
							lowInd = (int) Math.floor(pitches[k - 1] * timeSize / sampleRate);
						} else {
							lowInd = (int) Math.floor(pitches[k] * timeSize / sampleRate);
						}
						if (k < pitches.length - 1) {
							highInd = (int) Math.floor(pitches[k] * timeSize / sampleRate);
						} else {
							highInd = (int) Math.floor(pitches[k - 1] * timeSize / sampleRate);
						}
						break;
					}
				}
				// use the largest value of bins in vicinity
				val = spec[bin];
				// calculate the salience of the current candidate
				float weighting = (pitches[j] + 52) / (i * pitches[j] + 320);
				for (int k = lowInd; k <= highInd; k++) {
					if (val < spec[k]) {
						// val = spec[k];
					}
				}
				cSalience += val * weighting;
			}
			if (cSalience > maxSalience) {
				maxSalience = cSalience;
				fzeroInfo[0] = pitches[j];
				fzeroInfo[1] = cSalience;
				fzeroInfo[2] = j;
				// System.out.println(">>fzeros max: " + fzeroInfo[0] + ", "+
				// fzeroInfo[1] + ",
				// " + fzeroInfo[2]);
			} else {
				// System.out.println(">>fzeros min: " + pitches[j] + ", "+
				// cSalience + ", " +
				// j);
			}
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