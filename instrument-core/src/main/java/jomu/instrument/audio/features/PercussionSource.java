package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.onsets.OnsetHandler;
import jomu.instrument.Instrument;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class PercussionSource extends AudioEventSource<OnsetInfo[]> implements OnsetHandler {

	private static final Logger LOG = Logger.getLogger(PercussionSource.class.getName());

	private List<OnsetInfo> onsetInfos = new ArrayList<>();
	private int windowSize = 1024;
	private float sampleRate = 44100;

	private AudioDispatcher dispatcher;
	private ParameterManager parameterManager;

	private boolean microToneSwitch;

	public PercussionSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat()
				.getSampleRate();
		this.parameterManager = Instrument.getInstance()
				.getController()
				.getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
		this.microToneSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_MICRO_TONE_SWITCH);
	}

	public boolean isMicroToneSwitch() {
		return microToneSwitch;
	}

	public int getIncrement() {
		return windowSize;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public void handleOnset(double time, double salience) {
		onsetInfos.add(new OnsetInfo(time, salience));
	}

	void initialise() {

		double threshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_THRESHOLD);

		double sensitivity = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_SENSITIVITY);

		int overlap = 0;

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);

		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, windowSize, overlap);
		djp.setName("PERC");
		dispatcher.addAudioProcessor(djp);

		PercussionOnsetDetector onsetDetector = new PercussionOnsetDetector(sampleRate, windowSize, this, sensitivity,
				threshold);
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
