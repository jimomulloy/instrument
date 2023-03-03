package jomu.instrument.workspace.tonemap;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class NoteTracker {

	Set<NoteTrack> tracks = new HashSet<>();

	class NoteTrack {
		double salience;
		LinkedList<NoteListElement> notes = new LinkedList<>();

		void addNote(NoteListElement note) {
			notes.add(note);
		}

	}

}
