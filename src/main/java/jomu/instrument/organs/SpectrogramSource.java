package jomu.instrument.organs;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.audio.TarsosAudioIO;

public class SpectrogramSource implements PitchDetectionHandler {

	private TarsosAudioIO tarsosIO;

	private PitchEstimationAlgorithm algo;
	private PitchDetectionResult pitchDetectionResult;

	private float sampleRate = 44100;
	private int bufferSize = 1024 * 4;
	private int overlap = 768 * 4;

	private float binWidth;
	private float binHeight;
	private int binsPerOctave = 12;

	private float[] binStartingPointsInCents;
	private float[] binHeightsInCents;
	private final int frameSize = 1024 * 4;

	private List<SpectrogramInfo> spectrogramInfos = new ArrayList<SpectrogramInfo>();
	private SpectralPeakProcessor spectralPeakProcesser;
	private TreeMap<Double, SpectrogramInfo> features = new TreeMap<>();

	public SpectrogramSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
	}

	void initialise() {

		int fftsize = 1024;
		int stepsize = 512;
		int overlap = fftsize - stepsize;
		if (overlap < 1) {
			overlap = 128;
		}
		algo = PitchEstimationAlgorithm.DYNAMIC_WAVELET;

		binStartingPointsInCents = new float[frameSize];
		binHeightsInCents = new float[frameSize];
		FFT fft = new FFT(bufferSize);
		for (int i = 1; i < frameSize; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i, sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i - 1];
			if (i < 100) {
				System.out.println(">>SP Bin: " + i + ", " + binStartingPointsInCents[i] + ", " + binHeightsInCents[i]);
			}
		}

		binWidth = fftsize / sampleRate;
		binHeight = 1200 / (float) binsPerOctave;

		final double lag = frameSize / sampleRate - binWidth / 2.0;// in seconds

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(44100, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, fftsize, overlap);
		djp.setName("SP");
		tarsosIO.getDispatcher().addAudioProcessor(djp);

		djp.addAudioProcessor(new PitchProcessor(algo, sampleRate, bufferSize, this));
		djp.addAudioProcessor(fftProcessor);

		spectrogramInfos.clear();
		features.clear();
	}

	AudioProcessor fftProcessor = new AudioProcessor() {

		@Override
		public void processingFinished() {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean process(AudioEvent audioEvent) {
			FFT fft = new FFT(bufferSize);
			float[] audioFloatBuffer = audioEvent.getFloatBuffer();
			float[] transformbuffer = new float[bufferSize * 2];
			System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length);
			fft.forwardTransform(transformbuffer);
			float[] amplitudes = new float[bufferSize / 2];
			fft.modulus(transformbuffer, amplitudes);
			SpectrogramInfo si = new SpectrogramInfo(pitchDetectionResult, amplitudes, fft);
			spectrogramInfos.add(si);
			features.put(audioEvent.getTimeStamp(), si);
			return true;
		}

	};

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		this.pitchDetectionResult = pitchDetectionResult;
	}

	void clear() {
		spectrogramInfos.clear();
		features.clear();
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public float getBinWidth() {
		return binWidth;
	}

	public float getBinHeight() {
		return binHeight;
	}

	public float[] getBinStartingPointsInCents() {
		return binStartingPointsInCents;
	}

	public float[] getBinhHeightInCents() {
		return binHeightsInCents;
	}

	public SpectralPeakProcessor getSpectralPeakProcesser() {
		return spectralPeakProcesser;
	}

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		TreeMap<Double, SpectrogramInfo> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, SpectrogramInfo> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}
}
