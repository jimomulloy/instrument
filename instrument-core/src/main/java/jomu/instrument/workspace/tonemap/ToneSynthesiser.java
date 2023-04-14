package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

public class ToneSynthesiser {

	private static final double MIN_TIME_INCREMENT = 0.1;

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
		}
	}

	public void removeChord(ChordListElement cle) {
		if (chords.containsKey(cle.getStartTime())) {
			chords.remove(cle.getStartTime());
		}
	}

	public void addChord(ChordListElement cle) {
		chords.put(cle.getStartTime(), cle);
	}

	public void synthesise(ToneTimeFrame targetFrame, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent, int quantizeBeat) {
		LOG.severe(">>SYNTH: " + targetFrame.getStartTime());
		ChordListElement chord = targetFrame.getChord();
		if (chord != null) {
			addChord(targetFrame.getChord());
		}

		chord = fillChord(targetFrame.getStartTime(), targetFrame.getEndTime(), chord);
		quantizeChord(chord, calibrationMap, quantizeRange, quantizePercent, quantizeBeat);
		LOG.severe(">>SYNTH chord: " + targetFrame.getStartTime() + ", " + chord);

		NoteListElement[] nles = addNotes(targetFrame);
		quantizeNotes(nles, calibrationMap, quantizeRange, quantizePercent, quantizeBeat);
		trackNotes(nles);
		fillNotes(nles);
		LOG.severe(">>SYNTH notes: " + targetFrame.getStartTime() + ", " + nles);

	}

	private void trackNotes(NoteListElement[] nles) {
		for (NoteListElement nle : nles) {
			toneMap.getNoteTracker().trackNote(nle);
		}
	}

	private NoteListElement[] addNotes(ToneTimeFrame targetFrame) {
		List<NoteListElement> noteList = new ArrayList<>();
		ToneMapElement[] elements = targetFrame.getElements();
		Double time = targetFrame.getStartTime();
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			NoteListElement nle = element.noteListElement;
			if (nle != null && time * 1000 == nle.startTime) {
				addNote(nle);
				noteList.add(nle);
			}
		}
		return noteList.toArray(new NoteListElement[noteList.size()]);
	}

	private void fillNotes(NoteListElement[] nles) {

	}

	private void quantizeNotes(NoteListElement[] nles, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent, int quantizeBeat) {
		if (nles.length == 0) {
			return;
		}
		Double time = nles[0].startTime / 1000;
		double beatBeforeTime = calibrationMap.getBeatBeforeTime(time, quantizeRange);
		double beatAfterTime = calibrationMap.getBeatAfterTime(time, quantizeRange);
		double beforeTimeDiff = 0;
		double afterTimeDiff = 0;
		if (beatBeforeTime != 0) {
			beforeTimeDiff = ((time - beatBeforeTime) / quantizeBeat) * (quantizePercent / 100.0);
		}
		if (beatAfterTime != 0) {
			afterTimeDiff = ((beatAfterTime - time) / quantizeBeat) * (quantizePercent / 100.0);
		}
		if (beforeTimeDiff <= MIN_TIME_INCREMENT || afterTimeDiff <= MIN_TIME_INCREMENT) {
			return;
		}
		if (beforeTimeDiff > MIN_TIME_INCREMENT) {
			double targetTime = time - beforeTimeDiff;
			for (NoteListElement nle : nles) {
				quantizeNote(nle, targetTime);
			}
		} else {
			if (afterTimeDiff > MIN_TIME_INCREMENT) {
				double targetTime = time + afterTimeDiff;
				for (NoteListElement nle : nles) {
					quantizeNote(nle, targetTime);
				}
			}
		}
	}

	private void quantizeNote(NoteListElement nle, double targetTime) {
		double frameTime = nle.startTime / 1000;
		removeNote(nle);
		nle.startTime = targetTime * 1000;
		addNote(nle);
		int index = nle.pitchIndex;
		ToneTimeFrame frame = toneMap.getTimeFrame(frameTime);
		double time = frame.getStartTime();
		ToneMapElement element = frame.getElement(index);
		if (time < targetTime) {
			LOG.severe(">>SYNTH QUANT NOTE UP: " + time + ", " + targetTime + ", " + frameTime);
			while (time < targetTime && frame != null) {
				element.noteListElement = null;
				element.noteState = ToneMapConstants.OFF;
				frame = toneMap.getNextTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
					element = frame.getElement(index);
					element.noteState = ToneMapConstants.START;
				}
			}
		} else {
			LOG.severe(">>SYNTH QUANT NOTE DOWN: " + time + ", " + targetTime + ", " + frameTime);
			while (time > targetTime && frame != null) {
				element.noteListElement = nle;
				int state = element.noteState;
				frame = toneMap.getPreviousTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
					element = frame.getElement(index);
					element.noteState = state;
					element.noteListElement = nle;
				}
			}
		}
		element.noteState = ToneMapConstants.START;
	}

	private void quantizeChord(ChordListElement chord, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent, int quantizeBeat) {
		if (chord == null) {
			return;
		}
		double time = chord.getStartTime();
		double beatBeforeTime = calibrationMap.getBeatBeforeTime(time, quantizeRange);
		double beatAfterTime = calibrationMap.getBeatAfterTime(time, quantizeRange);
		if (beatBeforeTime != 0) {
			double targetTime = 0;
			double timeDiff = ((time - beatBeforeTime) / quantizeBeat) * (quantizePercent / 100.0);
			if (timeDiff > MIN_TIME_INCREMENT) {
				targetTime = time - timeDiff;
				quantizeChord(chord, targetTime);
			}
		} else if (beatAfterTime != 0) {
			double targetTime = 0;
			double timeDiff = ((beatAfterTime - time) / quantizeBeat) * (quantizePercent / 100.0);
			if (timeDiff > MIN_TIME_INCREMENT) {
				targetTime = time + timeDiff;
				quantizeChord(chord, targetTime);
			}
		}

	}

	private void quantizeChord(ChordListElement cle, double targetTime) {
		double frameTime = cle.getStartTime();
		removeChord(cle);
		cle.setStartTime(targetTime);
		addChord(cle);
		ToneTimeFrame frame = toneMap.getTimeFrame(frameTime);
		double time = frame.getStartTime();
		if (time < targetTime) {
			while (time <= targetTime && frame != null) {
				frame.setChord(cle);
				frame = toneMap.getNextTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
				}
			}
		} else {
			while (time >= targetTime && frame != null) {
				frame.setChord(cle);
				frame = toneMap.getPreviousTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
				}
			}
		}
	}

	private ChordListElement fillChord(double startTime, double endTime, ChordListElement chord) {
		if (chords.isEmpty()) {
			return chord;
		}
		Optional<ChordListElement> previousChord = getPreviousChord(startTime);
		if (previousChord.isEmpty() || (chord != null && chord.getChordNotes().size() > 2)) {
			return chord;
		}

		TreeSet<ChordNote> candidateChordNotes = new TreeSet<>();
		candidateChordNotes.addAll(previousChord.get().getChordNotes());

		if (chord != null) {
			for (ChordNote currentNote : chord.getChordNotes()) {
				candidateChordNotes.remove(currentNote);
			}
			if (candidateChordNotes.size() > 0) {
				boolean isChanged = false;
				for (ChordNote candidateNote : candidateChordNotes) {
					boolean isValid = true;
					for (ChordNote currentNote : chord.getChordNotes()) {
						if ((Math.abs(candidateNote.index - currentNote.index) <= 1)
								|| (candidateNote.index == 11 && currentNote.index == 0)
								|| (candidateNote.index == 0 && currentNote.index == 11)) {
							isValid = false;
							break;
						}
					}
					if (isValid) {
						chord.getChordNotes().add(candidateNote);
						isChanged = true;
						if (chord.getChordNotes().size() > 3) {
							break;
						}
					}
				}
				if (isChanged) {
					chords.put(previousChord.get().getStartTime(), chord);
					LOG.finer(
							">>Predict Chord changed: " + chord.getStartTime() + ", " + chord + ",  " + previousChord);
				}
			}
			return chord;
		} else {
			ChordListElement newChord = new ChordListElement(
					candidateChordNotes.toArray(new ChordNote[candidateChordNotes.size()]), startTime, endTime);
			addChord(newChord);
			LOG.severe(">>Predict Chord added: " + newChord.getStartTime() + ", " + newChord + ",  " + previousChord);
			return newChord;
		}
	}

	public boolean hasNote(double time) {
		return notes.containsKey(time);
	}

	public NoteListElement[] getNotes(double time) {
		if (notes.containsKey(time)) {
			Collection<NoteListElement> values = notes.get(time).values();
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
			if (chord.getStartTime() >= time) {
				result = Optional.of(chord);
				break;
			}
		}
		return result;
	}

	private Optional<ChordListElement> getPreviousChord(double time) {
		Optional<ChordListElement> result = Optional.empty();
		for (ChordListElement chord : chords.values()) {
			if (chord.getStartTime() >= time) {
				break;
			}
			result = Optional.of(chord);
		}
		return result;
	}
}
