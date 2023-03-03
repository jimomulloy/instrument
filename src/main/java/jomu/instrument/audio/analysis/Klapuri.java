/*
	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

	N.B.  the above text was copied from http://www.gnu.org/licenses/gpl.html
	unmodified. I have not attached a copy of the GNU license to the source...

    Copyright (C) 2011-2012 Timo Rantalainen
*/

package jomu.instrument.audio.analysis;

import java.util.ArrayList;
import java.util.Vector;

public class Klapuri {
	public double[] processedSpectrumData;
	public double[] gammaCoeff;
	public Vector<Double> f0s = new Vector<>();
	public Vector<Double> f0saliences = new Vector<>();
	PolyphonicPitchDetection pppContext;
	double alpha = 52.0; // Hz
	double beta = 320.0; // Hz
	double dee = 0.89;

	public Klapuri(double[] spectrumData, PolyphonicPitchDetection pppContext) {
		// whitened = (double[]) data.clone();
		this.pppContext = pppContext;
		/* Whiten the data */
		// processedSpectrumData = whiten(spectrumData, pppContext);
		processedSpectrumData = spectrumData.clone();
		detectF0s(processedSpectrumData, pppContext);
	}

	void detectF0s(double[] spectrumData, PolyphonicPitchDetection pppContext) {
		Vector<Double> F0salmaxes = new Vector<>();
		Vector<ArrayList> F0BinIndexes = new Vector<>();
		Vector<Double> S = new Vector<>();
		S.add(0.0);
		// Begin extracting F0s
		double smax = 0;
		int index = 0;
		int detectedF0s = 0;
		// F0 detection
		double[] resultsk = new double[pppContext.freq.length];
		double[] salience;
		double summa;
		double lastIndex = -1;
		double salMaxCeiling = 0;
		while (S.lastElement() >= smax) {
			// Calculating the salience function (the hard way...)
			salience = new double[pppContext.f0cands.length];
			double salmax = 0;

			for (int i = 0; i < pppContext.f0index.length; ++i) {
				summa = 0;
				for (int j = 0; j < pppContext.f0index[i].size(); ++j) {
					if (pppContext.f0index[i].get(j) >= spectrumData.length) {
						// System.out.println(">> WW: " + pppContext.freq.length + ", " +
						// pppContext.f0index[i].get(j)
						// + ", " + spectrumData.length);
					} else if (pppContext.f0index[i].get(j) >= pppContext.freq.length) {
						// System.out.println(">> DD: " + pppContext.f0index[i].get(j) + ", " +
						// pppContext.freq.length);
					} else {
						summa += (pppContext.samplingRate * pppContext.freq[pppContext.f0index[i].get(j)] + alpha)
								/ ((j + 1) * pppContext.samplingRate * pppContext.freq[pppContext.f0index[i].get(j)]
										+ beta)
								* spectrumData[pppContext.f0index[i].get(j)];
					}
				}
				salience[i] = summa;
				if (salience[i] > salmax) {
					index = i;
					salmax = salience[i];
				}
			}

			if (salmax <= pppContext.lowThreshold || lastIndex == index) {
				break;
			}

			// Salience calculated
			++detectedF0s;
			lastIndex = index;
			f0s.add(pppContext.f0cands[index]);
			F0salmaxes.add(salmax);
			// System.out.println("!!>KLAPURI ADD F0: " + index + ", " +
			// pppContext.f0index[index] + ", "
			// + pppContext.f0cands[index] + ", " + salmax);
			F0BinIndexes.add(pppContext.f0index[index]);

			if (salMaxCeiling < salmax) {
				salMaxCeiling = salmax;
			}

			/* Replace this with using f0cands indices at some point! */
			// Frequency cancellation
			int[] tempCancelled = new int[resultsk.length];
			for (int j = 0; j < pppContext.f0index[index].size(); ++j) {
				/* Suppress the surrounding bins as well */
				for (int i = -1; i <= 1; ++i) {
					if (tempCancelled[pppContext.f0index[index].get(j) + i] == 0
							&& pppContext.f0index[index].get(j) + i < resultsk.length) {
						resultsk[pppContext.f0index[index].get(j) + i] = resultsk[pppContext.f0index[index].get(j) + i]
								+ (pppContext.samplingRate * pppContext.freq[pppContext.f0index[index].get(j) + i]
										+ alpha)
										/ (((double) pppContext.f0indHarm[index].get(j)) * pppContext.samplingRate
												* pppContext.freq[pppContext.f0index[index].get(j) + i] + beta)
										* spectrumData[pppContext.f0index[index].get(j) + i];
						if (spectrumData[pppContext.f0index[index].get(j) + i]
								- resultsk[pppContext.f0index[index].get(j) + i] > 0) {
							spectrumData[pppContext.f0index[index].get(j)
									+ i] = spectrumData[pppContext.f0index[index].get(j) + i]
											- resultsk[pppContext.f0index[index].get(j) + i] * dee;
						} else {
							spectrumData[pppContext.f0index[index].get(j) + i] = 0;
						}
						tempCancelled[pppContext.f0index[index].get(j) + i] = 1;
					}

				}
				break; // !!TODO only do first

			}

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
			if (detectedF0s > 50) {
				break;
			}

		}
		// The last F0 is extra...
		System.out.println("Remove extra");
		if (f0s.size() > 1) {
			f0s.remove(f0s.size() - 1);
		}

		// Normalise saliences
		for (Double f0salmax : F0salmaxes) {
			double f0sNormal = f0salmax / salMaxCeiling;
			// System.out.println(">>KLAP F0s norm: " + f0sNormal);
			f0saliences.add(f0sNormal);
		}
	}

	double[] whiten(double[] dataIn, PolyphonicPitchDetection pppContext) {
		double[] whitened = new double[dataIn.length];

		/* Calculate signal energies in filter windows?? */
		Vector<Double> gammab = new Vector<>();
		Vector<Double> stdb = new Vector<>();

		int kk;
		/* The filter bank Hb could be pre-calculated, to be implemented... */
		for (int i = 0; i < pppContext.Hb.length; ++i) {
			double tempSum = 0;
			for (int j = 0; j < pppContext.Hb[i].size(); ++j) {
				if (pppContext.hbIndices[i].get(j) < dataIn.length) {
					tempSum += pppContext.Hb[i].get(j) * Math.pow(dataIn[pppContext.hbIndices[i].get(j)], 2.0);
				} else {
					// System.out.println(">>xx: " + pppContext.hbIndices[i].get(j) + ", " +
					// dataIn.length);
				}
			}
			stdb.add(Math.sqrt(tempSum / (dataIn.length)));
			gammab.add(Math.pow(stdb.lastElement(), 0.33 - 1.0));
		}

		// Interpolate gammab...
		gammaCoeff = new double[dataIn.length];

		kk = 0;
		while (pppContext.freq[kk] < pppContext.cb[1]) { // Search for the first frequency..
			gammaCoeff[kk] = gammab.get(0);
			++kk;
		}
		double whitemax = 0;
		for (int i = 0; i < gammab.size() - 1; ++i) {
			int init = ind(pppContext.freq, pppContext.cb[i + 1]);
			int end = ind(pppContext.freq, pppContext.cb[i + 2]);
			while (kk < end) {
				gammaCoeff[kk] = gammab.get(i)
						+ (gammab.get(i + 1) - gammab.get(i)) * Math.abs((double) (kk - init)) / (end - init);
				++kk;
			}
		}
		/* Fill in the rest of the whitened with the last gammab */
		while (kk < whitened.length) {
			gammaCoeff[kk] = gammab.get(gammab.size() - 1);
			++kk;
		}
		/* whiten the signal */
		for (int i = 0; i < whitened.length; ++i) {
			whitened[i] = dataIn[i] * gammaCoeff[i];
		}
		return whitened;
	}

	public static int ind(double[] arr, double a) {
		int b = 0;
		while (arr[b] < a) {
			++b;
		}
		return b;
	}
}
