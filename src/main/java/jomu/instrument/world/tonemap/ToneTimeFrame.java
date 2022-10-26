package jomu.instrument.world.tonemap;

public class ToneTimeFrame {

	private ToneMapElement[] elements;

	private TimeSet timeSet;

	private PitchSet pitchSet;

	private double maxAmplitude;

	private double minAmplitude;

	private double avgAmplitude;

	private double lowThres = 0;

	private double highThres = 100;

	private boolean ampType = LOGAMP;

	public static final boolean POWERAMP = false;

	public static final boolean LOGAMP = true;
	public final static int INIT_PITCH_LOW = 36;
	public final static int INIT_PITCH_HIGH = 72;
	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;

	private NoteStatus noteStatus;

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

	public ToneTimeFrame clone() {
		ToneTimeFrame copy = new ToneTimeFrame(this.timeSet, this.pitchSet);
		ToneMapElement[] copyElements = copy.getElements();
		for (int i = 0; i < elements.length; i++) {
			copyElements[i] = elements[i].clone();
		}
		return copy;
	}

	public ToneMapElement[] getElements() {
		return elements;
	}

	public ToneMapElement getElement(int index) {
		return elements[index];
	}

	public NoteStatus getNoteStatus() {
		return noteStatus;
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

	public double getMaxAmplitude() {
		return maxAmplitude;
	}

	public double getMinAmplitude() {
		return minAmplitude;
	}

	public double getAvgAmplitude() {
		return avgAmplitude;
	}

	public double getLowThres() {
		return lowThres;
	}

	public double getHighThres() {
		return highThres;
	}

	public int getPitchHigh() {
		return pitchHigh;
	}

	public int getPitchLow() {
		return pitchLow;
	}

	public double FTPowerToAmp(double power) {
		double amplitude = 0.0F;
		if (power <= 0.0)
			return 0.0F;
		if (ampType == LOGAMP) {
			amplitude = (float) Math.log10(1 + (100.0 * power));
			// amplitude = (20 * Math.log(1 + Math.abs(power)) / Math.log(10));
			// double logMinFTPower = Math.abs(Math.log(minFTPower / maxFTPower));
			// amplitude = (logMinFTPower - Math.abs(Math.log(FTPower / maxFTPower))) /
			// logMinFTPower;
			// amplitude = (20 * Math.log(1 + Math.abs(FTPower)) / Math.log(10));
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

	public double[] getPitches() {
		double[] result = new double[elements.length];
		for (int i = 0; i < elements.length; i++) {
			result[i] = pitchSet.getFreq(i + 1);
		}
		return result;
	}

	public void loadFFTSpectrum(FFTSpectrum fftSpectrum) {
		int elementIndex = 0;
		double binStartFreq = pitchSet.getFreq(elementIndex);
		double binEndFreq = pitchSet.getFreq(elementIndex + 1);
		elements[elementIndex].amplitude = 0;
		// System.out.println(">>loadFFT timeSet.getSampleRate(): " +
		// timeSet.getSampleRate());
		// System.out.println(">>loadFFT bufferSize: " + bufferSize);
		for (int i = 0; i < fftSpectrum.getSpectrum().length; i++) {
			// double currentFreq = (timeSet.getSampleRate() / (2.0)) * (((double) i) /
			// bufferSize);
			double currentFreq = i * (timeSet.getSampleRate() / (((double) fftSpectrum.getWindowSize()) * 2.0));
			// System.out.println(">>loadFFT currentFreq 1 : " + currentFreq);
			if (currentFreq < binStartFreq) {
				continue;
			}
			if (currentFreq >= binEndFreq) {
				elementIndex++;
				if (elementIndex == elements.length) {
					break;
				}
				elements[elementIndex].amplitude = 0;
				binEndFreq = pitchSet.getFreq(elementIndex + 1);
			}
			// System.out.println(">>loadFFT elementIndex: " + elementIndex);
			// System.out.println(">>loadFFT currentFreq : " + currentFreq);
			// System.out.println(">>loadFFT binStartFreq: " + binStartFreq);
			// System.out.println(">>loadFFT binEndFreq: " + binEndFreq);
			elements[elementIndex].amplitude += fftSpectrum.getSpectrum()[i];
		}
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
			System.out.println(">>extractFFT binStartFreq: " + binStartFreq);
			System.out.println(">>extractFFT binEndFreq: " + binEndFreq);
			System.out.println(">>extractFFT binStartIndex: " + binStartIndex);
			System.out.println(">>extractFFT binEndIndex: " + binEndIndex);
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

	public void compress(float factor) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				elements[i].amplitude = (float) Math.log10(1 + (factor * elements[i].amplitude));
			}
		}
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

	public void deNoise(double threshold) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null && elements[i].amplitude < threshold) {
				elements[i].amplitude = 0.00001;
			}
		}
		reset();
	}
}
