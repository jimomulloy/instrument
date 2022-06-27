package jomu.instrument.organs;

import java.util.Map.Entry;
import java.util.TreeMap;

import jomu.instrument.Instrument;
import jomu.instrument.tonemap.PitchSet;
import jomu.instrument.tonemap.TimeSet;
import jomu.instrument.tonemap.ToneMap;
import jomu.instrument.tonemap.ToneMapConstants;
import jomu.instrument.tonemap.ToneMapMatrix;
import jomu.instrument.tonemap.ToneMapMatrix.Iterator;

public class PitchDetectorFeatures implements ToneMapConstants {

	PitchDetectorSource pds;
	TreeMap<Double, SpectrogramInfo> features;
	private ToneMap toneMap;
	private TimeSet timeSet;
	private PitchSet pitchSet;
	private Visor visor;
	private PitchFrame pitchFrame;
	public boolean logSwitch = true;
	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;

	public int powerLow = 0;
	public int powerHigh = 100;

	void initialise(PitchFrame pitchFrame) {
		this.pitchFrame = pitchFrame;
		this.pds = pitchFrame.getPitchFrameProcessor().getTarsosFeatures().getPitchDetectorSource();
		this.features = pds.getFeatures();
		this.visor = Instrument.getInstance().getDruid().getVisor();
		pds.clear();
	}

	public PitchDetectorSource getPds() {
		return pds;
	}

	public TreeMap<Double, SpectrogramInfo> getFeatures() {
		return features;
	}

	public void buildToneMap() {

		if (features.size() > 0) {

			toneMap = new ToneMap();

			float binWidth = pds.getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, SpectrogramInfo> column : features.entrySet()) {

				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			timeSet = new TimeSet(timeStart, nextTime + binWidth, pds.getSampleRate(), nextTime + binWidth - timeStart);

			double highFreq = pds.getSampleRate() / (2.0); // * pds.getBufferSize());

			pitchSet = new PitchSet();

			toneMap.initialise(timeSet, pitchSet);

			ToneMapMatrix toneMapMatrix = toneMap.getMatrix();
			toneMapMatrix.setAmpType(logSwitch);
			toneMapMatrix.setLowThres(powerLow);
			toneMapMatrix.setHighThres(powerHigh);

			Iterator mapIterator = toneMapMatrix.newIterator();
			mapIterator.firstTime();
			mapIterator.firstPitch();
			for (Entry<Double, SpectrogramInfo> entry : features.entrySet()) {
				mapIterator.firstPitch();
				float[] spectralEnergy = entry.getValue().getAmplitudes();
				int p = mapIterator.getPitchIndex();
				double binStartFreq = pitchSet.getFreq(p);
				double binEndFreq = pitchSet.getFreq(p + 1);
				for (int i = 0; i < spectralEnergy.length; i++) {
					double currentFreq = highFreq * (((double) i) / pds.getBufferSize());
					if (currentFreq < binStartFreq) {
						continue;
					}
					if (currentFreq >= binEndFreq) {
						mapIterator.nextPitch();
						p = mapIterator.getPitchIndex();
						if (mapIterator.isLastPitch()) {
							break;
						}
						binStartFreq = binEndFreq;
						binEndFreq = pitchSet.getFreq(p + 1);
					}
					mapIterator.getElement().preFTPower += spectralEnergy[i];
				}
				if (mapIterator.isLastTime()) {
					break;
				}
				mapIterator.nextTime();
			}
			toneMapMatrix.reset();
			// visor.updateToneMap(pitchFrame);
		}
	}

	public void displayToneMap() {
		if (toneMap != null) {
			visor.updateToneMap(pitchFrame);
		}
	}

	public ToneMap getToneMap() {
		return toneMap;
	}

}
