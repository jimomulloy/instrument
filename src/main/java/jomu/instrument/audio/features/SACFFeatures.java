package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.logging.Logger;

import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class SACFFeatures extends AudioEventFeatures<Integer[]> implements ToneMapConstants {

	private static final Logger LOG = Logger.getLogger(SACFFeatures.class.getName());

	public boolean logSwitch = true;
	public int powerHigh = 100;
	public int powerLow = 0;
	private AudioFeatureFrame audioFeatureFrame;

	private ToneMap toneMap;
	private TimeSet timeSet;
	private PitchSet pitchSet;

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getSACFSource());
		this.features = getSource().getAndClearFeatures();
	}

	public float[] getSpectrum() {
		float[] spectrum = null;

		return spectrum;
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		this.toneMap = toneMap;

		if (features.size() > 0) {

			float binWidth = getSource().getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, Integer[]> column : features.entrySet()) {
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
		}
	}

	@Override
	public SACFSource getSource() {
		return (SACFSource) source;
	}

}
