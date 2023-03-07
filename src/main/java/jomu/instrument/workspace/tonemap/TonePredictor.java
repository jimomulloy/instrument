package jomu.instrument.workspace.tonemap;

import java.util.LinkedList;

public class TonePredictor {

	LinkedList<NoteListElement> notes = new LinkedList<>();
	LinkedList<ChordListElement> chords = new LinkedList<>();
	LinkedList<BeatListElement> beats = new LinkedList<>();
	ToneMap toneMap;
	
	public TonePredictor(ToneMap toneMap) {
		this.toneMap = toneMap;
	}

	void addNote(NoteListElement note) {
		notes.add(note);
	}

	void addChord(ChordListElement chord) {
		chords.add(chord);
	}
	
	void addBeat(BeatListElement beat) {
		beats.add(beat);
	}

}
