package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ToneTimeFrame {

	public final static int INIT_PITCH_HIGH = 72;

	public final static int INIT_PITCH_LOW = 36;

	public final static double AMPLITUDE_FLOOR = 1E-7;

	public static final boolean LOGAMP = true;

	public static final boolean POWERAMP = false;

	private boolean ampType = LOGAMP;

	private double avgAmplitude;

	private ToneMapElement[] elements;

	private double highThreshold = 100;
	private double lowThreshold = 0;
	private double maxAmplitude;
	private double minAmplitude;
	private NoteStatus noteStatus;
	private PitchSet pitchSet;

	private TimeSet timeSet;

	public ToneTimeFrame(TimeSet timeSet, PitchSet pitchSet) {
		this.timeSet = timeSet;
		this.pitchSet = pitchSet;
		int pitchRange = pitchSet.getRange();
		elements = new ToneMapElement[pitchRange];
		for (int i = 0; i < elements.length; i++) {
			elements[i] = new ToneMapElement(i);
		}
		noteStatus = new NoteStatus(pitchSet);
	}

	@Override
	public ToneTimeFrame clone() {
		ToneTimeFrame copy = new ToneTimeFrame(this.timeSet.clone(), this.pitchSet.clone());
		copy.noteStatus = this.noteStatus.clone();
		ToneMapElement[] copyElements = copy.getElements();
		for (int i = 0; i < elements.length; i++) {
			copyElements[i] = elements[i].clone();
		}
		copy.reset();
		copy.setLowThreshold(this.getLowThres());
		copy.setHighThreshold(this.getHighThreshold());
		return copy;
	}

	public void compress(float factor) {
		System.out.println(">>COMPRESS BEFORE: " + getHighThreshold());
		double highThreshold = (float) Math.log10(1 + (factor * getHighThreshold()));
		System.out.println(">>COMPRESS AFTER: " + getHighThreshold());
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				elements[i].amplitude = (float) Math.log10(1 + (factor * elements[i].amplitude));
			}
		}
		normalise(highThreshold);
	}

	public void normalise(double highThreshold) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				elements[i].amplitude = elements[i].amplitude / highThreshold;
			}
		}
		setHighThreshold(1.0);
		reset();
	}

	public void deNoise(double threshold) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null && elements[i].amplitude < threshold) {
				elements[i].amplitude = 0.00001;
			}
		}
		reset();
	}

	public FFTSpectrum extractFFTSpectrum(int windowSize) {

		float[] spec = new float[windowSize + 1];
		double binStartFreq, binEndFreq;
		int binStartIndex, binEndIndex;
		binStartFreq = pitchSet.getFreq(0);
		binStartIndex = (int) ((binStartFreq * windowSize * 2.0) / (timeSet.getSampleRate()));
		for (int j = 0; j < binStartIndex; j++) {
			spec[j] = 0F;
		}
		float maxA = 0, maxP = 0;
		for (int i = 0; i < elements.length; i++) {
			binStartFreq = pitchSet.getFreq(i);
			binEndFreq = pitchSet.getFreq(i + 1);
			binStartIndex = (int) ((binStartFreq * windowSize * 2.0) / (timeSet.getSampleRate()));
			binEndIndex = (int) ((binEndFreq * windowSize * 2.0) / (timeSet.getSampleRate()));
			// System.out.println(">>extractFFT binStartFreq: " + binStartFreq);
			// System.out.println(">>extractFFT binEndFreq: " + binEndFreq);
			// System.out.println(">>extractFFT binStartIndex: " +
			// binStartIndex);
			// System.out.println(">>extractFFT binEndIndex: " + binEndIndex);
			if (binEndIndex > binStartIndex) {
				float partialValue = (float) (elements[i].amplitude) / (binEndIndex - binStartIndex);
				for (int j = 0; j < (binEndIndex - binStartIndex); j++) {
					spec[binStartIndex + j] += partialValue;
				}
			} else {
				spec[binStartIndex] += elements[i].amplitude;
			}
		}
		System.out.println(">> MAXP, MAXA: " + maxP + ", " + maxA);
		FFTSpectrum fftSpectrum = new FFTSpectrum(timeSet.getSampleRate(), windowSize, spec);
		return fftSpectrum;
	}

	public double FTPowerToAmp(double power) {
		double amplitude = 0.0F;
		if (power <= 0.0)
			return 0.0F;
		if (ampType == LOGAMP) {
			amplitude = (float) Math.log10(1 + (100.0 * power));
			// amplitude = (20 * Math.log(1 + Math.abs(power)) / Math.log(10));
			// double logMinFTPower = Math.abs(Math.log(minFTPower /
			// maxFTPower));
			// amplitude = (logMinFTPower - Math.abs(Math.log(FTPower /
			// maxFTPower))) /
			// logMinFTPower;
			// amplitude = (20 * Math.log(1 + Math.abs(FTPower)) /
			// Math.log(10));
			if (amplitude < 0)
				amplitude = 0.0;
		} else {
			/*
			 * if ((maxPower - minPower) <= 0) { amplitude = 0.0; } else { double minpow =
			 * minPower + (maxPower - minPower) * ((double) lowThres / 100.0); double maxpow
			 * = maxPower - (maxPower - minPower) * ((double) (100 - highThres) / 100.0); if
			 * (power > maxpow) { amplitude = 1.0; } else if (power < minpow) { amplitude =
			 * 0.0; } else { amplitude = (Math.sqrt(power - minpow) / Math.sqrt(maxpow -
			 * minpow)); } }
			 */
		}

		return amplitude;
	}

	public double getAvgAmplitude() {
		return avgAmplitude;
	}

	public ToneMapElement getElement(int index) {
		return elements[index];
	}

	public ToneMapElement[] getElements() {
		return elements;
	}

	public double getHighThreshold() {
		return highThreshold;
	}

	public void setHighThreshold(double highThreshold) {
		this.highThreshold = highThreshold;
	}

	public void setLowThreshold(double lowThreshold) {
		this.lowThreshold = lowThreshold;
	}

	public double getLowThres() {
		return lowThreshold;
	}

	public double getMaxAmplitude() {
		return maxAmplitude;
	}

	public double getMinAmplitude() {
		return minAmplitude;
	}

	public NoteStatus getNoteStatus() {
		return noteStatus;
	}

	public ToneTimeFrame cens() {
		return this;
	}

	public ToneTimeFrame chroma(int basePitch, int lowPitch, int highPitch) {
		ToneTimeFrame chromaTimeFrame = new ToneTimeFrame(this.timeSet.clone(),
				new PitchSet(basePitch, basePitch + 11));
		Map<Integer, ToneMapElement> chromaClassMap = new HashMap<>();
		for (int i = 0; i < elements.length; i++) {
			int note = pitchSet.getNote(i);
			if (note < lowPitch) {
				continue;
			}
			if (note > highPitch) {
				break;
			}
			int chromaClass = note % 12;
			ToneMapElement chromaElement = null;
			if (!chromaClassMap.containsKey(chromaClass)) {
				chromaElement = new ToneMapElement(chromaClass);
				chromaClassMap.put(chromaClass, chromaElement);
			} else {
				chromaElement = chromaClassMap.get(chromaClass);
			}
			chromaElement.amplitude += elements[i].amplitude;
		}
		ToneMapElement[] chromaElements = chromaTimeFrame.getElements();
		for (int i = 0; i < chromaElements.length; i++) {
			chromaElements[i] = chromaClassMap.get(i);
		}
		chromaTimeFrame.reset();
		return chromaTimeFrame;
	}

	public double[] getPitches() {
		double[] result = new double[elements.length];
		for (int i = 0; i < elements.length - 1; i++) {
			result[i] = pitchSet.getFreq(i + 1); // ?? !! pitchSet.getFreq(i)
		}
		return result;
	}

	public int getPitchHigh() {
		return pitchSet.getHighNote();
	}

	public int getPitchLow() {
		return pitchSet.getLowNote();
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}

	public Double getStartTime() {
		return getTimeSet().getStartTime();
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

	public ToneTimeFrame loadFFTSpectrum(FFTSpectrum fftSpectrum) {
		int elementIndex = 0;
		for (int i = 0; i < elements.length; i++) {
			elements[i].amplitude = AMPLITUDE_FLOOR;
		}
		double binStartFreq = pitchSet.getFreq(elementIndex);
		double binEndFreq = pitchSet.getFreq(elementIndex + 1);
		elements[elementIndex].amplitude = 0;
		double maxfft = 0, maxampl = 0;
		int maxffti = 0, maxampli = 0;
		float[] spectrum = fftSpectrum.getSpectrum();
		for (int i = 0; i < fftSpectrum.getSpectrum().length; i++) {
			double currentFreq = i * timeSet.getSampleRate() / (double) fftSpectrum.getWindowSize();
			if (currentFreq < binStartFreq) {
				continue;
			}
			while (currentFreq >= binEndFreq) {
				elementIndex++;
				if (elementIndex == elements.length) {
					break;
				}
				elements[elementIndex].amplitude = AMPLITUDE_FLOOR;
				binEndFreq = pitchSet.getFreq(elementIndex + 1);
				if (binEndFreq == -1)
					break;
			}
			if (elementIndex == elements.length) {
				break;
			}
			elements[elementIndex].amplitude += spectrum[i];
			if (maxfft < spectrum[i]) {
				maxfft = spectrum[i];
				maxffti = i;
			}
			if (maxampl < elements[elementIndex].amplitude) {
				maxampl = elements[elementIndex].amplitude;
				maxampli = elementIndex;
			}
		}
		System.out.println(">>>currentFreq: " + maxfft + ", " + maxampl + ", " + maxffti + ", " + maxampli + ", "
				+ pitchSet.getFreq(maxampli + 1));
		reset();
		return this;
	}

	public ToneTimeFrame reset() {
		maxAmplitude = 0;
		minAmplitude = 1000000;
		avgAmplitude = 0;
		long count = 0;

		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				if (elements[i].amplitude == -1) {
					elements[i].amplitude = AMPLITUDE_FLOOR;
				}
				if (elements[i].amplitude < AMPLITUDE_FLOOR) {
					elements[i].amplitude = AMPLITUDE_FLOOR;
				}
				elements[i].noteState = 0;
				// elements[i].noteListElement = null;
				avgAmplitude += elements[i].amplitude;
				count++;
				if (maxAmplitude < elements[i].amplitude)
					maxAmplitude = elements[i].amplitude;
				if (minAmplitude > elements[i].amplitude)
					minAmplitude = elements[i].amplitude;
			}
		}
		avgAmplitude = avgAmplitude / count;
		return this;
	}

	public ToneTimeFrame square() {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				elements[i].amplitude = Math.pow(elements[i].amplitude, 2);
			}
		}
		reset();
		return this;
	}

	public ToneTimeFrame lowThreshold(double threshold, double value) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				if (elements[i].amplitude < threshold) {
					elements[i].amplitude = value;
				}
			}
		}
		reset();
		return this;
	}

	static double log(double x, int base) {
		return (Math.log(x) / Math.log(base));
	}

	public ToneTimeFrame decibel(double base) {
		double highThreshold = (float) (20.0 * Math.log(getHighThreshold() / base));
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				double value = elements[i].amplitude > base ? elements[i].amplitude : base;
				elements[i].amplitude = (float) (20.0 * Math.log(value / base));
				if (elements[i].amplitude < base) {
					elements[i].amplitude = base;
				}
			}
		}
		normalise(highThreshold);
		return this;
	}

	public ToneTimeFrame normaliseEuclidian(double threshold) {
		double scale = 0;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				scale += value * value;
			}
		}
		if (scale > threshold) {
			scale = 1 / Math.sqrt(scale);
			for (int i = 0; i < elements.length; i++) {
				elements[i].amplitude *= scale;
			}
		} else {
			for (int i = 0; i < elements.length; i++) {
				elements[i].amplitude = 0.1 / (double) elements.length;
			}
		}
		return this;
	}

	public ToneTimeFrame chromaQuantize() {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value > 0.4) {
					value = 4.0;
				} else if (value > 0.2) {
					value = 3.0;
				} else if (value > 0.1) {
					value = 2.0;
				} else if (value > 0.05) {
					value = 1.0;
				} else {
					value = 0;
				}
				elements[i].amplitude = value;
			}
		}
		reset();
		setHighThreshold(4.0);
		setLowThreshold(0.5);
		return this;
	}

	public ToneTimeFrame smoothMedian(ToneMap sourceToneMap, int factor, int sequence) {
		// collect previous frames
		List<ToneTimeFrame> frames = new ArrayList<>();
		ToneTimeFrame tf = sourceToneMap.getTimeFrame(sequence);
		int i = factor;
		while (tf != null && i > 1) {
			frames.add(tf);
			tf = sourceToneMap.getPreviousTimeFrame(tf.getStartTime());
			i--;
		}
		if (frames.size() > 1) {
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				if (elements[elementIndex] != null) {
					elements[elementIndex].amplitude = smoothMedian(frames, elementIndex);
				}
			}
			reset();
		}
		return this;
	}

	private double smoothMedian(List<ToneTimeFrame> frames, int elementIndex) {
		List<Double> amplitudes = new ArrayList<>();
		for (ToneTimeFrame frame : frames) {
			amplitudes.add(frame.elements[elementIndex].amplitude);
		}
		Collections.sort(amplitudes);
		int size = amplitudes.size();
		if (size % 2 != 0) {
			return amplitudes.get(size / 2);
		} else {
			return (amplitudes.get((size - 1) / 2) + amplitudes.get(size / 2)) / 2.0;
		}
	}

	public ToneTimeFrame downSample(ToneMap toneMap, int factor, int sequence) {
		List<ToneTimeFrame> frames = new ArrayList<>();
		ToneTimeFrame tf = toneMap.getTimeFrame(sequence);
		ToneTimeFrame tfEnd = tf;
		ToneTimeFrame tfStart = tf;
		int i = factor;
		while (tf != null && i > 0) {
			frames.add(tf);
			tf = toneMap.getPreviousTimeFrame(tf.getStartTime());
			if (tf != null) {
				tfStart = tf;
			}
			i--;
		}
		if (frames.size() > 1) {
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				if (elements[elementIndex] != null) {
					elements[elementIndex].amplitude = downSample(frames, elementIndex);
				}
			}

			for (ToneTimeFrame frame : frames) {
				toneMap.deleteTimeFrame(frame.getStartTime());
			}
			timeSet = new TimeSet(tfStart.getTimeSet().getStartTime(), tfEnd.getTimeSet().getEndTime(),
					timeSet.getSampleRate(), timeSet.getSampleTimeSize());
			toneMap.addTimeFrame(this);
			reset();
		}
		return this;
	}

	private double downSample(List<ToneTimeFrame> frames, int elementIndex) {
		double amplitude = 0;
		for (ToneTimeFrame frame : frames) {
			amplitude += frame.elements[elementIndex].amplitude;
		}
		return amplitude;
	}

	public void hpsPercussionMedian(int hpsPercussionMedianFactor, boolean hpsMedianSwitch) {

		ToneMapElement[] hpsElements = getElements();
		for (int i = 0; i < elements.length; i++) {
			hpsElements[i] = elements[i].clone();
		}

		int startIndex = 0, endIndex = 0;

		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			startIndex = elementIndex >= hpsPercussionMedianFactor / 2 ? elementIndex - hpsPercussionMedianFactor / 2
					: 0;
			endIndex = elementIndex + hpsPercussionMedianFactor / 2 < elements.length
					? elementIndex + hpsPercussionMedianFactor / 2
					: elements.length - 1;
			List<ToneMapElement> subElements = new ArrayList<>();

			double sum = 0;
			for (int i = 0, subElementIndex = startIndex; subElementIndex <= endIndex; i++, subElementIndex++) {
				subElements.add(elements[subElementIndex]);
				sum += elements[subElementIndex].amplitude;
			}

			if (hpsMedianSwitch) {
				List<ToneMapElement> sortedElements = subElements.stream()
						.sorted(Comparator.comparingDouble(tm -> tm.amplitude)).collect(Collectors.toList());

				ToneMapElement median = sortedElements.size() % 2 == 0
						? sortedElements.get(sortedElements.size() / 2 - 1)
						: sortedElements.get(sortedElements.size() / 2);

				hpsElements[elementIndex].amplitude = median.amplitude;
			} else {
				hpsElements[elementIndex].amplitude = sum / subElements.size();
			}
		}

		for (int i = 0; i < elements.length; i++) {
			elements[i] = hpsElements[i];
		}

		reset();

	}

	public void hpsHarmonicMedian(ToneMap sourceToneMap, ToneMap hpsToneMap, int hpsHarmonicMedianFactor,
			boolean hpsMedianSwitch) {

		System.out.println(">>hpsHarmonicMedianFactor: " + hpsHarmonicMedianFactor + ", " + getStartTime());

		List<ToneTimeFrame> sourceFrames = new ArrayList<>();
		ToneTimeFrame stf = sourceToneMap.getTimeFrame();
		int i = hpsHarmonicMedianFactor;
		while (stf != null && i > 0) {
			sourceFrames.add(stf);
			stf = sourceToneMap.getPreviousTimeFrame(stf.getStartTime());
			i--;
		}

		List<ToneTimeFrame> hpsFrames = new ArrayList<>();
		ToneTimeFrame htf = this;
		i = hpsHarmonicMedianFactor;
		while (htf != null && i > hpsHarmonicMedianFactor / 2) {
			hpsFrames.add(htf);
			htf = hpsToneMap.getPreviousTimeFrame(htf.getStartTime());
			i--;
		}

		if (sourceFrames.size() > 1) {
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				if (elements[elementIndex] != null) {
					List<ToneMapElement> subElements = new ArrayList<>();
					double sum = 0;
					for (ToneTimeFrame subFrame : sourceFrames) {
						subElements.add(subFrame.elements[elementIndex]);
						sum += subFrame.elements[elementIndex].amplitude;
					}

					if (hpsMedianSwitch) {
						List<ToneMapElement> sortedElements = subElements.stream()
								.sorted(Comparator.comparingDouble(tm -> tm.amplitude)).collect(Collectors.toList());
						ToneMapElement median = sortedElements.size() % 2 == 0
								? sortedElements.get(sortedElements.size() / 2 - 1)
								: sortedElements.get(sortedElements.size() / 2);
						for (ToneTimeFrame subFrame : hpsFrames) {
							hpsFrames.get(0).elements[elementIndex].amplitude = median.amplitude;
						}
					} else {
						hpsFrames.get(0).elements[elementIndex].amplitude = sum / sourceFrames.size(); // median.amplitude;
					}
				}
			}
			hpsFrames.get(0).reset();

		}

	}

	public void hpsMask(ToneTimeFrame hpsHarmonicTimeFrame, ToneTimeFrame hpsPercussionTimeFrame,
			double hpsHarmonicWeighting, double hpsPercussionWeighting) {

		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement hpsHarmonicElement = hpsHarmonicTimeFrame.elements[elementIndex];
			ToneMapElement hpsPercussionElement = hpsPercussionTimeFrame.elements[elementIndex];
			elements[elementIndex].amplitude = (hpsHarmonicWeighting * hpsHarmonicElement.amplitude)
					/ (hpsHarmonicWeighting + hpsPercussionWeighting)
					+ (hpsPercussionWeighting * hpsPercussionElement.amplitude)
							/ (hpsHarmonicWeighting + hpsPercussionWeighting);

		}
		reset();
	}

}
