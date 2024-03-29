package jomu.instrument.audio;

import java.util.Random;

// NOTE: 16 bit PCM data has a min value of -32768 (8000H)
//       and a max value of 32767 (7FFFFH).

public class Oscillator {

	public static final int NOISE = 1;
	public static final int NOTYPE = 0;
	public static final int SINEWAVE = 2;
	public static final int SQUAREWAVE = 4;
	public static final int TRIANGLEWAVE = 3;

	protected double amplitudeAdj;

	protected int frequency;

	protected int numberOfChannels;

	protected int pos;

	protected int sampleRate;

	// Class data
	protected int type;

	protected double[] waveTable;

	// Constructor with reasonable defaults
	public Oscillator() {

		this(SINEWAVE, 1000, 22050, 1);
	}

	public Oscillator(int type, int frequency, int sampleRate, int numberOfChannels) {
		// Save incoming
		this.type = type;
		this.frequency = frequency;
		this.sampleRate = sampleRate;
		this.numberOfChannels = numberOfChannels;

		// Set amplitude adjustment
		amplitudeAdj = 1.0;

		// Table of samples for oscillator waveform
		waveTable = null;

		// Generate wave table
		buildWaveTable();
	}

	public double getAmplitudeAdj() {

		return amplitudeAdj;
	}

	public int getFrequency() {

		return frequency;
	}

	public int getNumberOfChannels() {

		return numberOfChannels;
	}

	public int getOscType() {

		return type;
	}

	public double getSample() {

		double sample = 0;
		sample = amplitudeAdj * waveTable[pos];
		pos += frequency;
		if (pos >= sampleRate) {
			pos -= sampleRate;
		}
		return sample;
	}

	public int getSampleRate() {

		return sampleRate;
	}

	public int getSamples(float[] buffer, int length) {

		int sample = 0;
		int count = length;

		while (count-- != 0) {

			buffer[sample++] = (float) (amplitudeAdj * waveTable[pos]);

			pos += frequency;
			if (pos >= sampleRate)
				pos -= sampleRate;
		}
		return length;
	}

	public void reset() {
		pos = 0;
	}

	public void setAmplitudeAdj(double amplitudeAdj) {

		if (amplitudeAdj > 1.0)
			amplitudeAdj = 1.0;
		if (amplitudeAdj < 0.0)
			amplitudeAdj = 0.0;

		this.amplitudeAdj = amplitudeAdj;
	}

	public void setFrequency(int frequency) {

		this.frequency = frequency;

		// Reset waveTable index
		pos = 0;
	}

	public void setNumberOfChannels(int numberOfChannels) {

		this.numberOfChannels = numberOfChannels;
	}

	public void setOscType(int type) {

		this.type = type;

		buildWaveTable();
	}

	public void setSampleRate(int sampleRate) {

		this.sampleRate = sampleRate;

		buildWaveTable();
	}

	// Generate a wavetable for the waveform
	protected void buildWaveTable() {

		if (type == NOTYPE)
			return;

		// Initialize waveTable index as wave table is changing
		pos = 0;

		// Allocate a table for 1 cycle of waveform
		waveTable = new double[sampleRate];

		switch (type) {
			case NOISE:
				// Create a random number generator for returning gaussian
				// distributed numbers. The result is white noise.
				Random random = new Random();

				for (int sample = 0; sample < sampleRate; sample++)
					waveTable[sample] = (65535.0 * random.nextGaussian()) - 32768;
				break;

			case SINEWAVE:
				double scale = (2.0 * Math.PI) / sampleRate;

				for (int sample = 0; sample < sampleRate; sample++)
					waveTable[sample] = 32767.0 * Math.sin(sample * scale);

				break;

			case TRIANGLEWAVE:
				double sign = 1.0;
				double value = 0.0;

				int oneQuarterWave = sampleRate / 4;
				int threeQuarterWave = (3 * sampleRate) / 4;

				scale = 32767.0 / oneQuarterWave;

				for (int sample = 0; sample < sampleRate; sample++) {

					if ((sample > oneQuarterWave) && (sample <= threeQuarterWave))
						sign = -1.0;
					else
						sign = 1.0;

					value += sign * scale;
					waveTable[sample] = value;
				}
				break;

			case SQUAREWAVE:
				for (int sample = 0; sample < sampleRate; sample++) {
					if (sample < sampleRate / 2)
						waveTable[sample] = 32767;
					else
						waveTable[sample] = -32768;
				}
				break;
		}
	}
}