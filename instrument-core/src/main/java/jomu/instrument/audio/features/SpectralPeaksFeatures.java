package jomu.instrument.audio.features;

import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class SpectralPeaksFeatures extends AudioEventFeatures<SpectralInfo> {

	private static final Logger LOG = Logger.getLogger(SpectralPeaksFeatures.class.getName());

	List<SpectralInfo> spectralInfo;
	private PitchSet pitchSet;
	private TimeSet timeSet;
	private ToneMap toneMap;

	private AudioFeatureFrame audioFeatureFrame;

	public List<SpectralInfo> getSpectralInfo() {
		return spectralInfo;
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor()
				.getTarsosFeatures()
				.getSpectralPeaksSource());
		spectralInfo = getSource().getSpectralInfo();
		features = getSource().getAndClearFeatures();
	}

	public float[] getSpectrum(double lowThreshold) {
		float[] spectrum = null;
		for (Entry<Double, SpectralInfo> entry : features.entrySet()) {
			float[] spectralEnergy = entry.getValue()
					.getMagnitudes();
			if (spectrum == null) {
				spectrum = new float[spectralEnergy.length];
			}
			for (int i = 0; i < spectralEnergy.length; i++) {
				if (getSource().isPowerSquared()) {
					if (getSource().isMicroToneSwitch()) {
						if (spectrum[i] < spectralEnergy[i] * spectralEnergy[i]) {
							spectrum[i] = spectralEnergy[i] * spectralEnergy[i];
						}
					} else {
						spectrum[i] += spectralEnergy[i] * spectralEnergy[i];
					}
				} else {
					if (getSource().isMicroToneSwitch()) {
						if (spectrum[i] < spectralEnergy[i]) {
							spectrum[i] = spectralEnergy[i];
						}
					} else {
						spectrum[i] += spectralEnergy[i];
					}
					spectrum[i] += spectralEnergy[i];
				}
			}
		}
		if (spectrum == null) {
			spectrum = new float[0];
		}
		for (int i = 0; i < spectrum.length; i++) {
			if (spectrum[i] < lowThreshold) {
				spectrum[i] = 0;
			}
		}
		return spectrum;
	}

	public void buildToneMapFrame(ToneMap toneMap, boolean usePeaks, double lowThreshold) {

		this.toneMap = toneMap;

		if (features.size() > 0) {
			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;
			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);
			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			for (Entry<Double, SpectralInfo> entry : features.entrySet()) {
				List<SpectralPeak> spectralPeaks = entry.getValue()
						.getPeakList(getSource().getNoiseFloorMedianFilterLenth(), getSource().getNoiseFloorFactor(),
								getSource().getNumberOfSpectralPeaks(), getSource().getMinPeakSize());
				for (SpectralPeak peak : spectralPeaks) {
					int index = pitchSet.getIndex(peak.getFrequencyInHertz());
					if (index > 0 && index < ttf.getElements().length) {
						ttf.getElement(index).isPeak = true;
					}
				}
			}

			float[] spectrum = usePeaks ? processPeaks(getSpectrum(lowThreshold)) : getSpectrum(lowThreshold);

			FFTSpectrum fftSpectrum = new FFTSpectrum(getSource().getSampleRate(), getSource().getBufferSize(),
					spectrum);

			toneMap.getTimeFrame()
					.loadFFTSpectrum(fftSpectrum);

			ToneMapElement[] elements = ttf.getElements();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].amplitude > getSource().getMaxMagnitudeThreshold()) {
					getSource().setMaxMagnitudeThreshold(elements[i].amplitude);
				}
			}
			for (int i = 0; i < elements.length; i++) {
				elements[i].amplitude = elements[i].amplitude / getSource().getMaxMagnitudeThreshold();
				if (elements[i].amplitude < getSource().getMinMagnitudeThreshold()) {
					elements[i].amplitude = getSource().getMinMagnitudeThreshold();
				}
			}
			ttf.setHighThreshold(1.0);
			ttf.setLowThreshold(getSource().getMinMagnitudeThreshold());
			ttf.reset();
		} else {
			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;

			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);
			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

	private float[] processPeaks(float[] spectrum) {
		float[] peakSpectrum = new float[spectrum.length];
		for (Entry<Double, SpectralInfo> entry : features.entrySet()) {
			List<SpectralPeak> spectralPeaks = entry.getValue()
					.getPeakList(getSource().getNoiseFloorMedianFilterLenth(), getSource().getNoiseFloorFactor(),
							getSource().getNumberOfSpectralPeaks(), getSource().getMinPeakSize());
			for (SpectralPeak sp : spectralPeaks) {
				for (int i = 0; i < peakSpectrum.length; i++) {
					if (sp.getBin() == i) {
						if (peakSpectrum[i] < sp.getMagnitude()) {
							peakSpectrum[i] += sp.getMagnitude();
						}
					}
				}
			}
		}
		return peakSpectrum;

	}

	@Override
	public SpectralPeaksSource getSource() {
		return (SpectralPeaksSource) source;
	}
}
