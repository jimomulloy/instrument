package jomu.instrument.organs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.audio.TarsosAudioIO;

public class OnsetSource implements OnsetHandler {

	private Map<Double, OnsetInfo[]> features = new TreeMap<>();
	private List<OnsetInfo> onsetInfos = new ArrayList<>();
	int increment = 1024;
	int overlap = 0;
	float sampleRate = 44100;
	TarsosAudioIO tarsosIO;
	double threshold = 0.4;

	public OnsetSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
		this.sampleRate = tarsosIO.getContext().getSampleRate();
	}

	public TreeMap<Double, OnsetInfo[]> getFeatures() {
		TreeMap<Double, OnsetInfo[]> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, OnsetInfo[]> entry : features
				.entrySet()) {
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

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
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

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(
				sampleRate, 16, 1, true, true);

		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(
				tarsosDSPFormat, increment, overlap);
		djp.setName("SC");
		tarsosIO.getDispatcher().addAudioProcessor(djp);

		ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector(increment,
				threshold);
		onsetDetector.setHandler(this);
		// add a processor, handle percussion event.
		djp.addAudioProcessor(onsetDetector);

		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				features.put(audioEvent.getTimeStamp(),
						onsetInfos.toArray((new OnsetInfo[onsetInfos.size()])));
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
