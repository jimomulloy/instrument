package jomu.instrument.tonemap;

import java.util.ArrayList;
import java.util.List;

import jomu.instrument.tonemap.ToneMapMatrix.Iterator;

/**
 * This class is the main centre of control of program flow for the ToneMap.
 * This manages the data held in internal structures that define the "map"
 * including classes ToneMapMatrix, TimeSet and PitchSet. Functions include
 * Loading of the Map from the Audio data, Processing of the Map through the
 * Tuner functions to produce MIDI sequences and Saving and Opening of the data
 * objects in Serialised form
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMap implements ToneMapConstants {

	private ToneMapMatrix toneMapMatrix;

	private int matrixLength;

	private TimeSet timeSet;

	private PitchSet pitchSet;

	public boolean audioSwitch = false;
	public boolean midiSwitch = false;

	public ToneMap() {
	}

	public ToneMapElement[] addFrame() {
		TimeSet newTimeSet = new TimeSet(this.timeSet.getStartTime(),
				this.timeSet.getEndTime() + this.timeSet.getSampleTimeSize(), this.timeSet.getSampleRate(),
				this.timeSet.getSampleTimeSize());
		// Instantiate new ToneMapMatrix
		ToneMapMatrix newToneMapMatrix = new ToneMapMatrix(newTimeSet, pitchSet);

		Iterator fromMapIterator = this.toneMapMatrix.newIterator();
		Iterator toMapIterator = newToneMapMatrix.newIterator();

		fromMapIterator.firstPitch();
		toMapIterator.firstPitch();
		do {
			fromMapIterator.firstTime();
			toMapIterator.firstTime();
			do {
				ToneMapElement toneMapElement = fromMapIterator.getElement();
				toMapIterator.newElement(toneMapElement.preAmplitude, toneMapElement.preFTPower);
				toMapIterator.nextTime();
			} while (fromMapIterator.nextTime());

		} while (fromMapIterator.nextPitch() && toMapIterator.nextPitch());

		toMapIterator.firstPitch();
		do {
			toMapIterator.newElement(0, 0);
		} while (toMapIterator.nextPitch());

		this.toneMapMatrix = newToneMapMatrix;
		this.timeSet = newTimeSet;
		return this.getFrame();
	}

	public ToneMap cens() {
		return this;
	}

	public ToneMap chroma() {
		return this;
	}

	/**
	 * Clear current ToneMap objects after Reset
	 */
	public void clear() {
		toneMapMatrix = null;
	}

	public ToneMap clone() {
		return this;
	}

	public ToneMap compress() {
		return this;
	}

	public void deleteFrame() {
		TimeSet newTimeSet = new TimeSet(this.timeSet.getStartTime() + this.timeSet.getSampleTimeSize(),
				this.timeSet.getEndTime(), this.timeSet.getSampleRate(), this.timeSet.getSampleTimeSize());
		// Instantiate new ToneMapMatrix
		ToneMapMatrix newToneMapMatrix = new ToneMapMatrix(newTimeSet, pitchSet);

		Iterator fromMapIterator = this.toneMapMatrix.newIterator();
		Iterator toMapIterator = newToneMapMatrix.newIterator();

		fromMapIterator.firstPitch();
		toMapIterator.firstPitch();
		do {
			fromMapIterator.firstTime();
			fromMapIterator.nextTime();
			toMapIterator.firstTime();
			do {
				ToneMapElement toneMapElement = fromMapIterator.getElement();
				toMapIterator.newElement(toneMapElement.preAmplitude, toneMapElement.preFTPower);
			} while (fromMapIterator.nextTime() && toMapIterator.nextTime());

		} while (fromMapIterator.nextPitch() && toMapIterator.nextPitch());

		this.toneMapMatrix = newToneMapMatrix;
		this.timeSet = newTimeSet;
		return;
	}

	public ToneMap esacf() {
		return this;
	}

	public ToneMap formants() {
		return this;
	}

	public ToneMap gammatron() {
		return this;
	}

	public ToneMapElement[] getFrame() {
		List<ToneMapElement> elements = new ArrayList<ToneMapElement>();
		Iterator mapIterator = toneMapMatrix.newIterator();
		mapIterator.lastTime();
		mapIterator.firstPitch();
		do {
			elements.add(mapIterator.getElement());
		} while (mapIterator.nextPitch());
		return elements.toArray(new ToneMapElement[elements.size()]);
	}

	public ToneMapMatrix getMatrix() {
		return toneMapMatrix;
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

	public ToneMap harmonics() {
		return this;
	}

	public ToneMap hps() {
		return this;
	}

	public boolean initialise(TimeSet timeSet, PitchSet pitchSet) {

		this.timeSet = timeSet;
		this.pitchSet = pitchSet;

		int timeRange = timeSet.getRange();
		int pitchRange = pitchSet.getRange();

		int matrixLength = timeRange * (pitchRange + 1);

		if (matrixLength < 0) {
			System.out.println(">>WHA!!");
		}
		// Build TomeMapMatrix
		if (!initMap())
			return false;

		int lowThreshhold = 0;
		int highThreshhold = 100;
		// tunerModel.setThreshhold(lowThreshhold, highThreshhold);
		ToneMapConfig config = new ToneMapConfig();
		config.peakSwitch = true;
		config.formantSwitch = true;
		config.harmonicSwitch = true;
		config.undertoneSwitch = true;
		config.normalizeSwitch = true;
		config.processMode = NONE;

		return true;
	}

	private boolean initMap() {

		// Instantiate new ToneMapMatrix
		toneMapMatrix = new ToneMapMatrix(timeSet, pitchSet);

		Iterator mapIterator = toneMapMatrix.newIterator();

		mapIterator.firstPitch();

		int index;
		// Iterate through ToneMapMatrix and load elements
		do {
			mapIterator.firstTime();
			do {
				index = mapIterator.getIndex();

				mapIterator.newElement(0, 0);

			} while (mapIterator.nextTime());

		} while (mapIterator.nextPitch());

		// toneMapMatrix.setAmpType(audioModel.logSwitch);
		// toneMapMatrix.setLowThres(audioModel.powerLow);
		// toneMapMatrix.setHighThres(audioModel.powerHigh);

		toneMapMatrix.reset();

		return true;

	}

	public ToneMap instantaneousFrequency() {
		return this;
	}

	public ToneMap loadACF() {
		return this;
	}

	public ToneMap loadCQ() {
		return this;
	}

	public ToneMap loadFFT() {
		return this;
	}

	public ToneMap logCompress() {
		return this;
	}

	public ToneMap mfcc() {
		return this;
	}

	public ToneMap noiseRemove() {
		return this;
	}

	public ToneMap normalise() {
		return this;
	}

	public ToneMap noteTrack() {
		return this;
	}

	public ToneMap peakPick() {
		return this;
	}

	public ToneMap putFrame() {
		return this;
	}

	public ToneMap quantize() {
		return this;
	}

	public ToneMap reset() {
		return this;
	}

	public ToneMap salience() {
		return this;
	}

	public void setThreshhold(int lowThreshhold, int highThreshhold) {
		// tunerModel.setThreshhold(lowThreshhold, highThreshhold);
	}

	public ToneMap smooth() {
		return this;
	}

	public ToneMap timbre() {
		return this;
	}

	public ToneMap tune() {
		return this;
	}

	public ToneMap whiten() {
		return this;
	}

} // End ToneMap