package jomu.instrument.organs;

import jomu.instrument.audio.TarsosAudioIO;

public class TarsosFeatureSource {

	private TarsosAudioIO tarsosIO;
	private ConstantQSource constantQSource;
	private SpectralPeaksSource spectralPeaksSource;
	private PitchDetectorSource pitchDetectorSource;
	private SpectrogramSource spectrogramSource;
	private GoertzelSource goertzelSource;
	private AudioEventSource audioEventSource;

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	void initialise() {
		constantQSource = new ConstantQSource(tarsosIO);
		spectralPeaksSource = new SpectralPeaksSource(tarsosIO);
		pitchDetectorSource = new PitchDetectorSource(tarsosIO);
		spectrogramSource = new SpectrogramSource(tarsosIO);
		audioEventSource = new AudioEventSource(tarsosIO);
		goertzelSource = new GoertzelSource(tarsosIO);
		constantQSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		spectrogramSource.initialise();
		goertzelSource.initialise();
	}

	public AudioEventSource getAudioEventSource() {
		return audioEventSource;
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

	public SpectrogramSource getSpectrogramSource() {
		return spectrogramSource;
	}

	public GoertzelSource getGoertzelSource() {
		return goertzelSource;
	}
}
