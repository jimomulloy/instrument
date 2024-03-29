package jomu.instrument.workspace.tonemap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class defines the fields of the data elements contained within the
 * ToneMapMatrix object associated with a ToneMap object.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMapElement implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public double amplitude;
	public double harmonicAmplitude;
	public int noteState;
	public boolean isPeak;
	public Map<Integer, Double> harmonicWeights = new HashMap();
	public MicroTones microTones = new MicroTones();

	int index;
	int pitchIndex;
	int timeIndex;

	transient public NoteListElement noteListElement;

	public ToneMapElement(double amplitude, int index, int timeIndex, int pitchIndex) {

		this.amplitude = amplitude;
		this.index = index;
		this.timeIndex = timeIndex;
		this.pitchIndex = pitchIndex;
	}

	public ToneMapElement(int index) {
		this(0, index, 0, index);
	}

	@Override
	public ToneMapElement clone() {
		ToneMapElement clone = new ToneMapElement(this.amplitude, this.index, this.timeIndex, this.pitchIndex);
		clone.noteState = this.noteState;
		clone.isPeak = this.isPeak;
		clone.microTones = microTones.clone();
		return clone;
	}

	public int getIndex() {
		return index;
	}

	public int getPitchIndex() {
		return pitchIndex;
	}

	public int getTimeIndex() {
		return timeIndex;
	}

	public void addHarmonicWieght(int harmonic, double rootAmplitude) {
		harmonicWeights.put(harmonic, rootAmplitude);
	}

	public double getHarmonicWeights(int harmonic) {
		return harmonicWeights.get(harmonic);
	}

	public int[] getHarmonics() {
		return harmonicWeights.keySet()
				.stream()
				.mapToInt(num -> Integer.valueOf(num))
				.toArray();
	}

	@Override
	public String toString() {
		return "ToneMapElement [amplitude=" + amplitude + ", noteState=" + noteState + ", index=" + index + ", pitch="
				+ pitchIndex + ", isPeak" + isPeak + "]";
	}

} // End ToneMapElement