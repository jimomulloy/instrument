package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import jomu.instrument.audio.DispatchJunctionProcessor;

public class BeatSource implements OnsetHandler {

	private Map<Double, OnsetInfo[]> features = new TreeMap<>();
	private List<OnsetInfo> onsetInfos = new ArrayList<>();
	int increment = 1024;
	int overlap = 0;
	float sampleRate = 44100;

	double threshold = 0.4;
	private AudioDispatcher dispatcher;

	public BeatSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
	}

	public TreeMap<Double, OnsetInfo[]> getFeatures() {
		TreeMap<Double, OnsetInfo[]> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, OnsetInfo[]> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public int getIncrement() {
		return increment;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public void handleOnset(double time, double salience) {
		onsetInfos.add(new OnsetInfo(time, salience));
	}

	void clear() {
		features.clear();
	}

	void initialise() {

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);

		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, increment, overlap);
		djp.setName("SC");
		dispatcher.addAudioProcessor(djp);

		// djp.setZeroPadFirstBuffer(true);

		PercussionOnsetDetector beatDetector = new PercussionOnsetDetector(sampleRate, increment, overlap, this);

		beatDetector.setHandler(this);

		djp.addAudioProcessor(beatDetector);

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
