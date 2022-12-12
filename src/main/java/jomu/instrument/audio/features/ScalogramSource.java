package jomu.instrument.audio.features;

import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.wavelet.lift.Daubechies4Wavelet;
import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.ParameterManager;

public class ScalogramSource {

	private TreeMap<Double, ScalogramFrame> features = new TreeMap<>();
	private int windowSize = 1024;
	private ScalogramFrame prevFrame;

	private float sampleRate = 44100;
	private Daubechies4Wavelet wt = new Daubechies4Wavelet();

	private AudioDispatcher dispatcher;
	private ParameterManager parameterManager;

	public ScalogramSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
	}

	public TreeMap<Double, ScalogramFrame> getFeatures() {
		TreeMap<Double, ScalogramFrame> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, ScalogramFrame> entry : features.entrySet()) {
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

	void clear() {
		features.clear();
	}

	void initialise() {

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);

		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, 131072, 0);
		djp.setName("SC");
		dispatcher.addAudioProcessor(djp);

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

}
