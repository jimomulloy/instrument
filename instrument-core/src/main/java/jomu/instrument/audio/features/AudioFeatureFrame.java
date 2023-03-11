package jomu.instrument.audio.features;

import java.util.logging.Logger;

public class AudioFeatureFrame {

	private static final Logger LOG = Logger.getLogger(AudioFeatureFrame.class.getName());

	private AudioFeatureProcessor audioFeatureProcessor;
	private ConstantQFeatures constantQFeatures;
	private CQMicroToneFeatures cqMicroToneFeatures;
	private double end;
	private int frameSequence;

	private PercussionFeatures percussionFeatures;
	private BeatFeatures beatFeatures;
	private PitchDetectorFeatures pitchDetectorFeatures;
	private YINFeatures yinFeatures;
	private ResynthFeatures resynthFeatures;
	private SACFFeatures sacfFeatures;
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

	public PercussionFeatures getPercussionFeatures() {
		return percussionFeatures;
	}

	public PitchDetectorFeatures getPitchDetectorFeatures() {
		return pitchDetectorFeatures;
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

	public SpectralPeaksFeatures getSpectralPeaksFeatures() {
		return spectralPeaksFeatures;
	}

	public double getStart() {
		return start;
	}

	void initialise() {
		constantQFeatures = new ConstantQFeatures();
		cqMicroToneFeatures = new CQMicroToneFeatures();
		percussionFeatures = new PercussionFeatures();
		beatFeatures = new BeatFeatures();
		spectralPeaksFeatures = new SpectralPeaksFeatures();
		pitchDetectorFeatures = new PitchDetectorFeatures();
		yinFeatures = new YINFeatures();
		resynthFeatures = new ResynthFeatures();
		sacfFeatures = new SACFFeatures();
		constantQFeatures.initialise(this);
		cqMicroToneFeatures.initialise(this);
		percussionFeatures.initialise(this);
		spectralPeaksFeatures.initialise(this);
		pitchDetectorFeatures.initialise(this);
		yinFeatures.initialise(this);
		resynthFeatures.initialise(this);
		sacfFeatures.initialise(this);
		beatFeatures.initialise(this);

	}

}
