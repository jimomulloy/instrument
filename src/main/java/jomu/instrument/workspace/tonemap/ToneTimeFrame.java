package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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
	private int maxPitch;
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

	public int getMaxPitch() {
		return maxPitch;
	}

	public void setMaxPitch(int maxPitch) {
		this.maxPitch = maxPitch;
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

	public double getAvgAmplitude() {
		return avgAmplitude;
	}

	public ToneMapElement getElement(int index) {
		if (index >= elements.length) {
			System.out.println("!!erro");
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

	public ToneTimeFrame cens() {
		return this;
	}

	public ToneTimeFrame chroma(int basePitch, int lowPitch, int highPitch, boolean harmonics) {
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
		maxPitch = 0;
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
				if (maxAmplitude < elements[i].amplitude) {
					maxAmplitude = elements[i].amplitude;
					maxPitch = i;
				}
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

	public ToneTimeFrame chromaQuantize() {
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
				} else if (value > 0.05) {
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

	public ToneTimeFrame chromaChordify() {
		TreeSet<Integer> firstCandidates = new TreeSet<>();
		TreeSet<Integer> secondCandidates = new TreeSet<>();
		TreeSet<Integer> thirdCandidates = new TreeSet<>();
		TreeSet<Integer> fourthCandidates = new TreeSet<>();

		System.out.println(">>CHORDIFY!!: " + this.getStartTime() + ", " + maxAmplitude);
		if (maxAmplitude < 0.25) {
			return this;
		}

		for (int i = 0; i < elements.length; i++) {
			int index = i % 12;
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value == 1.0) {
					firstCandidates.add(index);
					System.out.println(">>CHORDIFY!!: ADD 1 index: " + index);
				}
			}
		}

		for (int i = 0; i < elements.length; i++) {
			int index = i % 12;
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value == 0.75) {
					secondCandidates.add(index);
					System.out.println(">>CHORDIFY!!: ADD 2 index: " + index);
				}
			}
		}

		for (int i = 0; i < elements.length; i++) {
			int index = i % 12;
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value == 0.5) {
					thirdCandidates.add(index);
					System.out.println(">>CHORDIFY!!: ADD 3 index: " + index);
				}
			}
		}

		for (int i = 0; i < elements.length; i++) {
			int index = i % 12;
			if (elements[i] != null) {
				double value = elements[i].amplitude;
				if (value == 0.25) {
					fourthCandidates.add(index);
					System.out.println(">>CHORDIFY!!: ADD 4 index: " + index);
				}
			}
		}

		System.out.println(">>CHORDIFY!! 1: " + firstCandidates.size());

		if (censHasClusters(firstCandidates, 4)) {
			return this;
		}

		censRemoveClusters(firstCandidates, 3);

		Set<Integer> level1Pairs = censGetPairs(firstCandidates);
		firstCandidates.removeAll(level1Pairs);

		Set<Integer> level1Singles = censGetSingles(firstCandidates);

		int level1CandidateNumber = level1Singles.size() / 2 + level1Singles.size();
		if (level1CandidateNumber >= 4) {
			for (int i = 0; i < elements.length; i++) {
				elements[i].amplitude = AMPLITUDE_FLOOR;
			}
			for (int i = 0; i < elements.length; i++) {
				int index = i % 12;
				if (elements[i] != null) {
					if (level1Singles.contains(index) || level1Pairs.contains(index)) {
						elements[i].amplitude = 1.0;
					}
				}
			}
		} else {

			System.out.println(">>CHORDIFY!! 2: " + secondCandidates.size());

			if (censHasClusters(secondCandidates, 4)) {
				if (level1CandidateNumber >= 3) {
					for (int i = 0; i < elements.length; i++) {
						elements[i].amplitude = AMPLITUDE_FLOOR;
					}
					for (int i = 0; i < elements.length; i++) {
						int index = i % 12;
						if (elements[i] != null) {
							if (level1Singles.contains(index) || level1Pairs.contains(index)) {
								elements[i].amplitude = 1.0;
							}
						}
					}
					System.out.println(">>CHORDIFY!! 2A: " + level1CandidateNumber);
				} else {
					System.out.println(">>CHORDIFY!! 2B: " + level1CandidateNumber);
					return this;
				}
			}
			censRemoveClusters(secondCandidates, 3);
			System.out.println(">>CHORDIFY!! 2.0: " + secondCandidates.size());

			Set<Integer> level2Pairs = censGetPairs(secondCandidates);
			secondCandidates.removeAll(level2Pairs);

			Set<Integer> level2Singles = censGetSingles(secondCandidates);
			int level2CandidateNumber = level2Singles.size() / 2 + level2Singles.size();
			if (level2CandidateNumber + level1CandidateNumber >= 4) {
				for (int i = 0; i < elements.length; i++) {
					elements[i].amplitude = AMPLITUDE_FLOOR;
				}
				for (int i = 0; i < elements.length; i++) {
					int index = i % 12;
					if (elements[i] != null) {
						if (level1Singles.contains(index) || level1Pairs.contains(index)) {
							elements[i].amplitude = 1.0;
						}
						if (level2Singles.contains(index) || level2Pairs.contains(index)) {
							elements[i].amplitude = 0.75;
						}
					}
				}
				System.out.println(">>CHORDIFY!! 2C: " + level1CandidateNumber);
			} else {
				System.out.println(">>CHORDIFY!! 3: " + thirdCandidates.size());

				if (censHasClusters(thirdCandidates, 4)) {
					for (int i = 0; i < elements.length; i++) {
						elements[i].amplitude = AMPLITUDE_FLOOR;
					}
					for (int i = 0; i < elements.length; i++) {
						int index = i % 12;
						if (elements[i] != null) {
							if (level1Singles.contains(index) || level1Pairs.contains(index)) {
								elements[i].amplitude = 1.0;
							}
							if (level2Singles.contains(index) || level2Pairs.contains(index)) {
								elements[i].amplitude = 0.75;
							}
						}
					}
					System.out.println(">>CHORDIFY!! 3A: " + level1CandidateNumber);
				}
				censRemoveClusters(thirdCandidates, 3);

				Set<Integer> level3Pairs = censGetPairs(thirdCandidates);
				thirdCandidates.removeAll(level3Pairs);

				Set<Integer> level3Singles = censGetSingles(thirdCandidates);
				int level3CandidateNumber = level3Singles.size() / 2 + level3Singles.size();
				if (level3CandidateNumber + level2CandidateNumber + level1CandidateNumber >= 3) {
					for (int i = 0; i < elements.length; i++) {
						elements[i].amplitude = AMPLITUDE_FLOOR;
					}
					int counter = 0;
					for (int i = 0; i < elements.length; i++) {
						int index = i % 12;
						if (elements[i] != null) {
							if (level1Singles.contains(index) || level1Pairs.contains(index)) {
								elements[i].amplitude = 1.0;
								counter++;
							}
							if (level2Singles.contains(index) || level2Pairs.contains(index)) {
								counter++;
								elements[i].amplitude = 0.75;
							}
							if (level3Singles.contains(index) || level3Pairs.contains(index)) {
								counter++;
								elements[i].amplitude = 0.5;
							}
						}
					}
					System.out.println(">>CHORDIFY!! 3B: " + counter);
				} else {
					System.out.println(">>CHORDIFY!! 3C: " + level1CandidateNumber);
				}
			}
		}

		return this;
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

	private void censRemoveClusters(TreeSet<Integer> candidates, int size) {
		int lastCandidate = -1;
		int cluster = 0;
		for (Integer candidate : candidates) {
			if (lastCandidate > -1) {
				if ((candidate - lastCandidate) == 1) {
					cluster++;
					if (cluster == size) {
						for (int count = 0; count < size; count++) {
							candidates.remove(candidate - count);
						}
					} else if (candidate == 11) {
						for (Integer candidate2 : candidates) {
							if (candidate2 == 0 || (candidate2 - lastCandidate) == 1) {
								cluster++;
								if (cluster == size) {
									for (int count = 0; count < size; count++) {
										candidates.remove(candidate - count);
									}
								}
							}
						}
						return;
					}
				} else {
					cluster = 0;
				}
			}
			lastCandidate = candidate;
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

	public ToneTimeFrame smoothMedian(ToneMap sourceToneMap, ToneMap targetToneMap, int factor, int sequence,
			boolean chromaChordifySwitch) {
		// collect previous frames
		List<ToneTimeFrame> sourceFrames = new ArrayList<>();
		ToneTimeFrame stf = sourceToneMap.getTimeFrame(sequence);
		int i = factor;
		while (stf != null && i > 1) {
			sourceFrames.add(stf);
			stf = sourceToneMap.getPreviousTimeFrame(stf.getStartTime());
			i--;
		}
		List<ToneTimeFrame> targetFrames = new ArrayList<>();
		ToneTimeFrame ttf = targetToneMap.getTimeFrame(sequence);
		i = factor / 2 + factor % 2;
		while (ttf != null && i > 1) {
			targetFrames.add(ttf);
			ttf = targetToneMap.getPreviousTimeFrame(ttf.getStartTime());
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
					tf.chromaChordify();
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
			timeSet = new TimeSet(tfStart.getTimeSet().getStartTime(), tfEnd.getTimeSet().getEndTime(),
					timeSet.getSampleRate(), timeSet.getSampleTimeSize());

			pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);

			targetToneMap.addTimeFrame(ttf);

			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				ttf.elements[elementIndex].amplitude = downSample(frames, elementIndex);
			}
			System.out.println(">>add TF: " + ttf.getStartTime());
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

	public ToneTimeFrame hpsPercussionMedian(int hpsPercussionMedianFactor, boolean hpsMedianSwitch) {

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
		return this;
	}

	public ToneTimeFrame hpsHarmonicMedian(ToneMap sourceToneMap, int sequence, int hpsHarmonicMedianFactor,
			boolean hpsMedianSwitch) {

		System.out.println(">>hpsHarmonicMedianFactor: " + hpsHarmonicMedianFactor + ", " + getStartTime());

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
								.sorted(Comparator.comparingDouble(tm -> tm.amplitude)).collect(Collectors.toList());
						ToneMapElement median = sortedElements.size() % 2 == 0
								? sortedElements.get(sortedElements.size() / 2 - 1)
								: sortedElements.get(sortedElements.size() / 2);
						elements[elementIndex].amplitude = median.amplitude;
					} else {
						elements[elementIndex].amplitude = sum / sourceFrames.size();
					}
				}
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
			if (((harmonicTimeFrame.elements[elementIndex].amplitude)
					/ (percussionTimeFrame.elements[elementIndex].amplitude + AMPLITUDE_FLOOR)) < hpsMaskFactor) {
				elements[elementIndex].amplitude = 0;
			}
		}
		reset();
		return this;
	}

	public ToneTimeFrame hpsPercussionMask(ToneTimeFrame harmonicTimeFrame, ToneTimeFrame percussionTimeFrame,
			double hpsMaskFactor) {
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			if (((percussionTimeFrame.elements[elementIndex].amplitude)
					/ (harmonicTimeFrame.elements[elementIndex].amplitude + AMPLITUDE_FLOOR)) < hpsMaskFactor) {
				elements[elementIndex].amplitude = 0;
			}
		}
		reset();
		return this;
	}

	public ToneTimeFrame onsetWhiten(ToneTimeFrame previousFrame, double onsetFactor) {
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			elements[elementIndex].amplitude = Math.max(
					((previousFrame.elements[elementIndex].amplitude * onsetFactor)
							+ (elements[elementIndex].amplitude * (1 - onsetFactor))),
					elements[elementIndex].amplitude);
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

	@Override
	public int hashCode() {
		return Objects.hash(pitchSet, timeSet);
	}

	@Override
	public String toString() {
		return "ToneTimeFrame [pitchSet=" + pitchSet + ", timeSet=" + timeSet + "]";
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

	public void sharpen() {
		int troughIndex = 0;
		int peakIndex = 0;
		double lastAmplitude = -1;
		boolean downSlope = false;
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			double originalAmplitude = element.amplitude;
			if (originalAmplitude < (lastAmplitude * 0.8)) {
				if (!downSlope) {
					peakIndex = elementIndex > 0 ? elementIndex - 1 : elementIndex;
					downSlope = true;
				}
				// element.amplitude = originalAmplitude * 0.5 * originalAmplitude /
				// lastAmplitude;
				if (troughIndex > 0) {
					element.amplitude = elements[troughIndex].amplitude;
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
					while (lastElementIndex > troughIndex) {
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
}
