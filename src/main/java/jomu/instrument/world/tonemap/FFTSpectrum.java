package jomu.instrument.world.tonemap;

public class FFTSpectrum {

	private float sampleRate;
	private int windowSize;
	private float[] spectrum;
	public double[] binFrequencies; /* FFT fequency bins */

	public FFTSpectrum(float sampleRate, int windowSize, float[] spectrum) {
		this.sampleRate = sampleRate;
		this.windowSize = windowSize;
		this.spectrum = spectrum;
		/*
		 * Frequencies, always the same after capture init... captured signal will be
		 * zero padded to twice its length, so valid fft bins are equal to original
		 * epoch length
		 */
		binFrequencies = new double[(int) Math.floor((double) getWindowSize())];
		for (int b = 0; b < Math.floor((double) getWindowSize()); ++b) {
			binFrequencies[b] = (double) b * (double) (getSampleRate() / 2.0) / (double) getWindowSize();
		}

	}

	public double[] getBinFrequencies() {
		return binFrequencies;
	}

	public float[] getSpectrum() {
		return spectrum;
	}
	
	public void magnify(float factor) {
		for (int i = 0; i < spectrum.length; i++) {
			spectrum[i] *= factor; 
		}
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public int getWindowSize() {
		return windowSize;
	}
}
