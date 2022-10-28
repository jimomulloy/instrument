package jomu.instrument.world.tonemap;

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;

import jomu.instrument.organs.Oscillator;

/**
 * This class defines the Audio Sub System Data Model processing functions for
 * the ToneMap including file reading, Audio data transformation, Playback
 * implementation and control settings management through the AudioPanel class.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class AudioModel implements ToneMapConstants {

	public int gainSetting = INIT_VOLUME_SETTING;
	public boolean logSwitch = false;
	public int osc1Setting = 0;
	public boolean osc1Switch = false;
	public int osc2Setting = 0;
	public boolean osc2Switch = false;
	public int oscType = Oscillator.SINEWAVE;
	public int panSetting = INIT_PAN_SETTING;
	public int pFactor = 100;
	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;
	public int pOffset = 0;
	public int powerHigh = 100;
	public int powerLow = 0;
	public int resolution = 1;
	public int reverbRSetting = 0;
	public int reverbSSetting = 0;
	public double sampleTimeSize = INIT_SAMPLE_SIZE;
	public int t1Setting = 50;
	public boolean t1Switch = false;
	public int t2Setting = 50;
	public boolean t2Switch = false;

	public int t3Setting = 50;
	public boolean t3Switch = false;
	public int t4Setting = 50;
	public boolean t4Switch = false;
	public int tFactor = 50;
	public double timeEnd = INIT_TIME_END;
	public double timeStart = INIT_TIME_START;
	public int transformMode = TRANSFORM_MODE_JAVA;
	private double[] lastAmps;

	// private int[] audioData = null;
	// private byte[] audioBytes = null;
	// private byte[] outAudioBytes = null;

	/**
	 * AudioModel constructor. Test Java Sound Audio System available
	 * Instantiate AudioPanel
	 */
	public AudioModel() {
	}

	/**
	 * Clear current AudioModel objects after Reset
	 */

	public void clear() {
	}

	/**
	 * Create audio output stream from ToneMap data
	 * 
	 * @param audioOutSamples
	 * @param audioOutput
	 * @return
	 */
	public AudioInputStream writeStream(ToneMap toneMap,
			float[] audioOutSamples, SourceDataLine audioOutput) {

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		int sampleLength = timeSet.getSampleWindow();
		for (int i = 0; i < sampleLength; i++) {
			audioOutSamples[i] = 0;
		}
		System.out.println("Write sream samples: " + sampleLength);

		double time, frequency;
		double maxSumAmp = 0;
		double minSumAmp = 0;
		double sumAmp = 0;
		int numPitches = 0;
		boolean condition = false;

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();
		sumAmp = 0;
		numPitches = 0;

		time = timeSet.getStartTime();
		NoteListElement noteListElement;
		for (ToneMapElement toneMapElement : ttfElements) {
			frequency = pitchSet.getFreq(toneMapElement.getIndex());
			noteListElement = toneMapElement.noteListElement;
			if (osc1Switch) {
				condition = (toneMapElement.amplitude == -1
						|| noteListElement == null
						|| noteListElement.underTone);
			} else {
				condition = (toneMapElement.amplitude == -1);
			}
			if (!condition) {
				sumAmp += toneMapElement.amplitude;
			}
			numPitches++;

		}
		if (maxSumAmp < sumAmp)
			maxSumAmp = sumAmp;
		if (minSumAmp > sumAmp)
			minSumAmp = sumAmp;

		if (lastAmps == null) {
			lastAmps = new double[numPitches];
		}

		System.out.println("min/max sums: " + maxSumAmp + ", " + minSumAmp);
		Oscillator[] oscillators = new Oscillator[numPitches];

		int i, iStart, iEnd;
		sumAmp = 0;
		i = 0;
		float sampleRate = timeSet.getSampleRate();
		for (ToneMapElement toneMapElement : ttfElements) {
			frequency = pitchSet.getFreq(toneMapElement.getIndex());
			oscillators[i] = new Oscillator(oscType, (int) frequency,
					(int) sampleRate, 1);
			i++;
		}
		System.out.println("created oscs: " + i);
		iStart = 0;
		iEnd = 0;
		i = 0;

		for (int j = 0; j < numPitches; j++) {
			lastAmps[j] = 0;
		}

		double lastSample = 0;
		double ampFactor = 0;
		double ampAdjust = 0;
		iStart = iEnd;
		if (iStart > (int) (time * sampleRate))
			iStart = (int) (time * sampleRate);
		iEnd = iStart + (int) (timeSet.getSampleTimeSize() * sampleRate);
		double power;
		System.out.println("istart/end: " + time + ", " + iStart + ", " + iEnd);
		for (ToneMapElement toneMapElement : ttfElements) {
			lastSample = 0;
			ampAdjust = 0;
			ampFactor = 0;
			power = toneMapElement.amplitude;
			frequency = pitchSet.getFreq(toneMapElement.getIndex());
			noteListElement = toneMapElement.noteListElement;
			if (osc1Switch) {
				condition = (toneMapElement.amplitude == -1
						|| noteListElement == null
						|| noteListElement.underTone);
			} else {
				// condition = (toneMapElement.amplitude == -1);
				condition = (toneMapElement.amplitude < 0.2);
			}
			if (condition) {
				power = 0;
			}
			if ((toneMapElement.getIndex() > 20
					&& toneMapElement.getIndex() < 40)
					&& (power != 0
							|| lastAmps[toneMapElement.getIndex()] != 0)) {
				for (i = iStart; i < iEnd; i++) {
					ampFactor = (double) (i - iStart)
							/ (double) (iEnd - iStart);
					ampAdjust = lastAmps[toneMapElement.getIndex()] + ampFactor
							* (power - lastAmps[toneMapElement.getIndex()]);
					oscillators[toneMapElement.getIndex()]
							.setAmplitudeAdj(ampAdjust / (1000 * maxSumAmp));
					oscillators[toneMapElement.getIndex()].setAmplitudeAdj(1.0);
					lastSample = oscillators[toneMapElement.getIndex()]
							.getSample();
					audioOutSamples[i] += lastSample / 100.0;
				}
			}
			if (ampAdjust == 0)
				oscillators[toneMapElement.getIndex()].reset();
			lastAmps[toneMapElement.getIndex()] = ampAdjust;
		}

		System.out.println("getout audio bytes");

		AudioFormat outFormat = new AudioFormat(timeSet.getSampleRate(), 16, 1,
				true, false);

		byte[] outAudioBytes = getOutAudioBytes(audioOutSamples, outFormat);

		ByteArrayInputStream bais = new ByteArrayInputStream(outAudioBytes);
		AudioInputStream outAudioStream = new AudioInputStream(bais, outFormat,
				outAudioBytes.length / outFormat.getFrameSize());
		System.out.println("made new out audio stream");

		audioOutput.write(outAudioBytes, 0, outAudioBytes.length);
		return outAudioStream;
	}

	// convert audioBytes sampled audio data into standard format in audioData
	// array.
	private byte[] getOutAudioBytes(float[] outAudioData, AudioFormat format) {

		byte[] outAudioBytes = new byte[outAudioData.length * 2];
		if (format.isBigEndian()) {
			for (int i = 0; i < outAudioData.length; i++) {
				// First byte is MSB (high order)
				int MSB = 255 & (((int) (outAudioData[i])) >> 8);
				outAudioBytes[2 * i] = (byte) MSB;
				// Second byte is LSB (low order)
				int LSB = 255 & ((int) (outAudioData[i]));
				outAudioBytes[2 * i + 1] = (byte) LSB;
			}
		} else {
			for (int i = 0; i < outAudioData.length; i++) {
				// First byte is MSB (high order)
				int MSB = 255 & (((int) (outAudioData[i])) >> 8);
				outAudioBytes[2 * i + 1] = (byte) MSB;
				// Second byte is LSB (low order)
				int LSB = 255 & ((int) (outAudioData[i]));
				outAudioBytes[2 * i] = (byte) LSB;
			}
		}
		return outAudioBytes;
	}

}
