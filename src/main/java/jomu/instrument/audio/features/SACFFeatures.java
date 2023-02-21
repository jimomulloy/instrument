package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.TreeMap;

import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class SACFFeatures implements ToneMapConstants {

	public boolean logSwitch = true;
	public int powerHigh = 100;
	public int powerLow = 0;
	private AudioFeatureFrame audioFeatureFrame;

	TreeMap<Double, Integer[]> features;
	SACFSource sss;
	private ToneMap toneMap;
	private TimeSet timeSet;
	private PitchSet pitchSet;

	public TreeMap<Double, Integer[]> getFeatures() {
		return features;
	}

	public SACFSource getSss() {
		return sss;
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		this.sss = audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getSACFSource();
		this.features = sss.getFeatures();
		sss.clear();
	}

	public float[] getSpectrum() {
		float[] spectrum = null;

		return spectrum;
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		this.toneMap = toneMap;

		if (features.size() > 0) {

			float binWidth = sss.getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, Integer[]> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			timeSet = new TimeSet(timeStart, nextTime + binWidth, sss.getSampleRate(), nextTime + binWidth - timeStart);

			pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

}
