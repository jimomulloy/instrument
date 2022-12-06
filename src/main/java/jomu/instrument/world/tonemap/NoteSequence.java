package jomu.instrument.world.tonemap;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The class contains a list of Note data derived from the NoteList by the
 * MidiModel processing function in a form used to write MIDI messages to a MIDI
 * Sequence.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteSequence {

	private NoteSequenceElement element;

	private int index;

	private ArrayList noteSequence = new ArrayList();

	public void add(NoteSequenceElement element) {

		noteSequence.add(element);

	}

	public NoteSequenceElement get(int index) {

		this.index = index;
		return (NoteSequenceElement) noteSequence.get(index);

	}

	public int size() {

		return noteSequence.size();

	}

	public void sort() {

		Collections.sort(noteSequence);

	}

} // End NoteSequence