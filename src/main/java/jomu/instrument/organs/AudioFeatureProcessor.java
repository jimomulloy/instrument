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

public class AudioFeatureProcessor implements SegmentListener, AudioProcessor {

	private Analyzer analyzer;
	private TarsosFeatureSource tarsosFeatures;
	private double interval = 100;
	private double lag = 0;
	private double lastTimeStamp = 0;
	private double firstTimeStamp = -1;
	private double endTimeStamp = -1;
	private int frameSequence = 0;
	private int maxFrames = -1;
	private String streamId;

	private List<AudioFeatureFrameObserver> observers = new ArrayList<>();

	private Map<Double, AudioFeatureFrame> audioFeatureFrames = new Hashtable<>();

	private Map<Integer, AudioFeatureFrame> audioFeatureFrameSequence = new Hashtable<>();
	private double currentProcessTime;

	public AudioFeatureProcessor(String streamId, Analyzer analyzer,
			TarsosFeatureSource tarsosFeatures) {
		this.streamId = streamId;
		this.analyzer = analyzer;
		this.tarsosFeatures = tarsosFeatures;
		analyzer.addSegmentListener(this);
		addObserver(Instrument.getInstance().getDruid().getVisor());
		Oscilloscope oscilloscope = new Oscilloscope(
				Instrument.getInstance().getDruid().getVisor());
		tarsosFeatures.getTarsosIO().getDispatcher()
				.addAudioProcessor(oscilloscope);
	}

	public void addAudioFeatureFrame(double time,
			AudioFeatureFrame audioFeatureFrame) {
		audioFeatureFrames.put(audioFeatureFrame.getStart(), audioFeatureFrame);
		audioFeatureFrameSequence.put(audioFeatureFrame.getFrameSequence(),
				audioFeatureFrame);
		for (AudioFeatureFrameObserver observer : this.observers) {
			observer.audioFeatureFrameAdded(audioFeatureFrame);
		}
	}

	public void addObserver(AudioFeatureFrameObserver observer) {
		this.observers.add(observer);
	}

	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		for (AudioFeatureFrameObserver observer : this.observers) {
			observer.audioFeatureFrameChanged(audioFeatureFrame);
		}
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public AudioFeatureFrame getAudioFeatureFrame(double startTime) {
		return audioFeatureFrames.get(startTime);
	}

	public AudioFeatureFrame getAudioFeatureFrame(int frameSequence) {
		return audioFeatureFrameSequence.get(frameSequence);
	}

	public Map<Double, AudioFeatureFrame> getAudioFeatureFrames() {
		return audioFeatureFrames;
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

	public List<AudioFeatureFrameObserver> getObservers() {
		return observers;
	}

	public String getStreamId() {
		return streamId;
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
			if (endTimeStamp == -1
					&& (end.getTimeMS() - lastTimeStamp >= interval)) {
				endTimeStamp = end.getTimeMS();
			}
			if (end.getTimeMS() - lastTimeStamp >= (interval + lag)) {
				frameSequence++;
				createAudioFeatureFrame(frameSequence, firstTimeStamp,
						endTimeStamp);
				lastTimeStamp = endTimeStamp;
				firstTimeStamp = -1;
				endTimeStamp = -1;
			}
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
		AudioFeatureFrame lastPitchFrame = createAudioFeatureFrame(
				frameSequence, lastTimeStamp, currentProcessTime);
		lastPitchFrame.close();
	}

	public void removeObserver(AudioFeatureFrameObserver observer) {
		this.observers.remove(observer);
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public void setMaxFrames(int maxFrames) {
		this.maxFrames = maxFrames;
	}

	private AudioFeatureFrame createAudioFeatureFrame(int frameSequence,
			double firstTimeStamp, double endTimeStamp) {
		AudioFeatureFrame audioFeatureFrame = new AudioFeatureFrame(this,
				frameSequence, firstTimeStamp, endTimeStamp);
		audioFeatureFrame.initialise();
		addAudioFeatureFrame(firstTimeStamp, audioFeatureFrame);
		return audioFeatureFrame;
	}
}
