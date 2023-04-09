package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.logging.Logger;

import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class MFCCFeatures extends AudioEventFeatures<MFCCInfo> implements ToneMapConstants {

	private static final Logger LOG = Logger.getLogger(MFCCFeatures.class.getName());

	public boolean logSwitch = true;
	public int powerHigh = 100;
	public int powerLow = 0;
	private AudioFeatureFrame audioFeatureFrame;

	private TimeSet timeSet;
	private PitchSet pitchSet;

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getMFCCSource());
		this.features = getSource().getAndClearFeatures();
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		if (features.size() > 0) {

			float binWidth = getSource().getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, MFCCInfo> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			TimeSet timeSet = new TimeSet(timeStart, nextTime + binWidth, getSource().getSampleRate(),
					nextTime + binWidth - timeStart);

			// TODO !!
			int lowPitch = 36;
			int highPitch = 120;

			PitchSet pitchSet = new PitchSet(lowPitch, highPitch);

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			if (features.size() > 0) {
				for (MFCCInfo feature : features.values()) {
					for (int peak : feature.peaks) {
						// float frequency = feature.getLength() / peak;
						float frequency = getSource().getSampleRate() / peak;
						int tmIndex = pitchSet.getIndex(frequency);
						ttf.getElement(tmIndex).amplitude += feature.correlations[peak];
					}
				}
				ttf.reset();
			}

		} else {
			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;
			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);

			// TODO !!
			int lowPitch = 36;
			int highPitch = 120;

			PitchSet pitchSet = new PitchSet(lowPitch, highPitch);

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

	@Override
	public MFCCSource getSource() {
		return (MFCCSource) source;
	}

}
