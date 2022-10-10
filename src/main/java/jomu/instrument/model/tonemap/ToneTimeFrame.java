package jomu.instrument.model.tonemap;

import java.util.ArrayList;
import java.util.List;

public class ToneTimeFrame {

	private ToneMapElement[] elements;

	private TimeSet timeSet;

	private PitchSet pitchSet;

	private double maxAmplitude;

	private double minAmplitude;

	private double maxFTPower;

	private double minFTPower;

	private double avgAmplitude;

	private double avgFTPower;

	private double lowThres = 0;

	private double highThres = 100;
	
	private boolean ampType = LOGAMP;

	public static final boolean POWERAMP = false;

	public static final boolean LOGAMP = true;
	public final static int INIT_PITCH_LOW = 36;
	public final static int INIT_PITCH_HIGH = 72;
	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;
	
	public ToneTimeFrame(TimeSet timeSet, PitchSet pitchSet) {
		this.timeSet = timeSet;
		this.pitchSet = pitchSet;
		int pitchRange = pitchSet.getRange();
		elements = new ToneMapElement[pitchRange];
		for (int i = 0; i < elements.length; i++) {
			elements[i] = new ToneMapElement(i);
		}
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
		int matrixSize = elements.length;
		float minPower = 5 / 1000000.0F;
		maxAmplitude = 0;
		minAmplitude = 0;
		maxFTPower = 0;
		minFTPower = 0;
		avgAmplitude = 0;
		avgFTPower = 0;
		long count = 0;
		for (int i = 0; i < (matrixSize - 1); i++) {
			if (elements[i] != null) {
				count++;
				if (elements[i].preFTPower < minPower) {
					elements[i].preFTPower = minPower;
				}
				avgFTPower += elements[i].preFTPower;

				if (maxFTPower < elements[i].preFTPower)
					maxFTPower = elements[i].preFTPower;
				if ((minFTPower == 0) || (minFTPower > elements[i].preFTPower))
					minFTPower = elements[i].preFTPower;
			}
		}

		avgFTPower = avgFTPower / count;

		count = 0;
		for (int i = 0; i < (matrixSize - 1); i++) {
			if (elements[i] != null) {
				elements[i].noteState = 0;
				elements[i].postFTPower = elements[i].preFTPower;
				// elements[i].noteListElement = null;
				elements[i].preAmplitude = FTPowerToAmp(elements[i].preFTPower);
				elements[i].postAmplitude = elements[i].preAmplitude;
				avgAmplitude += elements[i].preAmplitude;
				if (elements[i].preAmplitude != -1) {
					avgAmplitude += elements[i].preAmplitude;
					count++;
					if (maxAmplitude < elements[i].preAmplitude)
						maxAmplitude = elements[i].preAmplitude;
					if ((minAmplitude == 0) || (minAmplitude > elements[i].preAmplitude))
						minAmplitude = elements[i].preAmplitude;
				}
			}
		}
		avgAmplitude = avgAmplitude / count;
	}

	public double FTPowerToAmp(double power) {
		double amplitude = 0.0;
		if (power <= 0.0)
			return 0.0;
		if (ampType == LOGAMP) {
			if (minFTPower < maxFTPower / (double) lowThres * 10.0)
				minFTPower = maxFTPower / (double) lowThres * 10.0; // ??
			amplitude = (float) Math.log10(1 + (100.0 * power));
			// double logMinFTPower = Math.abs(Math.log(minFTPower / maxFTPower));
			// amplitude = (logMinFTPower - Math.abs(Math.log(FTPower / maxFTPower))) /
			// logMinFTPower;
			// amplitude = (20 * Math.log(1 + Math.abs(FTPower)) / Math.log(10));
			if (amplitude < 0)
				amplitude = 0.0;
		} else {
			if ((maxFTPower - minFTPower) <= 0) {
				amplitude = 0.0;
			} else {
				double minpow = minFTPower + (maxFTPower - minFTPower) * ((double) lowThres / 100.0);
				double maxpow = maxFTPower
						- (maxFTPower - minFTPower) * ((double) (100 - highThres) / 100.0);
				if (power > maxpow) {
					amplitude = 1.0;
				} else if (power < minpow) {
					amplitude = 0.0;
				} else {
					amplitude = Math.sqrt(power - minpow) / Math.sqrt(maxpow - minpow);
				}
			}
		}

		return amplitude;
	}

	public void loadFFT(float[] fft, int bufferSize) {
		int elementIndex = 0;
		double highFreq = timeSet.getSampleRate() / (2.0); 
		double binStartFreq = pitchSet.getFreq(elementIndex);
		double binEndFreq = pitchSet.getFreq(elementIndex + 1);
		for (int i = 0; i < fft.length; i++) {
			double currentFreq = highFreq * (((double) i) / bufferSize);
			if (currentFreq < binStartFreq) {
				continue;
			}
			if (currentFreq >= binEndFreq) {
				elementIndex++;
				if (elementIndex == elements.length) {
					break;
				}
				binEndFreq = pitchSet.getFreq(elementIndex + 1);
			} 
			elements[elementIndex].preFTPower += fft[i];
		}
	}
	
	public float[] extractFFT(int bufferSize) {
		List<Float> spectralEnergy = new ArrayList<>();
		double binStartFreq, binEndFreq;
		int binStartIndex, binEndIndex;
		for (int i = 0; i < elements.length; i++) {
			binStartFreq = pitchSet.getFreq(i);
			binEndFreq = pitchSet.getFreq(i + 1);
			binStartIndex = (int)((binStartFreq * bufferSize * 2.0) /(timeSet.getSampleRate()));
			binEndIndex = (int)((binEndFreq * bufferSize * 2.0) /(timeSet.getSampleRate()));
			double partialValue = (elements[i].postAmplitude) / (binEndIndex - binStartIndex);  
			for(int j = 0; j < (binEndIndex - binStartIndex); j++) {
				spectralEnergy.add((float)partialValue);
			}
		}
		float[] result = new float[spectralEnergy.size()];
		int i = 0;
		for(Float value: spectralEnergy) {
			result[i] = value;
			i++;			
		}
		return result;
	}
}
