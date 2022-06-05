package jomu.instrument.organs;

import java.util.List;

import jomu.instrument.audio.analysis.FeatureFrame;
import jomu.instrument.audio.analysis.FeatureSet;

public class PitchFrame {
	private List<FeatureFrame> beadsBeatsFeatures;
	private List<FeatureFrame> beadsFeatures;
	private ConstantQFeatures constantQFeatures;
	private SpectralPeaksFeatures spectralPeaksFeatures;
	private PitchDetectorFeatures pitchDetectorFeatures;
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
		spectralPeaksFeatures = new SpectralPeaksFeatures();
		pitchDetectorFeatures = new PitchDetectorFeatures();
		spectrogramFeatures = new SpectrogramFeatures();
		goertzelFeatures = new GoertzelFeatures();
		scalogramFeatures = new ScalogramFeatures();
		audioEventFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getAudioEventSource());
		constantQFeatures.initialise(this);
		spectralPeaksFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getSpectralPeaksSource());
		pitchDetectorFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getPitchDetectorSource());
		spectrogramFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getSpectrogramSource());
		goertzelFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getGoertzelSource());
		scalogramFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getScalogramSource());
		// System.out.println(">> PitchFrame: " + start + ", " + start);
		for (FeatureFrame beadsFeatureFrame : beadsFeatures) {
			// System.out.println(">> BEADS FRAME B: " + beadsFeatureFrame.getStartTimeMS()
			// + ", "
			// + beadsFeatureFrame.getEndTimeMS());
			// System.out.println(beadsFeatureFrame);
		}
		for (Double entry : constantQFeatures.getFeatures().keySet()) {
			// System.out.println(">> CQ Feature: " + entry);
		}
		for (Double entry : spectralPeaksFeatures.getFeatures().keySet()) {
			// System.out.println(">> SP Feature: " + entry);
		}
		for (Double entry : pitchDetectorFeatures.getFeatures().keySet()) {
			// System.out.println(">> PD Feature: " + entry);
		}
		for (Double entry : spectrogramFeatures.getFeatures().keySet()) {
			// System.out.println(">> SG Feature: " + entry);
		}
		for (Double entry : goertzelFeatures.getFeatures().keySet()) {
			// System.out.println(">> GZ Feature: " + entry);
		}
		for (Double entry : audioEventFeatures.getFeatures().keySet()) {
			// System.out.println(">> AE Feature: " + entry);
		}
		for (Double entry : scalogramFeatures.getFeatures().keySet()) {
			// System.out.println(">> SC Feature: " + entry);
		}
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

	public SpectralPeaksFeatures getSpectralPeaksFeatures() {
		return spectralPeaksFeatures;
	}

	public PitchDetectorFeatures getPitchDetectorFeatures() {
		return pitchDetectorFeatures;
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
