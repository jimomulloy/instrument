package jomu.instrument.workspace.tonemap;

public class DeNoiser {

	private FFTSpectrum fftSpectrum;

	public DeNoiser(FFTSpectrum fftSpectrum) {
		this.fftSpectrum = fftSpectrum;
	}

	public float[] deNoiseSpectrum() {
		return this.fftSpectrum.getSpectrum();

	}

}
