package jomu.instrument.organs;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.Instrument;
import jomu.instrument.tonemap.Oscillator;
import jomu.instrument.tonemap.PitchSet;
import jomu.instrument.tonemap.TimeSet;
import jomu.instrument.tonemap.ToneMap;
import jomu.instrument.tonemap.ToneMapConstants;
import jomu.instrument.tonemap.ToneMapMatrix;
import jomu.instrument.tonemap.ToneMapMatrix.Iterator;

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
	private PitchFrame pitchFrame;
	private boolean isCommitted;
	private Visor visor;

	void initialise(PitchFrame pitchFrame) {
		this.pitchFrame = pitchFrame;
		this.cqs = pitchFrame.getPitchFrameProcessor().getTarsosFeatures().getConstantQSource();
		this.visor = Instrument.getInstance().getDruid().getVisor();
		TreeMap<Double, float[]> newFeatures = this.cqs.getFeatures();
		for (Entry<Double, float[]> entry : newFeatures.entrySet()) {
			addFeature(entry.getKey(), entry.getValue());
		}
		this.cqs.clear();
	}

	public void addFeature(Double time, float[] values) {
		PitchFrame previousFrame = null;
		int frameSequence = pitchFrame.getFrameSequence() - 1;
		if (frameSequence > 0) {
			previousFrame = pitchFrame.getPitchFrameProcessor().getPitchFrame(pitchFrame.getFrameSequence() - 1);
		}
		if ((time < pitchFrame.getStart() / 1000.0) && previousFrame != null) {
			previousFrame.getConstantQFeatures().addFeature(time, values);
		} else {
			this.features.put(time, values);
			if (previousFrame != null && !previousFrame.getConstantQFeatures().isCommitted()) {
				// previousFrame.getConstantQFeatures().buildToneMap();
				previousFrame.getConstantQFeatures().commit();
			}
		}
	}

	private void commit() {
		isCommitted = true;
		pitchFrame.getPitchFrameProcessor().pitchFrameChanged(pitchFrame);
	}

	public void close() {
		PitchFrame previousFrame = null;
		int frameSequence = pitchFrame.getFrameSequence() - 1;
		if (frameSequence > 0) {
			previousFrame = pitchFrame.getPitchFrameProcessor().getPitchFrame(pitchFrame.getFrameSequence() - 1);
		}
		if (previousFrame != null && !previousFrame.getConstantQFeatures().isCommitted()) {
			previousFrame.close();
		}
		if (features.size() > 0) {
			// buildToneMap();
			commit();
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

			toneMap.initialise(timeSet, pitchSet);

			System.out.println(">>toneMap.initialise: " + pitchFrame.getFrameSequence() + ", " + features.size() + ", "
					+ binWidth + ", " + cqs.getBinHeight());
			System.out.println(">>toneMap.initialise: " + timeStart + ", " + (nextTime + binWidth));

			ToneMapMatrix toneMapMatrix = toneMap.getMatrix();
			toneMapMatrix.setAmpType(logSwitch);
			toneMapMatrix.setLowThres(powerLow);
			toneMapMatrix.setHighThres(powerHigh);

			Iterator mapIterator = toneMapMatrix.newIterator();
			mapIterator.firstTime();
			mapIterator.firstPitch();
			for (Map.Entry<Double, float[]> entry : features.entrySet()) {
				mapIterator.firstPitch();
				float[] spectralEnergy = entry.getValue();
				for (int i = 0; i < spectralEnergy.length; i++) {
					mapIterator.getElement().preFTPower += spectralEnergy[i];
					mapIterator.nextPitch();
				}
				mapIterator.nextTime();
			}
			toneMapMatrix.reset();
			visor.updateToneMap(pitchFrame);
		}
	}

	public void displayToneMap() {
		if (toneMap != null) {
			visor.updateToneMap(pitchFrame);
		}
	}

	public ConstantQSource getCqs() {
		return cqs;
	}

	public Map<Double, float[]> getFeatures() {
		return features;
	}

	public ToneMap getToneMap() {
		return toneMap;
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}

	public boolean isCommitted() {
		return isCommitted;
	}

}
