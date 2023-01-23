package jomu.instrument.workspace.tonemap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableSet;
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

	public boolean audioSwitch = false;

	public boolean midiSwitch = false;
	private ConcurrentSkipListMap<Double, ToneTimeFrame> toneMapStore = new ConcurrentSkipListMap<>();

	private String key;

	public ToneMap(String key) {
		this.key = key;
		toneMapStore = new ConcurrentSkipListMap<>();
	}

	public ToneTimeFrame addTimeFrame(ToneTimeFrame toneTimeFrame) {
		toneMapStore.put(toneTimeFrame.getStartTime(), toneTimeFrame);
		return toneTimeFrame;
	}

	public String getKey() {
		return key;
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

	@Override
	public ToneMap clone() {
		ToneMap copy = new ToneMap(key);
		for (Entry<Double, ToneTimeFrame> tmf : toneMapStore.entrySet()) {
			copy.addTimeFrame(tmf.getValue().clone());
		}
		return copy;
	}

	public ToneMap compress() {
		return this;
	}

	public void deleteTimeFrame() {
		toneMapStore.remove(toneMapStore.firstKey());
	}

	public void deleteTimeFrame(Double time) {
		toneMapStore.remove(time);
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

	public ToneTimeFrame getNextTimeFrame(Double key) {
		Entry<Double, ToneTimeFrame> nextEntry = toneMapStore.higherEntry(key);
		if (nextEntry != null) {
			return nextEntry.getValue();
		} else {
			return null;
		}
	}

	public ToneTimeFrame getPreviousTimeFrame() {
		Entry<Double, ToneTimeFrame> previousEntry = toneMapStore.lowerEntry(toneMapStore.lastKey());
		if (previousEntry != null) {
			return previousEntry.getValue();
		} else {
			return null;
		}
	}

	public ToneTimeFrame getPreviousTimeFrame(Double key) {
		Entry<Double, ToneTimeFrame> previousEntry = toneMapStore.lowerEntry(key);
		if (previousEntry != null) {
			return previousEntry.getValue();
		} else {
			return null;
		}
	}

	public ToneTimeFrame getTimeFrame() {
		if (!toneMapStore.isEmpty()) {
			return toneMapStore.lastEntry().getValue();
		}
		return null;
	}

	public ToneTimeFrame getTimeFrame(Double key) {
		return toneMapStore.get(key);
	}

	public ToneTimeFrame getTimeFrame(int sequence) {
		NavigableSet<Double> keySet = toneMapStore.keySet();
		int counter = sequence;
		Iterator<Double> iterator = keySet.iterator();
		Double key = null;
		while (counter > 0 && iterator.hasNext()) {
			key = iterator.next();
			counter--;
		}
		if (key == null) {
			return null;
		}
		return toneMapStore.get(key);
	}

	public ToneTimeFrame[] getTimeFramesFrom(Double key) {
		Collection<ToneTimeFrame> tailMap = toneMapStore.tailMap(key).values();
		return tailMap.toArray(new ToneTimeFrame[tailMap.size()]);
	}

	public ToneTimeFrame[] getTimeFramesTo(Double key) {
		Collection<ToneTimeFrame> headMap = toneMapStore.headMap(key).values();
		return headMap.toArray(new ToneTimeFrame[headMap.size()]);
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
		this.getTimeFrame().reset();
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