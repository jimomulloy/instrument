package jomu.instrument.audio.features;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class CQMicroToneFeatures implements ToneMapConstants {

	private AudioFeatureFrame audioFeatureFrame;
	private boolean isCommitted;

	private PitchSet pitchSet;
	private TimeSet timeSet;
	private ToneMap toneMap;

	CQMicroToneSource cqs;
	Map<Double, float[]> features = new TreeMap<>();

	public void addFeature(Double time, float[] values) {
		AudioFeatureFrame previousFrame = null;
		int frameSequence = audioFeatureFrame.getFrameSequence() - 1;
		if (frameSequence > 0) {
			previousFrame = audioFeatureFrame.getAudioFeatureProcessor()
					.getAudioFeatureFrame(audioFeatureFrame.getFrameSequence() - 1);
		}
		if ((time < audioFeatureFrame.getStart() / 1000.0) && previousFrame != null) {
			previousFrame.getCQMicroToneFeatures().addFeature(time, values);
		} else {
			this.features.put(time, values);
			if (previousFrame != null && !previousFrame.getCQMicroToneFeatures().isCommitted()) {
				// previousFrame.getConstantQFeatures().buildToneMap();
				previousFrame.getCQMicroToneFeatures().commit();
			}
		}
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		this.toneMap = toneMap;

		if (features.size() > 0) {

			float[] binStartingPointsInCents = cqs.getBinStartingPointsInCents();
			float binWidth = cqs.getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Map.Entry<Double, float[]> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			System.out.println(">>CQ: " + timeStart + ", " + nextTime + binWidth + ", " + cqs.getSampleRate());
			timeSet = new TimeSet(timeStart, nextTime + binWidth, cqs.getSampleRate(), nextTime + binWidth - timeStart);

			int window = timeSet.getSampleWindow();

			int lowPitch = PitchSet.freqToMidiNote(PitchConverter.absoluteCentToHertz(binStartingPointsInCents[0]));
			int highPitch = PitchSet.freqToMidiNote(
					PitchConverter.absoluteCentToHertz(binStartingPointsInCents[binStartingPointsInCents.length - 1]));

			pitchSet = new PitchSet(lowPitch, highPitch);
			System.out.println(">>CQ lowPitch: " + lowPitch + ", " + highPitch);

			// toneMap.initialise();
			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			if (features.size() > 0) {
				for (Map.Entry<Double, float[]> entry : features.entrySet()) {
					float[] spectralEnergy = entry.getValue();
					ToneMapElement[] elements = ttf.getElements();
					for (int i = 0; i < spectralEnergy.length; i++) {
						elements[i].amplitude += spectralEnergy[i];
					}
				}
				ToneMapElement[] elements = ttf.getElements();
				for (int i = 0; i < elements.length; i++) {
					if (elements[i].amplitude > getCqs().getMaxMagnitudeThreshold()) {
						// getCqs().setMaxMagnitudeThreshold(elements[i].amplitude);
						System.out.println(">>CQ MAX VALUE: " + getCqs().getMaxMagnitudeThreshold());
					}
				}
				for (int i = 0; i < elements.length; i++) {
					elements[i].amplitude = elements[i].amplitude / getCqs().getMaxMagnitudeThreshold();
					if (elements[i].amplitude < getCqs().getMinMagnitudeThreshold()) {
						// elements[i].amplitude = getCqs().getMinMagnitudeThreshold();
					}
				}
				// ttf.setHighThreshold(1.0);
				// ttf.setLowThreshold(getCqs().getMinMagnitudeThreshold());
				ttf.reset();
			}
		}
	}

	public void close() {
		AudioFeatureFrame previousFrame = null;
		int frameSequence = audioFeatureFrame.getFrameSequence() - 1;
		if (frameSequence > 0) {
			previousFrame = audioFeatureFrame.getAudioFeatureProcessor()
					.getAudioFeatureFrame(audioFeatureFrame.getFrameSequence() - 1);
		}
		if (previousFrame != null && !previousFrame.getCQMicroToneFeatures().isCommitted()) {
			previousFrame.close();
		}
		// if (features.size() > 0) {
		// buildToneMap();
		commit();
		// }
	}

	public CQMicroToneSource getCqs() {
		return cqs;
	}

	public Map<Double, float[]> getFeatures() {
		return features;
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

	public ToneMap getToneMap() {
		return toneMap;
	}

	public boolean isCommitted() {
		return isCommitted;
	}

	private void commit() {
		isCommitted = true;
		// !! TODO
		// audioFeatureFrame.getAudioFeatureProcessor().audioFeatureFrameChanged(audioFeatureFrame);
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		this.cqs = audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getCQMicroToneSource();
		TreeMap<Double, float[]> newFeatures = this.cqs.getFeatures();
		for (Entry<Double, float[]> entry : newFeatures.entrySet()) {
			addFeature(entry.getKey(), entry.getValue());
		}
		this.cqs.clear();
	}

}