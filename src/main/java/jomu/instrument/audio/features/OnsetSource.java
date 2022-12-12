package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.ParameterManager;

public class OnsetSource implements OnsetHandler {

	private Map<Double, OnsetInfo[]> features = new TreeMap<>();
	private List<OnsetInfo> onsetInfos = new ArrayList<>();
	private int windowSize = 1024;
	private float sampleRate = 44100;

	private AudioDispatcher dispatcher;
	private ParameterManager parameterManager;

	public OnsetSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
	}

	public TreeMap<Double, OnsetInfo[]> getFeatures() {
		TreeMap<Double, OnsetInfo[]> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, OnsetInfo[]> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public int getIncrement() {
		return windowSize;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public void handleOnset(double time, double salience) {
		// System.out.println(">>Percussion at:" + time + ", " + salience);
		onsetInfos.add(new OnsetInfo(time, salience));
	}

	void clear() {
		features.clear();
	}

	void initialise() {

		int overlap = 0;
		double threshold = 0.4;

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);

		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, windowSize, overlap);
		djp.setName("SC");
		dispatcher.addAudioProcessor(djp);

		ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector(windowSize, threshold);
		onsetDetector.setHandler(this);
		// add a processor, handle percussion event.
		djp.addAudioProcessor(onsetDetector);

		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				features.put(audioEvent.getTimeStamp(), onsetInfos.toArray((new OnsetInfo[onsetInfos.size()])));
				onsetInfos.clear();
				return true;
			}

			@Override
			public void processingFinished() {
				// TODO Auto-generated method stub

			}
		});

		features.clear();
	}

	void removeFeatures(double endTime) {
		features.keySet().removeIf(key -> key <= endTime);
	}

}
