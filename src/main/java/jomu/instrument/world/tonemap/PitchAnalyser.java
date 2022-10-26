package jomu.instrument.world.tonemap;

import java.util.ArrayList;
import java.util.Vector;

public class PitchAnalyser {

	private ArrayList<Integer>[] f0index; /* Klapuri F0 candidate indices */
	private ArrayList<Integer>[] f0indHarm; /*
											 * Klapuri F0 candidate indices
											 * harmonics
											 */
	private FFTSpectrum fftSpectrum;
	private int harmonics = 20;
	int surroundingBins = 1;
	private double alpha = 52.0; // Hz
	private double beta = 320.0; // Hz
	private double dee = 0.89;
	private double[] f0cands;

	public PitchAnalyser(FFTSpectrum fftSpectrum, double[] f0cands,
			int harmonics) {
		this.fftSpectrum = fftSpectrum;
		this.harmonics = harmonics;
		this.f0cands = f0cands;
		/*
		 * Create candidate frequencies here
		 * (http://www.phy.mtu.edu/~suits/NoteFreqCalcs.html) Five octaves of
		 * candidate notes. Use quarter a half-step to get out of tune freqs
		 * Lowest freq (f0) = 55.0 Hz, A three octaves below A above the middle
		 * C
		 */
		double f0Init = 55; // Hz
		double a = Math.pow(2.0, (1.0 / 12.0));
		f0cands = new double[5 * 12 * 4]; // 5 octaves, 12 half-steps per
											// octave, quarter half-steps
		for (int kk = 0; kk < f0cands.length; ++kk) {
			f0cands[kk] = f0Init * Math.pow(a, (kk) / 4.0);
		}

		/*
		 * Pre-calculate frequency bins for a given f0 candidate
		 */
		f0index = new ArrayList[f0cands.length];
		f0indHarm = new ArrayList[f0cands.length];
		double halfBinWidth = ((double) this.fftSpectrum.getSampleRate()
				/ (double) this.fftSpectrum.getWindowSize()) / 2.0;
		for (int k = 0; k < f0index.length; ++k) {
			f0index[k] = new ArrayList();
			f0indHarm[k] = new ArrayList();
			for (int h = 0; h < harmonics; ++h) {
				ArrayList<Integer> tempInd = find(
						this.fftSpectrum.getBinFrequencies(),
						f0cands[k] * (h + 1.0) - halfBinWidth,
						f0cands[k] * (h + 1.0) + halfBinWidth);
				f0index[k].addAll(tempInd);
				for (int t = 0; t < tempInd.size(); ++t) {
					f0indHarm[k].add(h + 1);
				}
			}
		}
	}

	public Vector<Double> detectF0s() {
		Vector<Double> F0s = new Vector<>();
		Vector<ArrayList> F0BinIndexes = new Vector<>();
		Vector<Double> S = new Vector<>();
		S.add(0.0);
		double[] freqs = fftSpectrum.getBinFrequencies();
		int fftWindow = fftSpectrum.getWindowSize();
		float samplingRate = fftSpectrum.getSampleRate();
		// Begin extracting F0s
		double smax = 0;
		int index = 0;
		int detectedF0s = 0;
		// F0 detection
		double[] resultsk = new double[fftSpectrum.getBinFrequencies().length];
		double[] salience;
		double summa;
		float[] spec = fftSpectrum.getSpectrum();
		while (S.lastElement() >= smax) {
			// Calculating the salience function (the hard way...)
			salience = new double[f0cands.length];
			double salmax = 0;

			for (int i = 0; i < f0index.length; ++i) {
				summa = 0;
				for (int j = 0; j < f0index[i].size(); ++j) {
					if (f0index[i].get(j) > freqs.length) {
						System.out
								.println(">>!! freqs!!: " + f0index[i].get(j));
					} else if (f0index[i].get(j) >= spec.length) {
						System.out.println(
								">>!! whitenedSpec!!: " + f0index[i].get(j));
					} else {
						summa += (samplingRate * freqs[f0index[i].get(j)]
								+ alpha)
								/ ((j + 1) * samplingRate
										* freqs[f0index[i].get(j)] + beta)
								* spec[f0index[i].get(j)];
					}
				}
				salience[i] = summa;
				if (salience[i] > salmax) {
					index = i;
					salmax = salience[i];
				}
			}

			// Salience calculated
			++detectedF0s;
			F0s.add(f0cands[index]); // First F0
			F0BinIndexes.add(f0index[index]);

			/* Replace this with using f0cands indices at some point! */
			// Frequency cancellation
			// System.out.println("To cancellation "+f0index[index].size()+"
			// "+f0indHarm[index].size());
			int[] tempCancelled = new int[resultsk.length];
			for (int j = 0; j < f0index[index].size(); ++j) {
				/* Suppress the surrounding bins as well */
				for (int i = -1; i <= 1; ++i) {
					if (tempCancelled[f0index[index].get(j) + i] == 0
							&& f0index[index].get(j) + i < resultsk.length) {
						// System.out.println(f0index[index].get(j)+"
						// "+freq[f0index[index].get(j)]);
						resultsk[f0index[index].get(j)
								+ i] = resultsk[f0index[index].get(j) + i]
										+ (samplingRate
												* freqs[f0index[index].get(j)
														+ i]
												+ alpha)
												/ (((double) f0indHarm[index]
														.get(j))
														* samplingRate
														* freqs[f0index[index]
																.get(j) + i]
														+ beta)
												* spec[f0index[index].get(j)
														+ i];
						if (spec[f0index[index].get(j) + i]
								- resultsk[f0index[index].get(j) + i] > 0) {
							spec[f0index[index].get(j)
									+ i] = (float) (spec[f0index[index].get(j)
											+ i]
											- resultsk[f0index[index].get(j)
													+ i] * dee);
						} else {
							spec[f0index[index].get(j) + i] = 0;
						}
						tempCancelled[f0index[index].get(j) + i] = 1;
					}

				}

			}
			// System.out.println("Cancellation done");
			// requency cancellation done
			// Polyphony estimation
			if (S.size() < detectedF0s) {
				S.add(0.0);
			}
			summa = 0;
			for (double element : resultsk) {
				summa += element;
			}
			S.set(S.size() - 1, summa / Math.pow(detectedF0s, 0.7));
			if (S.lastElement() > smax) {
				smax = S.lastElement();
			}
			// Polyphony estimated
		}
		// The last F0 is extra...
		// System.out.println("Remove extra");
		if (F0s.size() > 1) {
			F0s.remove(F0s.size() - 1);
			F0BinIndexes.remove(F0BinIndexes.size() - 1);
		}

		for (int i = 0; i < spec.length; i++) {
			spec[i] = 0F;
		}
		for (ArrayList<Integer> f0Bins : F0BinIndexes) {
			for (Integer f0Bin : f0Bins) {
				spec[f0Bin] = 1.0F;
			}
		}
		return F0s;
	}

	private ArrayList<Integer> find(double[] arr, double lower, double upper) {
		ArrayList<Integer> b = new ArrayList<>();
		for (int i = 0; i < arr.length; ++i) {
			if (arr[i] >= lower && arr[i] <= upper) {
				b.add(i);
			}
		}
		return b;
	}
}