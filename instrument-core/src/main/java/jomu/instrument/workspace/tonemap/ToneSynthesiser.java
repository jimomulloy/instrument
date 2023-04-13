package jomu.instrument.workspace.tonemap;

import java.util.Collection;
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
			notes.put(nle.startTime, noteMap);
		}
		noteMap.put(nle.note, nle);
	}

	public void removeNote(NoteListElement nle) {
		Map<Integer, NoteListElement> noteMap;
		if (notes.containsKey(nle.startTime)) {
			noteMap = notes.get(nle.startTime);
			noteMap.remove(nle.note);
			LOG.severe("TS REMOVE NOTE: " + nle);
		}
	}

	public void addChord(ChordListElement cle) {
		chords.put(cle.startTime, cle);
	}

	public void synthesise(ToneTimeFrame targetFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent) {
		ChordListElement chord = targetFrame.getChord();
		if (chord != null) {
			addChord(targetFrame.getChord());
		}
		addNotes(targetFrame);
		fillChord(targetFrame);
		fillNotes(targetFrame);
		quantizeChord(targetFrame, calibrationMap, quantizeRange, quantizePercent);
		quantizeNotes(targetFrame, calibrationMap, quantizeRange, quantizePercent);
	}

	private void addNotes(ToneTimeFrame targetFrame) {
		ToneMapElement[] elements = targetFrame.getElements();
		Double time = targetFrame.getStartTime();
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			NoteListElement nle = element.noteListElement;
			if (nle != null && time * 1000 == nle.startTime) {
				addNote(nle);
				toneMap.getNoteTracker().trackNote(nle);
			}
		}
	}

	private void fillNotes(ToneTimeFrame targetFrame) {

	}

	private void quantizeNotes(ToneTimeFrame sourceFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent) {
		Double time = sourceFrame.getStartTime();
		NoteListElement[] sourceNotes = getNotes(time * 1000);
		double beatTime = calibrationMap.getBeatTime(sourceFrame.getStartTime(), quantizeRange);
		if (beatTime != 0) {
			double targetTime = 0;
			if (time > beatTime) {
				targetTime = time - (time - beatTime) * (quantizePercent / 100.0);
			} else {
				targetTime = time + (beatTime - time) * (quantizePercent / 100.0);
			}
			if (Math.abs(time - targetTime) > 0.1) {
				LOG.severe(">>TS Quant notes in range: " + sourceNotes.length + ", " + time + ", " + targetTime);
				for (NoteListElement nle : sourceNotes) {
					if (nle.endTime / 1000 > targetTime) {
						quantizeNote(nle, targetTime, sourceFrame);
					}
				}
			}
		}
	}

	private void quantizeNote(NoteListElement nle, double targetTime, ToneTimeFrame sourceFrame) {
		LOG.severe(">>TS Quant note restart: " + nle.startTime + ", " + targetTime * 1000);
		removeNote(nle);
		// nle.startTime = targetTime * 1000;
		addNote(nle);
		int index = nle.pitchIndex;
		ToneTimeFrame frame = sourceFrame;
		double time = frame.getStartTime();
		ToneMapElement element = frame.getElement(index);
		if (time < targetTime) {
			while (time < targetTime && frame != null) {
				element.noteListElement = null;
				element.noteState = ToneMapConstants.OFF;
				LOG.severe(">>TS Quant note clear UP: " + time + ", " + targetTime);
				frame = toneMap.getNextTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
					element = frame.getElement(index);
					element.noteState = ToneMapConstants.START;
				}
			}
		} else {
			while (time > targetTime && frame != null) {
				element.noteListElement = nle;
				int state = element.noteState;
				frame = toneMap.getPreviousTimeFrame(time);
				LOG.severe(">>TS Quant note clear DOWN: " + time + ", " + targetTime);
				if (frame != null) {
					time = frame.getStartTime();
					element = frame.getElement(index);
					element.noteState = state;
				}
			}
		}
		element.noteState = ToneMapConstants.START;
		toneMap.getNoteTracker().moveNote(nle);
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
		LOG.severe("TS GET NOTES : " + time + ", " + notes.size());
		if (notes.containsKey(time)) {
			Collection<NoteListElement> values = notes.get(time).values();
			LOG.severe("TS GOT NOTES : " + time + ", " + values.size());
			return values.toArray(new NoteListElement[values.size()]);
		} else {
			return new NoteListElement[0];
		}
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
