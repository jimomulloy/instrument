package jomu.instrument.audio.features;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import jomu.instrument.Instrument;
import jomu.instrument.monitor.Visor;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class SpectralPeaksFeatures {

	private TreeMap<Double, SpectralInfo> features;
	List<SpectralInfo> spectralInfo;
	SpectralPeaksSource sps;
	private PitchSet pitchSet;
	private TimeSet timeSet;
	private ToneMap toneMap;
	private Visor visor;

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
		visor = Instrument.getInstance().getConsole().getVisor();
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

	public void buildToneMapFrame(ToneMap toneMap) {

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
			ttf.reset();
		}
	}

	public void displayToneMap() {
		if (toneMap != null) {
			visor.updateToneMapView(toneMap);
		}
	}
}
