package jomu.instrument.model.tonemap;

import java.util.concurrent.ConcurrentSkipListMap;

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
public class ToneMap {

	private ConcurrentSkipListMap<Double, ToneTimeFrame> toneMapStore = new ConcurrentSkipListMap<>();

	private int matrixLength;

	public boolean audioSwitch = false;
	public boolean midiSwitch = false;

	public ToneMap() {
	}

	public void addTimeFrame(ToneTimeFrame toneTimeFrame) {
		toneMapStore.put(toneTimeFrame.getStartTime(), toneTimeFrame);
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
		toneMapStore = new ConcurrentSkipListMap<>();
	}

	public ToneMap clone() {
		return this;
	}

	public ToneMap compress() {
		return this;
	}

	public void deleteFrame(Double time) {
		toneMapStore.remove(time);
	}
	
	public void deleteFrame() {
		toneMapStore.remove(toneMapStore.firstKey());
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

	public ToneTimeFrame getFrame() {
		return toneMapStore.lastEntry().getValue();
	}

	public ToneMap harmonics() {
		return this;
	}

	public ToneMap hps() {
		return this;
	}

	public void initialise() {
		toneMapStore = new ConcurrentSkipListMap<>();
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