package jomu.instrument.workspace.tonemap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

public class ToneSynthesiser {

	private static final Logger LOG = Logger.getLogger(ToneSynthesiser.class.getName());

	ConcurrentSkipListMap<Double, Map<Integer, NoteListElement>> notes = new ConcurrentSkipListMap<>();
	ConcurrentSkipListMap<Double, ChordListElement> chords = new ConcurrentSkipListMap<>();

	private ToneMap toneMap;

	public ToneSynthesiser(ToneMap toneMap) {
		this.toneMap = toneMap;
	}

	public void addNote(NoteListElement nle) {
		Map<Integer, NoteListElement> noteMap;
		if (notes.containsKey(nle.startTime)) {
			noteMap = notes.get(nle.startTime);
		} else {
			noteMap = new HashMap<>();
		}
		noteMap.put(nle.note, nle);
	}

	public void addChord(ChordListElement cle) {
		chords.put(cle.startTime, cle);
	}

	public void synthesise(ToneTimeFrame targetFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent) {
		fillChord(targetFrame);
		fillNotes(targetFrame);
		quantizeChord(targetFrame, calibrationMap, quantizeRange, quantizePercent);
		quantizeNotes(targetFrame, calibrationMap, quantizeRange, quantizePercent);
	}

	private void fillNotes(ToneTimeFrame targetFrame) {

	}

	private void quantizeNotes(ToneTimeFrame targetFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent) {

	}

	private void quantizeChord(ToneTimeFrame targetFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent) {
	}

	private void fillChord(ToneTimeFrame targetFrame) {
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
					chords.put(previousChord.get().startTime, chord.get());
					LOG.finer(">>Predict Chord changed: " + targetFrame.getStartTime() + ", " + chord + ",  "
							+ previousChord);
				}
			}
		} else {
			ChordListElement newChord = new ChordListElement(
					candidateChordNotes.toArray(new ChordNote[candidateChordNotes.size()]), targetFrame.getStartTime(),
					targetFrame.getEndTime());
			addChord(newChord);
			LOG.severe(">>Predict Chord added: " + targetFrame.getStartTime() + ", " + chord + ",  " + previousChord
					+ ", " + targetFrame.getSpectralCentroid());
		}
	}

	public boolean hasNote(double time) {
		return notes.containsKey(time);
	}

	public NoteListElement[] getNotes(double time) {
		return notes.get(time).values().toArray(new NoteListElement[notes.get(time).values().size()]);
	}

	public boolean hasChord(double time) {
		return chords.containsKey(time);
	}

	public Optional<ChordListElement> getChord(double time) {
		Optional<ChordListElement> result = Optional.empty();
		for (ChordListElement chord : chords.values()) {
			if (chord.startTime >= time) {
				result = Optional.of(chord);
				break;
			}
		}
		return result;
	}

	private Optional<ChordListElement> getPreviousChord(double time) {
		Optional<ChordListElement> result = Optional.empty();
		for (ChordListElement chord : chords.values()) {
			if (chord.startTime >= time) {
				break;
			}
			result = Optional.of(chord);
		}
		return result;
	}
}
