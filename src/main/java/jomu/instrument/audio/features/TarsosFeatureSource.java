package jomu.instrument.audio.features;

import be.tarsos.dsp.AudioDispatcher;

public class TarsosFeatureSource {

	private ConstantQSource constantQSource;
	private CQMicroToneSource cqMicroToneSource;
	private OnsetSource onsetSource;
	private PitchDetectorSource pitchDetectorSource;
	private YINSource yinSource;
	private ResynthSource resynthSource;
	private SACFSource sacfSource;
	private SpectralPeaksSource spectralPeaksSource;
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

	public OnsetSource getOnsetSource() {
		return onsetSource;
	}

	public PitchDetectorSource getPitchDetectorSource() {
		return pitchDetectorSource;
	}

	public YINSource getYINSource() {
		return yinSource;
	}

	public ResynthSource getResynthSource() {
		return resynthSource;
	}

	public SACFSource getSACFSource() {
		return sacfSource;
	}

	public SpectralPeaksSource getSpectralPeaksSource() {
		return spectralPeaksSource;
	}

	public void initialise() {
		constantQSource = new ConstantQSource(dispatcher);
		cqMicroToneSource = new CQMicroToneSource(dispatcher);
		onsetSource = new OnsetSource(dispatcher);
		spectralPeaksSource = new SpectralPeaksSource(dispatcher);
		pitchDetectorSource = new PitchDetectorSource(dispatcher);
		yinSource = new YINSource(dispatcher);
		resynthSource = new ResynthSource(dispatcher);
		sacfSource = new SACFSource(dispatcher);
		beatSource = new BeatSource(dispatcher);
		constantQSource.initialise();
		cqMicroToneSource.initialise();
		onsetSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		yinSource.initialise();
		resynthSource.initialise();
		sacfSource.initialise();
		beatSource.initialise();
	}
}
