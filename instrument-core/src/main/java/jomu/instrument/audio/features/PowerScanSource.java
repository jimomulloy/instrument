package jomu.instrument.audio.features;

import java.util.logging.Logger;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import jomu.instrument.Instrument;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class PowerScanSource extends AudioEventSource<Double> {

	private static final Logger LOG = Logger.getLogger(PowerScanSource.class.getName());
	private AudioDispatcher dispatcher;
	private float sampleRate;
	private ParameterManager parameterManager;
	private int windowSize;

	public PowerScanSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
	}

	public int getWindowSize() {
		return windowSize;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	void initialise() {
		int overlap = 0;
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, windowSize, overlap);
		djp.setName("POWER");
		dispatcher.addAudioProcessor(djp);
		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] values = audioEvent.getFloatBuffer();
				var numSamples = values.length;
				var total = 0;
				for (var cur = 0; cur < numSamples; cur++) {
					total += values[cur] * values[cur];
				}
				var result = Math.sqrt(total / numSamples);
				putFeature(audioEvent.getTimeStamp(), result);
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});
		clear();
	}

	@Override
	Double cloneFeatures(Double features) {
		return Double.valueOf(features);
	}

}
