package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.logging.Logger;

import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class SACFFeatures extends AudioEventFeatures<SACFInfo> implements ToneMapConstants {

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

	public void buildToneMapFrame(ToneMap toneMap) {

		if (features.size() > 0) {

			float binWidth = getSource().getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, SACFInfo> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			TimeSet timeSet = new TimeSet(timeStart, nextTime + binWidth, getSource().getSampleRate(),
					nextTime + binWidth - timeStart);
			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			if (features.size() > 0) {
				for (SACFInfo feature : features.values()) {
					for (int peak : feature.getPeaks()) {
						if (peak == feature.getMaxACFIndex()) {
							float frequency = getSource().getSampleRate() / peak;
							int tmIndex = pitchSet.getIndex(frequency);
							if (tmIndex > -1) {
								if (getSource().isMicroToneSwitch()) {
									if (ttf.getElement(tmIndex).amplitude < feature.correlations[peak]) {
										ttf.getElement(tmIndex).amplitude = feature.correlations[peak];
									}
								} else {
									ttf.getElement(tmIndex).amplitude += feature.correlations[peak];
								}
								ttf.getElement(tmIndex).isPeak = true;
							}
						}
					}
				}
				ttf.reset();
			}

		} else {
			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;
			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);
			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

	@Override
	public SACFSource getSource() {
		return (SACFSource) source;
	}

}
