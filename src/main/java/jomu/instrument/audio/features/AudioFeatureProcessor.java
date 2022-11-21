package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.Oscilloscope;
import jomu.instrument.Instrument;

public class AudioFeatureProcessor implements AudioProcessor {

	private Map<Double, AudioFeatureFrame> audioFeatureFrames = new Hashtable<>();
	private Map<Integer, AudioFeatureFrame> audioFeatureFrameSequence = new Hashtable<>();
	private double currentProcessTime;
	private double endTimeStamp = -1;
	private double firstTimeStamp = -1;
	private int frameSequence = 0;
	private double interval = 100;
	private double lag = 0;
	private double lastTimeStamp = 0;
	private int lastSequence = -1;

	private int maxFrames = -1;

	private List<AudioFeatureFrameObserver> observers = new ArrayList<>();

	private String streamId;
	private TarsosFeatureSource tarsosFeatures;
	private AudioFeatureFrameState state = AudioFeatureFrameState.INITIALISED;

	public AudioFeatureProcessor(String streamId, TarsosFeatureSource tarsosFeatures) {
		this.streamId = streamId;
		this.tarsosFeatures = tarsosFeatures;
		addObserver(Instrument.getInstance().getDruid().getVisor());
		Oscilloscope oscilloscope = new Oscilloscope(Instrument.getInstance().getDruid().getVisor());
		tarsosFeatures.getDispatcher().addAudioProcessor(oscilloscope);
	}

	public void addAudioFeatureFrame(double time, AudioFeatureFrame audioFeatureFrame) {
		audioFeatureFrames.put(audioFeatureFrame.getStart(), audioFeatureFrame);
		audioFeatureFrameSequence.put(audioFeatureFrame.getFrameSequence(), audioFeatureFrame);
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

	public int getLastSequence() {
		return lastSequence;
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

	public boolean isClosed() {
		return AudioFeatureFrameState.CLOSED.equals(state);
	}

	public boolean isLastSequence(int sequence) {
		return (getLastSequence() > -1 && sequence >= getLastSequence());
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		double startTimeMS = audioEvent.getTimeStamp() * 1000;
		// System.out.println(">>process audioEvent startTimeMS: " + startTimeMS + ",
		// firstTimeStamp: " + firstTimeStamp
		// + ", lastTimeStamp: " + lastTimeStamp + ", endTimeStamp: " + endTimeStamp +
		// ", frameSequence: "
		// + frameSequence);
		if (lastTimeStamp < startTimeMS) {
			if (maxFrames > 0 && maxFrames > frameSequence) {
				if (firstTimeStamp == -1) {
					firstTimeStamp = lastTimeStamp;
				}
				if (endTimeStamp == -1 && (startTimeMS - lastTimeStamp >= interval)) {
					endTimeStamp = startTimeMS;
				}
				if (startTimeMS - lastTimeStamp >= (interval + lag)) {
					frameSequence++;
					System.out.println(">>process audioEvent startTimeMS: " + startTimeMS + ", firstTimeStamp: "
							+ firstTimeStamp + ", lastTimeStamp: " + lastTimeStamp + ", endTimeStamp: " + endTimeStamp
							+ ", frameSequence: " + frameSequence);
					createAudioFeatureFrame(frameSequence, firstTimeStamp, endTimeStamp);
					lastTimeStamp = startTimeMS;
					firstTimeStamp = -1;
					endTimeStamp = -1;
				}
			}
		}
		currentProcessTime = startTimeMS;
		return true;
	}

	public boolean process2(AudioEvent audioEvent) {
		double startTimeMS = audioEvent.getTimeStamp() * 1000;
		if (currentProcessTime < startTimeMS) {
			if (maxFrames > 0 && maxFrames > frameSequence) {
				if (firstTimeStamp == -1) {
					firstTimeStamp = startTimeMS;
				}
				if (endTimeStamp == -1 && (currentProcessTime - lastTimeStamp >= interval)) {
					endTimeStamp = currentProcessTime;
				}
				if (currentProcessTime - lastTimeStamp >= (interval + lag)) {
					frameSequence++;
					createAudioFeatureFrame(frameSequence, firstTimeStamp, endTimeStamp);
					lastTimeStamp = endTimeStamp;
					firstTimeStamp = -1;
					endTimeStamp = -1;
				}
			}
		}
		currentProcessTime = audioEvent.getTimeStamp() * 1000;
		return true;
	}

	@Override
	public void processingFinished() {
		frameSequence++;
		AudioFeatureFrame lastPitchFrame = createAudioFeatureFrame(frameSequence, lastTimeStamp, currentProcessTime);
		lastPitchFrame.close();
		state = AudioFeatureFrameState.CLOSED;
		lastSequence = frameSequence;
		Instrument.getInstance().getCoordinator().getHearing().closeAudioStream(streamId);
	}

	public void removeObserver(AudioFeatureFrameObserver observer) {
		this.observers.remove(observer);
	}

	public void setMaxFrames(int maxFrames) {
		this.maxFrames = maxFrames;
	}

	private AudioFeatureFrame createAudioFeatureFrame(int frameSequence, double firstTimeStamp, double endTimeStamp) {
		AudioFeatureFrame audioFeatureFrame = new AudioFeatureFrame(this, frameSequence, firstTimeStamp, endTimeStamp);
		audioFeatureFrame.initialise();
		addAudioFeatureFrame(firstTimeStamp, audioFeatureFrame);
		return audioFeatureFrame;
	}
}
