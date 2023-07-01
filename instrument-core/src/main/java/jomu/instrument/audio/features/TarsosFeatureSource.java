package jomu.instrument.audio.features;

import java.util.logging.Logger;

import be.tarsos.dsp.AudioDispatcher;

public class TarsosFeatureSource {

	private static final Logger LOG = Logger.getLogger(TarsosFeatureSource.class.getName());

	private ConstantQSource constantQSource;
	private PercussionSource percussionSource;
	private PitchDetectorSource pitchDetectorSource;
	private PhaseDetectorSource phaseDetectorSource;
	private YINSource yinSource;
	private ResynthSource resynthSource;
	private SACFSource sacfSource;
	private MFCCSource mfccSource;
	private CepstrumSource cepstrumSource;
	private SpectralPeaksSource spectralPeaksSource;
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

	public PercussionSource getPercussionSource() {
		return percussionSource;
	}

	public PitchDetectorSource getPitchDetectorSource() {
		return pitchDetectorSource;
	}

	public PhaseDetectorSource getPhaseDetectorSource() {
		return phaseDetectorSource;
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

	public MFCCSource getMFCCSource() {
		return mfccSource;
	}

	public CepstrumSource getCepstrumSource() {
		return cepstrumSource;
	}

	public SpectralPeaksSource getSpectralPeaksSource() {
		return spectralPeaksSource;
	}

	public void initialise() {
		constantQSource = new ConstantQSource(dispatcher);
		percussionSource = new PercussionSource(dispatcher);
		spectralPeaksSource = new SpectralPeaksSource(dispatcher);
		pitchDetectorSource = new PitchDetectorSource(dispatcher);
		phaseDetectorSource = new PhaseDetectorSource(dispatcher);
		yinSource = new YINSource(dispatcher);
		resynthSource = new ResynthSource(dispatcher);
		sacfSource = new SACFSource(dispatcher);
		mfccSource = new MFCCSource(dispatcher);
		cepstrumSource = new CepstrumSource(dispatcher);
		beatSource = new BeatSource(dispatcher);
		constantQSource.initialise();
		percussionSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		phaseDetectorSource.initialise();
		yinSource.initialise();
		resynthSource.initialise();
		sacfSource.initialise();
		mfccSource.initialise();
		cepstrumSource.initialise();
		beatSource.initialise();
	}
}
