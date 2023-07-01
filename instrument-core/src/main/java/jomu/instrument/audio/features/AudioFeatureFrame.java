package jomu.instrument.audio.features;

import java.util.logging.Logger;

public class AudioFeatureFrame {

	private static final Logger LOG = Logger.getLogger(AudioFeatureFrame.class.getName());

	private AudioFeatureProcessor audioFeatureProcessor;
	private ConstantQFeatures constantQFeatures;
	private double end;
	private int frameSequence;

	private PercussionFeatures percussionFeatures;
	private BeatFeatures beatFeatures;
	private PitchDetectorFeatures pitchDetectorFeatures;
	private PhaseDetectorFeatures phaseDetectorFeatures;
	private YINFeatures yinFeatures;
	private ResynthFeatures resynthFeatures;
	private SACFFeatures sacfFeatures;
	private MFCCFeatures mfccFeatures;
	private CepstrumFeatures cepstrumFeatures;
	private SpectralPeaksFeatures spectralPeaksFeatures;
	private double start;

	public AudioFeatureFrame(AudioFeatureProcessor audioFeatureProcessor, int frameSequence, double start, double end) {
		this.audioFeatureProcessor = audioFeatureProcessor;
		this.frameSequence = frameSequence;
		this.start = start;
		this.end = end;
	}

	public void close() {
		constantQFeatures.close();
	}

	public AudioFeatureProcessor getAudioFeatureProcessor() {
		return audioFeatureProcessor;
	}

	public ConstantQFeatures getConstantQFeatures() {
		return constantQFeatures;
	}

	public BeatFeatures getBeatFeatures() {
		return beatFeatures;
	}

	public double getEnd() {
		return end;
	}

	public int getFrameSequence() {
		return frameSequence;
	}

	public PercussionFeatures getPercussionFeatures() {
		return percussionFeatures;
	}

	public PitchDetectorFeatures getPitchDetectorFeatures() {
		return pitchDetectorFeatures;
	}

	public PhaseDetectorFeatures getPhaseDetectorFeatures() {
		return phaseDetectorFeatures;
	}

	public ResynthFeatures getResynthFeatures() {
		return resynthFeatures;
	}

	public YINFeatures getYINFeatures() {
		return yinFeatures;
	}

	public SACFFeatures getSACFFeatures() {
		return sacfFeatures;
	}

	public MFCCFeatures getMFCCFeatures() {
		return mfccFeatures;
	}

	public CepstrumFeatures getCepstrumFeatures() {
		return cepstrumFeatures;
	}

	public SpectralPeaksFeatures getSpectralPeaksFeatures() {
		return spectralPeaksFeatures;
	}

	public double getStart() {
		return start;
	}

	void initialise() {
		constantQFeatures = new ConstantQFeatures();
		percussionFeatures = new PercussionFeatures();
		beatFeatures = new BeatFeatures();
		spectralPeaksFeatures = new SpectralPeaksFeatures();
		pitchDetectorFeatures = new PitchDetectorFeatures();
		phaseDetectorFeatures = new PhaseDetectorFeatures();
		yinFeatures = new YINFeatures();
		resynthFeatures = new ResynthFeatures();
		sacfFeatures = new SACFFeatures();
		mfccFeatures = new MFCCFeatures();
		cepstrumFeatures = new CepstrumFeatures();
		constantQFeatures.initialise(this);
		percussionFeatures.initialise(this);
		spectralPeaksFeatures.initialise(this);
		pitchDetectorFeatures.initialise(this);
		phaseDetectorFeatures.initialise(this);
		yinFeatures.initialise(this);
		resynthFeatures.initialise(this);
		sacfFeatures.initialise(this);
		mfccFeatures.initialise(this);
		cepstrumFeatures.initialise(this);
		beatFeatures.initialise(this);
	}

}
