package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.TreeMap;

import jomu.instrument.Instrument;
import jomu.instrument.monitor.Visor;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class BeatFeatures {

	private TreeMap<Double, OnsetInfo[]> features;
	private BeatSource bs;
	private AudioFeatureFrame audioFeatureFrame;
	private PitchSet pitchSet;
	private TimeSet timeSet;
	private ToneMap toneMap;
	private Visor visor;

	public TreeMap<Double, OnsetInfo[]> getFeatures() {
		return features;
	}

	public BeatSource getBs() {
		return bs;
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		this.bs = audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getBeatSource();
		this.features = bs.getFeatures();
		this.visor = Instrument.getInstance().getConsole().getVisor();
		bs.clear();
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

			timeSet = new TimeSet(timeStart, nextTime + binWidth, bs.getSampleRate(), nextTime + binWidth - timeStart);

			pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			int amplitude = 0;

			for (Entry<Double, OnsetInfo[]> entry : features.entrySet()) {

				OnsetInfo[] onsetInfo = entry.getValue();// in cents
				// draw the pixels
				for (OnsetInfo element : onsetInfo) {
					amplitude += element.salience;
				}

				ToneMapElement[] elements = ttf.getElements();
				for (int i = 0; i < elements.length; i++) {
					elements[i].amplitude = amplitude;
				}

			}

			ttf.reset();
			ttf.setLowThreshold(0.1);
		}
	}
}
