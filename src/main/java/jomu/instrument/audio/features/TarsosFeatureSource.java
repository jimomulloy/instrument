package jomu.instrument.audio.features;

import be.tarsos.dsp.AudioDispatcher;

public class TarsosFeatureSource {

	// private AudioEventSource audioEventSource;
	private BandedPitchDetectorSource bandedPitchDetectorSource;
	private ConstantQSource constantQSource;
	private GoertzelSource goertzelSource;
	private OnsetSource onsetSource;
	private PitchDetectorSource pitchDetectorSource;
	private ScalogramSource scalogramSource;
	private SpectralPeaksSource spectralPeaksSource;
	private SpectrogramSource spectrogramSource;
	private AudioDispatcher dispatcher;

	public TarsosFeatureSource(AudioDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	// public AudioEventSource getAudioEventSource() {
	// return audioEventSource;
	// }

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

	public void initialise() {
		constantQSource = new ConstantQSource(dispatcher);
		onsetSource = new OnsetSource(dispatcher);
		spectralPeaksSource = new SpectralPeaksSource(dispatcher);
		pitchDetectorSource = new PitchDetectorSource(dispatcher);
		bandedPitchDetectorSource = new BandedPitchDetectorSource(dispatcher);
		spectrogramSource = new SpectrogramSource(dispatcher);
		// audioEventSource = new AudioEventSource(dispatcher);
		goertzelSource = new GoertzelSource(dispatcher);
		scalogramSource = new ScalogramSource(dispatcher);
		constantQSource.initialise();
		onsetSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		bandedPitchDetectorSource.initialise();
		spectrogramSource.initialise();
		goertzelSource.initialise();
		// scalogramSource.initialise();
	}

	public AudioDispatcher getDispatcher() {
		return dispatcher;
	}
}
