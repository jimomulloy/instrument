package jomu.instrument.workspace.tonemap;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TonePredictor {

	private static final Logger LOG = Logger.getLogger(TonePredictor.class.getName());

	List<NoteListElement> notes = new CopyOnWriteArrayList<>();
	List<ChordListElement> chords = new CopyOnWriteArrayList<>();
	List<BeatListElement> beats = new CopyOnWriteArrayList<>();

	private String key;

	public TonePredictor(String key) {
		LOG.finer(">>Tone Predictor create");
		this.key = key;
	}

	@Override
	public TonePredictor clone() {
		TonePredictor copy = new TonePredictor(key);
		for (NoteListElement note : notes) {
			copy.addNote(note);
		}
		for (ChordListElement chord : chords) {
			copy.addChord(chord);
		}
		for (BeatListElement beat : beats) {
			copy.addBeat(beat);
		}
		return copy;
	}

	public void load(TonePredictor fromTonePredictor) {
		for (NoteListElement note : fromTonePredictor.notes) {
			addNote(note);
		}
		for (ChordListElement chord : fromTonePredictor.chords) {
			addChord(chord);
		}
		for (BeatListElement beat : fromTonePredictor.beats) {
			addBeat(beat);
		}
	}

	public String getKey() {
		return key;
	}

	public void addNote(NoteListElement note) {
		LOG.finer(">>Tone Predictor add note: " + note);
		notes.add(note);
	}

	public void addChord(ChordListElement chord) {
		LOG.finer(">>Tone Predictor add chord: " + chord);
		chords.add(chord);
	}

	public void predictNotes(ToneTimeFrame targetFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent) {

	}

	public void predictBeats(ToneTimeFrame targetFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent) {

	}

	public void predictChord(ToneTimeFrame targetFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent) {
		if (chords.isEmpty()) {
			return;
		}
		Optional<ChordListElement> chord = getChord(targetFrame.getStartTime());
		Optional<ChordListElement> previousChord = getPreviousChord(targetFrame.getStartTime());
		LOG.finer(">>Predict Chord: " + targetFrame.getStartTime() + ", " + chord + ",  " + previousChord);
		if (previousChord.isEmpty() || (chord.isPresent() && chord.get().getChordNotes().size() > 2)) {
			return;
		}

		TreeSet<ChordNote> candidateChordNotes = new TreeSet<>();
		candidateChordNotes.addAll(previousChord.get().getChordNotes());

		if (chord.isPresent()) {
			for (ChordNote currentNote : chord.get().getChordNotes()) {
				candidateChordNotes.remove(currentNote);
			}
			if (candidateChordNotes.size() > 0) {
				boolean isChanged = false;
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
						isChanged = true;
						if (chord.get().getChordNotes().size() > 3) {
							break;
						}
					}
				}
				if (isChanged) {
					chords.set(chords.indexOf(previousChord.get()), chord.get());
					LOG.finer(">>Predict Chord changed: " + targetFrame.getStartTime() + ", " + chord + ",  "
							+ previousChord);
				}
			}
		} else {
			ChordListElement newChord = new ChordListElement(
					candidateChordNotes.toArray(new ChordNote[candidateChordNotes.size()]), targetFrame.getStartTime(),
					targetFrame.getEndTime());
			chords.add(chords.indexOf(previousChord.get()) + 1, newChord);
			LOG.severe(">>Predict Chord added: " + targetFrame.getStartTime() + ", " + chord + ",  " + previousChord
					+ ", " + targetFrame.getSpectralCentroid());
		}
	}

	public void addBeat(BeatListElement beat) {
		LOG.finer(">>Tone Predictor add beat: " + beat);
		beats.add(beat);
	}

	public boolean hasNote(double time) {
		return notes.stream()
				.anyMatch(noteListElement -> (noteListElement.startTime <= time && noteListElement.endTime >= time));
	}

	public Optional<NoteListElement> getNote(double time) {
		return notes.stream()
				.filter(noteListElement -> (noteListElement.startTime <= time && noteListElement.endTime >= time))
				.findFirst();
	}

	public List<NoteListElement> getNotes(double time) {
		return notes.stream()
				.filter(noteListElement -> (noteListElement.startTime <= time && noteListElement.endTime >= time))
				.collect(Collectors.toList());
	}

	public boolean hasBeat(double time) {
		return notes.stream()
				.anyMatch(beatListElement -> (beatListElement.startTime <= time && beatListElement.endTime >= time));
	}

	public Optional<BeatListElement> getBeat(double time) {
		return beats.stream()
				.filter(beatListElement -> (beatListElement.startTime <= time && beatListElement.endTime >= time))
				.findFirst();
	}

	public boolean hasChord(double time) {
		return chords.stream()
				.anyMatch(chordListElement -> (chordListElement.startTime <= time && chordListElement.endTime >= time));
	}

	public Optional<ChordListElement> getChord(double time) {
		return chords.stream()
				.filter(chordListElement -> (chordListElement.startTime <= time && chordListElement.endTime >= time))
				.findFirst();
	}

	private Optional<ChordListElement> getPreviousChord(double time) {
		Optional<ChordListElement> result = Optional.empty();
		for (ChordListElement chord : chords) {
			if (chord.startTime >= time) {
				break;
			}
			result = Optional.of(chord);
		}
		return result;
	}
}
