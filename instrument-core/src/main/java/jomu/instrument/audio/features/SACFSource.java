package jomu.instrument.audio.features;

import java.util.List;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import jomu.instrument.Instrument;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.audio.analysis.Autocorrelation;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class SACFSource extends AudioEventSource<SACFInfo> {

	private static final Logger LOG = Logger.getLogger(SACFSource.class.getName());

	private static double MAX_MAGNITUDE_THRESHOLD = 1000.0F;
	private static double MIN_MAGNITUDE_THRESHOLD = 1E-12F;
	private double maxMagnitudeThreshold = MAX_MAGNITUDE_THRESHOLD;
	private double minMagnitudeThreshold = MIN_MAGNITUDE_THRESHOLD;

	private float binHeight;

	private float[] binHeightsInCents;
	private int binsPerOctave = 12;
	private float[] binStartingPointsInCents;
	private float binWidth;
	private int windowSize = 1024;

	private int overlap = 0;
	private float sampleRate = 44100;

	private AudioDispatcher dispatcher;

	private ParameterManager parameterManager;

	public SACFSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = (int) dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PD_WINDOW);
	}

	public SACFSource(AudioDispatcher dispatcher, int bufferSize) {
		this(dispatcher);
		this.windowSize = bufferSize;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public double getMaxMagnitudeThreshold() {
		return maxMagnitudeThreshold;
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

	public float[] getBinStartingPointsInCents() {
		return binStartingPointsInCents;
	}

	public float getBinWidth() {
		return binWidth;
	}

	public int getBufferSize() {
		return windowSize;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	void initialise() {

		boolean undertoneRemove = parameterManager.getBooleanParameter(
				InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_REMOVE_SWITCH);
		boolean sacfSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_SACF_SWITCH);
		int maxLag = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_MAX_LAG);
		double correlationThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_CORRELATION_THRESHOLD);
		int undertoneThreshold = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_THRESHOLD);
		int undertoneRange = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_RANGE);

		Autocorrelation ac = new Autocorrelation(maxLag);
		ac.setCorrelationThreshold(correlationThreshold);
		ac.setUndertoneRange(undertoneRange);
		ac.setUndertoneThreshold(undertoneThreshold);
		ac.setIsSacf(sacfSwitch);
		ac.setMaxLag(maxLag);
		ac.setIsRemoveUndertones(undertoneRemove);
		LOG.severe(">>SACF params: " + ac);

		binStartingPointsInCents = new float[windowSize];
		binHeightsInCents = new float[windowSize];
		FFT fft = new FFT(windowSize);
		for (int i = 1; i < windowSize; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i, sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i - 1];
		}

		binWidth = windowSize / sampleRate;
		binHeight = 1200 / (float) binsPerOctave;

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, windowSize, overlap);
		djp.setName("SACF");
		dispatcher.addAudioProcessor(djp);
		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				ac.evaluate(convertFloatsToDoubles(audioEvent.getFloatBuffer()));
				LOG.severe(">>AC find peaks: " + audioEvent.getTimeStamp());
				List<Integer> sacfPeaks = ac.findPeaks();
				SACFInfo sacfInfo = new SACFInfo(sacfPeaks, ac.correlations, ac.maxACF, ac.length);
				SACFSource.this.putFeature(audioEvent.getTimeStamp(), sacfInfo);
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});

		clear();
	}

	static double[] convertFloatsToDoubles(float[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}

	@Override
	SACFInfo cloneFeatures(SACFInfo features) {
		return features.clone();
	}

}
