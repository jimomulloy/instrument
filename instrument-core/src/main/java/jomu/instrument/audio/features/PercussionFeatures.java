package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.logging.Logger;

import jomu.instrument.Instrument;
import jomu.instrument.monitor.Visor;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class PercussionFeatures extends AudioEventFeatures<OnsetInfo[]> {

	private static final Logger LOG = Logger.getLogger(PercussionFeatures.class.getName());

	private AudioFeatureFrame audioFeatureFrame;
	private PitchSet pitchSet;
	private TimeSet timeSet;
	private ToneMap toneMap;
	private Visor visor;

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getPercussionSource());
		features = getSource().getAndClearFeatures();
		this.visor = Instrument.getInstance().getConsole().getVisor();
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		this.toneMap = toneMap;

		if (features.size() > 0) {

			float binWidth = 100; // ?? TODO
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, OnsetInfo[]> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			timeSet = new TimeSet(timeStart, nextTime + binWidth, getSource().getSampleRate(),
					nextTime + binWidth - timeStart);
			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			double amplitude = 0;

			for (Entry<Double, OnsetInfo[]> entry : features.entrySet()) {

				OnsetInfo[] onsetInfo = entry.getValue();// in cents
				// draw the pixels
				for (OnsetInfo element : onsetInfo) {
					amplitude = 1.0;
				}

				ToneMapElement[] elements = ttf.getElements();
				for (int i = 0; i < elements.length; i++) {
					elements[i].amplitude = amplitude;
				}

			}

			ttf.reset();
			ttf.setLowThreshold(0.1);
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
	public PercussionSource getSource() {
		return (PercussionSource) source;
	}
}
