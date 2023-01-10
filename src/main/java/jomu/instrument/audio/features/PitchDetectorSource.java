package jomu.instrument.audio.features;

import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
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

public class PitchDetectorSource implements PitchDetectionHandler {

	private float binHeight;

	private float[] binHeightsInCents;
	private int binsPerOctave = 12;
	private float[] binStartingPointsInCents;
	private float binWidth;
	private int windowSize = 1024;
	private TreeMap<Double, SpectrogramInfo> features = new TreeMap<>();

	private int overlap = 0;
	private PitchDetectionResult pitchDetectionResult;
	private float sampleRate = 44100;

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

	public PitchDetectorSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = (int) dispatcher.getFormat().getSampleRate();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PD_WINDOW);
		System.out.println(">>PD window: " + this.windowSize);
	}

	public PitchDetectorSource(AudioDispatcher dispatcher, int bufferSize) {
		this(dispatcher);
		this.windowSize = bufferSize;
	}

	public PitchDetectorSource(AudioDispatcher dispatcher, boolean isLow) {
		this(dispatcher);
		this.windowSize = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PD_LOW_WINDOW);
		System.out.println(">>PD LOW window: " + this.windowSize);
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

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		TreeMap<Double, SpectrogramInfo> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, SpectrogramInfo> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		this.pitchDetectionResult = pitchDetectionResult;
	}

	void clear() {
		features.clear();
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
		djp.setName("PD");
		dispatcher.addAudioProcessor(djp);
		djp.addAudioProcessor(new PitchProcessor(algo, sampleRate, windowSize, this));
		djp.addAudioProcessor(fftProcessor);
		features.clear();
	}
}
