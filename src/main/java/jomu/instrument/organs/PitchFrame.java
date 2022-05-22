package jomu.instrument.organs;

import java.util.List;

import jomu.instrument.audio.analysis.FeatureFrame;
import jomu.instrument.audio.analysis.FeatureSet;
import jomu.instrument.tonemap.PitchSet;
import jomu.instrument.tonemap.TimeSet;
import net.beadsproject.beads.core.TimeStamp;

public class PitchFrame {
	private List<FeatureFrame> beadsBeatsFeatures;
	private List<FeatureFrame> beadsFeatures;
	private ConstantQFeatures constantQFeatures;
	private SpectralPeaksFeatures spectralPeaksFeatures;
	private PitchDetectorFeatures pitchDetectorFeatures;
	private SpectrogramFeatures spectrogramFeatures;
	private GoertzelFeatures goertzelFeatures;
	private AudioEventFeatures audioEventFeatures;

	private TimeSet timeSet;
	private PitchSet pitchSet;
	private int frameSequence;
	private PitchFrameProcessor pitchFrameProcessor;

	public PitchFrame(PitchFrameProcessor pitchFrameProcessor) {
		this.pitchFrameProcessor = pitchFrameProcessor;
	}

	void initialise(int frameSequence, TimeStamp start, TimeStamp end) {
		this.frameSequence = frameSequence;
		FeatureSet results = this.pitchFrameProcessor.getAnalyzer().getResults();
		beadsFeatures = results.get("Low Level").getRange(start.getTimeMS(), end.getTimeMS());
		beadsBeatsFeatures = results.get("Beats").getRange(start.getTimeMS(), end.getTimeMS());
		results.get("Low Level").removeRange(start.getTimeMS(), end.getTimeMS());
		results.get("Beats").getRange(start.getTimeMS(), end.getTimeMS());
		audioEventFeatures = new AudioEventFeatures();
		constantQFeatures = new ConstantQFeatures();
		spectralPeaksFeatures = new SpectralPeaksFeatures();
		pitchDetectorFeatures = new PitchDetectorFeatures();
		spectrogramFeatures = new SpectrogramFeatures();
		goertzelFeatures = new GoertzelFeatures();
		audioEventFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getAudioEventSource());
		constantQFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getConstantQSource());
		spectralPeaksFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getSpectralPeaksSource());
		pitchDetectorFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getPitchDetectorSource());
		spectrogramFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getSpectrogramSource());
		goertzelFeatures.initialise(this.pitchFrameProcessor.getTarsosFeatures().getGoertzelSource());
		System.out.println(">> PitchFrame: " + start.getTimeMS() + ", " + start);
		for (FeatureFrame beadsFeatureFrame : beadsFeatures) {
			System.out.println(">> BEADS FRAME B: " + beadsFeatureFrame.getStartTimeMS() + ", "
					+ beadsFeatureFrame.getEndTimeMS());
			// System.out.println(beadsFeatureFrame);
		}
		for (Double entry : constantQFeatures.getFeatures().keySet()) {
			System.out.println(">> CQ Feature: " + entry);
		}
		for (Double entry : spectralPeaksFeatures.getFeatures().keySet()) {
			System.out.println(">> SP Feature: " + entry);
		}
		for (Double entry : pitchDetectorFeatures.getFeatures().keySet()) {
			System.out.println(">> PD Feature: " + entry);
		}
		for (Double entry : spectrogramFeatures.getFeatures().keySet()) {
			System.out.println(">> SG Feature: " + entry);
		}
		for (Double entry : goertzelFeatures.getFeatures().keySet()) {
			System.out.println(">> GZ Feature: " + entry);
		}
		for (Double entry : audioEventFeatures.getFeatures().keySet()) {
			System.out.println(">> AE Feature: " + entry);
		}
		// timeSet = new TimeSet();
		// pitchSet = new PitchSet();
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

	public GoertzelFeatures getGoertzelFeatures() {
		return goertzelFeatures;
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}

	public int getFrameSequence() {
		return frameSequence;
	}

	public PitchFrameProcessor getPitchFrameProcessor() {
		return pitchFrameProcessor;
	}

}
