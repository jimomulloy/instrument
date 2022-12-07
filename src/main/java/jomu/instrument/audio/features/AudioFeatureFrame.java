package jomu.instrument.audio.features;

public class AudioFeatureFrame {
	// private AudioEventFeatures audioEventFeatures;
	private AudioFeatureProcessor audioFeatureProcessor;
	private BandedPitchDetectorFeatures bandedPitchDetectorFeatures;
	// private List<FeatureFrame> beadsBeatsFeatures;
	// private List<FeatureFrame> beadsFeatures;
	private ConstantQFeatures constantQFeatures;
	private SpectrumFeatures spectrumFeatures;
	private double end;
	private int frameSequence;
	private GoertzelFeatures goertzelFeatures;
	private OnsetFeatures onsetFeatures;
	private BeatFeatures beatFeatures;
	private PitchDetectorFeatures pitchDetectorFeatures;

	private ScalogramFeatures scalogramFeatures;
	private SpectralPeaksFeatures spectralPeaksFeatures;
	private SpectrogramFeatures spectrogramFeatures;
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

	public BandedPitchDetectorFeatures getBandedPitchDetectorFeatures() {
		return bandedPitchDetectorFeatures;
	}

	// public List<FeatureFrame> getBeadsBeatsFeatures() {
	// return beadsBeatsFeatures;
	// }

	// public List<FeatureFrame> getBeadsFeatures() {
	// return beadsFeatures;
	// }

	public SpectrumFeatures getSpectrumFeatures() {
		return spectrumFeatures;
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

	public GoertzelFeatures getGoertzelFeatures() {
		return goertzelFeatures;
	}

	public OnsetFeatures getOnsetFeatures() {
		return onsetFeatures;
	}

	public PitchDetectorFeatures getPitchDetectorFeatures() {
		return pitchDetectorFeatures;
	}

	public ScalogramFeatures getScalogramFeatures() {
		return scalogramFeatures;
	}

	public SpectralPeaksFeatures getSpectralPeaksFeatures() {
		return spectralPeaksFeatures;
	}

	public SpectrogramFeatures getSpectrogramFeatures() {
		return spectrogramFeatures;
	}

	public double getStart() {
		return start;
	}

	void initialise() {
		System.out.println(">>PF INIT!!!: " + this.frameSequence + ", " + this.start);
		// FeatureSet results = this.audioFeatureProcessor.getAnalyzer()
		// .getResults();
		// beadsFeatures = results.get("Low Level").getRange(start, end);
		// beadsBeatsFeatures = results.get("Beats").getRange(start, end);
		// results.get("Low Level").removeRange(start, end);
		// results.get("Beats").getRange(start, end);
		// audioEventFeatures = new AudioEventFeatures();
		constantQFeatures = new ConstantQFeatures();
		onsetFeatures = new OnsetFeatures();
		beatFeatures = new BeatFeatures();
		spectralPeaksFeatures = new SpectralPeaksFeatures();
		pitchDetectorFeatures = new PitchDetectorFeatures();
		bandedPitchDetectorFeatures = new BandedPitchDetectorFeatures();
		spectrogramFeatures = new SpectrogramFeatures();
		goertzelFeatures = new GoertzelFeatures();
		scalogramFeatures = new ScalogramFeatures();
		spectrumFeatures = new SpectrumFeatures();
		// audioEventFeatures.initialise(this.audioFeatureProcessor
		// .getTarsosFeatures().getAudioEventSource());
		constantQFeatures.initialise(this);
		onsetFeatures.initialise(this);
		spectralPeaksFeatures.initialise(this);
		pitchDetectorFeatures.initialise(this);
		bandedPitchDetectorFeatures
				.initialise(this.audioFeatureProcessor.getTarsosFeatures().getBandedPitchDetectorSource());
		spectrogramFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getSpectrogramSource());
		goertzelFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getGoertzelSource());
		scalogramFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getScalogramSource());
		spectrumFeatures.initialise(this);
		beatFeatures.initialise(this);

	}

}
