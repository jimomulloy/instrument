package jomu.instrument.workspace.tonemap;

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

	int index;
	int pitchIndex;
	int timeIndex;

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
		return new ToneMapElement(this.amplitude, this.index, this.timeIndex, this.pitchIndex);
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

	@Override
	public String toString() {
		return "ToneMapElement [amplitude=" + amplitude + ", noteState=" + noteState + ", index=" + index + "]";
	}

} // End ToneMapElement