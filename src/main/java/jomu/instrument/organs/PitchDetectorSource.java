package jomu.instrument.organs;

import java.util.ArrayList;
import java.util.List;

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
	List<PitchDetectionResult> features = new ArrayList<PitchDetectionResult>();

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

	public List<PitchDetectionResult> getFeatures() {
		List<PitchDetectionResult> clonedFeatures = new ArrayList<PitchDetectionResult>();
		for (PitchDetectionResult pdr : features) {
			clonedFeatures.add(pdr);
		}
		return clonedFeatures;
	}

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		features.add(pitchDetectionResult);
	}
}
