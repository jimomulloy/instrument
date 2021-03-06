package jomu.instrument.organs;

import java.util.TreeMap;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.wavelet.lift.Daubechies4Wavelet;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.audio.TarsosAudioIO;

public class ScalogramSource {

	TarsosAudioIO tarsosIO;
	float sampleRate = 44100;
	int increment = 1024;

	Daubechies4Wavelet wt = new Daubechies4Wavelet();
	TreeMap<Double, ScalogramFrame> features = new TreeMap<Double, ScalogramFrame>();
	ScalogramFrame prevFrame;

	public ScalogramSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
		this.sampleRate = tarsosIO.getContext().getSampleRate();
	}

	void initialise() {

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);

		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, 131072, 0);
		djp.setName("SC");
		tarsosIO.getDispatcher().addAudioProcessor(djp);

		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] audioBuffer = audioEvent.getFloatBuffer().clone();
				wt.forwardTrans(audioBuffer);
				float currentMax = 0;
				if (prevFrame != null) {
					currentMax = prevFrame.currentMax * 0.99f;
				}
				ScalogramFrame currentFrame = new ScalogramFrame(audioBuffer, currentMax);
				features.put(audioEvent.getTimeStamp(), currentFrame);
				prevFrame = currentFrame;
				return true;
			}

			@Override
			public void processingFinished() {
				// TODO Auto-generated method stub

			}
		});

		features.clear();
	}

	void clear() {
		features.clear();
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public int getIncrement() {
		return increment;
	}

	public TreeMap<Double, ScalogramFrame> getFeatures() {
		TreeMap<Double, ScalogramFrame> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, ScalogramFrame> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

}
