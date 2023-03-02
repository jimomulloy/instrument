package jomu.instrument.audio.features;

public class AudioFeatureFrame {
	private AudioFeatureProcessor audioFeatureProcessor;
	private ConstantQFeatures constantQFeatures;
	private CQMicroToneFeatures cqMicroToneFeatures;
	private double end;
	private int frameSequence;

	private OnsetFeatures onsetFeatures;
	private BeatFeatures beatFeatures;
	private PitchDetectorFeatures pitchDetectorFeatures;
	private YINFeatures yinFeatures;
	private ResynthFeatures resynthFeatures;
	private SACFFeatures sacfFeatures;
	private ScalogramFeatures scalogramFeatures;
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

	public CQMicroToneFeatures getCQMicroToneFeatures() {
		return cqMicroToneFeatures;
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

	public OnsetFeatures getOnsetFeatures() {
		return onsetFeatures;
	}

	public PitchDetectorFeatures getPitchDetectorFeatures() {
		return pitchDetectorFeatures;
	}

	public YINFeatures getYINFeatures() {
		return yinFeatures;
	}

	public SACFFeatures getSACFFeatures() {
		return sacfFeatures;
	}

	public ScalogramFeatures getScalogramFeatures() {
		return scalogramFeatures;
	}

	public SpectralPeaksFeatures getSpectralPeaksFeatures() {
		return spectralPeaksFeatures;
	}

	public double getStart() {
		return start;
	}

	void initialise() {
		constantQFeatures = new ConstantQFeatures();
		cqMicroToneFeatures = new CQMicroToneFeatures();
		onsetFeatures = new OnsetFeatures();
		beatFeatures = new BeatFeatures();
		spectralPeaksFeatures = new SpectralPeaksFeatures();
		pitchDetectorFeatures = new PitchDetectorFeatures();
		yinFeatures = new YINFeatures();
		resynthFeatures = new ResynthFeatures();
		sacfFeatures = new SACFFeatures();
		scalogramFeatures = new ScalogramFeatures();
		constantQFeatures.initialise(this);
		cqMicroToneFeatures.initialise(this);
		onsetFeatures.initialise(this);
		spectralPeaksFeatures.initialise(this);
		pitchDetectorFeatures.initialise(this);
		yinFeatures.initialise(this);
		resynthFeatures.initialise(this);
		sacfFeatures.initialise(this);
		scalogramFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getScalogramSource());
		beatFeatures.initialise(this);

	}

}
