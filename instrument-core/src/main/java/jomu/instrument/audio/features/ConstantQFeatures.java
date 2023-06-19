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
				previousFrame.getConstantQFeatures().commit();
			}
		}
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		float[] binStartingPointsInCents = getSource().getBinStartingPointsInCents();
		int lowPitch = PitchSet
				.freqToMidiNote(PitchConverter.absoluteCentToHertz(getSource().getHearingMinimumFrequencyInCents()));
		int highPitch = PitchSet
				.freqToMidiNote(PitchConverter.absoluteCentToHertz(getSource().getHearingMaximumFrequencyInCents()));

		PitchSet pitchSet = new PitchSet(lowPitch, highPitch);

		if (features.size() > 0) {

			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;
			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			if (features.size() > 0) {
				for (Map.Entry<Double, float[]> entry : features.entrySet()) {
					float[] spectralEnergy = entry.getValue();
					ToneMapElement[] elements = ttf.getElements();

					for (int i = 0; i < spectralEnergy.length; i++) {
						int note = PitchSet
								.freqToMidiNote(PitchConverter.absoluteCentToHertz(binStartingPointsInCents[i]));
						int index = pitchSet.getIndex(note);
						elements[index].microTones.putMicroTone(entry.getKey(), spectralEnergy[i]);
						if (getSource().isMicroToneSwitch()) {
							if (elements[index].amplitude < spectralEnergy[i]) {
								elements[index].amplitude = spectralEnergy[i];
							}
						} else {
							elements[index].amplitude += spectralEnergy[i];
						}
					}
				}
				ttf.reset();
			}
		} else {
			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;

			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
			ttf.reset();
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
