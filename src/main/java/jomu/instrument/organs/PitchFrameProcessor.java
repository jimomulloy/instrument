package jomu.instrument.organs;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.Oscilloscope;
import jomu.instrument.Instrument;
import jomu.instrument.audio.analysis.Analyzer;
import net.beadsproject.beads.analysis.SegmentListener;
import net.beadsproject.beads.core.TimeStamp;

public class PitchFrameProcessor implements SegmentListener, AudioProcessor {

	private Analyzer analyzer;
	private TarsosFeatureSource tarsosFeatures;
	private double interval = 100;
	private double lag = 0;
	private double lastTimeStamp = 0;
	private double firstTimeStamp = -1;
	private double endTimeStamp = -1;
	private int frameSequence = 0;
	private int maxFrames = -1;

	private List<PitchFrameObserver> observers = new ArrayList<>();

	private Map<Double, PitchFrame> pitchFrames = new Hashtable<Double, PitchFrame>();

	private Map<Integer, PitchFrame> pitchFrameSequence = new Hashtable<Integer, PitchFrame>();
	private double currentProcessTime;

	public PitchFrameProcessor(Analyzer analyzer, TarsosFeatureSource tarsosFeatures) {
		this.analyzer = analyzer;
		this.tarsosFeatures = tarsosFeatures;
		analyzer.addSegmentListener(this);
		addObserver(Instrument.getInstance().getDruid().getVisor());
		Oscilloscope oscilloscope = new Oscilloscope(Instrument.getInstance().getDruid().getVisor());
		tarsosFeatures.getTarsosIO().getDispatcher().addAudioProcessor(oscilloscope);
	}

	public void addObserver(PitchFrameObserver observer) {
		this.observers.add(observer);
	}

	public void addPitchFrame(double time, PitchFrame pitchFrame) {
		pitchFrames.put(pitchFrame.getStart(), pitchFrame);
		pitchFrameSequence.put(pitchFrame.getFrameSequence(), pitchFrame);
		for (PitchFrameObserver observer : this.observers) {
			observer.pitchFrameAdded(pitchFrame);
		}
	}

	private PitchFrame createPitchFrame(int frameSequence, double firstTimeStamp, double endTimeStamp) {
		PitchFrame pitchFrame = new PitchFrame(this, frameSequence, firstTimeStamp, endTimeStamp);
		pitchFrame.initialise();
		addPitchFrame(firstTimeStamp, pitchFrame);
		return pitchFrame;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public int getFrameSequence() {
		return frameSequence;
	}

	public double getInterval() {
		return interval;
	}

	public int getMaxFrames() {
		return maxFrames;
	}

	public List<PitchFrameObserver> getObservers() {
		return observers;
	}

	public PitchFrame getPitchFrame(double startTime) {
		return pitchFrames.get(startTime);
	}

	public PitchFrame getPitchFrame(int frameSequence) {
		return pitchFrameSequence.get(frameSequence);
	}

	public Map<Double, PitchFrame> getPitchframes() {
		return pitchFrames;
	}

	public TarsosFeatureSource getTarsosFeatures() {
		return tarsosFeatures;
	}

	@Override
	public void newSegment(TimeStamp start, TimeStamp end) {
		if (maxFrames > 0 && maxFrames > frameSequence) {
			if (firstTimeStamp == -1) {
				firstTimeStamp = start.getTimeMS();
			}
			if (endTimeStamp == -1 && (end.getTimeMS() - lastTimeStamp >= interval)) {
				endTimeStamp = end.getTimeMS();
			}
			if (end.getTimeMS() - lastTimeStamp >= (interval + lag)) {
				frameSequence++;
				createPitchFrame(frameSequence, firstTimeStamp, endTimeStamp);
				lastTimeStamp = endTimeStamp;
				firstTimeStamp = -1;
				endTimeStamp = -1;
			}
		}
	}

	public void pitchFrameChanged(PitchFrame pitchFrame) {
		for (PitchFrameObserver observer : this.observers) {
			observer.pitchFrameChanged(pitchFrame);
		}
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		currentProcessTime = audioEvent.getEndTimeStamp() * 1000;
		return true;
	}

	@Override
	public void processingFinished() {
		frameSequence++;
		PitchFrame lastPitchFrame = createPitchFrame(frameSequence, lastTimeStamp, currentProcessTime);
		lastPitchFrame.close();
	}

	public void removeObserver(PitchFrameObserver observer) {
		this.observers.remove(observer);
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public void setMaxFrames(int maxFrames) {
		this.maxFrames = maxFrames;
	}
}
