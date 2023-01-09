package jomu.instrument.audio.features;

import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.ConstantQ;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.Instrument;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class ConstantQSource {

	private float binHeight;
	private float[] binStartingPointsInCents;
	private float binWidth;
	private Map<Double, float[]> features = new TreeMap<>();
	private int size;
	private float[] startingPointsInHertz;
	private int binsPerOctave = 12;
	private ConstantQ constantQ;
	private double constantQLag;
	private int windowSize = 1024;
	private int maximumFrequencyInCents = 10800;
	private int minimumFrequencyInCents = 3600;
	private float sampleRate = 44100;
	private AudioDispatcher dispatcher;
	private ParameterManager parameterManager;

	public ConstantQSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_CQ_WINDOW);
		System.out.println(">>CQ window: " + this.windowSize);

		this.minimumFrequencyInCents = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS);
		this.maximumFrequencyInCents = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS);
	}

	public float getBinHeight() {
		return binHeight;
	}

	public int getBinsPerOctave() {
		return binsPerOctave;
	}

	public float[] getBinStartingPointsInCents() {
		return binStartingPointsInCents;
	}

	public float getBinWidth() {
		return binWidth;
	}

	public ConstantQ getConstantQ() {
		return constantQ;
	}

	public double getConstantQLag() {
		return constantQLag;
	}

	public TreeMap<Double, float[]> getFeatures() {
		TreeMap<Double, float[]> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, float[]> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public int getIncrement() {
		return windowSize;
	}

	public int getMaximumFrequencyInCents() {
		return maximumFrequencyInCents;
	}

	public int getMinimumFrequencyInCents() {
		return minimumFrequencyInCents;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public int getSize() {
		return size;
	}

	public float[] getStartingPointsInHertz() {
		return startingPointsInHertz;
	}

	void clear() {
		features.clear();
	}

	void initialise() {
		float minimumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(minimumFrequencyInCents);
		float maximumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(maximumFrequencyInCents);
		System.out.println(">>CQS minimumFrequencyInHertz: " + minimumFrequencyInHertz);
		System.out.println(">>CQS window increment: " + windowSize);

		constantQ = new ConstantQ(sampleRate, minimumFrequencyInHertz, maximumFrequencyInHertz, binsPerOctave);

		binWidth = windowSize / sampleRate;
		binHeight = 1200 / (float) binsPerOctave;

		startingPointsInHertz = constantQ.getFreqencies();
		System.out.println(">>CQS startingPointsInHertz: " + startingPointsInHertz[0]);
		binStartingPointsInCents = new float[startingPointsInHertz.length];
		for (int i = 0; i < binStartingPointsInCents.length; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(startingPointsInHertz[i]);
		}
		System.out.println(">>CQS endPointsInHertz: " + startingPointsInHertz[startingPointsInHertz.length - 1]);
		System.out.println(">>CQS startingPointsInCents: " + binStartingPointsInCents[0]);
		System.out.println(">>CQS endPointsInCents: " + binStartingPointsInCents[binStartingPointsInCents.length - 1]);

		size = constantQ.getFFTlength();
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, size, size - windowSize);
		djp.setName("CQ");
		dispatcher.addAudioProcessor(djp);

		constantQLag = size / djp.getFormat().getSampleRate() - binWidth / 2.0;
		System.out.println(">>CQ size: " + size);
		System.out.println(">>CQ lag: " + constantQLag);
		features = new TreeMap<>();

		djp.addAudioProcessor(constantQ);
		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] values = constantQ.getMagnitudes().clone();
				features.put(audioEvent.getTimeStamp() - binWidth /* - constantQLag */, values);
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});

		features.clear();
	}

	void removeFeatures(double endTime) {
		features.keySet().removeIf(key -> key <= endTime);
	}

}
