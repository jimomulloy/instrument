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
import jomu.instrument.audio.DispatchJunctionProcessor;

public class SpectrumSource implements PitchDetectionHandler {

	private float binHeight;

	private float[] binHeightsInCents;
	private int binsPerOctave = 12;
	private float[] binStartingPointsInCents;
	private float binWidth;
	private int bufferSize = 1024;
	private TreeMap<Double, SpectrogramInfo> features = new TreeMap<>();

	private int overlap = 0;
	private PitchDetectionResult pitchDetectionResult;
	private float sampleRate = 44100;

	AudioProcessor fftProcessor = new AudioProcessor() {

		@Override
		public boolean process(AudioEvent audioEvent) {
			FFT fft = new FFT(bufferSize);
			float[] audioFloatBuffer = audioEvent.getFloatBuffer();
			float[] transformbuffer = new float[bufferSize * 2];
			System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0,
					audioFloatBuffer.length);
			fft.forwardTransform(transformbuffer);
			float[] amplitudes = new float[bufferSize / 2];
			fft.modulus(transformbuffer, amplitudes);
			SpectrogramInfo si = new SpectrogramInfo(pitchDetectionResult,
					amplitudes, fft);
			features.put(audioEvent.getTimeStamp(), si);
			return true;
		}

		@Override
		public void processingFinished() {
			// TODO Auto-generated method stub
		}

	};

	private AudioDispatcher dispatcher;

	public SpectrumSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = (int) dispatcher.getFormat().getSampleRate();
	}

	public SpectrumSource(AudioDispatcher dispatcher, int bufferSize) {
		this(dispatcher);
		this.bufferSize = bufferSize;
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
		return bufferSize;
	}

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		TreeMap<Double, SpectrogramInfo> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, SpectrogramInfo> entry : features
				.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult,
			AudioEvent audioEvent) {
		this.pitchDetectionResult = pitchDetectionResult;
	}

	void clear() {
		features.clear();
	}

	void initialise() {
		PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.FFT_YIN;

		binStartingPointsInCents = new float[bufferSize];
		binHeightsInCents = new float[bufferSize];
		FFT fft = new FFT(bufferSize);
		for (int i = 1; i < bufferSize; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter
					.hertzToAbsoluteCent(fft.binToHz(i, sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i]
					- binStartingPointsInCents[i - 1];
		}

		binWidth = bufferSize / sampleRate;
		binHeight = 1200 / (float) binsPerOctave;

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(
				sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(
				tarsosDSPFormat, bufferSize, overlap);
		djp.setName("PD");
		dispatcher.addAudioProcessor(djp);
		djp.addAudioProcessor(
				new PitchProcessor(algo, sampleRate, bufferSize, this));
		djp.addAudioProcessor(fftProcessor);
		features.clear();
	}
}
