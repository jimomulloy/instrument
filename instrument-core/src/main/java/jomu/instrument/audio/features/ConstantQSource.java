package jomu.instrument.audio.features;

import java.util.logging.Logger;

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

public class ConstantQSource extends AudioEventSource<float[]> {

	private static final Logger LOG = Logger.getLogger(ConstantQSource.class.getName());

	private static double MAX_MAGNITUDE_THRESHOLD = 0.5F;
	private static double MIN_MAGNITUDE_THRESHOLD = 1E-12F;
	private double maxMagnitudeThreshold = MAX_MAGNITUDE_THRESHOLD;
	private double minMagnitudeThreshold = MIN_MAGNITUDE_THRESHOLD;

	private float binHeight;
	private float[] binStartingPointsInCents;
	private float binWidth;
	private int size;
	private float[] startingPointsInHertz;
	private int binsPerOctave = 12;
	private ConstantQ constantQ;
	private double constantQLag;
	private int windowSize = 1024;
	private int maximumFrequencyInCents = 12000;
	private int minimumFrequencyInCents = 1200;
	private float sampleRate = 44100;
	private AudioDispatcher dispatcher;
	private ParameterManager parameterManager;
	float max = 0;

	private int hearingMinimumFrequencyInCents;

	private int hearingMaximumFrequencyInCents;

	public ConstantQSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
		this.minimumFrequencyInCents = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_MINIMUM_FREQUENCY_CENTS);
		this.maximumFrequencyInCents = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_MAXIMUM_FREQUENCY_CENTS);
		this.hearingMinimumFrequencyInCents = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS);
		this.hearingMaximumFrequencyInCents = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS);
		this.binsPerOctave = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_BINS_PER_OCTAVE);
	}

	public double getMaxMagnitudeThreshold() {
		return maxMagnitudeThreshold;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setMaxMagnitudeThreshold(double maxMagnitudeThreshold) {
		this.maxMagnitudeThreshold = maxMagnitudeThreshold;
	}

	public double getMinMagnitudeThreshold() {
		return minMagnitudeThreshold;
	}

	public void setMinMagnitudeThreshold(double minMagnitudeThreshold) {
		this.minMagnitudeThreshold = minMagnitudeThreshold;
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

	public int getIncrement() {
		return windowSize;
	}

	public int getMaximumFrequencyInCents() {
		return maximumFrequencyInCents;
	}

	public int getMinimumFrequencyInCents() {
		return minimumFrequencyInCents;
	}

	public int getHearingMaximumFrequencyInCents() {
		return hearingMaximumFrequencyInCents;
	}

	public int getHearingMinimumFrequencyInCents() {
		return hearingMinimumFrequencyInCents;
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

	void initialise() {
		float minimumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(minimumFrequencyInCents);
		float maximumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(maximumFrequencyInCents);
		LOG.finer(">>CQS minimumFrequencyInHertz: " + minimumFrequencyInHertz);
		LOG.finer(">>CQS window increment: " + windowSize);

		constantQ = new ConstantQ(sampleRate, minimumFrequencyInHertz, maximumFrequencyInHertz, binsPerOctave);

		binWidth = (float) windowSize / sampleRate;
		binHeight = 1200F / (float) binsPerOctave;

		startingPointsInHertz = constantQ.getFreqencies();
		LOG.finer(">>CQS startingPointsInHertz: " + startingPointsInHertz[0]);
		binStartingPointsInCents = new float[startingPointsInHertz.length];
		for (int i = 0; i < binStartingPointsInCents.length; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(startingPointsInHertz[i]);
		}
		LOG.finer(">>CQS endPointsInHertz: " + startingPointsInHertz[startingPointsInHertz.length - 1]);
		LOG.finer(">>CQS startingPointsInCents: " + binStartingPointsInCents[0]);
		LOG.finer(">>CQS endPointsInCents: " + binStartingPointsInCents[binStartingPointsInCents.length - 1]);

		size = constantQ.getFFTlength();
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);
		LOG.finer(">>CQS size: " + size + ", windowSize: " + windowSize);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, size, size - windowSize);
		djp.setName("CQ");
		dispatcher.addAudioProcessor(djp);

		constantQLag = size / djp.getFormat().getSampleRate() - binWidth / 2.0;
		LOG.finer(">>CQ size: " + size);
		LOG.finer(">>CQ lag: " + constantQLag);

		djp.addAudioProcessor(constantQ);
		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] values = constantQ.getMagnitudes().clone();
				if (audioEvent.getTimeStamp() >= (binWidth / 2)) {
					putFeature(audioEvent.getTimeStamp() - (binWidth / 2), values);
				} else {
					putFeature(audioEvent.getTimeStamp(), values);
				}
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});

		clear();
	}

	@Override
	float[] cloneFeatures(float[] features) {
		return features.clone();
	}

}
