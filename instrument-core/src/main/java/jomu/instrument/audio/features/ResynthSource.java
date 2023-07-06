package jomu.instrument.audio.features;

import java.util.Arrays;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.EnvelopeFollower;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import jomu.instrument.Instrument;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class ResynthSource extends AudioEventSource<ResynthInfo> implements PitchDetectionHandler {

	private static final Logger LOG = Logger.getLogger(ResynthSource.class.getName());

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

	private double phase = 0;
	private double phaseFirst = 0;
	private double phaseSecond = 0;
	private double prevFrequency = 0;
	private float samplerate;
	private final EnvelopeFollower envelopeFollower;
	private boolean usePureSine;
	private boolean followEnvelope;
	private final double[] previousFrequencies;
	private int previousFrequencyIndex;
	private float[] envelopeAudioBuffer;

	public ResynthSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = (int) dispatcher.getFormat()
				.getSampleRate();
		this.parameterManager = Instrument.getInstance()
				.getController()
				.getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
		envelopeFollower = new EnvelopeFollower(samplerate, 0.005, 0.01);
		this.followEnvelope = true;
		this.usePureSine = true;
		previousFrequencies = new double[5];
		previousFrequencyIndex = 0;
		LOG.finer(">>RS window: " + this.windowSize);
	}

	public ResynthSource(AudioDispatcher dispatcher, int bufferSize) {
		this(dispatcher);
		this.windowSize = bufferSize;
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

		PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.FFT_YIN;

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
		djp.setName("RS");
		dispatcher.addAudioProcessor(djp);
		djp.addAudioProcessor(new PitchProcessor(algo, sampleRate, windowSize, this));
		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] audioFloatBuffer = audioEvent.getFloatBuffer()
						.clone();
				ResynthInfo ri = new ResynthInfo(audioFloatBuffer, envelopeAudioBuffer);
				putFeature(audioEvent.getTimeStamp(), ri);
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});

		clear();
	}

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		double frequency = pitchDetectionResult.getPitch();

		if (frequency == -1) {
			frequency = prevFrequency;
		} else {
			if (previousFrequencies.length != 0) {
				// median filter
				// store and adjust pointer
				previousFrequencies[previousFrequencyIndex] = frequency;
				previousFrequencyIndex++;
				previousFrequencyIndex %= previousFrequencies.length;
				// sort to get median frequency
				double[] frequenciesCopy = previousFrequencies.clone();
				Arrays.sort(frequenciesCopy);
				// use the median as frequency
				frequency = frequenciesCopy[frequenciesCopy.length / 2];
			}

			prevFrequency = frequency;
		}

		final double twoPiF = 2 * Math.PI * frequency;
		envelopeAudioBuffer = audioEvent.getFloatBuffer()
				.clone(); // !!TODO CLONED
		float[] envelope = null;
		if (followEnvelope) {
			envelope = envelopeAudioBuffer.clone();
			envelopeFollower.calculateEnvelope(envelope);
		}

		for (int sample = 0; sample < envelopeAudioBuffer.length; sample++) {
			double time = sample / samplerate;
			double wave = Math.sin(twoPiF * time + phase);
			if (!usePureSine) {
				wave += 0.05 * Math.sin(twoPiF * 4 * time + phaseFirst);
				wave += 0.01 * Math.sin(twoPiF * 8 * time + phaseSecond);
			}
			envelopeAudioBuffer[sample] = (float) wave;
			if (followEnvelope) {
				envelopeAudioBuffer[sample] = envelopeAudioBuffer[sample] * envelope[sample];
			}
		}

		double timefactor = twoPiF * envelopeAudioBuffer.length / samplerate;
		phase = timefactor + phase;
		if (!usePureSine) {
			phaseFirst = 4 * timefactor + phaseFirst;
			phaseSecond = 8 * timefactor + phaseSecond;
		}
	}

	@Override
	ResynthInfo cloneFeatures(ResynthInfo features) {
		return features.clone();
	}
}
