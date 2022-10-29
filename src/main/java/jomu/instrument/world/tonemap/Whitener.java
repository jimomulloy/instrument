package jomu.instrument.world.tonemap;

import java.util.ArrayList;
import java.util.Vector;

public class Whitener {
	private double[] cb; /* Klapuri whitening ranges */
	private FFTSpectrum fftSpectrum;
	private double[] gammaCoeff;

	private ArrayList<Double>[] Hb; /* filter bank for whitening */

	private ArrayList<Integer>[] hbIndices; /*
											 * filter bank indices for whitening
											 */
	private float[] whitenedSpectrum;

	public Whitener(FFTSpectrum fftSpectrum) {
		this.fftSpectrum = fftSpectrum;

		cb = new double[32];
		/*
		 * CB filterbank always the same values, could be included from
		 * somewhere...
		 */
		for (

				int b = 0; b < 32; ++b) {
			cb[b] = 229.0 * (Math.pow(10.0, ((b + 1.0) / 21.4)) - 1.0); // frequency
																		// division
		}

		/* Create filter bank */
		Hb = new ArrayList[30];
		hbIndices = new ArrayList[30];
		double[] freqs = this.fftSpectrum.getBinFrequencies();
		for (int i = 1; i < 31; ++i) {
			Hb[i - 1] = new ArrayList<>();
			hbIndices[i - 1] = new ArrayList<>();
			int kk = ind(freqs, cb[i - 1]);
			while (freqs[kk] <= cb[i + 1]) {
				hbIndices[i - 1].add(kk);
				if (freqs[kk] <= cb[i]) {
					Hb[i - 1].add(1 - Math.abs(cb[i] - freqs[kk])
							/ (cb[i] - cb[i - 1]));
				} else {
					Hb[i - 1].add(1 - Math.abs(cb[i] - freqs[kk])
							/ (cb[i + 1] - cb[i]));
				}
				++kk;
			}
		}
	}

	public float[] getWhitenedSpectrum() {
		return whitenedSpectrum;
	}

	public float[] whiten() {
		whitenedSpectrum = new float[fftSpectrum.getSpectrum().length];
		System.out.println(">> WS Len: " + whitenedSpectrum.length);
		/* Calculate signal energies in filter windows?? */
		Vector<Double> gammab = new Vector<>();
		Vector<Double> stdb = new Vector<>();

		int kk;
		/* The filter bank Hb could be pre-calculated, to be implemented... */
		for (int i = 0; i < Hb.length; ++i) {
			double tempSum = 0;
			for (int j = 0; j < Hb[i].size(); ++j) {
				tempSum += Hb[i].get(j) * Math.pow(
						fftSpectrum.getSpectrum()[hbIndices[i].get(j)], 2.0);
			}
			stdb.add(Math.sqrt(tempSum / (fftSpectrum.getSpectrum().length)));
			gammab.add(Math.pow(stdb.lastElement(), 0.33 - 1.0));
		}

		// Interpolate gammab...
		gammaCoeff = new double[fftSpectrum.getSpectrum().length];

		kk = 0;
		double[] freqs = this.fftSpectrum.getBinFrequencies();
		while (freqs[kk] < cb[1]) { // Search for the first frequency..
			gammaCoeff[kk] = gammab.get(0);
			++kk;
		}
		double whitemax = 0;
		for (int i = 0; i < gammab.size() - 1; ++i) {
			int init = ind(freqs, cb[i + 1]);
			int end = ind(freqs, cb[i + 2]);
			while (kk < end) {
				gammaCoeff[kk] = gammab.get(i)
						+ (gammab.get(i + 1) - gammab.get(i))
								* Math.abs((double) (kk - init)) / (end - init);
				++kk;
			}
		}
		/* Fill in the rest of the whitenedSpectrum with the last gammab */
		while (kk < whitenedSpectrum.length) {
			gammaCoeff[kk] = gammab.get(gammab.size() - 1);
			++kk;
		}
		/* whiten the signal */
		for (int i = 0; i < whitenedSpectrum.length; ++i) {
			whitenedSpectrum[i] = (float) (fftSpectrum.getSpectrum()[i]
					* gammaCoeff[i]);
		}
		return whitenedSpectrum;
	}

	private int ind(double[] arr, double a) {
		int b = 0;
		while (arr[b] < a) {
			++b;
		}
		return b;
	}
}
