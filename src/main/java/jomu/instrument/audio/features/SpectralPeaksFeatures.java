package jomu.instrument.audio.features;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class SpectralPeaksFeatures {

	private TreeMap<Double, SpectralInfo> features;
	List<SpectralInfo> spectralInfo;
	SpectralPeaksSource sps;
	private PitchSet pitchSet;
	private TimeSet timeSet;
	private ToneMap toneMap;

	public TreeMap<Double, SpectralInfo> getFeatures() {
		return features;
	}

	public List<SpectralInfo> getSpectralInfo() {
		return spectralInfo;
	}

	public SpectralPeaksSource getSps() {
		return sps;
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.sps = audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getSpectralPeaksSource();
		spectralInfo = sps.getSpectralInfo();
		features = sps.getFeatures();
		sps.clear();
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

			float binWidth = sps.getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, SpectralInfo> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			timeSet = new TimeSet(timeStart, nextTime + binWidth, sps.getSampleRate(), nextTime + binWidth - timeStart);

			pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			float[] spectrum = usePeaks ? processPeaks(getSpectrum()) : getSpectrum();

			FFTSpectrum fftSpectrum = new FFTSpectrum(getSps().getSampleRate(), getSps().getBufferSize(), spectrum);

			toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);

			ToneMapElement[] elements = ttf.getElements();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].amplitude > getSps().getMaxMagnitudeThreshold()) {
					getSps().setMaxMagnitudeThreshold(elements[i].amplitude);
					System.out.println(">>SP MAX VALUE: " + getSps().getMaxMagnitudeThreshold());
				}
			}
			for (int i = 0; i < elements.length; i++) {
				elements[i].amplitude = elements[i].amplitude / getSps().getMaxMagnitudeThreshold();
				if (elements[i].amplitude < getSps().getMinMagnitudeThreshold()) {
					elements[i].amplitude = getSps().getMinMagnitudeThreshold();
				}
			}
			ttf.setHighThreshold(1.0);
			ttf.setLowThreshold(getSps().getMinMagnitudeThreshold());
			ttf.reset();
		}
	}

	private float[] processPeaks(float[] spectrum) {
		float[] peakSpectrum = new float[spectrum.length];

		for (Entry<Double, SpectralInfo> entry : features.entrySet()) {
			List<SpectralPeak> spectralPeaks = entry.getValue().getPeakList(sps.getNoiseFloorMedianFilterLenth(),
					sps.getNoiseFloorFactor(), sps.getNumberOfSpectralPeaks(), sps.getMinPeakSize());
			for (SpectralPeak sp : spectralPeaks) {
				for (int i = 0; i < peakSpectrum.length; i++) {
					if (sp.getBin() == i) {
						if (peakSpectrum[i] < sp.getMagnitude()) {
							peakSpectrum[i] += sp.getMagnitude();
						}
						;
					}
				}
			}
		}
		return peakSpectrum;

	}

}
