package jomu.instrument.organs;

import java.util.TreeMap;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.audio.TarsosAudioIO;

public class PitchDetectorSource implements PitchDetectionHandler {

	TarsosAudioIO tarsosIO;
	private TreeMap<Double, PitchDetectionResult> features = new TreeMap<>();

	public PitchDetectorSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
	}

	void initialise() {
		int sampleRate = 44100;
		int bufferSize = 1024;
		int overlap = 0;
		PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.YIN;
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, bufferSize, overlap);
		djp.setName("PD");
		tarsosIO.getDispatcher().addAudioProcessor(djp);
		djp.addAudioProcessor(new PitchProcessor(algo, sampleRate, bufferSize, this));

		features.clear();
	}

	void clear() {
		features.clear();
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public TreeMap<Double, PitchDetectionResult> getFeatures() {
		TreeMap<Double, PitchDetectionResult> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, PitchDetectionResult> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		features.put(audioEvent.getTimeStamp(), pitchDetectionResult);
	}
}
