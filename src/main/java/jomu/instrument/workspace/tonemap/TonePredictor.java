package jomu.instrument.workspace.tonemap;

import java.util.LinkedList;
import java.util.Optional;
import java.util.logging.Logger;

public class TonePredictor {

	private static final Logger LOG = Logger.getLogger(TonePredictor.class.getName());

	LinkedList<NoteListElement> notes = new LinkedList<>();
	LinkedList<ChordListElement> chords = new LinkedList<>();
	LinkedList<BeatListElement> beats = new LinkedList<>();
	ToneMap toneMap;

	public TonePredictor(ToneMap toneMap) {
		LOG.fine(">>Tone Predictor create");
		this.toneMap = toneMap;
	}

	void addNote(NoteListElement note) {
		LOG.fine(">>Tone Predictor add note: " + note);
		notes.add(note);
	}

	void addChord(ChordListElement chord) {
		LOG.fine(">>Tone Predictor add chord: " + chord);
		chords.add(chord);
	}

	void addBeat(BeatListElement beat) {
		LOG.fine(">>Tone Predictor add beat: " + beat);
		beats.add(beat);
	}

	boolean hasNote(double time) {
		return notes.stream()
				.anyMatch(noteListElement -> (noteListElement.startTime >= time && noteListElement.endTime <= time));
	}

	Optional<NoteListElement> getNote(double time) {
		return notes.stream()
				.filter(noteListElement -> (noteListElement.startTime >= time && noteListElement.endTime <= time))
				.findFirst();
	}

	boolean hasBeat(double time) {
		return notes.stream()
				.anyMatch(beatListElement -> (beatListElement.startTime >= time && beatListElement.endTime <= time));
	}

	Optional<NoteListElement> getBeat(double time) {
		return notes.stream()
				.filter(beatListElement -> (beatListElement.startTime >= time && beatListElement.endTime <= time))
				.findFirst();
	}

	boolean hasChord(double time) {
		return notes.stream()
				.anyMatch(chordListElement -> (chordListElement.startTime >= time && chordListElement.endTime <= time));
	}

	Optional<NoteListElement> getChord(double time) {
		return notes.stream()
				.filter(chordListElement -> (chordListElement.startTime >= time && chordListElement.endTime <= time))
				.findFirst();
	}
}
