package jomu.instrument.organs;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.Instrument;
import jomu.instrument.model.tonemap.PitchSet;
import jomu.instrument.model.tonemap.TimeSet;
import jomu.instrument.model.tonemap.ToneMap;
import jomu.instrument.model.tonemap.ToneMapConstants;
import jomu.instrument.model.tonemap.ToneMapElement;
import jomu.instrument.model.tonemap.ToneTimeFrame;

public class ConstantQFeatures implements ToneMapConstants {

	public boolean logSwitch = true;
	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;
	public int gainSetting = INIT_VOLUME_SETTING;
	public int resolution = 1;
	public int tFactor = 50;
	public int pFactor = 100;
	public int pOffset = 0;
	public int transformMode = TRANSFORM_MODE_JAVA;
	public int powerHigh = 100;
	public int oscType = Oscillator.SINEWAVE;

	public int powerLow = 0;

	ConstantQSource cqs;
	Map<Double, float[]> features = new TreeMap<Double, float[]>();

	private ToneMap toneMap;
	private TimeSet timeSet;
	private PitchSet pitchSet;
	private AudioFeatureFrame audioFeatureFrame;
	private boolean isCommitted;
	private Visor visor;

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

	public void buildToneMap() {

		if (features.size() > 0) {

			toneMap = new ToneMap();

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

			timeSet = new TimeSet(timeStart, nextTime + binWidth, cqs.getSampleRate(), nextTime + binWidth - timeStart);

			int lowPitch = PitchSet.freqToMidiNote(PitchConverter.absoluteCentToHertz(binStartingPointsInCents[0]));
			int highPitch = PitchSet.freqToMidiNote(
					PitchConverter.absoluteCentToHertz(binStartingPointsInCents[binStartingPointsInCents.length - 1]));

			pitchSet = new PitchSet(lowPitch, highPitch);

			System.out.println(">>CS start hertz: " + pitchSet.getFreq(pitchSet.pitchToIndex(pitchSet.getLowNote())));
			System.out.println(">>CS end hertz: " + pitchSet.getFreq(pitchSet.pitchToIndex(pitchSet.getHighNote())));
			System.out.println(">>CS start cents: " + PitchConverter.hertzToAbsoluteCent(pitchSet.getFreq(pitchSet.pitchToIndex(pitchSet.getLowNote()))));
			System.out.println(">>CS end cents: " + PitchConverter.hertzToAbsoluteCent(pitchSet.getFreq(pitchSet.pitchToIndex(pitchSet.getHighNote()))));
			System.out.println(">>CS start midi: " + pitchSet.getLowNote());
			System.out.println(">>CSQ end midi: " + pitchSet.getHighNote());
			
			toneMap.initialise();
			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			System.out.println(">>toneMap.initialise: " + audioFeatureFrame.getFrameSequence() + ", " + features.size()
					+ ", " + binWidth + ", " + cqs.getBinHeight());
			System.out.println(">>toneMap.initialise: " + timeStart + ", " + (nextTime + binWidth));
			System.out.println(">>toneMap.initialise: " + timeSet + ", " + pitchSet);

			for (Map.Entry<Double, float[]> entry : features.entrySet()) {
				System.out.println(">>feature: " + entry.getKey());
				float[] spectralEnergy = entry.getValue();
				ToneMapElement[] elements = ttf.getElements();
				for (int i = 0; i < spectralEnergy.length; i++) {
					elements[i].preFTPower += spectralEnergy[i];
				}
			}

			ttf.reset();
			//visor.updateToneMap(audioFeatureFrame);
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
		if (features.size() > 0) {
			// buildToneMap();
			commit();
		}
	}

	private void commit() {
		isCommitted = true;
		audioFeatureFrame.getAudioFeatureProcessor().audioFeatureFrameChanged(audioFeatureFrame);
	}

	public void displayToneMap() {
		if (toneMap != null) {
			visor.updateToneMap(audioFeatureFrame);
		}
	}

	public ConstantQSource getCqs() {
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

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		this.cqs = audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getConstantQSource();
		this.visor = Instrument.getInstance().getDruid().getVisor();
		TreeMap<Double, float[]> newFeatures = this.cqs.getFeatures();
		for (Entry<Double, float[]> entry : newFeatures.entrySet()) {
			addFeature(entry.getKey(), entry.getValue());
		}
		this.cqs.clear();
	}

	public boolean isCommitted() {
		return isCommitted;
	}

}
