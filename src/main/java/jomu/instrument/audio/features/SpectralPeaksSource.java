package jomu.instrument.audio.features;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.SpectralPeakProcessor.SpectralPeak;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import jomu.instrument.audio.DispatchJunctionProcessor;

public class SpectralPeaksSource {

	private TreeMap<Double, SpectralInfo> features = new TreeMap<>();
	int currentFrame;
	int increment = 1024;
	int minPeakSize = 100;
	float noiseFloorFactor = 1.0F;
	int noiseFloorMedianFilterLenth = 10;
	int numberOfSpectralPeaks = 3;
	int sampleRate = 44100;
	int bufferSize = 1024;
	private float[] binHeightsInCents;
	private int binsPerOctave = 12;
	private float binHeight;
	private float[] binStartingPointsInCents;
	private float binWidth;
	List<SpectralInfo> spectralInfos = new ArrayList<>();
	SpectralPeakProcessor spectralPeakProcesser;
	private AudioDispatcher dispatcher;

	public SpectralPeaksSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = (int) dispatcher.getFormat().getSampleRate();
		this.bufferSize = (int) dispatcher.getFormat().getSampleRate();
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

	public int getBufferSize() {
		return bufferSize;
	}

	public TreeMap<Double, SpectralInfo> getFeatures() {
		TreeMap<Double, SpectralInfo> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, SpectralInfo> entry : features
				.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public int getIncrement() {
		return increment;
	}

	public int getMinPeakSize() {
		return minPeakSize;
	}

	public float getNoiseFloorFactor() {
		return noiseFloorFactor;
	}

	public int getNoiseFloorMedianFilterLenth() {
		return noiseFloorMedianFilterLenth;
	}

	public int getNumberOfSpectralPeaks() {
		return numberOfSpectralPeaks;
	}

	public int getSampleRate() {
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

	void clear() {
		spectralInfos.clear();
		features.clear();
	}

	void initialise() {
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
		bufferSize = 1024;
		int stepsize = 512;
		int overlap = bufferSize - stepsize;
		if (overlap < 1) {
			overlap = 128;
		}

		spectralPeakProcesser = new SpectralPeakProcessor(bufferSize, overlap,
				sampleRate);
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(44100,
				16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(
				tarsosDSPFormat, bufferSize, overlap);
		djp.setName("SP");
		dispatcher.addAudioProcessor(djp);
		djp.addAudioProcessor(spectralPeakProcesser);

		djp.addAudioProcessor(new AudioProcessor() {
			int frameCounter = 0;

			@Override
			public boolean process(AudioEvent audioEvent) {
				currentFrame = frameCounter;
				SpectralInfo si = new SpectralInfo(
						spectralPeakProcesser.getMagnitudes(),
						spectralPeakProcesser.getFrequencyEstimates());
				spectralInfos.add(si);
				features.put(audioEvent.getTimeStamp(), si);
				SpectralInfo info = spectralInfos.get(currentFrame);

				List<SpectralPeak> peaks = info.getPeakList(
						noiseFloorMedianFilterLenth, noiseFloorFactor,
						numberOfSpectralPeaks, minPeakSize);

				StringBuilder sb = new StringBuilder(
						"Frequency(Hz);Step(cents);Magnitude\n");
				for (SpectralPeak peak : peaks) {

					String message = String.format("%.2f;%.2f;%.2f\n",
							peak.getFrequencyInHertz(),
							peak.getRelativeFrequencyInCents(),
							peak.getMagnitude());
					sb.append(message);
				}
				return true;
			}

			@Override
			public void processingFinished() {
			}
		});

		spectralInfos.clear();
		features.clear();
	}
}
