package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.TreeMap;

import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class YINFeatures implements ToneMapConstants {

	public boolean logSwitch = true;
	public int powerHigh = 100;
	public int powerLow = 0;
	private AudioFeatureFrame audioFeatureFrame;
	private PitchSet pitchSet;
	private TimeSet timeSet;
	private ToneMap toneMap;

	TreeMap<Double, SpectrogramInfo> features;
	YINSource yns;

	public float[] getSpectrum() {
		float[] spectrum = null;
		for (Entry<Double, SpectrogramInfo> entry : features.entrySet()) {
			float[] spectralEnergy = entry.getValue().getAmplitudes();
			if (spectrum == null) {
				spectrum = new float[spectralEnergy.length];
			}
			for (int i = 0; i < spectralEnergy.length; i++) {
				spectrum[i] += spectralEnergy[i];
			}
		}
		if (spectrum == null) {
			spectrum = new float[0];
		}
		return spectrum;
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		this.toneMap = toneMap;

		if (features.size() > 0) {

			float binWidth = yns.getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, SpectrogramInfo> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			timeSet = new TimeSet(timeStart, nextTime + binWidth, yns.getSampleRate(), nextTime + binWidth - timeStart);

			pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		return features;
	}

	public YINSource getYns() {
		return yns;
	}

	public ToneMap getToneMap() {
		return toneMap;
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		this.yns = audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getYINSource();
		this.features = this.yns.getFeatures();
		yns.clear();
	}
}
