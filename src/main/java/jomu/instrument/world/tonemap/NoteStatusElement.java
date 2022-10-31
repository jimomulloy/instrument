package jomu.instrument.world.tonemap;

import java.util.Objects;

/**
 * This class defines the fields of the data elements contained within the
 * NoteSequence object which register state of Note data used for creating MIDI
 * Messages as written to a MIDI Sequence
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteStatusElement {

	public boolean highFlag; // Note High state flag

	public int index; // Index
	public int note; // Midi note pitch
	public double offTime; // Note OFF time
	public double onTime; // Note ON time
	public int state; // Note state code

	public NoteStatusElement(int note, int index) {
		this.note = note;
		this.index = index;
	}

	@Override
	public int hashCode() {
		return Objects.hash(highFlag, index, note, offTime, onTime, state);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (getClass() != obj.getClass()))
			return false;
		NoteStatusElement other = (NoteStatusElement) obj;
		return highFlag == other.highFlag && index == other.index
				&& note == other.note
				&& Double.doubleToLongBits(offTime) == Double
						.doubleToLongBits(other.offTime)
				&& Double.doubleToLongBits(onTime) == Double
						.doubleToLongBits(other.onTime)
				&& state == other.state;
	}

	@Override
	public String toString() {
		return "NoteStatusElement [highFlag=" + highFlag + ", index=" + index
				+ ", note=" + note + ", offTime=" + offTime + ", onTime="
				+ onTime + ", state=" + state + "]";
	}


} // End NoteStatusElement