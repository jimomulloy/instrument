package jomu.instrument.workspace.tonemap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class defines the fields of the data elements contained within the
 * ToneMapMatrix object associated with a ToneMap object.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMapElement {

	public double amplitude;
	public NoteListElement noteListElement;
	public int noteState;
	public boolean isPeak;
	public Map<Integer, Double> harmonicWeights = new HashMap();

	int index;
	int pitchIndex;
	int timeIndex;

	public ToneMapElement(double amplitude, int index, int timeIndex, int pitchIndex) {

		this.amplitude = amplitude;
		this.index = index;
		this.timeIndex = timeIndex;
		this.pitchIndex = pitchIndex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(index);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ToneMapElement other = (ToneMapElement) obj;
		return index == other.index;
	}

	public ToneMapElement(int index) {
		this(0, index, 0, index);
	}

	@Override
	public ToneMapElement clone() {
		ToneMapElement clone = new ToneMapElement(this.amplitude, this.index, this.timeIndex, this.pitchIndex);
		clone.noteState = this.noteState;
		clone.isPeak = this.isPeak;
		if (this.noteListElement != null) {
			clone.noteListElement = this.noteListElement.clone();
		}
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
		return harmonicWeights.keySet().stream().mapToInt(num -> Integer.valueOf(num)).toArray();
	}

	@Override
	public String toString() {
		return "ToneMapElement [amplitude=" + amplitude + ", noteState=" + noteState + ", index=" + index + "]";
	}

} // End ToneMapElement