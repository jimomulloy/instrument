package jomu.instrument.organs;

import java.util.List;

import jomu.instrument.audio.analysis.FeatureFrame;
import jomu.instrument.audio.analysis.FeatureSet;

public class PitchFrame {
	private List<FeatureFrame> beadsBeatsFeatures;
	private List<FeatureFrame> beadsFeatures;
	private ConstantQFeatures constantQFeatures;
	private OnsetFeatures onsetFeatures;
	private SpectralPeaksFeatures spectralPeaksFeatures;
	private PitchDetectorFeatures pitchDetectorFeatures;
	private BandedPitchDetectorFeatures bandedPitchDetectorFeatures;
	private SpectrogramFeatures spectrogramFeatures;
	private GoertzelFeatures goertzelFeatures;
	private AudioEventFeatures audioEventFeatures;
	private ScalogramFeatures scalogramFeatures;

	private int frameSequence;
	private PitchFrameProcessor pitchFrameProcessor;
	private double start;
	private double end;

	public PitchFrame(PitchFrameProcessor pitchFrameProcessor, int frameSequence, double start, double end) {
		this.pitchFrameProcessor = pitchFrameProcessor;
		this.frameSequence = frameSequence;
		this.start = start;
		this.end = end;
	}

	void initialise() {
		System.out.println(">>PF INIT!!!: " + this.frameSequence + ", " + this.start);
		FeatureSet results = this.pitchFrameProcessor.getAnalyzer().getResults();
		beadsFeatures = results.get("Low Level").getRange(start, end);
		beadsBeatsFeatures = results.get("Beats").getRange(start, end);
		results.get("Low Level").removeRange(start, end);
		results.get("Beats").getRange(start, end);
		audioEventFeatures = new AudioEventFeatures();
		constantQFeatures = new ConstantQFeatures();
		onsetFeatures = new OnsetFeatures();
		spectralPeaksFeatures = new SpectralPeaksFeatures();
		pitchDetectorFeatures = new PitchDetectorFeatures();
		bandedPitchDetectorFeatures = new BandedPitchDetectorFeatures();
		spectrogramFeatures = new SpectrogramFeatures();
		goertzelFeatures = new GoertzelFeatures();
		scalogramFeatures = new ScalogramFeatures();
		audioEventFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getAudioEventSource());
		constantQFeatures.initialise(this);
		onsetFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getOnsetSource());
		spectralPeaksFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getSpectralPeaksSource());
		pitchDetectorFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getPitchDetectorSource());
		bandedPitchDetectorFeatures
				.initialise(this.pitchFrameProcessor.getTarsosFeatures().getBandedPitchDetectorSource());
		spectrogramFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getSpectrogramSource());
		goertzelFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getGoertzelSource());
		scalogramFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getScalogramSource());

	}

	public List<FeatureFrame> getBeadsBeatsFeatures() {
		return beadsBeatsFeatures;
	}

	public List<FeatureFrame> getBeadsFeatures() {
		return beadsFeatures;
	}

	public ConstantQFeatures getConstantQFeatures() {
		return constantQFeatures;
	}

	public OnsetFeatures getOnsetFeatures() {
		return onsetFeatures;
	}

	public SpectralPeaksFeatures getSpectralPeaksFeatures() {
		return spectralPeaksFeatures;
	}

	public PitchDetectorFeatures getPitchDetectorFeatures() {
		return pitchDetectorFeatures;
	}

	public BandedPitchDetectorFeatures getBandedPitchDetectorFeatures() {
		return bandedPitchDetectorFeatures;
	}

	public SpectrogramFeatures getSpectrogramFeatures() {
		return spectrogramFeatures;
	}

	public ScalogramFeatures getScalogramFeatures() {
		return scalogramFeatures;
	}

	public GoertzelFeatures getGoertzelFeatures() {
		return goertzelFeatures;
	}

	public int getFrameSequence() {
		return frameSequence;
	}

	public PitchFrameProcessor getPitchFrameProcessor() {
		return pitchFrameProcessor;
	}

	public double getStart() {
		return start;
	}

	public double getEnd() {
		return end;
	}

	public void close() {
		constantQFeatures.close();
	}

}
