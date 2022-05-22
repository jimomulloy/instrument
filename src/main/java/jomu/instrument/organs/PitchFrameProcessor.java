package jomu.instrument.organs;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.Oscilloscope;
import jomu.instrument.Instrument;
import jomu.instrument.audio.analysis.Analyzer;
import net.beadsproject.beads.analysis.SegmentListener;
import net.beadsproject.beads.core.TimeStamp;

public class PitchFrameProcessor implements SegmentListener {

	private Analyzer analyzer;
	private TarsosFeatureSource tarsosFeatures;
	private double interval = 100;
	private TimeStamp lastTimeStamp;
	private TimeStamp firstTimeStamp;
	private int frameSequence = 0;
	private int maxFrames = -1;

	private List<AudioFeatureObserver> observers = new ArrayList<>();

	private final static Map<TimeStamp, PitchFrame> pitchFrames = new Hashtable<TimeStamp, PitchFrame>();

	public PitchFrameProcessor(Analyzer analyzer, TarsosFeatureSource tarsosFeatures) {
		this.analyzer = analyzer;
		this.tarsosFeatures = tarsosFeatures;
		analyzer.addSegmentListener(this);
		addObserver(Instrument.getInstance().getDruid().getVisor());
		Oscilloscope oscilloscope = new Oscilloscope(Instrument.getInstance().getDruid().getVisor());
		tarsosFeatures.getTarsosIO().getDispatcher().addAudioProcessor(oscilloscope);
		lastTimeStamp = new TimeStamp(tarsosFeatures.getTarsosIO().getContext(), 0);
	}

	@Override
	public void newSegment(TimeStamp start, TimeStamp end) {
		System.out.println(">>TM segment at: " + start + ", " + end);
		if (maxFrames > 0 && maxFrames > frameSequence) {
			if (firstTimeStamp == null) {
				firstTimeStamp = start;
			}
			if (end.getTimeMS() - lastTimeStamp.getTimeMS() >= interval) {
				frameSequence++;
				System.out.println(">>TM Create Frame: " + frameSequence);
				createPitchFrame(firstTimeStamp, end);
				lastTimeStamp = end;
				firstTimeStamp = null;
			}
		}
	}

	public int getMaxFrames() {
		return maxFrames;
	}

	public void setMaxFrames(int maxFrames) {
		this.maxFrames = maxFrames;
	}

	private PitchFrame createPitchFrame(TimeStamp start, TimeStamp end) {
		System.out.println(">>TM Create Pitch Frame: " + start.getTimeMS() + ", " + end.getTimeMS());
		PitchFrame pitchFrame = new PitchFrame(this);
		pitchFrame.initialise(frameSequence, start, end);
		addPitchFrame(start, pitchFrame);
		return pitchFrame;
	}

	public void addObserver(AudioFeatureObserver observer) {
		this.observers.add(observer);
	}

	public void removeObserver(AudioFeatureObserver observer) {
		this.observers.remove(observer);
	}

	public void addPitchFrame(TimeStamp start, PitchFrame pitchFrame) {
		pitchFrames.put(start, pitchFrame);
		for (AudioFeatureObserver observer : this.observers) {
			observer.pitchFrameAdded(pitchFrame);
		}
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public TarsosFeatureSource getTarsosFeatures() {
		return tarsosFeatures;
	}

	public double getInterval() {
		return interval;
	}

	public int getFrameSequence() {
		return frameSequence;
	}

	public List<AudioFeatureObserver> getObservers() {
		return observers;
	}

	public static Map<TimeStamp, PitchFrame> getPitchframes() {
		return pitchFrames;
	}
}
