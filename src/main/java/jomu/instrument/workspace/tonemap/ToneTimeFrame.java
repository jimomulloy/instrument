package jomu.instrument.workspace.tonemap;

import java.util.HashMap;
import java.util.Map;

public class ToneTimeFrame {

	public final static int INIT_PITCH_HIGH = 72;

	public final static int INIT_PITCH_LOW = 36;

	public static final boolean LOGAMP = true;

	public static final boolean POWERAMP = false;

	private boolean ampType = LOGAMP;

	private double avgAmplitude;

	private ToneMapElement[] elements;

	private double highThres = 100;

	private double lowThres = 0;
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
		ToneMapElement[] copyElements = copy.getElements();
		for (int i = 0; i < elements.length; i++) {
			copyElements[i] = elements[i].clone();
		}
		copy.reset();
		return copy;
	}

	public void compress(float factor) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				elements[i].amplitude = (float) Math.log10(1 + (factor * elements[i].amplitude));
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

	public double getHighThres() {
		return highThres;
	}

	public double getLowThres() {
		return lowThres;
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
				new PitchSet(basePitch, basePitch + 12));
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

	public void loadFFTSpectrum(FFTSpectrum fftSpectrum) {
		int elementIndex = 0;
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
				// elements[elementIndex].amplitude = 0;
				binEndFreq = pitchSet.getFreq(elementIndex + 1);
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
	}

	public void normalise(float threshold) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				elements[i].amplitude = elements[i].amplitude / threshold;
			}
		}
		reset();
	}

	public void reset() {
		maxAmplitude = 0;
		minAmplitude = 0;
		avgAmplitude = 0;
		long count = 0;

		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				elements[i].noteState = 0;
				// elements[i].noteListElement = null;
				avgAmplitude += elements[i].amplitude;
				if (elements[i].amplitude != -1) {
					avgAmplitude += elements[i].amplitude;
					count++;
					if (maxAmplitude < elements[i].amplitude)
						maxAmplitude = elements[i].amplitude;
					if ((minAmplitude == 0) || (minAmplitude > elements[i].amplitude))
						minAmplitude = elements[i].amplitude;
				}
			}
		}
		avgAmplitude = avgAmplitude / count;
	}

	public void square() {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				elements[i].amplitude = Math.pow(elements[i].amplitude, 2);
			}
		}
		reset();
	}

	public void lowThreshold(double threshold, double value) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				if (elements[i].amplitude < threshold) {
					elements[i].amplitude = value;
				}
			}
		}
		reset();
	}

	static double log(double x, int base) {
		return (Math.log(x) / Math.log(base));
	}

	public void normaliseThreshold(double threshold, double value) {
		reset();
		System.out.println(">>maxAmplitude: " + maxAmplitude);
		float maxdbRatio = 0;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				float dbRatio = (float) (20.0 * Math.log(maxAmplitude / elements[i].amplitude));
				if (dbRatio > threshold) {
					elements[i].amplitude = value;
				}
				if (maxdbRatio < dbRatio) {
					maxdbRatio = dbRatio;
				}
			}
		}
		System.out.println(">>maxdb: " + maxdbRatio);
		reset();
	}

	public void decibel(double base) {
		reset();
		System.out.println(">>maxAmplitude decibel before: " + maxAmplitude);
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				double value = elements[i].amplitude > base ? elements[i].amplitude : base;
				elements[i].amplitude = (float) (20.0 * Math.log(value / base));
				if (elements[i].amplitude < base) {
					elements[i].amplitude = base;
				}
			}
		}
		reset();
		System.out.println(">>maxAmplitude decibel after: " + maxAmplitude);
	}
}
