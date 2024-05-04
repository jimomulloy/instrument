package jomu.instrument.workspace.tonemap;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.NoteTracker.NoteTrack;

public class ToneSynthesiser implements ToneMapConstants {

	private static final double MIN_TIME_INCREMENT = 0.11;

	private static final Logger LOG = Logger
			.getLogger(ToneSynthesiser.class.getName());

	ConcurrentSkipListMap<Double, ChordListElement> chords = new ConcurrentSkipListMap<>();

	double minTimeIncrement = MIN_TIME_INCREMENT;
	double quantizeRange;
	double quantizePercent;
	int quantizeBeat;
	boolean synthFillChords;
	boolean synthFillNotes;
	boolean synthCleanTracks;
	boolean synthChordFirstSwitch;
	boolean synthFillLegatoSwitch;

	private ToneMap toneMap;

	private int quantizeSource;

	private boolean synthAggregateChordsSwitch;

	public ToneSynthesiser(ToneMap toneMap) {
		this.toneMap = toneMap;
		minTimeIncrement = toneMap.getParameterManager()
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_MIN_TIME_INCREMENT);
		quantizeRange = toneMap.getParameterManager()
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE);
		quantizePercent = toneMap.getParameterManager()
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT);
		quantizeBeat = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_BEAT);
		quantizeSource = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_SOURCE);
		synthFillChords = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_CHORDS_SWITCH);
		synthFillLegatoSwitch = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_LEGATO_SWITCH);
		synthFillNotes = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_NOTES_SWITCH);
		synthCleanTracks = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CLEAN_TRACKS_SWITCH);
		synthChordFirstSwitch = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD_FIRST_SWITCH);
		synthAggregateChordsSwitch = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_AGGREGATE_CHORDS_SWITCH);

	}

	public void removeChord(ChordListElement cle) {
		if (chords.containsKey(cle.getStartTime())) {
			chords.remove(cle.getStartTime());
		}
	}

	public void addChord(ChordListElement cle) {
		chords.put(cle.getStartTime(), cle);
	}

	public void synthesiseNotes(ToneTimeFrame toneTimeFrame, CalibrationMap calibrationMap) {
		toneMap.getNoteTracker()
				.trackBeats(toneTimeFrame);
		NoteTrack quantizeBeatTrack = toneMap.getNoteTracker()
				.getBeatTrack(quantizeSource);
		NoteListElement quantizeBeatNote = quantizeBeatTrack.getLastNote();
		ChordListElement chord = toneTimeFrame.getChord();
		if (synthChordFirstSwitch) {
			if (chord != null) {
				addChord(chord);
			}
			if (synthFillChords) {
				chord = backFillChord(toneTimeFrame, chord);
			}
			if (toneTimeFrame.getChord() == null) {
				toneTimeFrame.setChord(chord);
			}
			// if (chord != null) {
			// chord = quantizeChord(chord, calibrationMap, quantizeBeatNote, quantizeRange,
			// quantizePercent,
			// quantizeBeat);
			// }
		}
		Set<NoteListElement> discardedNotes = new HashSet<>();
		Set<NoteListElement> nles = addNotes(toneTimeFrame);
		if (nles.size() > 0) {
			quantizeNotes(nles, calibrationMap, quantizeBeatNote, quantizeRange, quantizePercent, quantizeBeat);
			trackNotes(nles, discardedNotes, synthFillLegatoSwitch);
			if (synthFillNotes) {
				fillNotes(nles, calibrationMap, quantizeBeatNote, quantizeRange, quantizePercent, quantizeBeat);
			}
			if (synthCleanTracks) {
				discardedNotes.addAll(toneMap.getNoteTracker()
						.cleanTracks(toneTimeFrame.getStartTime() * 1000));
			}
			discardNotes(discardedNotes);
		}
	}

	public void synthesiseChords(ToneTimeFrame toneTimeFrame) {
		ChordListElement chord = toneTimeFrame.getChord();
		if (!synthChordFirstSwitch) {
			if (chord != null) {
				addChord(chord);
			}
			if (synthFillChords) {
				chord = fillChord(toneTimeFrame, chord);
			}
			if (toneTimeFrame.getChord() == null) {
				toneTimeFrame.setChord(chord);
				chord = toneTimeFrame.getChord();
			}
		}

		ChordListElement ac = chord;
		if (synthAggregateChordsSwitch) {
			ac = aggregateChords(toneTimeFrame, chord);
		}
		NoteListElement nle = toneMap.getNoteTracker()
				.trackBase(ac, toneTimeFrame);
		if (nle != null && synthFillLegatoSwitch) {
			addLegato(toneMap.getNoteTracker()
					.getBaseTrack(), nle);
		}
		toneMap.getNoteTracker()
				.trackChords(ac, toneTimeFrame);
	}

	private ChordListElement aggregateChords(ToneTimeFrame toneTimeFrame, ChordListElement chord) {
		ChordListElement ac = null;
		if (chord == null) {
			ac = toneMap.getNoteTracker()
					.getChord(toneTimeFrame.getStartTime(), toneTimeFrame.getEndTime());
			if (ac == null) {
				return null;
			}
		} else {
			ac = chord.clone();
		}

		ChordListElement chordPre = null;
		Optional<ChordListElement> ocp = toneTimeFrame.getChordList(CellTypes.AUDIO_PRE_CHROMA.name());
		if (ocp.isPresent()) {
			chordPre = ocp.get();
			ac.merge(chordPre);
		}
		ChordListElement chordHm = null;
		Optional<ChordListElement> ochm = toneTimeFrame.getChordList(CellTypes.AUDIO_HPS.name() + "_HARMONIC");
		if (ochm.isPresent()) {
			chordHm = ochm.get();
			ac.merge(chordHm);
		}
		ChordListElement chordOs = null;
		Optional<ChordListElement> ocos = toneTimeFrame.getChordList(CellTypes.AUDIO_ONSET.name() + "_SMOOTHED");
		if (ocos.isPresent()) {
			chordOs = ocos.get();
			ac.merge(chordOs);
		}
		toneTimeFrame.sharpenChord(ac);
		return ac;
	}

	private void discardNotes(Set<NoteListElement> discardedNotes) {
		for (NoteListElement nle : discardedNotes) {
			ToneTimeFrame frame = toneMap.getTimeFrame(nle.startTime / 1000);
			while (frame != null && frame.getStartTime() <= nle.endTime) {
				if (nle.equals(frame.getElement(nle.pitchIndex).noteListElement)) {
					frame.getElement(nle.pitchIndex).noteListElement = null;
					frame.getElement(nle.pitchIndex).noteState = ToneMapConstants.OFF;
					frame.getElement(nle.pitchIndex).amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
				}
				frame = toneMap.getNextTimeFrame(frame.getStartTime());
			}
		}
	}

	private void trackNotes(Set<NoteListElement> nles, Set<NoteListElement> discardedNotes, boolean legato) {
		for (NoteListElement nle : nles) {
			NoteTrack track = toneMap.getNoteTracker()
					.trackNote(nle, discardedNotes, 0);
			if (track != null && legato) {
				addLegato(track, nle);
			}
		}
	}

	private void addLegato(NoteTrack track, NoteListElement nle) {
		NoteListElement pnle = track.getPreviousNote(nle);
		LOG.finer(">>ToneSynthesiser addLegato for nle A: " + nle.note + ", " + nle.startTime);
		if (pnle != null) {
			LOG.finer(">>ToneSynthesiser addLegato for nle B: " + nle.note + ", " + nle.startTime + ", " + pnle.note
					+ ", " + pnle.startTime + ", " + pnle.endTime);
			if ((Math.abs(pnle.note - nle.note) <= 2) && ((nle.startTime - pnle.endTime) < 1000)) {
				ToneTimeFrame frame = toneMap.getTimeFrame(pnle.endTime / 1000);
				ToneTimeFrame lastFrame = null;
				LOG.finer(">>ToneSynthesiser addLegato for nle C: " + nle.note + ", " + nle.startTime + ", "
						+ pnle.note + ", " + pnle.startTime + ", " + pnle.endTime + ", " + frame);
				if (frame != null) {
					double time = frame.getStartTime();
					while (frame != null && (time * 1000) < nle.startTime) {
						LOG.finer(">>ToneSynthesiser addLegato: " + time + ", " + nle.note + ", " + pnle.endTime + ", "
								+ track.getNumber());
						frame.getElement(pnle.pitchIndex).noteListElement = pnle;
						frame.getElement(pnle.pitchIndex).noteState = ON;
						frame.getElement(pnle.pitchIndex).amplitude = pnle.maxAmp;
						pnle.endTime = frame.getStartTime() * 1000;
						LOG.finer(
								">>ToneSynthesiser addLegato ENDTIME: " + time + ", " + pnle.note + ", "
										+ pnle.endTime);
						frame = toneMap.getNextTimeFrame(time);
						lastFrame = frame;
						if (frame != null) {
							time = frame.getStartTime();
						}
					}
					if (frame != null && pnle.note == nle.note) {
						while (frame != null && (time * 1000) <= nle.endTime) {
							frame.getElement(pnle.pitchIndex).noteListElement = pnle;
							frame.getElement(pnle.pitchIndex).noteState = ON;
							pnle.endTime = frame.getStartTime() * 1000;
							frame = toneMap.getNextTimeFrame(time);
							lastFrame = frame;
							if (frame != null) {
								time = frame.getStartTime();
							}
						}
						if (nle.legatoAfter != null) {
							pnle.legatoAfter = nle.legatoAfter;
							nle.legatoAfter.legatoBefore = pnle;
						}
						track.removeNote(nle);
						LOG.finer(">>SYNTH legato remove: " + track.getNumber() + ", " + nle.startTime);
					} else {
						pnle.addLegato(nle);
					}
					if (lastFrame != null) {
						lastFrame.getElement(pnle.pitchIndex).noteState = END;
					}
					LOG.finer(">>ToneSynthesiser addLegato for nle X: " + pnle.note + ", " + pnle.startTime + ", "
							+ nle.note + ", " + nle.startTime + ", " + pnle.endTime);

				}
			}
		}
	}

	private Set<NoteListElement> addNotes(ToneTimeFrame targetFrame) {
		Set<NoteListElement> notes = new HashSet<>();
		ToneMapElement[] elements = targetFrame.getElements();
		Double time = targetFrame.getStartTime();
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			ToneMapElement element = elements[elementIndex];
			NoteListElement nle = element.noteListElement;
			if (nle != null && time * 1000 == nle.startTime) {
				notes.add(nle);
			}
		}
		return notes;

	}

	private void fillNotes(Set<NoteListElement> nles, CalibrationMap calibrationMap, NoteListElement quantizeNote,
			double quantizeRange, double quantizePercent, int quantizeBeat) {
		if (nles.size() == 0 || quantizeNote == null) {
			return;
		}

		double toTime = Double.MAX_VALUE;
		double fromTime = 0;
		for (NoteListElement nle : nles) {
			if (toTime > nle.startTime) {
				toTime = nle.startTime;
			}
		}

		double beatBeforeTime = calibrationMap.getBeatBeforeTime(toTime / 1000, quantizeRange);
		double beatAfterTime = calibrationMap.getBeatAfterTime(toTime / 1000, quantizeRange);
		if (quantizeNote != null) {
			beatBeforeTime = quantizeNote.startTime / 1000.0;
			if (beatAfterTime == 0) {
				beatAfterTime = quantizeNote.endTime / 1000.0;
			}
		}

		if (beatBeforeTime <= 0) {
			beatBeforeTime = toTime / 1000;
		}
		if (beatAfterTime <= 0) {
			beatAfterTime = toTime / 1000;
		}

		double beatRange = (beatAfterTime - beatBeforeTime) * 1000;
		if (beatRange > 0) {
			Set<NoteTrack> earlyTracks = new HashSet<>();

			for (NoteTrack track : toneMap.getNoteTracker()
					.getTracks()) {
				NoteListElement nle = track.getLastNote();
				if (nle != null && nle.startTime < toTime) {
					if (toTime - nle.endTime > beatRange) {
						earlyTracks.add(track);
					} else {
						return;
					}
				}
			}

			Set<NoteTrack> currentTracks = new HashSet<>();
			for (NoteTrack track : toneMap.getNoteTracker()
					.getTracks()) {
				NoteListElement nle = track.getLastNote();
				if (nle != null && !earlyTracks.contains(track)) {
					NoteListElement pnle = track.getPreviousNote(nle);
					if (pnle != null) {
						if (nle.startTime - pnle.endTime > beatRange) {
							if (fromTime < pnle.endTime) {
								fromTime = pnle.endTime;
							}
							currentTracks.add(track);
						} else {
							return;
						}
					} else {
						currentTracks.add(track);
					}
				}
			}

			if (toTime - fromTime > beatRange) {
				for (NoteTrack track : currentTracks) {
					synthesiseNotes(fromTime, toTime, track, calibrationMap, true);
				}
				for (NoteTrack track : earlyTracks) {
					synthesiseNotes(fromTime, toTime, track, calibrationMap, false);
				}
			}
		}
	}

	private void synthesiseNotes(double fromTime, double toTime, NoteTrack track, CalibrationMap calibrationMap,
			boolean before) {
		List<Integer> noteList = track.getNoteList();
		NoteListElement nle = track.getLastNote();
		NoteListElement newNle = null;
		NoteListElement insertNle;
		if (before) {
			insertNle = track.getPreviousNote(nle);
		} else {
			insertNle = nle;
		}
		ToneTimeFrame frame = toneMap.getNextTimeFrame(fromTime / 1000);
		ToneTimeFrame endFrame = toneMap.getPreviousTimeFrame(toTime / 1000);
		if (frame == null) {
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
		ToneTimeFrame lastFrame = null;
		LOG.severe(">>synthesiseNote A: " + track.number + ", " + beatRange + ", " + time + ", " + fromTime + ", "
				+ toTime + ", " + beatBeforeTime + ", "
				+ beatAfterTime);

		while (frame != null && time < (toTime / 1000)) {
			if (calibrationMap.getBeat(time, (MIN_TIME_INCREMENT * 1000)) > -1) {
				int counter = quantizeBeat;
				LOG.severe(
						">>synthesiseNote A1: " + counter + ", " + time + ", " + beatBeforeTime + ", " + beatAfterTime);
				while (counter > 0 && frame != null && time < (toTime / 1000)) {
					int r = (int) (Math.random() * (noteList.size()));
					newNle = nle.clone();
					newNle.startTime = time * 1000;

					double length = beatRange < ((endFrame.getStartTime() * 1000) - newNle.startTime)
							? beatRange
							: beatRange - ((endFrame.getStartTime() * 1000) - newNle.startTime);
					if (length < 100) {
						lastFrame = frame;
						frame = toneMap.getNextTimeFrame(time);
						if (frame != null) {
							time = frame.getStartTime();
						}
						break;
					}
					newNle.endTime = time * 1000;
					newNle.note = noteList.get(r);
					newNle.pitchIndex = newNle.pitchIndex + (newNle.note - nle.note);
					frame.getElement(newNle.pitchIndex).noteListElement = newNle;
					frame.getElement(newNle.pitchIndex).amplitude = newNle.maxAmp;
					frame.getElement(newNle.pitchIndex).noteState = START;
					lastFrame = frame;
					frame = toneMap.getNextTimeFrame(time);
					if (frame != null) {
						time = frame.getStartTime();
						while (frame != null && time < (nle.startTime / 1000)
								&& time <= ((newNle.startTime + length) / 1000)) {
							if (lastFrame.getElement(newNle.pitchIndex).noteState != START) {
								lastFrame.getElement(newNle.pitchIndex).noteState = ON;
							}
							frame.getElement(newNle.pitchIndex).noteListElement = newNle;
							frame.getElement(newNle.pitchIndex).amplitude = newNle.maxAmp;
							frame.getElement(newNle.pitchIndex).noteState = END;
							LOG.finer(">>synthesiseNote B: " + time + ", " + newNle.maxAmp);
							newNle.endTime = time * 1000;
							lastFrame = frame;
							frame = toneMap.getNextTimeFrame(time);
							if (frame != null) {
								time = frame.getStartTime();
							}
						}
					}
					track.insertNote(newNle, insertNle);
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

	private void quantizeNotes(Set<NoteListElement> nles, CalibrationMap calibrationMap, NoteListElement quantizeNote,
			double quantizeRange, double quantizePercent, int quantizeBeat) {
		if (nles.size() == 0 || quantizeNote == null) {
			return;
		}

		double time = nles.iterator()
				.next().startTime / 1000;
		double beatBeforeTime = calibrationMap.getBeatBeforeTime(time, quantizeRange);
		double beatAfterTime = calibrationMap.getBeatAfterTime(time, quantizeRange);
		if (quantizeNote != null) {
			beatBeforeTime = quantizeNote.startTime / 1000.0;
			if (beatAfterTime == 0) {
				beatAfterTime = quantizeNote.endTime / 1000.0;
			}
		}

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
			ToneTimeFrame ttf = toneMap.getPreviousTimeFrame(targetTime);
			if (ttf == null) {
				ttf = toneMap.getNextTimeFrame(targetTime);
			}
			if (ttf != null) {
				for (NoteListElement nle : nles) {
					quantizeNote(nle, ttf.getStartTime());
				}
			}
		} else {
			if (afterTimeDiff > MIN_TIME_INCREMENT) {
				double targetTime = time + afterTimeDiff;
				ToneTimeFrame ttf = toneMap.getNextTimeFrame(targetTime);
				if (ttf == null) {
					ttf = toneMap.getPreviousTimeFrame(targetTime);
				}
				if (ttf != null) {
					for (NoteListElement nle : nles) {
						quantizeNote(nle, ttf.getStartTime());
					}
				}
			}
		}
	}

	private void quantizeNote(NoteListElement nle, double targetTime) {
		double frameTime = nle.startTime / 1000;
		nle.startTime = targetTime * 1000;
		int index = nle.pitchIndex;
		ToneTimeFrame frame = toneMap.getTimeFrame(frameTime);
		double time = frame.getStartTime();
		ToneMapElement element = frame.getElement(index);
		if (time < targetTime) {
			LOG.finer(">>SYNTH QUANT NOTE UP: " + time + ", " + targetTime + ", " + frameTime);
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
			LOG.finer(">>SYNTH QUANT NOTE DOWN: " + (element.noteListElement.equals(nle)) + ", " + time + ", "
					+ targetTime + ", " + frameTime);
			while (time > targetTime && frame != null) {
				element.noteListElement = nle;
				element.amplitude = nle.maxAmp;
				element.noteState = ToneMapConstants.ON;
				int state = element.noteState;
				frame = toneMap.getPreviousTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
					element = frame.getElement(index);
					element.noteState = state;
					element.noteListElement = nle;
					element.amplitude = nle.maxAmp;
				}
			}
		}
		LOG.finer(">>SYNTH QUANT NOTE: " + time + ", " + targetTime + ", " + frameTime);
		element.noteState = ToneMapConstants.START;
	}

	private ChordListElement quantizeChord(ChordListElement chord, CalibrationMap calibrationMap,
			NoteListElement quantizeNote,
			double quantizeRange, double quantizePercent, int quantizeBeat) {
		if (chord == null || quantizeNote == null) {
			return chord;
		}
		double time = chord.getStartTime();

		double beatBeforeTime = calibrationMap.getBeatBeforeTime(time, quantizeRange);
		double beatAfterTime = calibrationMap.getBeatAfterTime(time, quantizeRange);
		if (quantizeNote != null) {
			beatBeforeTime = quantizeNote.startTime / 1000.0;
			if (beatAfterTime == 0) {
				beatAfterTime = quantizeNote.endTime / 1000.0;
			}
		}
		if (beatBeforeTime > 0) {
			double targetTime = 0;
			double timeDiff = ((time - beatBeforeTime) / quantizeBeat) * (quantizePercent / 100.0);
			if (timeDiff > MIN_TIME_INCREMENT) {
				targetTime = time - timeDiff;
				return quantizeChord(chord, targetTime);
			}
		} else if (beatAfterTime > 0) {
			double targetTime = 0;
			double timeDiff = ((beatAfterTime - time) / quantizeBeat) * (quantizePercent / 100.0);
			if (timeDiff > MIN_TIME_INCREMENT) {
				targetTime = time + timeDiff;
				return quantizeChord(chord, targetTime);
			}
		}
		return chord;

	}

	private ChordListElement quantizeChord(ChordListElement cle, double targetTime) {
		double frameTime = cle.getStartTime();
		removeChord(cle);
		ToneTimeFrame frame = toneMap.getTimeFrame(frameTime);
		double time = frame.getStartTime();
		if (time < targetTime) {
			while (time <= targetTime && frame != null) {
				frameTime = frame.getStartTime();
				frame.setChord(cle);
				frame = toneMap.getNextTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
				}
			}
		} else {
			while (time >= targetTime && frame != null) {
				frameTime = frame.getStartTime();
				frame.setChord(cle);
				frame = toneMap.getPreviousTimeFrame(time);
				if (frame != null) {
					time = frame.getStartTime();
				}
			}
		}
		cle.setStartTime(frameTime);
		addChord(cle);
		return cle;
	}

	private ChordListElement backFillChord(ToneTimeFrame targetFrame, ChordListElement chord) {
		double startTime = targetFrame.getStartTime();
		double endTime = targetFrame.getEndTime();
		if (chords.isEmpty()) {
			return chord;
		}
		Optional<ChordListElement> previousChord = getPreviousChord(startTime);
		if (previousChord.isEmpty() || (chord != null && chord.getChordNotes()
				.size() > 3)) {
			return chord;
		}

		TreeSet<ChordNote> candidateChordNotes = new TreeSet<>();
		candidateChordNotes.addAll(previousChord.get()
				.getChordNotes());

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
						chord.getChordNotes()
								.add(candidateNote);
						isChanged = true;
						if (chord.getChordNotes()
								.size() > 3) {
							break;
						}
					}
				}
				if (isChanged) {
					chords.put(previousChord.get()
							.getStartTime(), chord);
				}
			}
			if (!previousChord.isEmpty() && chord.getChordNotes()
					.equals(previousChord.get()
							.getChordNotes())) {
				chord.setStartTime(previousChord.get()
						.getStartTime());
			}
			targetFrame.setChord(chord);
			return chord;
		} else {
			ChordListElement newChord = new ChordListElement(startTime, endTime,
					candidateChordNotes.toArray(new ChordNote[candidateChordNotes.size()]));
			addChord(newChord);
			targetFrame.setChord(newChord);
			return newChord;
		}
	}

	private ChordListElement fillChord(ToneTimeFrame targetFrame, ChordListElement chord) {
		double startTime = targetFrame.getStartTime();
		double endTime = targetFrame.getEndTime();

		ChordListElement nextChord = getNextChord(targetFrame, chord);

		TreeSet<ChordNote> candidateChordNotes = new TreeSet<>();

		if (nextChord != null) {
			candidateChordNotes.addAll(nextChord
					.getChordNotes());
		}

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
						chord.getChordNotes()
								.add(candidateNote);
						isChanged = true;
						if (chord.getChordNotes()
								.size() > 3) {
							break;
						}
					}
				}

				if (isChanged) {
					if (nextChord != null) {
						chords.put(nextChord
								.getStartTime(), chord);
					}
				}
			}
			if (nextChord != null && chord.getChordNotes()
					.equals(nextChord
							.getChordNotes())) {
				chord.setStartTime(nextChord
						.getStartTime());
			}
			targetFrame.setChord(chord);
			return chord;
		} else {
			ChordListElement newChord = new ChordListElement(startTime, endTime,
					candidateChordNotes.toArray(new ChordNote[candidateChordNotes.size()]));
			addChord(newChord);
			targetFrame.setChord(newChord);
			return newChord;
		}
	}

	private ChordListElement getNextChord(ToneTimeFrame targetFrame, ChordListElement targetChord) {
		ToneTimeFrame frame = targetFrame;
		ChordListElement nextChord = targetChord;
		while (frame != null && (nextChord == null || nextChord.getChordNotes().size() < 3)) {
			frame = toneMap.getNextTimeFrame(frame.getStartTime());
			if (frame != null) {
				nextChord = frame.getChord();
			}
		}
		return nextChord;
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
