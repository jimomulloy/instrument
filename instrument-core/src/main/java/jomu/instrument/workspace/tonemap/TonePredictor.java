package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Logger;

public class TonePredictor {

	private static final Logger LOG = Logger.getLogger(TonePredictor.class.getName());

	LinkedList<NoteListElement> notes = new LinkedList<>();
	LinkedList<ChordListElement> chords = new LinkedList<>();
	LinkedList<BeatListElement> beats = new LinkedList<>();
	ToneMap toneMap;

	public TonePredictor(ToneMap toneMap) {
		LOG.finer(">>Tone Predictor create");
		this.toneMap = toneMap;
	}

	public void addNote(NoteListElement note) {
		LOG.finer(">>Tone Predictor add note: " + note);
		notes.add(note);
	}

	public void addChord(ChordListElement chord) {
		LOG.finer(">>Tone Predictor add chord: " + chord);
		chords.add(chord);
	}

	public void predictChord(ToneTimeFrame targetFrame) {
		ChordListElement chord = targetFrame.getChord();
		if (chord != null && chord.getChordNotes().size() > 2) {
			return;
		}
		List<ToneTimeFrame> previousFrames = new ArrayList<>();
		ToneTimeFrame ptf = toneMap.getPreviousTimeFrame(targetFrame.getStartTime());

		int i = 60;
		ChordListElement previousChord = null;
		TreeSet<ChordNote> chordNotes = new TreeSet<>();
		if (chord != null) {
			chordNotes.addAll(chord.getChordNotes());
		}
		while (ptf != null && i > 0) {
			previousChord = ptf.getChord();
			if (previousChord != null) {
				if (previousChord.getChordNotes().size() > 2) {
					break;
				}
				chordNotes.addAll(previousChord.getChordNotes());
				previousChord = null;
			}
			previousFrames.add(ptf);
			ptf = toneMap.getPreviousTimeFrame(ptf.getStartTime());
			i--;
		}
		if (previousChord != null) {
			chordNotes.addAll(previousChord.getChordNotes());
		}
		chord = new ChordListElement(chordNotes.toArray(new ChordNote[chordNotes.size()]), targetFrame.getStartTime(),
				targetFrame.getEndTime());
		targetFrame.setChord(new ChordListElement(chordNotes.toArray(new ChordNote[chordNotes.size()]),
				targetFrame.getStartTime(), targetFrame.getEndTime()));
		targetFrame.sharpenChord();
	}

	public void addBeat(BeatListElement beat) {
		LOG.finer(">>Tone Predictor add beat: " + beat);
		beats.add(beat);
	}

	public boolean hasNote(double time) {
		return notes.stream()
				.anyMatch(noteListElement -> (noteListElement.startTime >= time && noteListElement.endTime <= time));
	}

	public Optional<NoteListElement> getNote(double time) {
		return notes.stream()
				.filter(noteListElement -> (noteListElement.startTime >= time && noteListElement.endTime <= time))
				.findFirst();
	}

	public boolean hasBeat(double time) {
		return notes.stream()
				.anyMatch(beatListElement -> (beatListElement.startTime >= time && beatListElement.endTime <= time));
	}

	public Optional<NoteListElement> getBeat(double time) {
		return notes.stream()
				.filter(beatListElement -> (beatListElement.startTime >= time && beatListElement.endTime <= time))
				.findFirst();
	}

	public boolean hasChord(double time) {
		return notes.stream()
				.anyMatch(chordListElement -> (chordListElement.startTime >= time && chordListElement.endTime <= time));
	}

	public Optional<NoteListElement> getChord(double time) {
		return notes.stream()
				.filter(chordListElement -> (chordListElement.startTime >= time && chordListElement.endTime <= time))
				.findFirst();
	}
}
