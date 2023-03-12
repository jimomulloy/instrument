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
		initialise(audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getSpectralPeaksSource());
		spectralInfo = getSource().getSpectralInfo();
		features = getSource().getAndClearFeatures();
	}

	public float[] getSpectrum() {
		float[] spectrum = null;
		for (Entry<Double, SpectralInfo> entry : features.entrySet()) {
			float[] spectralEnergy = entry.getValue().getMagnitudes();
			if (spectrum == null) {
				spectrum = new float[spectralEnergy.length];
			}
			for (int i = 0; i < spectralEnergy.length; i++) {
				spectrum[i] += spectralEnergy[i];
			}
		}
		return spectrum;
	}

	public void buildToneMapFrame(ToneMap toneMap, boolean usePeaks) {

		this.toneMap = toneMap;

		if (features.size() > 0) {

			float binWidth = getSource().getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, SpectralInfo> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			timeSet = new TimeSet(timeStart, nextTime + binWidth, getSource().getSampleRate(),
					nextTime + binWidth - timeStart);

			pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			for (Entry<Double, SpectralInfo> entry : features.entrySet()) {
				List<SpectralPeak> spectralPeaks = entry.getValue().getPeakList(
						getSource().getNoiseFloorMedianFilterLenth(), getSource().getNoiseFloorFactor(),
						getSource().getNumberOfSpectralPeaks(), getSource().getMinPeakSize());
				for (SpectralPeak peak : spectralPeaks) {
					int index = pitchSet.getIndex(peak.getFrequencyInHertz());
					ttf.getElement(index).isPeak = true;
				}
			}

			float[] spectrum = usePeaks ? processPeaks(getSpectrum()) : getSpectrum();

			FFTSpectrum fftSpectrum = new FFTSpectrum(getSource().getSampleRate(), getSource().getBufferSize(),
					spectrum);

			toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);

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

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

	private float[] processPeaks(float[] spectrum) {
		float[] peakSpectrum = new float[spectrum.length];
		LOG.finer(">>AA processPeaks");
		for (Entry<Double, SpectralInfo> entry : features.entrySet()) {
			List<SpectralPeak> spectralPeaks = entry.getValue().getPeakList(
					getSource().getNoiseFloorMedianFilterLenth(), getSource().getNoiseFloorFactor(),
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