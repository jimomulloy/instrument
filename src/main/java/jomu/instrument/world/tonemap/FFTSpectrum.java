package jomu.instrument.world.tonemap;

public class FFTSpectrum {

	public double[] binFrequencies; /* FFT fequency bins */
	private float sampleRate;
	private float[] spectrum;
	private int windowSize;

	public FFTSpectrum(float sampleRate, int windowSize, float[] spectrum) {
		this.sampleRate = sampleRate;
		this.windowSize = windowSize;
		this.spectrum = spectrum;
		/*
		 * Frequencies, always the same after capture init... captured signal will be
		 * zero padded to twice its length, so valid fft bins are equal to original
		 * epoch length
		 */
		binFrequencies = new double[(int) Math.floor(getWindowSize())];
		for (int b = 0; b < Math.floor(getWindowSize()); ++b) {
			binFrequencies[b] = b * (getSampleRate() / 2.0) / getWindowSize();
		}

	}

	public double[] getBinFrequencies() {
		return binFrequencies;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public float[] getSpectrum() {
		return spectrum;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void magnify(float factor) {
		for (int i = 0; i < spectrum.length; i++) {
			spectrum[i] *= factor;
		}
	}
}
