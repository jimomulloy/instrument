package jomu.instrument.organs;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.SpectralPeakProcessor.SpectralPeak;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.audio.TarsosAudioIO;

public class SpectralPeaksSource {

	TarsosAudioIO tarsosIO;
	int sampleRate = 44100;
	int increment = 2048;
	int currentFrame;
	int noiseFloorMedianFilterLenth = 17;
	float noiseFloorFactor = 1.5F;
	int numberOfSpectralPeaks = 7;
	int minPeakSize = 5;
	List<SpectralInfo> spectralInfos = new ArrayList<SpectralInfo>();
	SpectralPeakProcessor spectralPeakProcesser;
	private TreeMap<Double, SpectralInfo> features = new TreeMap<>();

	public SpectralPeaksSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
	}

	void initialise() {

		int fftsize = 2048;
		int stepsize = 512;
		int overlap = fftsize - stepsize;
		if (overlap < 1) {
			overlap = 128;
		}

		spectralPeakProcesser = new SpectralPeakProcessor(fftsize, overlap, sampleRate);
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(44100, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, fftsize, overlap);
		djp.setName("SP");
		tarsosIO.getDispatcher().addAudioProcessor(djp);
		djp.addAudioProcessor(spectralPeakProcesser);

		djp.addAudioProcessor(new AudioProcessor() {
			int frameCounter = 0;

			@Override
			public void processingFinished() {
			}

			@Override
			public boolean process(AudioEvent audioEvent) {
				currentFrame = frameCounter;
				SpectralInfo si = new SpectralInfo(spectralPeakProcesser.getMagnitudes(),
						spectralPeakProcesser.getFrequencyEstimates());
				spectralInfos.add(si);
				features.put(audioEvent.getTimeStamp(), si);
				SpectralInfo info = spectralInfos.get(currentFrame);

				List<SpectralPeak> peaks = info.getPeakList(noiseFloorMedianFilterLenth, noiseFloorFactor,
						numberOfSpectralPeaks, minPeakSize);

				StringBuilder sb = new StringBuilder("Frequency(Hz);Step(cents);Magnitude\n");
				for (SpectralPeak peak : peaks) {

					String message = String.format("%.2f;%.2f;%.2f\n", peak.getFrequencyInHertz(),
							peak.getRelativeFrequencyInCents(), peak.getMagnitude());
					sb.append(message);
				}
				return true;
			}
		});

		spectralInfos.clear();
		features.clear();
	}

	void clear() {
		spectralInfos.clear();
		features.clear();
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getIncrement() {
		return increment;
	}

	public int getCurrentFrame() {
		return currentFrame;
	}

	public int getNoiseFloorMedianFilterLenth() {
		return noiseFloorMedianFilterLenth;
	}

	public float getNoiseFloorFactor() {
		return noiseFloorFactor;
	}

	public int getNumberOfSpectralPeaks() {
		return numberOfSpectralPeaks;
	}

	public int getMinPeakSize() {
		return minPeakSize;
	}

	public List<SpectralInfo> getSpectralInfo() {
		List<SpectralInfo> clonedSpectralInfo = new ArrayList<SpectralInfo>();
		for (SpectralInfo si : spectralInfos) {
			clonedSpectralInfo.add(si);
		}
		return clonedSpectralInfo;
	}

	public SpectralPeakProcessor getSpectralPeakProcesser() {
		return spectralPeakProcesser;
	}

	public TreeMap<Double, SpectralInfo> getFeatures() {
		TreeMap<Double, SpectralInfo> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, SpectralInfo> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue());
		}
		return clonedFeatures;
	}
}
