package jomu.instrument.organs;

import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.tonemap.Oscillator;
import jomu.instrument.tonemap.PitchSet;
import jomu.instrument.tonemap.TimeSet;
import jomu.instrument.tonemap.ToneMap;
import jomu.instrument.tonemap.ToneMapConstants;
import jomu.instrument.tonemap.ToneMapMatrix;
import jomu.instrument.tonemap.ToneMapMatrix.Iterator;

public class ConstantQFeatures implements ToneMapConstants {

	public ToneMap getToneMap() {
		return toneMap;
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}

	public boolean logSwitch = false;
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
	TreeMap<Double, float[]> features;

	private ToneMap toneMap;
	private TimeSet timeSet;
	private PitchSet pitchSet;

	void initialise(ConstantQSource cqs) {
		this.cqs = cqs;
		this.features = cqs.getFeatures();
		cqs.clear();
		buildToneMap();
	}

	private void buildToneMap() {

		if (features.size() > 0) {

			toneMap = new ToneMap();

			float[] binStartingPointsInCents = cqs.getBinStartingPointsInCents();
			float binWidth = cqs.getBinWidth();
			double timeStart = -1;
			double nextTime = -1;
			double maxEnergy = -1;
			
			for (Map.Entry<Double, float[]> column : features.entrySet()) {

				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}

				float[] spectralEnergy = column.getValue();
				// draw the pixels
				for (int i = 0; i < spectralEnergy.length; i++) {
					double energy = Math.log1p(spectralEnergy[i]);
					if (energy > maxEnergy) {
						maxEnergy = energy;
					}
				}
				System.out.println(">>");
				maxEnergy = -1;
			}

			timeSet = new TimeSet(timeStart, nextTime + binWidth, cqs.getSampleRate(), cqs.getBinWidth());

			int lowPitch = PitchSet.freqToMidiNote(PitchConverter.absoluteCentToHertz(binStartingPointsInCents[0]));
			int highPitch = PitchSet.freqToMidiNote(
					PitchConverter.absoluteCentToHertz(binStartingPointsInCents[binStartingPointsInCents.length - 1]));

			pitchSet = new PitchSet(lowPitch, highPitch);

			toneMap.initialise(timeSet, pitchSet);

			ToneMapMatrix toneMapMatrix = toneMap.getMatrix();
			toneMapMatrix.setAmpType(logSwitch);
			toneMapMatrix.setLowThres(powerLow);
			toneMapMatrix.setHighThres(powerHigh);

			Iterator mapIterator = toneMapMatrix.newIterator();
			mapIterator.firstTime();
			mapIterator.firstPitch();
			for (Map.Entry<Double, float[]> column : features.entrySet()) {
				nextTime = column.getKey();
				float[] spectralEnergy = column.getValue();
				for (int i = 0; i < spectralEnergy.length; i++) {
					double energy = Math.log1p(spectralEnergy[i]);
					mapIterator.getElement().postAmplitude = energy;
					mapIterator.nextPitch();
				}
				mapIterator.nextTime();
			}
			toneMapMatrix.reset();
		}
	}

	public ConstantQSource getCqs() {
		return cqs;
	}

	public TreeMap<Double, float[]> getFeatures() {
		return features;
	}

}
