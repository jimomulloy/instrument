package jomu.instrument.audio.features;

import java.util.ArrayList;
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
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class SpectralPeaksSource extends AudioEventSource<SpectralInfo> {

	private static final Logger LOG = Logger.getLogger(SpectralPeaksSource.class.getName());

	private static double MAX_MAGNITUDE_THRESHOLD = 1000.0F;
	private static double MIN_MAGNITUDE_THRESHOLD = 1E-12F;

	private double maxMagnitudeThreshold = MAX_MAGNITUDE_THRESHOLD;
	private double minMagnitudeThreshold = MIN_MAGNITUDE_THRESHOLD;

	int currentFrame;
	int minPeakSize = 100;
	float noiseFloorFactor = 1.0F;
	int noiseFloorMedianFilterLength = 10;
	int numberOfSpectralPeaks = 3;
	float sampleRate = 44100F;
	int windowSize = 1024;
	private float[] binHeightsInCents;
	private int binsPerOctave = 12;
	private float binHeight;
	private float[] binStartingPointsInCents;
	private float binWidth;
	List<SpectralInfo> spectralInfos = new ArrayList<>();
	SpectralPeakProcessor spectralPeakProcesser;
	private AudioDispatcher dispatcher;
	private ParameterManager parameterManager;

	private boolean isPowerSquared;

	private boolean microToneSwitch;

	public SpectralPeaksSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
		this.isPowerSquared = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_POWER_SQUARED_SWITCH);
		this.microToneSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_MICRO_TONE_SWITCH);
	}

	public boolean isMicroToneSwitch() {
		return microToneSwitch;
	}

	public double getMaxMagnitudeThreshold() {
		return maxMagnitudeThreshold;
	}

	public boolean isPowerSquared() {
		return isPowerSquared;
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

	public int getBufferSize() {
		return windowSize;
	}

	public int getCurrentFrame() {
		return currentFrame;
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

	public int getMinPeakSize() {
		return minPeakSize;
	}

	public float getNoiseFloorFactor() {
		return noiseFloorFactor;
	}

	public int getNoiseFloorMedianFilterLenth() {
		return noiseFloorMedianFilterLength;
	}

	public int getNumberOfSpectralPeaks() {
		return numberOfSpectralPeaks;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public List<SpectralInfo> getSpectralInfo() {
		List<SpectralInfo> clonedSpectralInfo = new ArrayList<>();
		for (SpectralInfo si : spectralInfos) {
			clonedSpectralInfo.add(si);
		}
		return clonedSpectralInfo;
	}

	public SpectralPeakProcessor getSpectralPeakProcesser() {
		return spectralPeakProcesser;
	}

	void initialise() {
		noiseFloorMedianFilterLength = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH);
		noiseFloorFactor = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR);
		numberOfSpectralPeaks = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS);
		minPeakSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE);

		binStartingPointsInCents = new float[windowSize];
		binHeightsInCents = new float[windowSize];
		FFT fft = new FFT(windowSize);
		for (int i = 1; i < windowSize; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i, sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i - 1];
		}

		binWidth = (float) windowSize / sampleRate;
		binHeight = 1200F / (float) binsPerOctave;

		LOG.finer(">>SP binWidth: " + binWidth);

		int stepsize = windowSize / 2; // 512;
		int overlap = windowSize - stepsize;
		if (overlap < 1) {
			overlap = stepsize / 4; // 128;
		}

		spectralPeakProcesser = new SpectralPeakProcessor(windowSize, overlap, (int) sampleRate);
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(44100, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, windowSize, overlap);
		djp.setName("SP");
		dispatcher.addAudioProcessor(djp);
		djp.addAudioProcessor(spectralPeakProcesser);

		djp.addAudioProcessor(new AudioProcessor() {
			int frameCounter = 0;

			@Override
			public boolean process(AudioEvent audioEvent) {
				currentFrame = frameCounter;
				SpectralInfo si = new SpectralInfo(spectralPeakProcesser.getMagnitudes(),
						spectralPeakProcesser.getFrequencyEstimates());
				spectralInfos.add(si);
				SpectralPeaksSource.this.putFeature(audioEvent.getTimeStamp(), si);
				return true;
			}

			@Override
			public void processingFinished() {
			}
		});

		spectralInfos.clear();
		clear();
	}

	@Override
	SpectralInfo cloneFeatures(SpectralInfo features) {
		return features.clone();
	}
}
