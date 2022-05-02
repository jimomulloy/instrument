package jomu.instrument.organs;

import jomu.instrument.audio.TarsosAudioIO;

public class TarsosFeatureSource {

	private TarsosAudioIO tarsosIO;
	private ConstantQSource constantQSource;
	private SpectralPeaksSource spectralPeaksSource;
	private PitchDetectorSource pitchDetectorSource;

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	void initialise() {
		constantQSource = new ConstantQSource(tarsosIO);
		spectralPeaksSource = new SpectralPeaksSource(tarsosIO);
		pitchDetectorSource = new PitchDetectorSource(tarsosIO);
		constantQSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
	}

	public ConstantQSource getConstantQSource() {
		return constantQSource;
	}

	public SpectralPeaksSource getSpectralPeaksSource() {
		return spectralPeaksSource;
	}

	public TarsosFeatureSource(TarsosAudioIO tarsosIO) {
		this.tarsosIO = tarsosIO;
	}

	public PitchDetectorSource getPitchDetectorSource() {
		return pitchDetectorSource;
	}
}
