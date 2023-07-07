package jomu.instrument.workspace.tonemap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ToneTimeFrame implements Serializable {

	private static final int OCTAVE_LENGTH = 12;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = Logger.getLogger(ToneTimeFrame.class.getName());

	public final static double AMPLITUDE_FLOOR = 1E-7;

	private static AdaptiveWhitenConfig[] adaptiveWhitenConfigs = new AdaptiveWhitenConfig[] {
			new AdaptiveWhitenConfig(0, 23, 50, 1.0, 1.0, 1.0), new AdaptiveWhitenConfig(24, 35, 50, 1.0, 1.0, 1.0),
			new AdaptiveWhitenConfig(26, 47, 50, 1.0, 1.0, 1.0), new AdaptiveWhitenConfig(48, 59, 50, 1.0, 1.0, 1.0),
			new AdaptiveWhitenConfig(60, 71, 40, 1.0, 1.0, 1.0), new AdaptiveWhitenConfig(72, 83, 30, 1.0, 1.0, 1.0),
			new AdaptiveWhitenConfig(84, 95, 12, 1.0, 1.0, 1.0), new AdaptiveWhitenConfig(96, 107, 12, 1.0, 1.0, 1.0),
			new AdaptiveWhitenConfig(108, 119, 12, 1.0, 1.0, 1.0),
			new AdaptiveWhitenConfig(120, 1000, 12, 1.0, 1.0, 1.0), };

	double avgAmplitude = AMPLITUDE_FLOOR;

	ToneMapElement[] elements;

	double highThreshold = 100;
	double lowThreshold = 0;
	double maxAmplitude;
	int maxPitch;
	double minAmplitude;
	NoteStatus noteStatus;
	PitchSet pitchSet;
	double rmsPower = 0;
	double rawRmsPower = 0;
	double spectralFlux = 0;
	double rawSpectralFlux = 0;
	double totalAmplitude = 0;
	double totalRawAmplitude;
	int spectralMean = -1;
	boolean isSilent = true;
	ToneMapStatistics statistics = new ToneMapStatistics();
	Map<Integer, ToneMapStatistics> statisticsBands = new HashMap<>();

	TimeSet timeSet;
	TreeSet<ChordNote> chordNotes = new TreeSet<>();
	ChordListElement chordListElement = null;
	OnsetElement onsetElement = null;

	Map<String, ChordListElement> chordsMap = new HashMap<String, ChordListElement>();
	Map<String, BeatListElement> beatsMap = new HashMap<String, BeatListElement>();
	double beatAmplitude = AMPLITUDE_FLOOR;

	transient ToneMap toneMap;

	static class AdaptiveWhitenConfig {
		public int lowNote;
		public int highNote;
		public double attackFactor;
		public double decayFactor;
		public double thresholdFactor;
		public int range;

		public AdaptiveWhitenConfig(int lowNote, int highNote, int range, double attackFactor, double decayFactor,
				double thresholdFactor) {
			this.lowNote = lowNote;
			this.highNote = highNote;
			this.range = range;
			this.attackFactor = attackFactor;
			this.decayFactor = decayFactor;
			this.thresholdFactor = thresholdFactor;
		}
	};

	public static AdaptiveWhitenConfig getAdaptiveWhitenConfig(int note) {
		for (AdaptiveWhitenConfig config : adaptiveWhitenConfigs) {
			if (config.lowNote <= note && config.highNote > note) {
				return config;
			}
		}
		return adaptiveWhitenConfigs[0];
	}

	public ToneTimeFrame(ToneMap toneMap, TimeSet timeSet, PitchSet pitchSet) {
		this.toneMap = toneMap;
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
		ToneTimeFrame copy = new ToneTimeFrame(this.toneMap, this.timeSet.clone(), this.pitchSet.clone());
		copy.noteStatus = this.noteStatus.clone();
		for (ChordNote chord : chordNotes) {
			copy.chordNotes.add(chord.clone());
		}

		ToneMapElement[] copyElements = copy.getElements();
		for (int i = 0; i < elements.length; i++) {
			copyElements[i] = elements[i].clone();
		}
		copy.reset();
		copy.setLowThreshold(this.getLowThres());
		copy.setHighThreshold(this.getHighThreshold());
		return copy;
	}

	public ToneMap getToneMap() {
		return toneMap;
	}

	public void setToneMap(ToneMap toneMap) {
		this.toneMap = toneMap;
	}

	public double getRmsPower() {
		return rmsPower;
	}

	public double getRawRmsPower() {
		return rawRmsPower;
	}

	public OnsetElement getOnsetElement() {
		return onsetElement;
	}

	public void setOnsetElement(OnsetElement onsetElement) {
		this.onsetElement = onsetElement;
	}

	public double getSpectralFlux() {
		return spectralFlux;
	}

	public double getRawSpectralFlux() {
		return rawSpectralFlux;
	}

	public double getTotalAmplitude() {
		return totalAmplitude;
	}

	public int getSpectralCentroid() {
		int spectralCentroid = -1;
		if (!isSilent && totalAmplitude > 0) {
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				spectralCentroid += elementIndex * (elements[elementIndex].amplitude / totalAmplitude);
			}
		}
		return spectralCentroid;
	}

	public int getSpectralMean() {
		return spectralMean;
	}

	public double getBeatAmplitude() {
		return beatAmplitude;
	}

	public void setBeatAmplitude(double beatAmplitude) {
		this.beatAmplitude = beatAmplitude;
	}

	public int getMaxPitch() {
		return maxPitch;
	}

	public void setMaxPitch(int maxPitch) {
		this.maxPitch = maxPitch;
	}

	public void compress(float factor) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null && elements[i].amplitude > AMPLITUDE_FLOOR) {
				elements[i].amplitude = (float) Math.log10(1 + (factor * elements[i].amplitude));
			}
		}
	}

	public void normalise(double highThreshold) {
		assert highThreshold > AMPLITUDE_FLOOR;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null && elements[i].amplitude > AMPLITUDE_FLOOR) {
				elements[i].amplitude = elements[i].amplitude / highThreshold;
			}
		}
		setHighThreshold(1.0);
		reset();
	}

	public void scale(double lowThreshold, double highThreshold, boolean useLog) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].amplitude > highThreshold) {
				elements[i].amplitude = 1.0;
			} else if (elements[i].amplitude <= lowThreshold) {
				elements[i].amplitude = AMPLITUDE_FLOOR;
			} else {
				if (useLog) {
					elements[i].amplitude = Math
							.log1p((elements[i].amplitude - lowThreshold) / (highThreshold - lowThreshold));
				} else {
					elements[i].amplitude = (elements[i].amplitude - lowThreshold) / (highThreshold - lowThreshold);
				}
			}
		}
		reset();
	}

	public void filter(double minFrequency, double maxFrequency) {
		for (int i = 0; i < elements.length; i++) {
			double freq = pitchSet.getFreq(i);
			if (freq < minFrequency || freq > maxFrequency) {
				elements[i].amplitude = AMPLITUDE_FLOOR;
			}
		}
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

			if (binEndIndex > binStartIndex) {
				float partialValue = (float) (elements[i].amplitude) / (binEndIndex - binStartIndex);
				for (int j = 0; j < (binEndIndex - binStartIndex); j++) {
					if (binStartIndex + j < spec.length) {
						spec[binStartIndex + j] += partialValue;
					}
				}
			} else {
				if (binStartIndex < spec.length) {
					spec[binStartIndex] += elements[i].amplitude;
				}
			}
		}
		FFTSpectrum fftSpectrum = new FFTSpectrum(timeSet.getSampleRate(), windowSize, spec);
		return fftSpectrum;
	}

	public double getAvgAmplitude() {
		return avgAmplitude;
	}

	public ToneMapElement getElement(int index) {
		if (index >= elements.length) {
			return elements[elements.length - 1];
		}
		if (index < 0) {
			return elements[0];
		}
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

	public ToneTimeFrame chroma(int basePitch, int lowPitch, int highPitch, boolean harmonics) {
		ToneTimeFrame chromaTimeFrame = new ToneTimeFrame(this.toneMap, this.timeSet.clone(),
				new PitchSet(basePitch, basePitch + OCTAVE_LENGTH - 1));
		Map<Integer, ToneMapElement> chromaClassMap = new HashMap<>();
		for (int i = 0; i < elements.length; i++) {
			int note = pitchSet.getNote(i);
			if (note < lowPitch) {
				continue;
			}
			if (note > highPitch) {
				break;
			}
			int chromaClass = note % OCTAVE_LENGTH;
			ToneMapElement chromaElement = null;
			if (!chromaClassMap.containsKey(chromaClass)) {
				chromaElement = new ToneMapElement(chromaClass);
				chromaClassMap.put(chromaClass, chromaElement);
			} else {
				chromaElement = chromaClassMap.get(chromaClass);
			}
			if (elements[i].amplitude > 0.0001) {
				chromaElement.amplitude += elements[i].amplitude;
			}
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
			result[i] = pitchSet.getFreq(i + 1);
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

	public Double getEndTime() {
		return getTimeSet().getEndTime();
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

	public ToneTimeFrame loadFFTSpectrum(FFTSpectrum fftSpectrum) {
		int elementIndex = 1;
		for (int i = 0; i < elements.length; i++) {
			elements[i].amplitude = AMPLITUDE_FLOOR;
		}
		double binStartFreq = pitchSet.getFreq(elementIndex - 1);
		double binEndFreq = pitchSet.getFreq(elementIndex);
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
				binEndFreq = pitchSet.getFreq(elementIndex);
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
		reset();
		return this;
	}

	public ToneMapStatistics getStatistics() {
		return statistics;
	}

	public Map<Integer, ToneMapStatistics> getStatisticsBands() {
		return statisticsBands;
	}

	public ToneTimeFrame reset() {
		maxPitch = 0;
		maxAmplitude = 0;
		minAmplitude = 1000000;
		avgAmplitude = 0;
		totalAmplitude = 0;
		totalRawAmplitude = 0;
		double sumRawSquareAmplitude = 0;
		double sumSquareAmplitude = 0;
		long count = 0;

		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				if (elements[i].amplitude == -1) {
					elements[i].amplitude = AMPLITUDE_FLOOR;
				}
				if (elements[i].amplitude < AMPLITUDE_FLOOR) {
					elements[i].amplitude = AMPLITUDE_FLOOR;
				}
				avgAmplitude += elements[i].amplitude;
				count++;
				if (maxAmplitude < elements[i].amplitude) {
					maxAmplitude = elements[i].amplitude;
					maxPitch = i;
				}
				if (minAmplitude > elements[i].amplitude) {
					minAmplitude = elements[i].amplitude;
				}
				totalAmplitude += elements[i].amplitude;
				totalRawAmplitude += elements[i].microTones.getPower();
				sumRawSquareAmplitude += elements[i].microTones.getPower() * elements[i].microTones.getPower();
				sumSquareAmplitude += elements[i].amplitude * elements[i].amplitude;
			}
		}

		int bandIndex = pitchSet.getNote(0);
		int elementIndex = 0;
		int note = 0;
		double amplitude = 0;
		ToneMapStatistics statisticsBand = null;
		int bandCount = 0;
		double bandSum = 0;
		for (elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			if (elements[elementIndex] != null) {
				ToneMapElement element = elements[elementIndex];
				note = pitchSet.getNote(element.pitchIndex);
				amplitude = element.amplitude;
				bandCount++;
				bandSum += amplitude;
				if (!statisticsBands.containsKey(bandIndex)) {
					statisticsBand = new ToneMapStatistics();
					statisticsBands.put(bandIndex, statisticsBand);
				} else {
					statisticsBand = statisticsBands.get(bandIndex);
				}

				if ((note - bandIndex) < 24) {
					if (statisticsBand.max < amplitude) {
						statisticsBand.max = amplitude;
					}
					if (statisticsBand.min > amplitude) {
						statisticsBand.min = amplitude;
					}
					statisticsBand.mean = bandSum / bandCount;
				} else {
					if (statisticsBand.max < amplitude) {
						statisticsBand.max = amplitude;
					}
					if (statisticsBand.min > amplitude) {
						statisticsBand.min = amplitude;
					}
					statisticsBand.mean = bandSum / bandCount;
					statisticsBand.sum = bandSum;
					bandIndex += OCTAVE_LENGTH;
					elementIndex -= OCTAVE_LENGTH;
					bandCount = 0;
					bandSum = 0;

				}
			}
		}

		if (maxAmplitude <= AMPLITUDE_FLOOR) {
			isSilent = true;
			avgAmplitude = AMPLITUDE_FLOOR;
			rawRmsPower = 0;
			rmsPower = 0;
			spectralMean = -1;
			spectralFlux = 0;
		} else {
			isSilent = false;
			avgAmplitude = avgAmplitude / count;
			rawRmsPower = Math.sqrt(sumRawSquareAmplitude / count);
			rmsPower = Math.sqrt(sumSquareAmplitude / count);
			List<Integer> meanFreqs = new ArrayList<Integer>();
			double minAmp = Double.MAX_VALUE;
			double maxAmp = 0;
			int minIndex = 0;
			int maxIndex = 0;
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].amplitude == avgAmplitude) {
					meanFreqs.add(i);
				} else {
					if (elements[i].amplitude > avgAmplitude && elements[i].amplitude < minAmp) {
						minIndex = i;
						minAmp = elements[i].amplitude;
					}
					if (elements[i].amplitude < avgAmplitude && elements[i].amplitude > maxAmp) {
						maxIndex = i;
						maxAmp = elements[i].amplitude;
					}
				}
			}
			if (meanFreqs.isEmpty()) {
				if (maxIndex > 0 && minIndex > 0) {
					spectralMean = (maxIndex + minIndex) / 2;
				} else {
					spectralMean = minIndex;
				}
			} else {
				Collections.sort(meanFreqs);
				if (meanFreqs.size() % 2 == 0 && meanFreqs.size() > 1) {
					int lowMean = meanFreqs.size() / 2;
					int highMean = meanFreqs.size() / 2 + 1;
					spectralMean = meanFreqs.get((highMean + lowMean) / 2);
				} else {
					spectralMean = meanFreqs.get(meanFreqs.size() / 2);
				}
			}
		}

		statistics.max = maxAmplitude;
		statistics.min = minAmplitude;
		statistics.mean = avgAmplitude;
		statistics.sum = totalAmplitude;

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

	public ToneTimeFrame calibrate(ToneMap toneMap, CalibrationMap cm, double calibrateRange, boolean calibrateFuture,
			double lowThreshold, boolean keepPeaks) {
		boolean hasPower = false;
		double power = cm.getPower(getStartTime());
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				if (elements[i].amplitude <= lowThreshold || power < lowThreshold) {
					elements[i].amplitude = AMPLITUDE_FLOOR;
					if (!keepPeaks) {
						elements[i].isPeak = false;
					}
				} else {
					hasPower = true;
				}
			}
		}
		reset();
		if (hasPower) {
			ToneTimeFrame tf = this;
			double maxAmp = getMaxAmplitude();
			if (toneMap.getAmplitudeThreshold() < maxAmp) {
				toneMap.setAmplitudeThreshold(maxAmp);
			}
			double maxAmpTime = tf.getStartTime();
			double firstTime = getStartTime() - calibrateRange / 2;
			double lastTime = tf.getStartTime() + calibrateRange / 2;
			while (tf != null && tf.getStartTime() >= firstTime) {
				if (maxAmp < toneMap.getAmplitudeThreshold()) {
					maxAmp = toneMap.getAmplitudeThreshold();
					maxAmpTime = tf.getStartTime();
				}
				tf = toneMap.getPreviousTimeFrame(tf.getStartTime());
				// if (tf != null) {
				// maxAmpTime = tf.getStartTime();
				// }
			}
			if (cm.getPower(maxAmpTime) > lowThreshold) {
				double maxFutureAmp = 0;
				double maxFuturePower = cm.getMaxPower(getStartTime(), lastTime);
				if (maxAmp > lowThreshold && calibrateFuture) {
					double maxPastPower = cm.getPower(maxAmpTime) > lowThreshold
							? cm.getPower(maxAmpTime)
							: lowThreshold;
					if (maxPastPower > 0) {
						maxFutureAmp = maxFuturePower * maxAmp / maxPastPower;
						if (maxAmp < maxFutureAmp) {
							maxAmp = maxFutureAmp;
						}
					}
				}
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] != null) {
						if (elements[i].amplitude > lowThreshold && maxAmp > lowThreshold) {
							elements[i].amplitude = elements[i].amplitude / maxAmp;
							if (elements[i].amplitude > 1.0) {
								elements[i].amplitude = 1.0;
							}
						} else {
							elements[i].amplitude = AMPLITUDE_FLOOR;
							if (!keepPeaks) {
								elements[i].isPeak = false;
							}
						}
					}
				}
				reset();
			}
		}
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

	public ToneTimeFrame normaliseEuclidian(double threshold, boolean ceiling) {
		double scale = 0;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				scale += value * value;
			}
		}
		if (scale > threshold) {
			scale = 1 / Math.sqrt(scale);
			double max = AMPLITUDE_FLOOR;
			for (int i = 0; i < elements.length; i++) {
				elements[i].amplitude *= scale;
				if (max < elements[i].amplitude) {
					max = elements[i].amplitude;
				}
			}
			if (ceiling) {
				for (int i = 0; i < elements.length; i++) {
					elements[i].amplitude = elements[i].amplitude / max;
				}
			}
		} else {
			for (int i = 0; i < elements.length; i++) {
				elements[i].amplitude = 0.1 / (double) elements.length;
			}
		}
		return this;
	}

	public ChordListElement getFrameChord(ToneTimeFrame originFrame) {
		ChordListElement chordListElement = null;
		TreeSet<ChordNote> cnotes = new TreeSet<>();
		for (int i = 0; i < elements.length; i++) {
			int index = i % OCTAVE_LENGTH;
			int note = pitchSet.getNote(i);
			if (elements[i] != null && elements[i].amplitude > 0.01) {
				ChordNote cn = new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 0);
				if (!cnotes.contains(cn)) {
					cnotes.add(cn);
				}
			}
		}

		Map<Integer, ToneMapElement> chromaClassMap = new HashMap<>();
		Map<Integer, Integer> chromaClassOctaveMaxMap = new HashMap<>();
		Map<Integer, Double> chromaClassOctaveAmpMap = new HashMap<>();
		double amp = 0;
		ToneMapElement[] originElements = originFrame.getElements();
		for (int i = 0; i < originElements.length; i++) {
			int note = originFrame.getPitchSet()
					.getNote(i);
			int chromaClass = note % OCTAVE_LENGTH;
			ToneMapElement chromaElement = null;
			int chromaOctaveMax = 0;
			if (!chromaClassMap.containsKey(chromaClass)) {
				chromaElement = new ToneMapElement(chromaClass);
				chromaClassMap.put(chromaClass, chromaElement);
				chromaOctaveMax = i / 12;
				amp = originElements[i].amplitude;
				chromaClassOctaveMaxMap.put(chromaClass, chromaOctaveMax);
				chromaClassOctaveAmpMap.put(chromaClass, amp);
			} else {
				chromaElement = chromaClassMap.get(chromaClass);
				chromaOctaveMax = i / 12;
				amp = originElements[i].amplitude;
				if (amp > chromaClassOctaveAmpMap.get(chromaClass)) {
					chromaClassOctaveMaxMap.put(chromaClass, chromaOctaveMax);
					chromaClassOctaveAmpMap.put(chromaClass, amp);
				}
			}
		}

		int maxOctave = 0;
		for (ChordNote cn : cnotes) {
			cn.octave = chromaClassOctaveMaxMap.get(cn.pitchClass);
			if (maxOctave < cn.octave) {
				maxOctave = cn.octave;
			}
		}
		for (ChordNote cn : cnotes) {
			if (cn.octave == 0) {
				cn.octave = maxOctave;
			}
		}

		if (cnotes.size() >= 2) {
			chordListElement = new ChordListElement(getStartTime(), getTimeSet().getEndTime(),
					cnotes.toArray(new ChordNote[cnotes.size()]));
		}
		return chordListElement;

	}

	public ToneTimeFrame chromaQuantize(double threshold) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value > 0.8) {
					value = 1.0;
				} else if (value > 0.4) {
					value = 0.75;
				} else if (value > 0.2) {
					value = 0.5;
				} else if (value > 0.1) {
					value = 0.25;
				} else if (value >= threshold) {
					value = 0.1;
				} else {
					value = AMPLITUDE_FLOOR;
				}
				elements[i].amplitude = value;
			}
		}
		reset();
		return this;
	}

	public ToneTimeFrame chromaChordify(double threshold, boolean sharpen) {
		chordNotes = new TreeSet<>();
		TreeSet<Integer> firstCandidates = new TreeSet<>();
		TreeSet<Integer> secondCandidates = new TreeSet<>();
		TreeSet<Integer> thirdCandidates = new TreeSet<>();
		TreeSet<Integer> fourthCandidates = new TreeSet<>();

		if (maxAmplitude < threshold) {
			return this;
		}

		for (int i = 0; i < elements.length; i++) {
			int index = i % OCTAVE_LENGTH;
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value == 1.0) {
					firstCandidates.add(index);
				}
			}
		}

		for (int i = 0; i < elements.length; i++) {
			int index = i % OCTAVE_LENGTH;
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value == 0.75) {
					secondCandidates.add(index);
				}
			}
		}

		for (int i = 0; i < elements.length; i++) {
			int index = i % OCTAVE_LENGTH;
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value <= 0.5 && value >= 0.25) {
					thirdCandidates.add(index);
				}
			}
		}

		for (int i = 0; i < elements.length; i++) {
			int index = i % OCTAVE_LENGTH;
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value == 0.25 || value == 0.1) {
					fourthCandidates.add(index);
				}
			}
		}

		if (censHasClusters(firstCandidates, 4)) {
			return this;
		}

		censRemoveClusters(firstCandidates, 3, true);

		Set<Integer> level1Pairs = censGetPairs(firstCandidates);
		firstCandidates.removeAll(level1Pairs);

		Set<Integer> level1Singles = censGetSingles(firstCandidates);

		int level1CandidateNumber = level1Pairs.size() / 2 + level1Singles.size();
		if (level1CandidateNumber >= 4) {
			for (int i = 0; i < elements.length; i++) {
				elements[i].amplitude = AMPLITUDE_FLOOR;
			}
			for (int i = 0; i < elements.length; i++) {
				int index = i % OCTAVE_LENGTH;
				if (elements[i] != null) {
					if (level1Singles.contains(index) || level1Pairs.contains(index)) {
						elements[i].amplitude = 1.0;
						int note = pitchSet.getNote(i);
						chordNotes.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 1));
					}
				}
			}
		} else {

			if (censHasClusters(secondCandidates, 4)) {
				if (level1CandidateNumber >= 3) {
					for (int i = 0; i < elements.length; i++) {
						elements[i].amplitude = AMPLITUDE_FLOOR;
					}
					for (int i = 0; i < elements.length; i++) {
						int index = i % OCTAVE_LENGTH;
						if (elements[i] != null) {
							if (level1Singles.contains(index) || level1Pairs.contains(index)) {
								elements[i].amplitude = 1.0;
								int note = pitchSet.getNote(i);
								chordNotes
										.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 1));
							}
						}
					}
				} else {
					return this;
				}
			}
			censRemoveClusters(secondCandidates, 3, true);

			Set<Integer> level2Pairs = censGetPairs(secondCandidates);
			secondCandidates.removeAll(level2Pairs);

			Set<Integer> level2Singles = censGetSingles(secondCandidates);
			int level2CandidateNumber = level2Pairs.size() / 2 + level2Singles.size();
			if (level2CandidateNumber + level1CandidateNumber >= 4) {
				for (int i = 0; i < elements.length; i++) {
					elements[i].amplitude = AMPLITUDE_FLOOR;
				}
				for (int i = 0; i < elements.length; i++) {
					int index = i % OCTAVE_LENGTH;
					if (elements[i] != null) {
						if (level1Singles.contains(index) || level1Pairs.contains(index)) {
							elements[i].amplitude = 1.0;
							int note = pitchSet.getNote(i);
							chordNotes.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 1));
						}
						if (level2Singles.contains(index) || level2Pairs.contains(index)) {
							elements[i].amplitude = 0.75;
							int note = pitchSet.getNote(i);
							chordNotes.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 2));
						}
					}
				}
			} else {

				if (censHasClusters(thirdCandidates, 4)) {
					for (int i = 0; i < elements.length; i++) {
						elements[i].amplitude = AMPLITUDE_FLOOR;
					}
					for (int i = 0; i < elements.length; i++) {
						int index = i % OCTAVE_LENGTH;
						if (elements[i] != null) {
							if (level1Singles.contains(index) || level1Pairs.contains(index)) {
								elements[i].amplitude = 1.0;
								int note = pitchSet.getNote(i);
								chordNotes
										.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 1));
							}
							if (level2Singles.contains(index) || level2Pairs.contains(index)) {
								elements[i].amplitude = 0.75;
								int note = pitchSet.getNote(i);
								chordNotes
										.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 2));
							}
						}
					}
				}
				censRemoveClusters(thirdCandidates, 3, true);

				Set<Integer> level3Pairs = censGetPairs(thirdCandidates);
				thirdCandidates.removeAll(level3Pairs);

				Set<Integer> level3Singles = censGetSingles(thirdCandidates);
				int level3CandidateNumber = level3Pairs.size() / 2 + level3Singles.size();
				if (level3CandidateNumber + level2CandidateNumber + level1CandidateNumber >= 2) {
					for (int i = 0; i < elements.length; i++) {
						elements[i].amplitude = AMPLITUDE_FLOOR;
					}
					int counter = 0;
					for (int i = 0; i < elements.length; i++) {
						int index = i % OCTAVE_LENGTH;
						if (elements[i] != null) {
							if (level1Singles.contains(index) || level1Pairs.contains(index)) {
								elements[i].amplitude = 1.0;
								int note = pitchSet.getNote(i);
								chordNotes
										.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 1));
								counter++;
							}
							if (level2Singles.contains(index) || level2Pairs.contains(index)) {
								counter++;
								elements[i].amplitude = 0.75;
								int note = pitchSet.getNote(i);
								chordNotes
										.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 2));
							}
							if (level3Singles.contains(index) || level3Pairs.contains(index)) {
								counter++;
								elements[i].amplitude = 0.5;
								int note = pitchSet.getNote(i);
								chordNotes
										.add(new ChordNote(i, index, elements[i].amplitude, note / OCTAVE_LENGTH, 3));
							}
						}
					}
				}
			}
		}

		if (getChord() != null && sharpen) {
			sharpenChord();
			reset();
		}
		return this;
	}

	public ToneTimeFrame chordNoteOctivate(ToneTimeFrame[] originFrames) {
		Map<Integer, ToneMapElement> chromaClassMap = new HashMap<>();
		Map<Integer, Integer> chromaClassOctaveMaxMap = new HashMap<>();
		Map<Integer, Double> chromaClassOctaveAmpMap = new HashMap<>();
		double amp = 0;
		for (ToneTimeFrame originFrame : originFrames) {
			ToneMapElement[] originElements = originFrame.getElements();
			for (int i = 0; i < originElements.length; i++) {
				int note = originFrame.getPitchSet()
						.getNote(i);
				int chromaClass = note % OCTAVE_LENGTH;
				ToneMapElement chromaElement = null;
				int chromaOctaveMax = 0;
				if (!chromaClassMap.containsKey(chromaClass)) {
					chromaElement = new ToneMapElement(chromaClass);
					chromaClassMap.put(chromaClass, chromaElement);
					chromaOctaveMax = i / 12;
					amp = originElements[i].amplitude;
					chromaClassOctaveMaxMap.put(chromaClass, chromaOctaveMax);
					chromaClassOctaveAmpMap.put(chromaClass, amp);
				} else {
					chromaElement = chromaClassMap.get(chromaClass);
					chromaOctaveMax = i / 12;
					amp = originElements[i].amplitude;
					if (amp > chromaClassOctaveAmpMap.get(chromaClass)) {
						chromaClassOctaveMaxMap.put(chromaClass, chromaOctaveMax);
						chromaClassOctaveAmpMap.put(chromaClass, amp);
					}
				}
			}
		}

		int maxOctave = 0;
		for (ChordNote cn : chordNotes) {
			cn.octave = chromaClassOctaveMaxMap.get(cn.pitchClass);
			if (maxOctave < cn.octave) {
				maxOctave = cn.octave;
			}
		}
		for (ChordNote cn : chordNotes) {
			if (cn.octave == 0) {
				cn.octave = maxOctave;
			}
		}

		reset();
		return this;

	}

	public void sharpenChord() {
		LOG.severe(">>Sharpen: " + getStartTime() + ", " + chordNotes.size());
		TreeSet<ChordNote> result = new TreeSet<>();
		ChordNote lastCandidate = null;
		int pass = 0;
		do {
			pass++;
			LOG.finer(">>Sharpen pass: " + pass);
			result.clear();
			lastCandidate = null;
			for (ChordNote candidate : chordNotes) {
				if (lastCandidate != null) {
					if (candidate.pitchClass - lastCandidate.pitchClass == 1) {
						result.add(lastCandidate);
						result.add(candidate);
						LOG.severe(">>Sharpen A semi: " + getStartTime() + " ," + lastCandidate + " ," + candidate);
						break;
					}
					if (candidate.pitchClass == 11 && chordNotes.first().pitchClass == 0) {
						result.add(chordNotes.first());
						result.add(candidate);
						LOG.severe(">>Sharpen B semi: " + getStartTime() + " ," + lastCandidate + " ," + candidate);
						break;
					}
				}
				lastCandidate = candidate;
			}
			if (result.size() > 0) {
				// clear pair
				if (result.first().amplitude >= result.last().amplitude) {
					chordNotes.remove(result.last());
					elements[result.last().index].amplitude = AMPLITUDE_FLOOR;
					LOG.severe(">>Sharpened chord note remove: " + result.last());
				} else {
					elements[result.first().index].amplitude = AMPLITUDE_FLOOR;
					chordNotes.remove(result.first());
					LOG.severe(">>Sharpened chord note remove: " + result.first());
				}
				chordListElement = new ChordListElement(getStartTime(), getTimeSet().getEndTime(),
						chordNotes.toArray(new ChordNote[chordNotes.size()]));
				LOG.severe(">>Sharpened chord: " + chordNotes.size() + ", " + chordNotes);
			}
		} while (result.size() > 0);
	}

	public void sharpenChord(ChordListElement cle) {
		TreeSet<ChordNote> cleNotes = cle.getChordNotes();
		TreeSet<ChordNote> result = new TreeSet<>();
		ChordNote lastCandidate = null;
		int pass = 10;
		do {
			result.clear();
			lastCandidate = null;
			for (ChordNote candidate : cleNotes) {
				if (lastCandidate != null) {
					if (candidate.pitchClass - lastCandidate.pitchClass == 1) {
						result.add(lastCandidate);
						result.add(candidate);
						break;
					}
					if (candidate.pitchClass == 11 && cleNotes.first().pitchClass == 0) {
						result.add(cleNotes.first());
						result.add(candidate);
						break;
					}
				}
				lastCandidate = candidate;
			}
			if (result.size() > 0) {
				if (result.first().amplitude >= result.last().amplitude) {
					cleNotes.remove(result.last());
					elements[result.last().index].amplitude = AMPLITUDE_FLOOR;
				} else {
					elements[result.first().index].amplitude = AMPLITUDE_FLOOR;
					cleNotes.remove(result.first());
				}
			}
			pass--;
		} while (result.size() > 0 && pass > 0);
	}

	private Set<Integer> censGetSingles(TreeSet<Integer> candidates) {
		TreeSet<Integer> result = new TreeSet<>();
		int lastCandidate = -1;
		for (Integer candidate : candidates) {
			if (lastCandidate > -1) {
				if ((candidate - lastCandidate) == 1) {
					result.remove(lastCandidate);
				}
				if (candidate == 11 && candidates.first() == 0) {
					result.remove(0);
				} else {
					result.add(candidate);
				}
			} else {
				result.add(candidate);
			}
			lastCandidate = candidate;
		}
		return result;
	}

	private Set<Integer> censGetPairs(TreeSet<Integer> candidates) {
		TreeSet<Integer> result = new TreeSet<>();
		int lastCandidate = -1;
		for (Integer candidate : candidates) {
			if (lastCandidate > -1) {
				if ((candidate - lastCandidate) == 1) {
					result.add(lastCandidate);
					result.add(candidate);
				}
				if (candidate == 11 && candidates.first() == 0) {
					result.add(0);
					result.add(11);
				}
			}
			lastCandidate = candidate;
		}
		return result;
	}

	private void censRemoveClusters(TreeSet<Integer> candidates, int size, boolean keepMedian) {
		Set<Integer> candidateSet = new TreeSet<>();
		Set<Integer> removeSet = new TreeSet<>();
		int lastCandidate = -1;
		for (Integer candidate : candidates) {
			if (lastCandidate > -1) {
				if ((candidate - lastCandidate) == 1) {
					candidateSet.add(candidate);
					if (candidateSet.size() == size) {
						for (int count = 0; count < candidateSet.size(); count++) {
							if (!keepMedian || count != size / 2) {
								removeSet.add(candidate - count);
							}
						}
						candidateSet.clear();
					} else if (candidate == 11) {
						for (Integer candidate2 : candidates) {
							candidateSet.add(candidate2);
							if (candidate2 == 0 || (candidate2 - lastCandidate) == 1) {
								if (candidateSet.size() == size) {
									for (int count = 0; count < candidateSet.size(); count++) {
										if (!keepMedian || count != size / 2) {
											removeSet.add(candidate - count);
										}
									}
									candidateSet.clear();
								}
							}
						}
					}
				} else {
					candidateSet.clear();
				}
			}
			lastCandidate = candidate;
		}
		for (Integer removeCandidate : removeSet) {
			candidates.remove(removeCandidate);
		}
		return;
	}

	private boolean censHasClusters(TreeSet<Integer> candidates, int size) {
		int lastCandidate = -1;
		int cluster = 0;
		for (Integer candidate : candidates) {
			if (lastCandidate > -1) {
				if ((candidate - lastCandidate) == 1) {
					cluster++;
					if (cluster == size) {
						return true;
					}
					if (candidate == 11) {
						for (Integer candidate2 : candidates) {
							if (candidate2 == 0 || (candidate2 - lastCandidate) == 1) {
								cluster++;
								if (cluster == size) {
									return true;
								}
							} else {
								return false;
							}
							lastCandidate = candidate2;
						}
						return false;
					}
				} else {
					cluster = 0;
				}
			}
			lastCandidate = candidate;
		}
		return false;
	}

	public ToneTimeFrame chromaCens(ToneMap sourceToneMap, ToneMap targetToneMap, ToneMap originToneMap,
			int chromaSmoothFactor, int sequence, boolean chromaChordifySwitch, double chromaChordifyThreshold,
			boolean chromaChordifySharpen) {
		// collect previous frames
		List<ToneTimeFrame> sourceFrames = new ArrayList<>();
		ToneTimeFrame stf = sourceToneMap.getTimeFrame(sequence);
		int i = chromaSmoothFactor;
		while (stf != null && i > 0) {
			sourceFrames.add(stf);
			stf = sourceToneMap.getPreviousTimeFrame(stf.getStartTime());
			i--;
		}
		List<ToneTimeFrame> targetFrames = new ArrayList<>();
		ToneTimeFrame ttf = targetToneMap.getTimeFrame(sequence);
		i = chromaSmoothFactor / 2 + chromaSmoothFactor % 2 + 1;
		while (ttf != null && i > 0) {
			targetFrames.add(ttf);
			ttf = targetToneMap.getPreviousTimeFrame(ttf.getStartTime());
			i--;
		}
		List<ToneTimeFrame> originFrames = new ArrayList<>();
		ToneTimeFrame otf = originToneMap.getTimeFrame(sequence);
		i = chromaSmoothFactor;
		while (otf != null && i > 0) {
			originFrames.add(otf);
			otf = originToneMap.getPreviousTimeFrame(otf.getStartTime());
			i--;
		}

		if (sourceFrames.size() > 1 && ttf != null) {
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				if (elements[elementIndex] != null) {
					double amplitude = smoothMedian(sourceFrames, elementIndex);
					for (ToneTimeFrame tf : targetFrames) {
						tf.elements[elementIndex].amplitude = amplitude;
					}
				}
			}
			for (ToneTimeFrame tf : targetFrames) {
				tf.reset();
				if (chromaChordifySwitch) {
					tf.chromaChordify(chromaChordifyThreshold, chromaChordifySharpen);
					tf.chordNoteOctivate(originFrames.toArray(new ToneTimeFrame[originFrames.size()]));
				}
				tf.reset();
			}
		} else if (stf != null) {
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				if (elements[elementIndex] != null) {
					elements[elementIndex].amplitude = stf.elements[elementIndex].amplitude;
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

	public ToneTimeFrame downSample(ToneMap sourceToneMap, ToneMap targetToneMap, int factor, int sequence) {
		List<ToneTimeFrame> frames = new ArrayList<>();
		ToneTimeFrame tf = sourceToneMap.getTimeFrame(sequence);
		ToneTimeFrame tfEnd = tf;
		ToneTimeFrame tfStart = tf;
		int i = factor;
		while (tf != null && i > 1) {
			frames.add(tf);
			tf = sourceToneMap.getPreviousTimeFrame(tf.getStartTime());
			if (tf != null) {
				tfStart = tf;
			}
			i--;
		}
		if (frames.size() > 2) {
			timeSet = new TimeSet(tfStart.getTimeSet()
					.getStartTime(),
					tfEnd.getTimeSet()
							.getEndTime(),
					timeSet.getSampleRate(), timeSet.getSampleTimeSize());

			pitchSet = new PitchSet(getPitchLow(), getPitchHigh());

			ToneTimeFrame ttf = new ToneTimeFrame(this.toneMap, timeSet, pitchSet);

			targetToneMap.addTimeFrame(ttf);

			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				ttf.elements[elementIndex].amplitude = downSample(frames, elementIndex);
			}
			ttf.noteStatus = tfStart.noteStatus.clone();
			ttf.reset();
			ttf.setLowThreshold(tfStart.getLowThres());
			ttf.setHighThreshold(tfStart.getHighThreshold());
		}
		return this;
	}

	private double downSample(List<ToneTimeFrame> frames, int elementIndex) {
		double amplitude = 0;
		for (ToneTimeFrame frame : frames) {
			amplitude += frame.elements[elementIndex].amplitude;
		}
		return amplitude / frames.size();
	}

	public ToneTimeFrame hpsPercussionMedian(int hpsPercussionMedianFactor, double lowThreshold) {

		ToneMapElement[] hpsElements = getElements();
		for (int i = 0; i < elements.length; i++) {
			hpsElements[i] = elements[i].clone();
		}

		int startIndex = 0, endIndex = 0;

		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			startIndex = elementIndex >= hpsPercussionMedianFactor / 2
					? elementIndex - hpsPercussionMedianFactor / 2
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

			hpsElements[elementIndex].amplitude = sum / subElements.size();
			if (hpsElements[elementIndex].amplitude <= lowThreshold) {
				hpsElements[elementIndex].amplitude = AMPLITUDE_FLOOR;
			}
		}

		for (int i = 0; i < elements.length; i++) {
			elements[i] = hpsElements[i];
		}

		reset();
		return this;
	}

	public ToneTimeFrame hpsHarmonicMedian(ToneMap sourceToneMap, int sequence, int hpsHarmonicMedianFactor,
			boolean hpsMedianSwitch) {

		LOG.finer(">>hpsHarmonicMedianFactor: " + hpsHarmonicMedianFactor + ", " + getStartTime());

		ToneMapElement[] hpsElements = getElements();
		for (int i = 0; i < elements.length; i++) {
			hpsElements[i] = elements[i].clone();
		}

		List<ToneTimeFrame> sourceFrames = new ArrayList<>();
		ToneTimeFrame stf = sourceToneMap.getTimeFrame(sequence);
		int i = hpsHarmonicMedianFactor;
		while (stf != null && i > 0) {
			sourceFrames.add(stf);
			stf = sourceToneMap.getPreviousTimeFrame(stf.getStartTime());
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
								.sorted(Comparator.comparingDouble(tm -> tm.amplitude))
								.collect(Collectors.toList());
						if (sortedElements.size() > 0) {
							if (sortedElements.size() == 1) {
								hpsElements[elementIndex].amplitude = sortedElements.get(0).amplitude;
							} else if (sortedElements.size() % 2 == 0) {
								ToneMapElement median1 = sortedElements.get(sortedElements.size() / 2 - 1);
								ToneMapElement median2 = sortedElements.get(sortedElements.size() / 2);
								hpsElements[elementIndex].amplitude = (median1.amplitude + median2.amplitude) / 2;
							} else {
								ToneMapElement median = sortedElements.get(sortedElements.size() / 2);
								hpsElements[elementIndex].amplitude = median.amplitude;
							}
						}
					} else {
						hpsElements[elementIndex].amplitude = sum / sourceFrames.size();
					}
				}
			}

			for (int j = 0; j < elements.length; j++) {
				elements[j] = hpsElements[j];
			}
			reset();
		}
		return this;
	}

	public ToneTimeFrame hpsMask(ToneTimeFrame hpsHarmonicTimeFrame, ToneTimeFrame hpsPercussionTimeFrame,
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
		return this;
	}

	public ToneTimeFrame hpsHarmonicMask(ToneTimeFrame harmonicTimeFrame, ToneTimeFrame percussionTimeFrame,
			double hpsMaskFactor) {
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			if (harmonicTimeFrame.elements[elementIndex].amplitude <= AMPLITUDE_FLOOR
					|| ((harmonicTimeFrame.elements[elementIndex].amplitude)
							/ (percussionTimeFrame.elements[elementIndex].amplitude
									+ AMPLITUDE_FLOOR)) < hpsMaskFactor) {
				elements[elementIndex].amplitude = 0;
			} else {
				elements[elementIndex].amplitude = 1.0;
			}
		}
		reset();
		return this;
	}

	public ToneTimeFrame hpsPercussionMask(ToneTimeFrame harmonicTimeFrame, ToneTimeFrame percussionTimeFrame,
			double hpsMaskFactor) {
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			if (percussionTimeFrame.elements[elementIndex].amplitude <= AMPLITUDE_FLOOR
					|| ((percussionTimeFrame.elements[elementIndex].amplitude + AMPLITUDE_FLOOR)
							/ (harmonicTimeFrame.elements[elementIndex].amplitude + AMPLITUDE_FLOOR)) < hpsMaskFactor) {
				elements[elementIndex].amplitude = 0;
			} else {
				elements[elementIndex].amplitude = 1.0;
			}
		}
		reset();
		return this;
	}

	public ToneTimeFrame mask(ToneTimeFrame maskTimeFrame, boolean maskMode) {
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			if (maskMode) {
				if (maskTimeFrame.getElement(elementIndex).amplitude > AMPLITUDE_FLOOR) {
					elements[elementIndex].amplitude = AMPLITUDE_FLOOR;
				}
			} else {
				if (maskTimeFrame.getElement(elementIndex).amplitude <= AMPLITUDE_FLOOR) {
					elements[elementIndex].amplitude = AMPLITUDE_FLOOR;
				}
			}
		}
		reset();
		return this;

	}

	public ToneTimeFrame onsetWhiten(ToneTimeFrame previousFrame, double onsetFactor) {
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			elements[elementIndex].amplitude = Math
					.max(((previousFrame.elements[elementIndex].amplitude * onsetFactor)
							+ (elements[elementIndex].amplitude * (1 - onsetFactor))),
							elements[elementIndex].amplitude);
		}
		reset();
		return this;
	}

	public ToneTimeFrame updateSpectralFlux(ToneTimeFrame previousFrame) {
		reset();
		spectralFlux = 0;
		if (previousFrame != null) {
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				double value = elements[elementIndex].amplitude / totalAmplitude
						- previousFrame.getElement(elementIndex).amplitude / previousFrame.getTotalAmplitude();
				spectralFlux += value * value;
			}
		}
		return this;
	}

	public ToneTimeFrame adaptiveWhiten(ToneMap controlMap, ToneTimeFrame previousFrame, double onsetFactor,
			double threshold, boolean compensate) {
		ToneTimeFrame controlFrame = controlMap.getTimeFrame();
		ToneTimeFrame previousControlFrame = controlMap.getPreviousTimeFrame(controlFrame.getStartTime());
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement toneMapElement = elements[elementIndex];
			int note = pitchSet.getNote(toneMapElement.getPitchIndex());
			AdaptiveWhitenConfig config = getAdaptiveWhitenConfig(note);
			double onsetAttackFactor = onsetFactor * config.attackFactor;
			double onsetDecayFactor = onsetFactor * config.decayFactor;
			double controlFactor = onsetAttackFactor;
			double onsetThresholdFactor = onsetFactor * config.thresholdFactor;
			int rangeStart = elementIndex - config.range / 2 > 0 ? elementIndex - config.range / 2 : 0;
			int rangeEnd = elementIndex + config.range / 2 <= elements.length
					? elementIndex + config.range / 2
					: elements.length;
			if (previousFrame != null
					&& previousFrame.getElements()[elementIndex].amplitude > toneMapElement.amplitude) {
				controlFactor = onsetDecayFactor;
			}
			double controlAmplitude;
			if (previousControlFrame != null) {
				double maxControlAmpl = 0;
				for (int controlElementIndex = rangeStart; controlElementIndex < rangeEnd; controlElementIndex++) {
					ToneMapElement controlElement = previousControlFrame.getElement(controlElementIndex);
					if (maxControlAmpl < controlElement.amplitude) {
						maxControlAmpl = controlElement.amplitude;
					}
				}
				if (compensate) {
					controlAmplitude = Math.max(
							((maxControlAmpl * controlFactor)
									+ (elements[elementIndex].amplitude * (1 - controlFactor))),
							Math.max(onsetThresholdFactor, elements[elementIndex].amplitude));
				} else {
					controlAmplitude = Math.max(((maxControlAmpl * controlFactor)),
							Math.max(onsetThresholdFactor, elements[elementIndex].amplitude));
				}
			} else {
				controlAmplitude = Math.max(onsetThresholdFactor, elements[elementIndex].amplitude);
			}
			elements[elementIndex].amplitude = elements[elementIndex].amplitude / controlAmplitude;
			controlFrame.getElement(elementIndex).amplitude = controlAmplitude;
		}
		reset();
		return this;
	}

	public ToneTimeFrame onsetEdge(ToneTimeFrame previousFrame, double onsetEdgeFactor) {
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			if (elements[elementIndex].amplitude <= AMPLITUDE_FLOOR
					|| ((elements[elementIndex].amplitude - previousFrame.elements[elementIndex].amplitude)
							/ elements[elementIndex].amplitude) < onsetEdgeFactor) {
				elements[elementIndex].amplitude = 0;
			}
		}
		reset();
		return this;
	}

	public ToneTimeFrame envelopeWhiten(ToneMap envelopeToneMap, double whitenThreshold, double decayWhitenFactor,
			double attackWhitenFactor, double lowThreshold) {
		LOG.finer(">>TTF envelopeWhiten: " + getStartTime() + ", " + whitenThreshold + ", " + decayWhitenFactor);
		reset();
		ToneTimeFrame envelopeFrame = envelopeToneMap.getTimeFrame(getStartTime());
		ToneTimeFrame previousFrame = envelopeToneMap.getPreviousTimeFrame(getStartTime());
		if (previousFrame != null) {
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				double currentPower = elements[elementIndex].amplitude;
				double previousPower = previousFrame.getElement(elementIndex).amplitude;
				if (previousPower > lowThreshold) {
					if (currentPower < previousPower
							&& (((previousPower - currentPower) / previousPower) > whitenThreshold)
							&& decayWhitenFactor > 0) {
						double decayWhitenValue = (previousFrame.getElement(elementIndex).amplitude
								- elements[elementIndex].amplitude) * decayWhitenFactor;
						if (elements[elementIndex].amplitude < previousFrame.getElement(elementIndex).amplitude
								- decayWhitenValue) {
							LOG.finer(">>TTF envelopeWhiten decayWhitenValue before: " + getStartTime() + ", "
									+ previousFrame.getStartTime() + ", " + elementIndex + ", " + decayWhitenValue
									+ ", "
									+ elements[elementIndex].amplitude + ", "
									+ previousFrame.getElement(elementIndex).amplitude);
							elements[elementIndex].amplitude = previousFrame.getElement(elementIndex).amplitude
									- decayWhitenValue;
							LOG.finer(">>TTF envelopeWhiten decayWhitenValue after: " + decayWhitenValue + ", "
									+ elements[elementIndex].amplitude);
						}
					} else if (currentPower > previousPower
							&& (((currentPower - previousPower) / previousPower) > whitenThreshold)
							&& attackWhitenFactor > 0) {
						double attackWhitenValue = (elements[elementIndex].amplitude
								- previousFrame.getElement(elementIndex).amplitude) * attackWhitenFactor;
						if (elements[elementIndex].amplitude > previousFrame.getElement(elementIndex).amplitude
								+ attackWhitenValue) {
							LOG.finer(">>ATTACK " + getStartTime() + ", " + elementIndex + ", " + attackWhitenValue);
							elements[elementIndex].amplitude = previousFrame.getElement(elementIndex).amplitude
									+ attackWhitenValue;
						}
					}
					envelopeFrame.getElement(elementIndex).amplitude = elements[elementIndex].amplitude;
				}
			}
			reset();
		}
		return this;
	}

	public ToneTimeFrame whiten(int centroid, double whitenFactor) {
		reset();
		LOG.finer(">>CQ WHITEN: " + getStartTime() + ", " + whitenFactor);

		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			if (element.amplitude > AMPLITUDE_FLOOR) {
				int note = pitchSet.getNote(element.pitchIndex);
				double whitening = 1 + ((double) ((note - centroid) * (note - centroid)) / (4.0 * whitenFactor));
				if (element.amplitude > 0.1) {
					LOG.finer(">>CQ WHITENED note : " + getStartTime() + ", " + note + ", " + whitening);
				}
				element.amplitude *= whitening;
			}
		}
		reset();
		return this;
	}

	public ToneTimeFrame whiten(double whitenFactor) {
		int centroidIndex = getSpectralCentroid();
		int centroid = pitchSet.getNote(centroidIndex);
		LOG.finer(">>CQ WHITEN centroid: " + centroid + ", " + getStartTime() + ", " + whitenFactor);
		return whiten(centroid, whitenFactor);
	}

	public int getNote(int pitchIndex) {
		return pitchSet.getNote(pitchIndex);
	}

	@Override
	public int hashCode() {
		return Objects.hash(pitchSet, timeSet);
	}

	@Override
	public String toString() {
		return "ToneTimeFrame [pitchSet=" + pitchSet + ", timeSet=" + timeSet + ", maxAmpl=" + maxAmplitude + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ToneTimeFrame other = (ToneTimeFrame) obj;
		return Objects.equals(pitchSet, other.pitchSet) && Objects.equals(timeSet, other.timeSet);
	}

	public void sharpen(double sharpenThreshold) {
		int troughIndex = 0;
		int peakIndex = 0;
		double lastAmplitude = -1;
		boolean downSlope = false;
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			double originalAmplitude = element.amplitude;
			if (originalAmplitude < (lastAmplitude * sharpenThreshold)) {
				if (!downSlope) {
					peakIndex = elementIndex > 0 ? elementIndex - 1 : elementIndex;
					downSlope = true;
				}
				// element.amplitude = originalAmplitude * 0.5 * originalAmplitude /
				// lastAmplitude;
				if (troughIndex > 0) {
					if (element.amplitude < elements[troughIndex].amplitude) {
						troughIndex = elementIndex;
					} else {
						element.amplitude = elements[troughIndex].amplitude;
					}
				} else {
					element.amplitude = AMPLITUDE_FLOOR;
				}
			}
			if (originalAmplitude > lastAmplitude) {
				if (downSlope) {
					troughIndex = elementIndex > 0 ? elementIndex - 1 : elementIndex;
					downSlope = false;
				}
				if (elementIndex > 0) {
					int lastElementIndex = elementIndex - 1;
					while (lastElementIndex > troughIndex
							&& elements[lastElementIndex].amplitude > elements[troughIndex].amplitude) {
						ToneMapElement lastElement = elements[lastElementIndex];
						// lastElement.amplitude = lastElement.amplitude * 0.5 * lastElement.amplitude /
						// element.amplitude;
						if (troughIndex > 0) {
							lastElement.amplitude = elements[troughIndex].amplitude;
						} else {
							lastElement.amplitude = AMPLITUDE_FLOOR;
						}

						lastElementIndex--;
					}
				}
			}
			lastAmplitude = originalAmplitude;
		}
	}

	public ChordListElement getChord() {
		if (chordListElement == null) {
			if (chordNotes.size() > 1) {
				chordListElement = new ChordListElement(getStartTime(), getTimeSet().getEndTime(),
						chordNotes.toArray(new ChordNote[chordNotes.size()]));
			}
		}
		return chordListElement;
	}

	public int getOctave() {
		int octave = pitchSet.getOctave(this.getSpectralCentroid());
		return octave;
	}

	public BeatListElement getBeat() {
		BeatListElement beatListElement = null;
		if (beatAmplitude > AMPLITUDE_FLOOR) {
			beatListElement = new BeatListElement(beatAmplitude, getStartTime(), getTimeSet().getEndTime());
		}
		return beatListElement;
	}

	public void integratePeaks(ToneTimeFrame sourceTimeFrame) {
		ToneMapElement[] ses = sourceTimeFrame.getElements();
		for (int elementIndex = 0; elementIndex < elements.length && elementIndex < ses.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			if (ses[elementIndex].isPeak) {
				element.amplitude = 1.0; // += ses[elementIndex].amplitude;
				element.isPeak = true;
			}
		}
		reset();
	}

	public void clear() {
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			element.amplitude = AMPLITUDE_FLOOR;
		}
		reset();
	}

	public void merge(ToneTimeFrame sourceTimeFrame) {
		ToneMapElement[] ses = sourceTimeFrame.getElements();
		for (int elementIndex = 0; elementIndex < elements.length && elementIndex < ses.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			element.amplitude += ses[elementIndex].amplitude;
		}
		reset();

	}

	public void merge(ToneMap toneMap, ToneTimeFrame sourceTimeFrame) {
		merge(sourceTimeFrame);
		ToneMapElement[] ses = sourceTimeFrame.getElements();
		for (int elementIndex = 0; elementIndex < elements.length && elementIndex < ses.length; elementIndex++) {
			ToneMapElement sourceElement = ses[elementIndex];
			ToneMapElement element = elements[elementIndex];
			if (sourceElement.noteListElement != null) {
				merge(toneMap, sourceElement.noteListElement);
			}
			element.amplitude += ses[elementIndex].amplitude;
		}
		reset();

	}

	public void mergeNotes(ToneMap toneMap, ToneTimeFrame sourceTimeFrame) {
		ToneMapElement[] ses = sourceTimeFrame.getElements();
		// LOG.finer(">>mergeNotes: " + getStartTime());
		for (int elementIndex = 0; elementIndex < elements.length && elementIndex < ses.length; elementIndex++) {
			ToneMapElement sourceElement = ses[elementIndex];
			if (sourceElement.noteListElement != null) {
				merge(toneMap, sourceElement.noteListElement);
			}
		}
		reset();

	}

	public void merge(ToneMap toneMap, NoteListElement sourceNoteListElement) {
		int elementIndex = sourceNoteListElement.pitchIndex;
		LOG.finer(">>TTF merge: " + elementIndex + ", " + getStartTime() + ", " + sourceNoteListElement.note + ", "
				+ sourceNoteListElement.startTime + ", " + sourceNoteListElement.endTime);
		double startTime = sourceNoteListElement.startTime / 1000;
		double endTime = sourceNoteListElement.endTime / 1000;
		NoteListElement newNoteListElement = sourceNoteListElement.clone();
		ToneTimeFrame ttf = toneMap.getPreviousTimeFrame(startTime);
		List<ToneTimeFrame> ttfs = new ArrayList<>();
		NoteListElement firstNoteListElement = null;
		NoteListElement lastNoteListElement = null;
		while (ttf != null && ttf.getStartTime() <= endTime) {
			ttfs.add(ttf);
			ToneMapElement element = ttf.getElement(elementIndex);
			if (element.noteListElement != null) {
				if (firstNoteListElement == null) {
					firstNoteListElement = element.noteListElement;
				} else {
					lastNoteListElement = element.noteListElement;
				}
			}
			ttf = toneMap.getNextTimeFrame(ttf.getStartTime());
		}
		if (firstNoteListElement != null && firstNoteListElement.equals(lastNoteListElement)
				&& firstNoteListElement.startTime == newNoteListElement.startTime
				&& firstNoteListElement.endTime == newNoteListElement.endTime) {
			ToneMapElement element = getElement(elementIndex);
			element.noteListElement = firstNoteListElement;
			LOG.finer(">>TTF merge Z: " + elementIndex + ", " + getStartTime() + ", " + sourceNoteListElement.note
					+ ", " + sourceNoteListElement.startTime + ", " + sourceNoteListElement.endTime);
			return;

		}

		if (firstNoteListElement == null || firstNoteListElement.endTime <= newNoteListElement.startTime) {
			LOG.finer(">>TTF merge A: " + elementIndex + ", " + getStartTime() + ", " + sourceNoteListElement.note
					+ ", " + sourceNoteListElement.startTime + ", " + sourceNoteListElement.endTime);
			ttf = toneMap.getTimeFrame(startTime);
			while (ttf != null && ttf.getStartTime() <= endTime) {
				ToneMapElement element = ttf.getElement(elementIndex);
				element.noteListElement = newNoteListElement;
				ttf = toneMap.getNextTimeFrame(ttf.getStartTime());
				LOG.finer(">>TTF merge B: " + elementIndex + ", " + getStartTime() + ", " + sourceNoteListElement.note
						+ ", " + sourceNoteListElement.startTime + ", " + sourceNoteListElement.endTime);
			}
		} else {
			LOG.finer(">>TTF merge X1: " + elementIndex + ", " + getStartTime() + ", " + sourceNoteListElement.note
					+ ", " + sourceNoteListElement.startTime + ", " + sourceNoteListElement.endTime);
			LOG.finer(">>TTF merge X2: " + elementIndex + ", " + getStartTime() + ", " + firstNoteListElement.note
					+ ", " + firstNoteListElement.startTime + ", " + firstNoteListElement.endTime);
			ttf = toneMap.getTimeFrame(firstNoteListElement.startTime / 1000);
			firstNoteListElement.endTime = newNoteListElement.endTime;
			firstNoteListElement.merge(newNoteListElement);
			newNoteListElement = firstNoteListElement;
			while (ttf != null && ttf.getStartTime() <= firstNoteListElement.endTime / 1000) {
				startTime = ttf.getStartTime();
				ToneMapElement element = ttf.getElement(elementIndex);
				element.noteListElement = newNoteListElement;
				ttf = toneMap.getNextTimeFrame(startTime);
			}
			LOG.finer(">>TTF merged X21: " + elementIndex + ", " + getStartTime() + ", " + newNoteListElement.note
					+ ", " + newNoteListElement.startTime + ", " + newNoteListElement.endTime);
		}
		if (lastNoteListElement != null) {
			LOG.finer(">>TTF merge X3: " + elementIndex + ", " + getStartTime() + ", " + lastNoteListElement.note
					+ ", " + lastNoteListElement.startTime + ", " + lastNoteListElement.endTime);
			if (lastNoteListElement.endTime > newNoteListElement.endTime) {
				newNoteListElement.endTime = lastNoteListElement.endTime;
			}
			while (ttf != null && ttf.getStartTime() <= (newNoteListElement.endTime / 1000)) {
				startTime = ttf.getStartTime();
				ToneMapElement element = ttf.getElement(elementIndex);
				if (element.noteListElement != null) {
					newNoteListElement.merge(element.noteListElement);
					element.noteListElement = newNoteListElement;
				} else {
					element.noteListElement = newNoteListElement;
				}
				ttf = toneMap.getNextTimeFrame(startTime);
			}
			LOG.finer(">>TTF merged X31: " + elementIndex + ", " + getStartTime() + ", " + newNoteListElement.note
					+ ", " + newNoteListElement.startTime + ", " + newNoteListElement.endTime);

		}
	}

	public void setChord(ToneTimeFrame sourceTimeFrame) {
		chordNotes.clear();
		ChordListElement chord = sourceTimeFrame.getChord();
		if (chord != null) {
			chordNotes.addAll(chord.getChordNotes());
		}
	}

	public void setChord(ChordListElement chord) {
		chordListElement = chord;
		chordNotes.clear();
		if (chord != null) {
			chordNotes.addAll(chord.getChordNotes());
		}
	}

	public boolean isSilent() {
		return isSilent;
	}

	public void putChordList(String source, ChordListElement chordList) {
		if (chordList != null) {
			chordsMap.put(source, chordList);
		}
	}

	public Optional<ChordListElement> getChordList(String source) {
		return Optional.ofNullable(chordsMap.get(source));
	}

	public void putBeat(String source, BeatListElement beat) {
		beatsMap.put(source, beat);
	}

	public Optional<BeatListElement> getBeat(String source) {
		return Optional.ofNullable(beatsMap.get(source));
	}

	public Map<String, ChordListElement> getChordsMap() {
		return chordsMap;
	}

	public void setChordNotesMap(Map<String, ChordListElement> chordsMap) {
		this.chordsMap = chordsMap;
	}

	public Map<String, BeatListElement> getBeatsMap() {
		return beatsMap;
	}

	public void setBeatsMap(Map<String, BeatListElement> beatsMap) {
		this.beatsMap = beatsMap;
	}

	public ToneTimeFrame onsetPeaks(ToneTimeFrame previousFrame, double onsetPeaksEdgeFactor, int onsetPeaksSweep,
			double onsetPeaksThreshold, double onsetFactor) {
		ToneTimeFrame sf = this;
		int i = onsetPeaksSweep;
		double totalAmp = 0;
		while (sf != null && i > 0) {
			totalAmp += sf.getTotalAmplitude();
			sf = sf.getToneMap()
					.getPreviousTimeFrame(sf.getStartTime());
			i--;
		}

		boolean isPeak = false;
		if (previousFrame.getOnsetElement() == null) {
			previousFrame.setOnsetElement(new OnsetElement(AMPLITUDE_FLOOR, false));
			if (totalAmp > onsetPeaksThreshold) {
				isPeak = true;
			}
			setOnsetElement(new OnsetElement(totalAmp, isPeak));
		} else {
			if (previousFrame.getOnsetElement().amplitude >= totalAmp) {
				totalAmp = Math.max(
						((previousFrame.getOnsetElement().amplitude * onsetFactor) + totalAmp * (1 - onsetFactor)),
						totalAmp);
				setOnsetElement(new OnsetElement(totalAmp, false));
			} else {
				if (totalAmp > 0 && totalAmp > onsetPeaksThreshold
						&& (((totalAmp - previousFrame.getOnsetElement().amplitude)
								/ totalAmp) > onsetPeaksEdgeFactor)) {
					isPeak = true;
					previousFrame.getOnsetElement().isPeak = false;
				}
				setOnsetElement(new OnsetElement(totalAmp, isPeak));
			}
		}
		reset();
		return this;
	}

}
