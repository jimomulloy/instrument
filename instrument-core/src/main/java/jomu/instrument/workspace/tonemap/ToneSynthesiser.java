package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import jomu.instrument.workspace.tonemap.NoteTracker.NoteTrack;

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
			double quantizePercent, int quantizeBeat, boolean synthFillChords, boolean synthFillNotes,
			boolean chordFirst, boolean legato) {
		LOG.finer(">>SYNTH: " + targetFrame.getStartTime());
		if (chordFirst) {
			ChordListElement chord = targetFrame.getChord();
			if (chord != null) {
				addChord(targetFrame.getChord());
			}
			if (synthFillChords) {
				chord = fillChord(targetFrame, chord);
			}
			if (chord != null) {
				quantizeChord(chord, calibrationMap, quantizeRange, quantizePercent, quantizeBeat);
			}
		}
		Set<NoteListElement> discardedNotes = new HashSet<>();
		NoteListElement[] nles = addNotes(targetFrame);
		quantizeNotes(nles, calibrationMap, quantizeRange, quantizePercent, quantizeBeat);
		trackNotes(nles, discardedNotes, legato);
		if (synthFillNotes) {
			fillNotes(nles, calibrationMap, quantizeRange, quantizePercent, quantizeBeat);
		}
		discardedNotes.addAll(toneMap.getNoteTracker().cleanTracks(targetFrame.getStartTime()));
		discardNotes(discardedNotes);
		if (!chordFirst) {
			ChordListElement chord = targetFrame.getChord();
			if (chord != null) {
				addChord(chord);
			}
			if (synthFillChords) {
				chord = fillChord(targetFrame, chord);
			}
			if (targetFrame.getChord() == null) {
				targetFrame.setChord(chord);
			}
			if (chord != null) {
				quantizeChord(chord, calibrationMap, quantizeRange, quantizePercent, quantizeBeat);
			}
		}
	}

	private void discardNotes(Set<NoteListElement> discardedNotes) {
		for (NoteListElement nle : discardedNotes) {
			ToneTimeFrame frame = toneMap.getTimeFrame(nle.startTime / 1000);
			while (frame != null && frame.getStartTime() < nle.endTime) {
				if (nle.equals(frame.getElement(nle.pitchIndex).noteListElement)) {
					frame.getElement(nle.pitchIndex).noteListElement = null;
					frame.getElement(nle.pitchIndex).noteState = ToneMapConstants.OFF;
					frame.getElement(nle.pitchIndex).amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
				}
				frame = toneMap.getNextTimeFrame(frame.getStartTime());
			}
		}
	}

	private void trackNotes(NoteListElement[] nles, Set<NoteListElement> discardedNotes, boolean legato) {
		for (NoteListElement nle : nles) {
			NoteTrack track = toneMap.getNoteTracker().trackNote(nle, discardedNotes);
			if (track != null && legato) {
				addLegato(track, nle);
			}
		}
	}

	private void addLegato(NoteTrack track, NoteListElement nle) {
		NoteListElement pnle = track.getPenultimateNote();
		if (pnle != null) {
			if ((Math.abs(pnle.note - nle.note) <= 2) && ((pnle.endTime - nle.startTime) < 1000)
					&& (pnle.endTime <= nle.startTime)) {
				ToneTimeFrame frame = toneMap.getNextTimeFrame(pnle.startTime / 1000);
				if (frame != null) {
					double time = frame.getStartTime();
					while (frame != null && time < nle.startTime) {
						frame.getElement(pnle.pitchIndex).noteListElement = pnle;
						pnle.endTime = frame.getStartTime() * 1000;
						frame = toneMap.getNextTimeFrame(time);
						if (frame != null) {
							time = frame.getStartTime();
						}
					}
				}
			}
			pnle.addLegato(nle);
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

	private void fillNotes(NoteListElement[] nles, CalibrationMap calibrationMap, double quantizeRange,
			double quantizePercent, int quantizeBeat) {
		for (NoteListElement nle : nles) {
			NoteTrack track = toneMap.getNoteTracker().getTrack(nle);
			if (track != null) {
				double time = nle.endTime / 1000;
				double beatBeforeTime = calibrationMap.getBeatBeforeTime(time, quantizeRange);
				double beatAfterTime = calibrationMap.getBeatAfterTime(time, quantizeRange);
				if (beatBeforeTime <= 0) {
					beatBeforeTime = time;
				}
				if (beatAfterTime <= 0) {
					beatAfterTime = time;
				}
				double beatRange = beatAfterTime - beatBeforeTime;
				if (beatRange > 0) {
					NoteListElement pnle = track.getPenultimateNote();
					if (pnle != null && nle.startTime - pnle.endTime > beatRange) {
						// if (pnle != null && nle.startTime - pnle.startTime > beatRange) {
						synthesiseNotes(pnle, nle, track, calibrationMap, quantizeRange, quantizePercent, quantizeBeat);
					}
				}
			}
		}
	}

	private void synthesiseNotes(NoteListElement pnle, NoteListElement nle, NoteTrack track,
			CalibrationMap calibrationMap, double quantizeRange, double quantizePercent, int quantizeBeat) {
		List<Integer> noteList = track.getNoteList();
		NoteListElement newNle = null;
		ToneTimeFrame frame = toneMap.getNextTimeFrame(pnle.endTime / 1000);
		if (frame == null) {
			LOG.severe(">>synthesiseNotes null frame: " + pnle.endTime);
			return;
		}
		double time = frame.getStartTime();
		double beatBeforeTime = calibrationMap.getBeatBeforeTime(time, quantizeRange);
		double beatAfterTime = calibrationMap.getBeatAfterTime(time, quantizeRange);
		if (beatBeforeTime <= 0) {
			beatBeforeTime = time;
		}
		if (beatAfterTime <= 0) {
			beatAfterTime = time;
		}
		double beatRange = ((beatAfterTime - beatBeforeTime) / quantizeBeat) * 1000;
		while (frame != null && time < (nle.startTime / 1000)) {
			if (calibrationMap.getBeat(time, 0.11) != 0) {
				int counter = quantizeBeat;
				while (counter > 0 && frame != null && time < (nle.startTime / 1000)) {
					int r = (int) (Math.random() * (noteList.size()));
					newNle = nle.clone();
					newNle.startTime = time * 1000;

					double length = beatRange < ((nle.startTime - 100) - newNle.startTime) ? beatRange
							: beatRange - ((nle.startTime - 100) - newNle.startTime);
					if (length < 100) {
						frame = toneMap.getNextTimeFrame(time);
						if (frame != null) {
							time = frame.getStartTime();
						}
						break;
					}
					newNle.endTime = newNle.startTime + length;
					newNle.note = noteList.get(r);
					newNle.pitchIndex = newNle.pitchIndex + (newNle.note - nle.note);
					frame.getElement(newNle.pitchIndex).noteListElement = newNle;
					track.insertNote(newNle, pnle);
					frame = toneMap.getNextTimeFrame(time);
					time = frame.getStartTime();
					while (frame != null && time <= (newNle.endTime / 1000)) {
						frame.getElement(newNle.pitchIndex).noteListElement = newNle;
						frame = toneMap.getNextTimeFrame(time);
						if (frame != null) {
							time = frame.getStartTime();
						}
					}
					counter--;
				}
			} else {
				frame = toneMap.getNextTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
				}
			}
		}
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
		if (beatBeforeTime > 0) {
			beforeTimeDiff = ((time - beatBeforeTime) / quantizeBeat) * (quantizePercent / 100.0);
		}
		if (beatAfterTime > 0) {
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
				element.amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
				element.noteState = ToneMapConstants.OFF;
				frame = toneMap.getNextTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
					element = frame.getElement(index);
					element.noteState = ToneMapConstants.START;
				}
			}
		} else {
			LOG.severe(">>SYNTH QUANT NOTE DOWN: " + (element.noteListElement.equals(nle)) + ", " + time + ", "
					+ targetTime + ", " + frameTime);
			while (time > targetTime && frame != null) {
				element.noteListElement = nle;
				element.noteState = ToneMapConstants.ON;
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
		LOG.severe(">>SYNTH QUANT NOTE: " + time + ", " + targetTime + ", " + frameTime);
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
		if (beatBeforeTime > 0) {
			double targetTime = 0;
			double timeDiff = ((time - beatBeforeTime) / quantizeBeat) * (quantizePercent / 100.0);
			if (timeDiff > MIN_TIME_INCREMENT) {
				targetTime = time - timeDiff;
				quantizeChord(chord, targetTime);
			}
		} else if (beatAfterTime > 0) {
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

	private ChordListElement fillChord(ToneTimeFrame targetFrame, ChordListElement chord) {
		double startTime = targetFrame.getStartTime();
		double endTime = targetFrame.getEndTime();
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
			targetFrame.setChord(newChord);
			LOG.finer(">>Predict Chord added: " + newChord.getStartTime() + ", " + newChord + ",  " + previousChord);
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
			if (chord.getStartTime() <= time && chord.getEndTime() >= time) {
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
