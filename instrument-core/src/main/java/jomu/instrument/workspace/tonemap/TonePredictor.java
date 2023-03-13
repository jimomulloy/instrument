package jomu.instrument.workspace.tonemap;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class TonePredictor {

	private static final Logger LOG = Logger.getLogger(TonePredictor.class.getName());

	List<NoteListElement> notes = new CopyOnWriteArrayList<>();
	List<ChordListElement> chords = new CopyOnWriteArrayList<>();
	List<BeatListElement> beats = new CopyOnWriteArrayList<>();

	public TonePredictor() {
		LOG.finer(">>Tone Predictor create");
	}

	public void addNote(NoteListElement note) {
		LOG.finer(">>Tone Predictor add note: " + note);
		notes.add(note);
	}

	public void addChord(ChordListElement chord) {
		LOG.finer(">>Tone Predictor add chord: " + chord);
		chords.add(chord);
	}

	public ChordListElement[] getChords() {
		// LOG.severe(">>Tone Predictor chords size: " + chords.size());
		return chords.toArray(new ChordListElement[chords.size()]);
	}

	public void predictChord(ToneTimeFrame targetFrame) {
		if (chords.isEmpty()) {
			return;
		}
		Optional<ChordListElement> chord = getChord(targetFrame.getStartTime());
		Optional<ChordListElement> previousChord = getPreviousChord(targetFrame.getStartTime());
		if (previousChord.isEmpty() || (chord.isPresent() && chord.get().getChordNotes().size() > 2)) {
			return;
		}

		TreeSet<ChordNote> candidateChordNotes = new TreeSet<>();
		candidateChordNotes.addAll(previousChord.get().getChordNotes());

		if (chord.isPresent()) {
			for (ChordNote currentNote : chord.get().getChordNotes()) {
				candidateChordNotes.remove(currentNote);
			}
			for (ChordNote candidateNote : candidateChordNotes) {
				boolean isValid = true;
				for (ChordNote currentNote : chord.get().getChordNotes()) {
					if ((Math.abs(candidateNote.index - currentNote.index) <= 1)
							|| (candidateNote.index == 11 && currentNote.index == 0)
							|| (candidateNote.index == 0 && currentNote.index == 11)) {
						isValid = false;
						break;
					}
				}
				if (isValid) {
					chord.get().getChordNotes().add(candidateNote);
					if (chord.get().getChordNotes().size() > 3) {
						break;
					}
				}
			}
			LOG.severe(">>Update chord: " + targetFrame.getStartTime() + ", " + chord);
		} else {
			ChordListElement newChord = new ChordListElement(
					candidateChordNotes.toArray(new ChordNote[candidateChordNotes.size()]), targetFrame.getStartTime(),
					targetFrame.getEndTime());
			chords.add(chords.indexOf(previousChord.get()) + 1, newChord);
			LOG.severe(">>Add chord: " + targetFrame.getStartTime() + ", " + newChord);
		}
	}

	public void addBeat(BeatListElement beat) {
		LOG.finer(">>Tone Predictor add beat: " + beat);
		beats.add(beat);
	}

	public boolean hasNote(double time) {
		for (NoteListElement noteListElement : notes) {
			if (noteListElement.startTime >= time && noteListElement.endTime <= time) {
				return true;
			}
		}
		return false;
	}

	public Optional<NoteListElement> getNote(double time) {
		Optional<NoteListElement> result = Optional.empty();
		for (NoteListElement noteListElement : notes) {
			if (noteListElement.startTime >= time && noteListElement.endTime <= time) {
				result = Optional.of(noteListElement);
				break;
			}
		}
		return result;
	}

	public boolean hasBeat(double time) {
		for (BeatListElement beatListElement : beats) {
			if (beatListElement.startTime >= time && beatListElement.endTime <= time) {
				return true;
			}
		}
		return false;
	}

	public Optional<BeatListElement> getBeat(double time) {
		Optional<BeatListElement> result = Optional.empty();
		for (BeatListElement beatListElement : beats) {
			if (beatListElement.startTime >= time && beatListElement.endTime <= time) {
				result = Optional.of(beatListElement);
				break;
			}
		}
		return result;
	}

	public boolean hasChord(double time) {
		for (ChordListElement chordListElement : chords) {
			if (chordListElement.startTime >= time && chordListElement.endTime <= time) {
				return true;
			}
		}
		return false;
	}

	public Optional<ChordListElement> getChord(double time) {
		Optional<ChordListElement> result = Optional.empty();
		for (ChordListElement chordListElement : chords) {
			if (chordListElement.startTime >= time && chordListElement.endTime <= time) {
				result = Optional.of(chordListElement);
				break;
			}
		}
		return result;
	}

	private Optional<ChordListElement> getPreviousChord(double time) {
		Optional<ChordListElement> result = Optional.empty();
		if (chords.size() > 0) {
			ChordListElement chord = chords.get(chords.size() - 1);
			int index = chords.indexOf(chord);
			while (index > 0 && chord.startTime >= time) {
				index--;
				chord = chords.get(index);
				if (chord.startTime < time) {
					result = Optional.of(chord);
				}
			}
		}
		return result;
	}
}
