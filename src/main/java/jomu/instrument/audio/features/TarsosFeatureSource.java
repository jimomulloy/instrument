package jomu.instrument.audio.features;

import jomu.instrument.audio.TarsosAudioIO;

public class TarsosFeatureSource {

	private AudioEventSource audioEventSource;
	private BandedPitchDetectorSource bandedPitchDetectorSource;
	private ConstantQSource constantQSource;
	private GoertzelSource goertzelSource;
	private OnsetSource onsetSource;
	private PitchDetectorSource pitchDetectorSource;
	private ScalogramSource scalogramSource;
	private SpectralPeaksSource spectralPeaksSource;
	private SpectrogramSource spectrogramSource;
	private TarsosAudioIO tarsosIO;

	public TarsosFeatureSource(TarsosAudioIO tarsosIO) {
		this.tarsosIO = tarsosIO;
	}

	public AudioEventSource getAudioEventSource() {
		return audioEventSource;
	}

	public BandedPitchDetectorSource getBandedPitchDetectorSource() {
		return bandedPitchDetectorSource;
	}

	public ConstantQSource getConstantQSource() {
		return constantQSource;
	}

	public GoertzelSource getGoertzelSource() {
		return goertzelSource;
	}

	public OnsetSource getOnsetSource() {
		return onsetSource;
	}

	public PitchDetectorSource getPitchDetectorSource() {
		return pitchDetectorSource;
	}

	public ScalogramSource getScalogramSource() {
		return scalogramSource;
	}

	public SpectralPeaksSource getSpectralPeaksSource() {
		return spectralPeaksSource;
	}

	public SpectrogramSource getSpectrogramSource() {
		return spectrogramSource;
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public void initialise() {
		constantQSource = new ConstantQSource(tarsosIO);
		onsetSource = new OnsetSource(tarsosIO);
		spectralPeaksSource = new SpectralPeaksSource(tarsosIO);
		pitchDetectorSource = new PitchDetectorSource(tarsosIO);
		bandedPitchDetectorSource = new BandedPitchDetectorSource(tarsosIO);
		spectrogramSource = new SpectrogramSource(tarsosIO);
		audioEventSource = new AudioEventSource(tarsosIO);
		goertzelSource = new GoertzelSource(tarsosIO);
		scalogramSource = new ScalogramSource(tarsosIO);
		constantQSource.initialise();
		onsetSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		bandedPitchDetectorSource.initialise();
		spectrogramSource.initialise();
		goertzelSource.initialise();
		// scalogramSource.initialise();
	}
}
