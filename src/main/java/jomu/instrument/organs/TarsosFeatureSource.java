package jomu.instrument.organs;

import jomu.instrument.audio.TarsosAudioIO;

public class TarsosFeatureSource {

	private TarsosAudioIO tarsosIO;
	private ConstantQSource constantQSource;
	private OnsetSource onsetSource;
	private SpectralPeaksSource spectralPeaksSource;
	private PitchDetectorSource pitchDetectorSource;
	private SpectrogramSource spectrogramSource;
	private GoertzelSource goertzelSource;
	private AudioEventSource audioEventSource;
	private ScalogramSource scalogramSource;

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	void initialise() {
		constantQSource = new ConstantQSource(tarsosIO);
		onsetSource = new OnsetSource(tarsosIO);
		spectralPeaksSource = new SpectralPeaksSource(tarsosIO);
		pitchDetectorSource = new PitchDetectorSource(tarsosIO);
		spectrogramSource = new SpectrogramSource(tarsosIO);
		audioEventSource = new AudioEventSource(tarsosIO);
		goertzelSource = new GoertzelSource(tarsosIO);
		scalogramSource = new ScalogramSource(tarsosIO);
		constantQSource.initialise();
		onsetSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		spectrogramSource.initialise();
		goertzelSource.initialise();
		// scalogramSource.initialise();
	}

	public AudioEventSource getAudioEventSource() {
		return audioEventSource;
	}

	public ConstantQSource getConstantQSource() {
		return constantQSource;
	}

	public OnsetSource getOnsetSource() {
		return onsetSource;
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

	public ScalogramSource getScalogramSource() {
		return scalogramSource;
	}
}
