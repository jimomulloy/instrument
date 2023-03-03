package jomu.instrument.audio.features;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class ConstantQFeatures extends AudioEventFeatures<float[]> implements ToneMapConstants {

	private static final Logger LOG = Logger.getLogger(ConstantQFeatures.class.getName());

	private AudioFeatureFrame audioFeatureFrame;
	private boolean isCommitted;

	public void addFeature(Double time, float[] values) {
		AudioFeatureFrame previousFrame = null;
		int frameSequence = audioFeatureFrame.getFrameSequence() - 1;
		if (frameSequence > 0) {
			previousFrame = audioFeatureFrame.getAudioFeatureProcessor()
					.getAudioFeatureFrame(audioFeatureFrame.getFrameSequence() - 1);
		}
		if ((time < audioFeatureFrame.getStart() / 1000.0) && previousFrame != null) {
			previousFrame.getConstantQFeatures().addFeature(time, values);
		} else {
			this.features.put(time, values);
			if (previousFrame != null && !previousFrame.getConstantQFeatures().isCommitted()) {
				// previousFrame.getConstantQFeatures().buildToneMap();
				previousFrame.getConstantQFeatures().commit();
			}
		}
	}

	public void buildToneMapFrame(ToneMap toneMap) {
		if (features.size() > 0) {

			float[] binStartingPointsInCents = getSource().getBinStartingPointsInCents();
			float binWidth = getSource().getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Map.Entry<Double, float[]> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			LOG.info(">>CQ: " + timeStart + ", " + nextTime + binWidth + ", " + getSource().getSampleRate());
			TimeSet timeSet = new TimeSet(timeStart, nextTime + binWidth, getSource().getSampleRate(),
					nextTime + binWidth - timeStart);

			int window = timeSet.getSampleWindow();

			int lowPitch = PitchSet.freqToMidiNote(PitchConverter.absoluteCentToHertz(binStartingPointsInCents[0]));
			int highPitch = PitchSet.freqToMidiNote(
					PitchConverter.absoluteCentToHertz(binStartingPointsInCents[binStartingPointsInCents.length - 1]));

			PitchSet pitchSet = new PitchSet(lowPitch, highPitch);
			LOG.info(">>CQ lowPitch: " + lowPitch + ", " + highPitch);

			// toneMap.initialise();
			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			if (features.size() > 0) {
				for (Map.Entry<Double, float[]> entry : features.entrySet()) {
					float[] spectralEnergy = entry.getValue();
					ToneMapElement[] elements = ttf.getElements();
					for (int i = 0; i < spectralEnergy.length; i++) {
						elements[i].amplitude += spectralEnergy[i];
						elements[i].microTones.addMicroTone(entry.getKey(), spectralEnergy[i]);
					}
				}
				ToneMapElement[] elements = ttf.getElements();
				for (int i = 0; i < elements.length; i++) {
					if (elements[i].amplitude > getSource().getMaxMagnitudeThreshold()) {
						// !!TODO getCqs().setMaxMagnitudeThreshold(elements[i].amplitude);
						LOG.info(">>CQ MAX VALUE: " + getSource().getMaxMagnitudeThreshold());
					}
				}
				for (int i = 0; i < elements.length; i++) {
					elements[i].amplitude = elements[i].amplitude / getSource().getMaxMagnitudeThreshold();
					if (elements[i].amplitude < getSource().getMinMagnitudeThreshold()) {
						// !!TODO elements[i].amplitude = getCqs().getMinMagnitudeThreshold();
					}
				}
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
		if (previousFrame != null && !previousFrame.getConstantQFeatures().isCommitted()) {
			previousFrame.close();
		}
		commit();
	}

	public boolean isCommitted() {
		return isCommitted;
	}

	private void commit() {
		isCommitted = true;
		audioFeatureFrame.getAudioFeatureProcessor().audioFeatureFrameChanged(audioFeatureFrame);
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getConstantQSource());
		TreeMap<Double, float[]> newFeatures = getSource().getAndClearFeatures();
		for (Entry<Double, float[]> entry : newFeatures.entrySet()) {
			addFeature(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public ConstantQSource getSource() {
		return (ConstantQSource) source;
	}

}
