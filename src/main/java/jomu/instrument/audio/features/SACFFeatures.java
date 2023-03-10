package jomu.instrument.audio.features;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
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

	private TimeSet timeSet;
	private PitchSet pitchSet;

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getSACFSource());
		this.features = getSource().getAndClearFeatures();
	}

	public float[] getSpectrum() {
		float[] spectrum = new float[getSource().getWindowSize() / 2 + 1];
		Set<Integer> peaks = new HashSet<Integer>();
		for (Integer[] indexes : features.values()) {
			Collections.addAll(peaks, indexes);
		}
		for (int i = 0; i < spectrum.length; i++) {
			if (peaks.contains(i)) {
				spectrum[i] = 1.0F;
			}
		}
		return spectrum;
	}

	public void buildToneMapFrame(ToneMap toneMap) {

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

			TimeSet timeSet = new TimeSet(timeStart, nextTime + binWidth, getSource().getSampleRate(),
					nextTime + binWidth - timeStart);

			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		} else {
			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;
			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);

			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

	@Override
	public SACFSource getSource() {
		return (SACFSource) source;
	}

}
