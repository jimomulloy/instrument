package jomu.instrument.audio.features;

import java.util.logging.Logger;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;
import jomu.instrument.Instrument;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class YINSource extends AudioEventSource<SpectrogramInfo> implements PitchDetectionHandler {

	private static final Logger LOG = Logger.getLogger(YINSource.class.getName());

	private float binHeight;

	private float[] binHeightsInCents;
	private int binsPerOctave = 12;
	private float[] binStartingPointsInCents;
	private float binWidth;
	private int windowSize = 4096;

	private int overlap = 0;
	private PitchDetectionResult pitchDetectionResult;
	private float sampleRate = 44100;

	AudioProcessor fftProcessor = new AudioProcessor() {

		private float[] frequencyEstimates;
		private float[] currentPhaseOffsets;
		private float[] previousPhaseOffsets;

		private double dt;
		private double cbin;
		private double inv_2pi;
		private double inv_deltat;
		private double inv_2pideltat;
		private FFT fft;

		@Override
		public boolean process(AudioEvent audioEvent) {
			fft = new FFT(windowSize, new HammingWindow());
			float[] audioFloatBuffer = audioEvent.getFloatBuffer();
			float[] transformbuffer = new float[windowSize];

			float[] amplitudes = new float[windowSize / 2];
			currentPhaseOffsets = new float[windowSize / 2];
			frequencyEstimates = new float[windowSize / 2];

			dt = (windowSize - overlap) / (double) sampleRate;
			cbin = dt * sampleRate / windowSize;

			inv_2pi = 1.0 / (2.0 * Math.PI);
			inv_deltat = 1.0 / dt;
			inv_2pideltat = inv_deltat * inv_2pi;

			System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length);

			fft.powerPhaseFFT(transformbuffer, amplitudes, currentPhaseOffsets);
			// fft.forwardTransform(transformbuffer);
			// fft.modulus(transformbuffer, amplitudes);

			calculateFrequencyEstimates();

			SpectrogramInfo si = new SpectrogramInfo(pitchDetectionResult, amplitudes, currentPhaseOffsets,
					frequencyEstimates);
			YINSource.this.putFeature(audioEvent.getTimeStamp(), si);
			return true;
		}

		private float getFrequencyForBin(int binIndex) {
			final float frequencyInHertz;
			// use the phase delta information to get a more precise
			// frequency estimate
			// if the phase of the previous frame is available.
			// See
			// * Moore 1976
			// "The use of phase vocoder in computer music applications"
			// * Sethares et al. 2009 - Spectral Tools for Dynamic
			// Tonality and Audio Morphing
			// * Laroche and Dolson 1999
			if (previousPhaseOffsets != null) {
				float phaseDelta = currentPhaseOffsets[binIndex] - previousPhaseOffsets[binIndex];
				long k = Math.round(cbin * binIndex - inv_2pi * phaseDelta);
				frequencyInHertz = (float) (inv_2pideltat * phaseDelta + inv_deltat * k);
			} else {
				frequencyInHertz = (float) fft.binToHz(binIndex, sampleRate);
			}
			return frequencyInHertz;
		}

		private void calculateFrequencyEstimates() {
			for (int i = 0; i < frequencyEstimates.length; i++) {
				frequencyEstimates[i] = getFrequencyForBin(i);
			}
		}

		@Override
		public void processingFinished() {
			// TODO Auto-generated method stub
		}

	};

	private AudioDispatcher dispatcher;

	private ParameterManager parameterManager;

	private int lowPassFrequency;

	private boolean isPowerSquared;

	private boolean microToneSwitch;

	public YINSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = (int) dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW)
				* 4;
		this.lowPassFrequency = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_YIN_LOW_PASS);
		this.isPowerSquared = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_POWER_SQUARED_SWITCH);
		this.microToneSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_MICRO_TONE_SWITCH);
	}

	public boolean isMicroToneSwitch() {
		return microToneSwitch;
	}

	public YINSource(AudioDispatcher dispatcher, int bufferSize) {
		this(dispatcher);
		this.windowSize = bufferSize;
	}

	public boolean isPowerSquared() {
		return isPowerSquared;
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

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		this.pitchDetectionResult = pitchDetectionResult;
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
		djp.setName("YIN");
		dispatcher.addAudioProcessor(djp);
		djp.addAudioProcessor(new LowPassFS((float) this.lowPassFrequency, sampleRate));
		djp.addAudioProcessor(new PitchProcessor(algo, sampleRate, windowSize, this));
		djp.addAudioProcessor(fftProcessor);
		clear();
	}

	@Override
	SpectrogramInfo cloneFeatures(SpectrogramInfo features) {
		return features.clone();
	}
}
