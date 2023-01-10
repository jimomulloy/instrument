package jomu.instrument.audio.features;

import be.tarsos.dsp.AudioDispatcher;

public class TarsosFeatureSource {

	// private AudioEventSource audioEventSource;
	// private BandedPitchDetectorSource bandedPitchDetectorSource;
	private ConstantQSource constantQSource;
	private GoertzelSource goertzelSource;
	private OnsetSource onsetSource;
	private PitchDetectorSource pitchDetectorSource;
	private PitchDetectorSource lowPitchDetectorSource;
	private ScalogramSource scalogramSource;
	private SpectralPeaksSource spectralPeaksSource;
	private SpectrogramSource spectrogramSource;
	private BeatSource beatSource;
	private AudioDispatcher dispatcher;

	public TarsosFeatureSource(AudioDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	public ConstantQSource getConstantQSource() {
		return constantQSource;
	}

	public AudioDispatcher getDispatcher() {
		return dispatcher;
	}

	public BeatSource getBeatSource() {
		return beatSource;
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

	public PitchDetectorSource getLowPitchDetectorSource() {
		return lowPitchDetectorSource;
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
		lowPitchDetectorSource = new PitchDetectorSource(dispatcher, true);
		spectrogramSource = new SpectrogramSource(dispatcher);
		beatSource = new BeatSource(dispatcher);
		goertzelSource = new GoertzelSource(dispatcher);
		scalogramSource = new ScalogramSource(dispatcher);
		constantQSource.initialise();
		onsetSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		lowPitchDetectorSource.initialise();
		spectrogramSource.initialise();
		goertzelSource.initialise();
		beatSource.initialise();
	}
}
