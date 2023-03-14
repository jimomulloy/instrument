package jomu.instrument.workspace.tonemap;

/**
 * This class defines the fields of the data elements contained within the
 * NoteSequence object which register state of Note data used for creating MIDI
 * Messages as written to a MIDI Sequence
 *
 * @author Jim O'Mulloy
 */
public class NoteStatusElement {

	public boolean highFlag;
	public int index;
	public int note;
	public double offTime;
	public double onTime;
	public int state;
	public boolean isContinuation;

	public NoteStatusElement(int note, int index) {
		this.note = note;
		this.index = index;
	}

	public NoteStatusElement clone() {
		NoteStatusElement clone = new NoteStatusElement(note, index);
		clone.offTime = offTime;
		clone.onTime = offTime;
		clone.state = state;
		clone.isContinuation = isContinuation;
		return clone;
	}

	@Override
	public String toString() {
		return "NoteStatusElement [highFlag=" + highFlag + ", index=" + index + ", note=" + note + ", offTime="
				+ offTime + ", onTime=" + onTime + ", state=" + state + ", isContinuation=" + isContinuation + "]";
	}

} // End NoteStatusElement