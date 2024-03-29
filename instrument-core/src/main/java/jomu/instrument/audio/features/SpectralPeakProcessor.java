package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class SpectralPeakProcessor extends SpectralPeakDetector implements AudioProcessor {

	private static final Logger LOG = Logger.getLogger(SpectralPeakProcessor.class.getName());

	/**
	 * The sample rate of the signal.
	 */
	private final int sampleRate;

	/**
	 * Cached calculations for the frequency calculation
	 */
	private final double dt;
	private final double cbin;
	private final double inv_2pi;
	private final double inv_deltat;
	private final double inv_2pideltat;

	/**
	 * The fft object used to calculate phase and magnitudes.
	 */
	private final FFT fft;

	/**
	 * The phase info of the current frame.
	 */
	private final float[] currentPhaseOffsets;

	/**
	 * The phase information of the previous frame, or null.
	 */
	private float[] previousPhaseOffsets;

	public SpectralPeakProcessor(int bufferSize, int overlap, int sampleRate) {
		fft = new FFT(bufferSize, new HammingWindow());

		magnitudes = new float[bufferSize / 2];
		currentPhaseOffsets = new float[bufferSize / 2];
		frequencyEstimates = new float[bufferSize / 2];

		dt = (bufferSize - overlap) / (double) sampleRate;
		cbin = dt * sampleRate / bufferSize;

		inv_2pi = 1.0 / (2.0 * Math.PI);
		inv_deltat = 1.0 / dt;
		inv_2pideltat = inv_deltat * inv_2pi;

		this.sampleRate = sampleRate;

	}

	private void calculateFFT(float[] audio) {
		// Clone to prevent overwriting audio data
		float[] fftData = audio.clone();
		// Extract the power and phase data
		fft.powerPhaseFFT(fftData, magnitudes, currentPhaseOffsets);
	}

	private void normalizeMagnitudes() {
		float maxMagnitude = (float) -1e6;
		for (float magnitude : magnitudes) {
			maxMagnitude = Math.max(maxMagnitude, magnitude);
		}

		// log10 of the normalized value
		// adding 75 makes sure the value is above zero, a bit ugly though...
		for (int i = 1; i < magnitudes.length; i++) {
			magnitudes[i] = (float) (10 * Math.log10(magnitudes[i] / maxMagnitude)) + 75;
		}
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] audio = audioEvent.getFloatBuffer();

		// 1. Extract magnitudes, and phase using an FFT.
		calculateFFT(audio);

		// 2. Estimate a detailed frequency for each bin.
		calculateFrequencyEstimates();

		// 3. Normalize the each magnitude.
		// normalizeMagnitudes();

		// 4. Store the current phase so it can be used for the next frequency estimates
		// block.
		previousPhaseOffsets = currentPhaseOffsets.clone();

		return true;
	}

	@Override
	public void processingFinished() {
	}

	/**
	 * For each bin, calculate a precise frequency estimate using phase offset.
	 */
	private void calculateFrequencyEstimates() {
		for (int i = 0; i < frequencyEstimates.length; i++) {
			frequencyEstimates[i] = getFrequencyForBin(i);
		}
	}

	/**
	 * @return the magnitudes.
	 */
	@Override
	public float[] getMagnitudes() {
		return magnitudes.clone();
	}

	/**
	 * @return the precise frequency for each bin.
	 */
	@Override
	public float[] getFrequencyEstimates() {
		return frequencyEstimates.clone();
	}

	/**
	 * Calculates a frequency for a bin using phase info, if available.
	 *
	 * @param binIndex
	 *            The FFT bin index.
	 * @return a frequency, in Hz, calculated using available phase info.
	 */
	private float getFrequencyForBin(int binIndex) {
		final float frequencyInHertz;
		// use the phase delta information to get a more precise
		// frequency estimate
		// if the phase of the previous frame is available.
		// See
		// * Moore 1976
		// "The use of phase vocoder in computer music applications"
		// * Sethares et al. 2009 - Spectral Tools for Dynamic
		// Tonality and Audio Morphing
		// * Laroche and Dolson 1999
		if (previousPhaseOffsets != null) {
			float phaseDelta = currentPhaseOffsets[binIndex] - previousPhaseOffsets[binIndex];
			long k = Math.round(cbin * binIndex - inv_2pi * phaseDelta);
			frequencyInHertz = (float) (inv_2pideltat * phaseDelta + inv_deltat * k);
		} else {
			frequencyInHertz = (float) fft.binToHz(binIndex, sampleRate);
		}
		return frequencyInHertz;
	}

	/**
	 * Calculate a noise floor for an array of magnitudes.
	 *
	 * @param magnitudes
	 *            The magnitudes of the current frame.
	 * @param medianFilterLength
	 *            The length of the median filter used to determine
	 *            the noise floor.
	 * @param noiseFloorFactor
	 *            The noise floor is multiplied with this factor to
	 *            determine if the information is either noise or an interesting
	 *            spectral
	 *            peak.
	 * @return a float array representing the noise floor.
	 */
	public static float[] calculateNoiseFloor(float[] magnitudes, int medianFilterLength, float noiseFloorFactor) {
		double[] noiseFloorBuffer;
		double maxMagnitude = 0;
		for (int i = 0; i < magnitudes.length; i++) {
			if (maxMagnitude < magnitudes[i]) {
				maxMagnitude = magnitudes[i];
			}
		}

		double noiseFloorMin = ToneTimeFrame.AMPLITUDE_FLOOR;
		if (maxMagnitude > ToneTimeFrame.AMPLITUDE_FLOOR * 10.0) {
			noiseFloorMin = maxMagnitude * 0.1;
		}

		float[] noisefloor = new float[magnitudes.length];

		float median = (float) median(magnitudes.clone());

		// Naive median filter implementation.
		// For each element take a median of surrounding values (noiseFloorBuffer)
		// Store the median as the noise floor.
		for (int i = 0; i < magnitudes.length; i++) {
			noiseFloorBuffer = new double[medianFilterLength];
			int index = 0;
			for (int j = i - medianFilterLength / 2; j <= i + medianFilterLength / 2
					&& index < noiseFloorBuffer.length; j++) {
				if (j >= 0 && j < magnitudes.length) {
					noiseFloorBuffer[index] = magnitudes[j];
				} else {
					noiseFloorBuffer[index] = median;
				}
				index++;
			}
			// calculate the noise floor value.
			noisefloor[i] = median(noiseFloorBuffer) * (noiseFloorFactor);
			if (noisefloor[i] < noiseFloorMin) {
				noisefloor[i] = (float) noiseFloorMin;
			}
		}

		float rampLength = 12.0f;
		for (int i = 0; i <= rampLength; i++) {
			// ramp
			float ramp = 1.0f;
			ramp = (float) (-1 * (Math.log(i / rampLength))) + 1.0f;
			noisefloor[i] = ramp * noisefloor[i];
			if (noisefloor[i] < noiseFloorMin) {
				noisefloor[i] = (float) noiseFloorMin;
			}
		}

		return noisefloor;
	}

	/**
	 * Finds the local magintude maxima and stores them in the given list.
	 *
	 * @param magnitudes
	 *            The magnitudes.
	 * @param noisefloor
	 *            The noise floor.
	 * @return a list of local maxima.
	 */
	public static List<Integer> findLocalMaxima(float[] magnitudes, float[] noisefloor) {
		List<Integer> localMaximaIndexes = new ArrayList<>();
		for (int i = 1; i < magnitudes.length - 1; i++) {
			boolean largerThanPrevious = (magnitudes[i - 1] < magnitudes[i]);
			boolean largerThanNext = (magnitudes[i] > magnitudes[i + 1]);
			boolean largerThanNoiseFloor = (magnitudes[i] > noisefloor[i]);
			if (largerThanPrevious && largerThanNext && largerThanNoiseFloor) {
				localMaximaIndexes.add(i);
			}
		}
		return localMaximaIndexes;
	}

	/**
	 * @param magnitudes
	 *            the magnitudes.
	 * @return the index for the maximum magnitude.
	 */
	private static int findMaxMagnitudeIndex(float[] magnitudes) {
		int maxMagnitudeIndex = 0;
		float maxMagnitude = (float) -1e6;
		for (int i = 1; i < magnitudes.length - 1; i++) {
			if (magnitudes[i] > maxMagnitude) {
				maxMagnitude = magnitudes[i];
				maxMagnitudeIndex = i;
			}
		}
		return maxMagnitudeIndex;
	}

	/**
	 * @param magnitudes
	 *            the magnitudes..
	 * @param frequencyEstimates
	 *            The frequency estimates for each bin.
	 * @param localMaximaIndexes
	 *            The indexes of the local maxima.
	 * @param numberOfPeaks
	 *            The requested number of peaks.
	 * @param minDistanceInCents
	 *            The minimum distance in cents between the peaks
	 * @return A list with spectral peaks.
	 */
	public static List<SpectralPeak> findPeaks(float[] magnitudes, float[] frequencyEstimates,
			List<Integer> localMaximaIndexes, int numberOfPeaks, int minDistanceInCents) {
		int maxMagnitudeIndex = findMaxMagnitudeIndex(magnitudes);
		List<SpectralPeak> spectralPeakList = new ArrayList<>();

		if (localMaximaIndexes.size() == 0)
			return spectralPeakList;

		float referenceFrequency = 0;
		// the frequency of the bin with the highest magnitude
		referenceFrequency = frequencyEstimates[maxMagnitudeIndex];

		// remove frequency estimates below zero
		for (int i = 0; i < localMaximaIndexes.size(); i++) {
			if (frequencyEstimates[localMaximaIndexes.get(i)] < 0) {
				localMaximaIndexes.remove(i);
				frequencyEstimates[localMaximaIndexes.get(i)] = 1;// Hz
				i--;
			}
		}

		// filter the local maxima indexes, remove peaks that are too close to each
		// other
		// assumes that localmaximaIndexes is sorted from lowest to higest index
		for (int i = 1; i < localMaximaIndexes.size(); i++) {
			double centCurrent = PitchConverter.hertzToAbsoluteCent(frequencyEstimates[localMaximaIndexes.get(i)]);
			double centPrev = PitchConverter.hertzToAbsoluteCent(frequencyEstimates[localMaximaIndexes.get(i - 1)]);
			double centDelta = centCurrent - centPrev;
			if (centDelta < minDistanceInCents) {
				if (magnitudes[localMaximaIndexes.get(i)] > magnitudes[localMaximaIndexes.get(i - 1)]) {
					localMaximaIndexes.remove(i - 1);
				} else {
					localMaximaIndexes.remove(i);
				}
				i--;
			}
		}

		// Retrieve the maximum values for the indexes
		float[] maxMagnitudes = new float[localMaximaIndexes.size()];
		for (int i = 0; i < localMaximaIndexes.size(); i++) {
			maxMagnitudes[i] = magnitudes[localMaximaIndexes.get(i)];
		}
		// Sort the magnitudes in ascending order
		Arrays.sort(maxMagnitudes);

		// Find the threshold, the first value or somewhere in the array.
		float peakthresh = maxMagnitudes[0];
		if (maxMagnitudes.length > numberOfPeaks) {
			peakthresh = maxMagnitudes[maxMagnitudes.length - numberOfPeaks];
		}

		// store the peaks
		for (Integer i : localMaximaIndexes) {
			if (magnitudes[i] >= peakthresh) {
				final float frequencyInHertz = frequencyEstimates[i];
				// ignore frequencies lower than 30Hz
				float binMagnitude = magnitudes[i];
				SpectralPeak peak = new SpectralPeak(0, frequencyInHertz, binMagnitude, referenceFrequency, i);
				spectralPeakList.add(peak);
			}
		}
		return spectralPeakList;
	}

}
