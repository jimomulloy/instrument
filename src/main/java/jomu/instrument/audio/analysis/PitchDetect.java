package jomu.instrument.audio.analysis;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a robust polyphonic multi-pitch detector. The algorithm is described
 * by Anssi Klapuri in Multiple Fundamental Frequency Estimation by Summing
 * Harmonic Amplitudes. Yuan Gao, 2011
 */

public class PitchDetect {
	/** array to hold fzeros info, 1 := positive, 0 := negative */
	public float[] fzeros;
	public Map<Integer, Float> fzeroBins;

	private float[] pitches;

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

	public PitchDetect(int timeSize, float sampleRate) {
		this.timeSize = timeSize;
		this.sampleRate = sampleRate;
		/*
		 * Create candidate frequencies here
		 * (http://www.phy.mtu.edu/~suits/NoteFreqCalcs.html) Five octaves of candidate
		 * notes. Use quarter a half-step to get out of tune freqs Lowest freq (f0) =
		 * 55.0 Hz, A three octaves below A above the middle C
		 */
		float f0Init = 55; // Hz
		float a = (float) Math.pow(2.0, (1.0 / 12.0));
		this.pitches = new float[5 * 12]; // 5 octaves, 12 half-steps per octave
		for (int kk = 0; kk < this.pitches.length; ++kk) {
			this.pitches[kk] = (float) (f0Init * Math.pow(a, (kk)));
		}

		sw = new SpecWhitener(timeSize, sampleRate);
		fzeros = new float[pitches.length];
	}

	public void whiten(float[] spec) {
		sw.whiten(spec);
	}

	/**
	 * This method takes an AudioBuffer object as argument. It detects all notes in
	 * presence in buffer.
	 */
	public void detect(float[] spec) {
		// for (int i = 0; i < spec.length; i++)
		// spec[i] *= 1000;
		// sw.whiten(spec);

		System.out.println(">>fzeros detecting");
		// iteratively find all presented pitches
		float test = 0, lasttest = 0;
		int loopcount = 1;
		float[] fzeroInfo = new float[4]; // ind 0 is the pitch, ind 1 its
											// salience, ind 2 its ind in
											// pitches
											// ind 3 is bin

		fzeroBins = new HashMap<>();

		while (true) {
			detectfzero(spec, fzeroInfo);
			System.out.println(">>detectfzero: " + fzeroInfo[2] + ", " + fzeroInfo[3]);
			lasttest = test;
			test = (float) ((test + fzeroInfo[1]) / Math.pow(loopcount, .2f));
			if (test <= lasttest)
				break;
			loopcount++;
			fzeroBins.put((int) fzeroInfo[3], fzeroInfo[1]);
			// subtract the information of the found pitch from the current
			// spectrum
			for (int i = 1; i * fzeroInfo[0] < sampleRate / 2; i++) {
				int partialInd = (int) Math.floor(i * fzeroInfo[0] * timeSize / sampleRate);
				float weighting = i == 1 ? (1.0f / 0.89f) : (fzeroInfo[0] + 52) / (i * fzeroInfo[0] + 320);
				System.out.println(">>MUTE PITCH BEFORE: " + partialInd + ", " + spec[partialInd] + ", " + i + ", "
						+ fzeroInfo[0]);
				spec[partialInd] *= (1 - 0.89f * weighting);
				if (spec.length < partialInd + 1) {
					spec[partialInd + 1] *= (1 - 0.89f * weighting);
				}
				spec[partialInd - 1] *= (1 - 0.89f * weighting);
				if (partialInd > 1) {
					spec[partialInd - 1] *= (1 - 0.89f * weighting);
					if (partialInd > 2) {
						spec[partialInd - 2] *= (1 - 0.89f * weighting);
						if (partialInd > 3) {
							spec[partialInd - 3] *= (1 - 0.89f * weighting);
						}
					}
				}
				System.out.println(">>MUTE PITCH AFTER: " + partialInd + ", " + spec[partialInd]);
			}

			// update fzeros
			if (fzeros[(int) fzeroInfo[2]] == 0) {
				fzeros[(int) fzeroInfo[2]] = fzeroInfo[1];
				System.out.println(">>SET PITCH: " + fzeroInfo[2] + ", " + fzeroInfo[1] + ", " + fzeroInfo[0]);
			}
		}
		System.out.println(">>done detectes");

		for (int i = 0; i < spec.length; i++) {
			if (fzeroBins.containsKey(i)) {
				spec[i] = fzeroBins.get(i);
			} else {
				spec[i] = 0;
			}
		}

	}

	// utility function for detecting a single pitch
	private void detectfzero(float[] spec, float[] fzeroInfo) {
		float maxSalience = 0;
		for (int j = 0; j < pitches.length; ++j) {
			float cSalience = 0; // salience of the candidate pitch
			float val = 0;
			int f0bin = 0;
			for (int i = 1; i * pitches[j] < sampleRate / 2; i++) {
				int bin = (int) Math.floor(i * pitches[j] * timeSize / sampleRate);
				if (i == 1) {
					f0bin = bin;
				}
				// use the largest value of bins in vicinity
				// System.out.println(">> Bin: " + bin + ", " + pitches[j] + ", " + j + " ," +
				// i);
				if (bin == timeSize / 2)
					val = Math.max(Math.max(spec[bin - 3], spec[bin - 2]), spec[bin - 1]);
				else if (bin == timeSize / 2 - 1)
					val = Math.max(Math.max(spec[bin - 3], spec[bin - 2]), Math.max(spec[bin - 1], spec[bin]));
				else if (bin > 2)
					val = Math.max(Math.max(Math.max(spec[bin - 3], spec[bin - 2]), Math.max(spec[bin - 1], spec[bin])),
							spec[bin + 1]);
				else if (bin > 1)
					val = Math.max(Math.max(spec[bin - 2], Math.max(spec[bin - 1], spec[bin])), spec[bin + 1]);
				else if (bin < 1)
					val = Math.max(Math.max(spec[bin - 1], spec[bin]), spec[bin + 1]);

				//
				val = spec[bin];
				// calculate the salience of the current candidate
				float weighting = i == 1 ? 1.0F : (pitches[j] + 52) / (i * pitches[j] + 320);
				cSalience += val * weighting;
			}
			if (cSalience > maxSalience) {
				maxSalience = cSalience;
				fzeroInfo[0] = pitches[j];
				fzeroInfo[1] = cSalience;
				fzeroInfo[2] = j;
				fzeroInfo[3] = f0bin;
				System.out.println(">> Detected: " + j + ", " + pitches[j] + ", " + cSalience + ", " + f0bin);
			}
		}
		System.out.println(">> Detecting: " + fzeroInfo[2]);
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