package jomu.instrument.world.tonemap;

/**
 * This class defines the fields of the data elements contained within the
 * NoteSequence object which register state of Note data used for creating MIDI
 * Messages as written to a MIDI Sequence
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteStatusElement {

	public int note; // Midi note pitch

	public int index; // Index
	public int state; // Note state code
	public boolean highFlag; // Note High state flag
	public double onTime; // Note ON time
	public double offTime; // Note OFF time

	public NoteStatusElement(int note, int index) {
		this.note = note;
		this.index = index;
	}

} // End NoteStatusElement