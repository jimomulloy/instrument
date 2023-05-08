package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.beatroot.Agent;
import be.tarsos.dsp.beatroot.AgentList;
import be.tarsos.dsp.beatroot.Event;
import be.tarsos.dsp.beatroot.EventList;
import be.tarsos.dsp.beatroot.Induction;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import jomu.instrument.Instrument;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class BeatSource extends AudioEventSource<OnsetInfo[]> implements OnsetHandler {

	private static final Logger LOG = Logger.getLogger(BeatSource.class.getName());

	private List<OnsetInfo> onsetInfos = new ArrayList<>();
	private int windowSize = 1024;
	private int overlap = 0;
	private float sampleRate = 44100;

	private AudioDispatcher dispatcher;
	private ParameterManager parameterManager;

	private final EventList onsetList = new EventList();

	public BeatSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
	}

	public int getIncrement() {
		return windowSize;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public void handleOnset(double time, double salience) {
		onsetInfos.add(new OnsetInfo(time, salience, onsetList));
		double roundedTime = Math.round(time * 100) / 100.0;
		Event e = newEvent(roundedTime, 0);
		e.salience = salience;
		onsetList.add(e);
	}

	/**
	 * Guess the beats using the populated list of onsets.
	 * 
	 * @param beatHandler Use this handler to get the time of the beats. The
	 *                    salience of the beat is not calculated: -1 is returned.
	 */
	public void trackBeats(OnsetHandler beatHandler) {
		AgentList agents = null;
		// tempo not given; use tempo induction
		agents = Induction.beatInduction(onsetList);
		agents.beatTrack(onsetList, -1);
		Agent best = agents.bestAgent();
		if (best != null) {
			best.fillBeats(-1.0);
			EventList beats = best.events;
			Iterator<Event> eventIterator = beats.iterator();
			while (eventIterator.hasNext()) {
				Event beat = eventIterator.next();
				double time = beat.keyDown;
				beatHandler.handleOnset(time, -1);
			}
		} else {
			LOG.finer(">> BeatSource trackBeats - no best agent");
		}
	}

	/**
	 * Creates a new Event object representing an onset or beat.
	 * 
	 * @param time    The time of the beat in seconds
	 * @param beatNum The index of the beat or onset.
	 * @return The Event object representing the beat or onset.
	 */
	private Event newEvent(double time, int beatNum) {
		return new Event(time, time, time, 56, 64, beatNum, 0, 1);
	}

	void initialise() {

		double threshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_THRESHOLD);

		double onsetInterval = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_INTERVAL);

		int overlap = 0;

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);

		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, windowSize, overlap);
		djp.setName("BEAT");
		dispatcher.addAudioProcessor(djp);

		ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector(windowSize, threshold, onsetInterval);
		onsetDetector.setHandler(this);
		djp.addAudioProcessor(onsetDetector);

		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				putFeature(audioEvent.getTimeStamp(), onsetInfos.toArray((new OnsetInfo[onsetInfos.size()])));
				onsetInfos.clear();
				return true;
			}

			@Override
			public void processingFinished() {
				// TODO Auto-generated method stub

			}
		});

		clear();
	}

	@Override
	OnsetInfo[] cloneFeatures(OnsetInfo[] features) {
		return features.clone();
	}

}
