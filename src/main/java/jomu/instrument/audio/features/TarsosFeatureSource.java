package jomu.instrument.audio.features;

import be.tarsos.dsp.AudioDispatcher;

public class TarsosFeatureSource {

	// private AudioEventSource audioEventSource;
	// private BandedPitchDetectorSource bandedPitchDetectorSource;
	private ConstantQSource constantQSource;
	private CQMicroToneSource cqMicroToneSource;
	private GoertzelSource goertzelSource;
	private OnsetSource onsetSource;
	private PitchDetectorSource pitchDetectorSource;
	private ResynthSource resynthSource;
	private SACFSource sacfSource;
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

	public CQMicroToneSource getCQMicroToneSource() {
		return cqMicroToneSource;
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

	public ResynthSource getResynthSource() {
		return resynthSource;
	}

	public SACFSource getSACFSource() {
		return sacfSource;
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
		cqMicroToneSource = new CQMicroToneSource(dispatcher);
		onsetSource = new OnsetSource(dispatcher);
		spectralPeaksSource = new SpectralPeaksSource(dispatcher);
		pitchDetectorSource = new PitchDetectorSource(dispatcher);
		resynthSource = new ResynthSource(dispatcher);
		sacfSource = new SACFSource(dispatcher);
		spectrogramSource = new SpectrogramSource(dispatcher);
		beatSource = new BeatSource(dispatcher);
		goertzelSource = new GoertzelSource(dispatcher);
		scalogramSource = new ScalogramSource(dispatcher);
		constantQSource.initialise();
		cqMicroToneSource.initialise();
		onsetSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		resynthSource.initialise();
		sacfSource.initialise();
		spectrogramSource.initialise();
		goertzelSource.initialise();
		beatSource.initialise();
	}
}
