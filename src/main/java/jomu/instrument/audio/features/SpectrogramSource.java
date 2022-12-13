package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.control.ParameterManager;

public class SpectrogramSource implements PitchDetectionHandler {

	private PitchEstimationAlgorithm algo;

	private float binHeight;
	private float[] binHeightsInCents;

	private int binsPerOctave = 12;
	private float[] binStartingPointsInCents;
	private float binWidth;

	private int windowSize = 1024 * 4;
	private TreeMap<Double, SpectrogramInfo> features = new TreeMap<>();

	private PitchDetectionResult pitchDetectionResult;
	private float sampleRate = 44100;

	private SpectralPeakProcessor spectralPeakProcesser;
	private List<SpectrogramInfo> spectrogramInfos = new ArrayList<>();

	AudioProcessor fftProcessor = new AudioProcessor() {

		@Override
		public boolean process(AudioEvent audioEvent) {
			FFT fft = new FFT(windowSize);
			float[] audioFloatBuffer = audioEvent.getFloatBuffer();
			float[] transformbuffer = new float[windowSize * 2];
			System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length);
			fft.forwardTransform(transformbuffer);
			float[] amplitudes = new float[windowSize / 2];
			fft.modulus(transformbuffer, amplitudes);
			SpectrogramInfo si = new SpectrogramInfo(pitchDetectionResult, amplitudes, fft);
			spectrogramInfos.add(si);
			features.put(audioEvent.getTimeStamp(), si);
			return true;
		}

		@Override
		public void processingFinished() {
			// TODO Auto-generated method stub
		}

	};

	private AudioDispatcher dispatcher;

	private ParameterManager parameterManager;

	public SpectrogramSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
		this.windowSize *= 4; // TODO !!
	}

	public float getBinHeight() {
		return binHeight;
	}

	public float[] getBinhHeightInCents() {
		return binHeightsInCents;
	}

	public float[] getBinStartingPointsInCents() {
		return binStartingPointsInCents;
	}

	public float getBinWidth() {
		return binWidth;
	}

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		TreeMap<Double, SpectrogramInfo> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, SpectrogramInfo> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public SpectralPeakProcessor getSpectralPeakProcesser() {
		return spectralPeakProcesser;
	}

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		this.pitchDetectionResult = pitchDetectionResult;
	}

	void clear() {
		spectrogramInfos.clear();
		features.clear();
	}

	void initialise() {

		int fftsize = 1024;
		int stepsize = 512;
		int overlap = fftsize - stepsize;
		if (overlap < 1) {
			overlap = 128;
		}
		algo = PitchEstimationAlgorithm.DYNAMIC_WAVELET;

		binStartingPointsInCents = new float[windowSize];
		binHeightsInCents = new float[windowSize];
		FFT fft = new FFT(windowSize);
		for (int i = 1; i < windowSize; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i, sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i - 1];
			if (i < 100) {
				// System.out.println(">>SP Bin: " + i + ", " +
				// binStartingPointsInCents[i] + ",
				// " + binHeightsInCents[i]);
			}
		}

		binWidth = fftsize / sampleRate;
		binHeight = 1200 / (float) binsPerOctave;

		final double lag = windowSize / sampleRate - binWidth / 2.0;// in seconds

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(44100, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, fftsize, overlap);
		djp.setName("SP");
		dispatcher.addAudioProcessor(djp);

		djp.addAudioProcessor(new PitchProcessor(algo, sampleRate, windowSize, this));
		djp.addAudioProcessor(fftProcessor);

		spectrogramInfos.clear();
		features.clear();
	}
}
