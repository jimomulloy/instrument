package jomu.instrument.organs;

import java.util.List;

import jomu.instrument.audio.analysis.FeatureFrame;
import jomu.instrument.audio.analysis.FeatureSet;

public class AudioFeatureFrame {
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
	private AudioFeatureProcessor audioFeatureProcessor;
	private double start;
	private double end;

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

	public List<FeatureFrame> getBeadsBeatsFeatures() {
		return beadsBeatsFeatures;
	}

	public List<FeatureFrame> getBeadsFeatures() {
		return beadsFeatures;
	}

	public ConstantQFeatures getConstantQFeatures() {
		return constantQFeatures;
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
		FeatureSet results = this.audioFeatureProcessor.getAnalyzer().getResults();
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
		audioEventFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getAudioEventSource());
		constantQFeatures.initialise(this);
		onsetFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getOnsetSource());
		spectralPeaksFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getSpectralPeaksSource());
		pitchDetectorFeatures.initialise(this);
		bandedPitchDetectorFeatures
				.initialise(this.audioFeatureProcessor.getTarsosFeatures().getBandedPitchDetectorSource());
		spectrogramFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getSpectrogramSource());
		goertzelFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getGoertzelSource());
		scalogramFeatures.initialise(this.audioFeatureProcessor.getTarsosFeatures().getScalogramSource());

	}

}
