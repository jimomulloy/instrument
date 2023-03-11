package jomu.instrument.workspace.tonemap;

import java.util.Objects;

/**
 * This class defines the fields of the data elements contained within the
 * NoteSequence object which represent Note data used for creating MIDI Messages
 * for writing to a MIDI Sequence
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteSequenceElement implements Comparable {

	@Override
	public int hashCode() {
		return Objects.hash(note);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NoteSequenceElement other = (NoteSequenceElement) obj;
		return note == other.note;
	}

	public int note; // MIDI note pitch

	public int state; // State of MIDI note (on/off)

	public long tick; // Tick value representing relative time of note in
						// sequence
	public int velocity; // velocity (amplitude) of note

	public NoteSequenceElement(int note, int state, long tick, int velocity) {
		this.note = note;
		this.state = state;
		this.tick = tick;
		this.velocity = velocity;
	}

	@Override
	public int compareTo(Object o) {

		long otick = ((NoteSequenceElement) o).tick;
		return (tick < otick ? -1 : (tick == otick ? 0 : 1));
	}

} // End NoteSequenceElement