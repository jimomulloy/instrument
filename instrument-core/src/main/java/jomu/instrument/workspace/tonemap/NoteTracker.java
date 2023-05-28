package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;

public class NoteTracker {
	
	record SynthChordParameters(int chordSource, int chordMeasure, int chordPattern, int chordOctave, int chordOffset, int chordTimeSignature){};

	record SynthBeatParameters(int beatSource, int beatDrum, int beatOffset, int beatTimeSignature){};
	
	private static final Logger LOG = Logger.getLogger(NoteTracker.class.getName());

	ConcurrentHashMap<Integer, NoteTrack> tracks = new ConcurrentHashMap<>();

	ConcurrentHashMap<Integer, NoteTrack> beatTracks = new ConcurrentHashMap<>();

	ConcurrentHashMap<Integer, NoteTrack> chordTracks = new ConcurrentHashMap<>();

	ToneMap toneMap;

	int maxTracksUpper;
	int maxTracksLower;
	int clearRangeUpper;
	int clearRangeLower;
	int discardTrackRange;
	int overlapSalientNoteRange;
	int overlapSalientTimeRange;
	int salientNoteRange;
	int salientTimeRange;
	int salientTimeNoteFactor;

	private boolean synthFillLegatoSwitch;

	private NoteTrack baseTrack;

	private int synthBasePattern;

	private int synthBaseMeasure;

	private int baseTimeSignature;

	private int synthBaseOctave;

	private int incrementTime;

	private int synthChordBeat;

	private int synthChordPattern;

	private int synthChordOctave;

	private int synthBeat1Measure;

	private int synthBeat1Offset;

	private int synthBeat1Pattern;
	
	private int synthBeatTiming;
	
	private int synthBeat1Source;

	private int synthBeat2Measure;

	private int synthBeat2Offset;

	private int synthBeat2Pattern;
	
	private int synthBeat2Timing;
	
	private int synthBeat2Source;

	private int beat1TimeSignature;

	private int beat2TimeSignature;

	private int synthBeat1Drum;

	private int synthBeat2Drum;

	private int synthBeat3Drum;

	private int synthBeat4Drum;

	private int synthChord1Measure;

	private int synthChord1Offset;

	private int synthChord1Pattern;

	private int synthChordTiming;

	private int chord1TimeSignature;

	private int synthChord1Octave;

	private int synthChord2Measure;

	private int synthChord2Offset;

	private int synthChord2Pattern;

	private int synthChord2Timing;

	private int chord2TimeSignature;

	private int synthChord2Octave;

	private int synthChord1Source;

	private int synthChord2Source;

	public class NoteTrack {

		int number;
		double salience;
		List<NoteListElement> notes = new CopyOnWriteArrayList<>();

		public NoteTrack(int number) {
			this.number = number;
		}

		public int getNumber() {
			return this.number;
		}

		public NoteListElement getNote(double time) {
			double startTime = Double.MAX_VALUE;
			NoteListElement firstNote = null;
			for (NoteListElement note : notes) {
				if (note.startTime <= time && note.endTime >= time && note.startTime < startTime) {
					startTime = note.startTime;
					firstNote = note;
				}
			}
			return firstNote;
		}

		public NoteListElement getStartNote(double time) {
			for (NoteListElement note : notes) {
				if (note.startTime == time) {
					return note;
				}
			}
			return null;
		}

		public NoteListElement getEndNote(double time) {
			for (NoteListElement note : notes) {
				if (note.endTime == time) {
					return note;
				}
			}
			return null;
		}

		public boolean isEmpty() {
			return notes.isEmpty();
		}

		public NoteListElement[] getEndNotes(double time) {
			List<NoteListElement> noteList = new ArrayList<>();
			for (NoteListElement note : notes) {
				LOG.finer(">>NoteTracker getEndNotes: " + time + ", " + note.note + ", "
						+ (note.endTime + note.incrementTime) + ", " + number + ", " + note.startTime + " ,"
						+ note.endTime + " ," + note.incrementTime);
				if (Math.floor(note.endTime + note.incrementTime) == Math.floor(time)) {
					LOG.finer(">>NoteTracker GOT getEndNotes: " + time + ", " + note.note + ", " + number + ", "
							+ note.startTime + " ," + note.endTime + ", " + (note.endTime + note.incrementTime));
					noteList.add(note);
				}
			}
			return noteList.toArray(new NoteListElement[noteList.size()]);
		}

		public NoteListElement[] getStartNotes(double time) {
			List<NoteListElement> noteList = new ArrayList<>();
			for (NoteListElement note : notes) {
				if (Math.floor(note.startTime) == Math.floor(time)) {
					noteList.add(note);
				}
			}
			return noteList.toArray(new NoteListElement[noteList.size()]);
		}

		public NoteListElement[] getNotes(double time) {
			List<NoteListElement> noteList = new ArrayList<>();
			for (NoteListElement note : notes) {
				if (note.startTime <= time && Math.floor(note.endTime + note.incrementTime) >= Math.floor(time)) {
					noteList.add(note);
				}
			}
			return noteList.toArray(new NoteListElement[noteList.size()]);
		}

		public NoteListElement getFirstNote() {
			if (notes.size() > 0) {
				return notes.get(0);
			}
			return null;
		}

		public NoteListElement getPreviousNote(NoteListElement nle) {
			int index = notes.indexOf(nle);
			if (index > 0) {
				return notes.get(index - 1);
			}
			return null;
		}

		public void addNote(NoteListElement note) {
			notes.add(note);
		}

		public NoteListElement getLastNote() {
			if (notes.size() > 0) {
				return notes.get(notes.size() - 1);
			}
			return null;
		}

		public List<NoteListElement> getNotes() {
			return notes;
		}

		public boolean hasNote(NoteListElement note) {
			return notes.contains(note);
		}

		public NoteListElement getPenultimateNote() {
			if (notes.size() > 1) {
				return notes.get(notes.size() - 2);
			}
			return null;
		}

		public void removeNote(NoteListElement disconnectedNote) {
			notes.remove(disconnectedNote);
		}

		@Override
		public String toString() {
			return "NoteTrack [number=" + number + ", salience=" + salience + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(number);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NoteTrack other = (NoteTrack) obj;
			return number == other.number;
		}

		public List<Integer> getNoteList() {
			List<Integer> noteList = new ArrayList<>();
			for (NoteListElement nle : notes) {
				noteList.add(nle.note);
			}
			Collections.sort(noteList);
			return noteList;
		}

		public int getSize() {
			return notes.size();
		}

		public void insertNote(NoteListElement nle, NoteListElement pnle) {
			int addIndex = 0;
			if (pnle != null) {
				addIndex = notes.indexOf(pnle) + 1;
			}
			notes.add(addIndex, nle);
		}

		public NoteListElement getNote(double startTime, double endTime) {
			for (NoteListElement nle : notes) {
				if ((nle.startTime <= startTime && Math.floor(nle.endTime + nle.incrementTime) >= Math.floor(endTime))
						|| (nle.startTime > startTime
								&& Math.floor(nle.endTime + nle.incrementTime) < Math.floor(endTime))) {
					return nle;
				}
			}
			return null;
		}

		public NoteListElement[] getCurrentNotes(double time) {
			Set<NoteListElement> noteList = new HashSet<>();
			for (NoteListElement note : notes) {
				if (note.startTime <= time && Math.floor(note.endTime + note.incrementTime) >= Math.floor(time)) {
					noteList.add(note);
				}
			}
			return noteList.toArray(new NoteListElement[noteList.size()]);
		}
	}

	public NoteTracker(ToneMap toneMap) {
		this.toneMap = toneMap;
		maxTracksUpper = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_MAX_TRACKS_UPPER);
		maxTracksLower = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_MAX_TRACKS_LOWER);
		clearRangeUpper = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_CLEAR_RANGE_UPPER);
		clearRangeLower = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_CLEAR_RANGE_LOWER);
		discardTrackRange = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_DISCARD_TRACK_RANGE);
		overlapSalientNoteRange = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_OVERLAP_SALIENT_NOTE_RANGE);
		overlapSalientTimeRange = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_OVERLAP_SALIENT_TIME_RANGE);
		salientNoteRange = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_NOTE_RANGE);
		salientTimeRange = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_TIME_RANGE);
		salientTimeNoteFactor = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_TIME_NOTE_FACTOR);
		synthFillLegatoSwitch = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_LEGATO_SWITCH);
		synthBaseMeasure = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_MEASURE);
		synthBasePattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_PATTERN);
		if (synthBasePattern == 1 || synthBasePattern != 2) {
			baseTimeSignature = 4;
		} else {
			baseTimeSignature = 3;
		}
		synthBaseOctave = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_OCTAVE);
		
		synthChordTiming = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD_TIMING);
		synthChord1Measure = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_MEASURE);
		synthChord1Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_OFFSET);
		synthChord1Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_PATTERN);
		synthChord1Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_SOURCE);

		if (synthChord1Pattern == 1 || synthChord1Pattern != 2) {
			chord1TimeSignature = 4;
		} else {
			chord1TimeSignature = 3;
		}
		synthChord1Octave = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_OCTAVE);
		
		synthChord2Measure = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_MEASURE);
		synthChord2Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_OFFSET);
		synthChord2Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_PATTERN);
		synthChord2Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_SOURCE);
		if (synthChord2Pattern == 1 || synthChord2Pattern != 2) {
			chord2TimeSignature = 4;
		} else {
			chord2TimeSignature = 3;
		}
		synthChord2Octave = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_OCTAVE);
		
		synthBeatTiming = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT_TIMING);
		synthBeat1Measure = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_MEASURE);
		synthBeat1Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_OFFSET);
		synthBeat1Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_PATTERN);
		synthBeat1Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_SOURCE);
	
		if (synthBeat1Pattern == 1) {
			beat1TimeSignature = 1;
		} else if (synthBeat1Pattern == 2) {
			beat1TimeSignature = 2;
		} else if (synthBeat1Pattern == 3) {
			beat1TimeSignature = 3;
		} else if (synthBeat1Pattern == 4) {
			beat1TimeSignature = 4;
		}
		
		synthBeat2Measure = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_MEASURE);
		synthBeat2Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_OFFSET);
		synthBeat2Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_PATTERN);
		synthBeat2Timing = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_TIMING);
		synthBeat2Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_SOURCE);
		if (synthBeat2Pattern == 1) {
			beat2TimeSignature = 1;
		} else if (synthBeat2Pattern == 2) {
			beat2TimeSignature = 2;
		} else if (synthBeat2Pattern == 3) {
			beat2TimeSignature = 3;
		} else if (synthBeat2Pattern == 4) {
			beat2TimeSignature = 4;
		}

		synthBeat1Drum = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_1);
		synthBeat2Drum = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_2);
		synthBeat3Drum = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_3);
		synthBeat4Drum = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_4);

		incrementTime = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL);
	}

	public NoteTrack trackNote(NoteListElement noteListElement, Set<NoteListElement> discardedNotes) {
		LOG.finer(">>NoteTracker trackNote: " + noteListElement.note + ", " + noteListElement);
		NoteTrack pendingOverlappingSalientTrack = null;
		NoteTrack salientTrack = null;
		NoteListElement disconnectedNote = null;
		if (tracks.isEmpty()) {
			salientTrack = createTrack();
			LOG.finer(">>NoteTracker trackNote create track A: " + salientTrack + ", " + noteListElement.note);
		} else {
			NoteTrack[] pendingTracks = getPendingTracks(noteListElement);
			if (pendingTracks.length > 0) {
				pendingOverlappingSalientTrack = getPendingOverlappingSalientTrack(pendingTracks, noteListElement);
				salientTrack = pendingOverlappingSalientTrack;
			}

			NoteTrack[] nonPendingTracks = getNonPendingTracks(noteListElement);

			if (nonPendingTracks.length > 0) {
				salientTrack = getSalientTrack(nonPendingTracks, pendingOverlappingSalientTrack, noteListElement);
				if (pendingOverlappingSalientTrack == salientTrack) {
					LOG.finer(">>NoteTracker trackNote B: " + noteListElement.note);
					if (synthFillLegatoSwitch) {
						LOG.finer(">>NoteTracker addLegato for nle B: "
								+ pendingOverlappingSalientTrack.getLastNote().note + ", " + noteListElement + ", "
								+ pendingOverlappingSalientTrack.getLastNote());
						pendingOverlappingSalientTrack.getLastNote().addLegato(noteListElement);
					}
				}
				if (salientTrack != null) {
					LOG.finer(">>NoteTracker trackNote C: " + noteListElement.note + ", " + salientTrack);
				}
			}

			if (salientTrack == null && pendingTracks.length > 0) {
				LOG.finer(">>NoteTracker trackNote getPendingSalientTrack note: " + noteListElement.note);
				salientTrack = getPendingSalientTrack(pendingTracks, noteListElement);
				if (salientTrack != null) {
					LOG.finer(">>NoteTracker trackNote gotPendingSalientTrack note: " + noteListElement.note);
					disconnectedNote = salientTrack.getLastNote();
					LOG.finer(">>NT salient remove: " + salientTrack.getNumber() + ", " + disconnectedNote.startTime);
					salientTrack.removeNote(disconnectedNote);
				}
			}
		}
		if (salientTrack == null) {
			if (tracks.size() > maxTracksUpper
					&& ((noteListElement.endTime - noteListElement.startTime) <= discardTrackRange)) {
				discardedNotes.add(noteListElement);
				LOG.finer(">>NoteTracker trackNote discard: " + noteListElement.note);
				return null;
			} else {
				salientTrack = createTrack();
				LOG.finer(">>NoteTracker trackNote CREATED B: " + salientTrack);
				if (salientTrack == null) {
					discardedNotes.add(noteListElement);
					LOG.finer(">>NoteTracker trackNote discard B: " + noteListElement.note);
					return null;
				}
				LOG.finer(">>NoteTracker trackNote create track B: " + noteListElement.note);
			}
		}
		Set<NoteTrack> discardedTracks = new HashSet<>();
		for (NoteTrack track : tracks.values()) {
			if (!track.equals(salientTrack)) {
				NoteListElement noteInRange = track.getNote(noteListElement.startTime, noteListElement.endTime);
				if (noteInRange != null && Math.abs(noteListElement.note - noteInRange.note) <= 1) {
					if (noteListElement.startTime == noteInRange.startTime
							&& noteListElement.endTime == noteInRange.endTime) {
						if (noteListElement.maxAmp < noteInRange.maxAmp) {
							discardedNotes.add(noteListElement);
							if (salientTrack.isEmpty()) {
								discardedTracks.add(salientTrack);
							}
							LOG.finer(">>NoteTracker trackNote DISCARD B: " + noteListElement);
							return null;
						} else {
							track.removeNote(noteInRange);
							discardedNotes.add(noteInRange);
							if (track.isEmpty()) {
								discardedTracks.add(track);
							}
							LOG.finer(">>NoteTracker trackNote DISCARD E: " + noteInRange);
						}
					} else if ((noteListElement.startTime >= noteInRange.startTime
							&& noteListElement.endTime <= noteInRange.endTime)) {
						discardedNotes.add(noteListElement);
						if (salientTrack.isEmpty()) {
							discardedTracks.add(salientTrack);
						}
						LOG.finer(">>NoteTracker trackNote DISCARD C: " + noteListElement);
						return null;
					} else {
						track.removeNote(noteInRange);
						discardedNotes.add(noteInRange);
						if (track.isEmpty()) {
							discardedTracks.add(track);
						}
						LOG.finer(">>NoteTracker trackNote DISCARD D: " + noteInRange);
					}
				}
			}
		}
		for (NoteTrack track : discardedTracks) {
			removeTrack(track);
		}

		salientTrack.addNote(noteListElement);
		LOG.finer(">>NoteTracker addNote: " + noteListElement.note + ", " + salientTrack + ", "
				+ noteListElement.startTime + " ," + noteListElement.endTime);

		for (NoteTrack track : tracks.values()) {
			if (track.isEmpty()) {
				removeTrack(track);
			}
		}

		LOG.finer(">>NoteTracker trackNote ADDED note: " + noteListElement.note + ", " + salientTrack.number);
		if (disconnectedNote != null) {
			LOG.finer(">>NoteTracker trackNote disconnected note: " + disconnectedNote + ", " + noteListElement.note);
			trackNote(disconnectedNote, discardedNotes);
		}
		return salientTrack;
	}

	public NoteListElement trackBase(BeatListElement beatListElement, ChordListElement chordListElement,
			PitchSet pitchSet) {
		if (baseTrack == null) {
			baseTrack = new NoteTrack(1);
		}

		if (beatListElement.getAmplitude() > ToneTimeFrame.AMPLITUDE_FLOOR) {
			NoteListElement lastNote = baseTrack.getLastNote();
			if (lastNote == null || beatListElement.getStartTime() * 1000 >= lastNote.endTime) {
				return addBaseNote(baseTrack, beatListElement, chordListElement, pitchSet);
			}
		}
		return null;
	}

	private NoteListElement addBaseNote(NoteTrack track, BeatListElement beatListElement,
			ChordListElement chordListElement, PitchSet pitchSet) {
		if (beatListElement == null || chordListElement == null) {
			return null;
		}
		boolean isBar = track.getSize() % baseTimeSignature == 0;
		int barNote = track.getSize() % baseTimeSignature + 1;
		int barCount = track.getSize() / baseTimeSignature;

		int note = 0;
		double startTime = beatListElement.getStartTime() * 1000;
		double endTime = startTime;
		endTime += beatListElement.getTimeRange() > 0 ? beatListElement.getTimeRange() * 1000 * synthBaseMeasure
				: synthBaseMeasure * 200;
		double amplitude = 1.0;

		List<Double> camps = new ArrayList<>();
		List<Integer> cnotes = new ArrayList<>();

		ChordNote[] chordNotes = chordListElement.getChordNotes()
				.toArray(new ChordNote[chordListElement.getChordNotes().size()]);
		Arrays.sort(chordNotes, new Comparator<ChordNote>() {
			public int compare(ChordNote c1, ChordNote c2) {
				return Double.valueOf(c2.getAmplitude()).compareTo(Double.valueOf(c1.getAmplitude()));
			}
		});
		int rootNote = -1;
		double rootAmp = -1;
		for (ChordNote chordNote : chordNotes) {
			note = 0;
			amplitude = chordNote.getAmplitude();

			note = chordNote.getPitchClass();
			if (rootNote < 0) {
				rootNote = note;
				rootAmp = amplitude;
			}
			if (note < rootNote) {
				note += 12;
			}
			note += synthBaseOctave * 12;
			camps.add(amplitude);
			cnotes.add(note);
		}
		rootNote += synthBaseOctave * 12;
		if (isBar) {
			note = rootNote;
			amplitude = rootAmp;
		} else {
			if (synthBasePattern == 1 || synthBasePattern == 2) {
				int noteIndex = 0;
				if (cnotes.size() > barNote) {
					noteIndex = barNote;
				} else {
					int r = (int) (Math.random() * (cnotes.size()));
					noteIndex = r;
				}
				note = cnotes.get(noteIndex);
				amplitude = camps.get(noteIndex);
			}
		}

		NoteListElement baseNote = new NoteListElement(note, pitchSet.getIndex(note), startTime, endTime, 0, 0,
				amplitude, amplitude, amplitude, 0, false, incrementTime);
		track.addNote(baseNote);
		return baseNote;
	}

	public void trackChords(BeatListElement beatListElement, ChordListElement chordListElement,
			ToneTimeFrame toneTimeFrame) {
		processChordTrack(1, toneTimeFrame, beatListElement, chordListElement, new SynthChordParameters(synthChord1Source, synthChord1Measure, synthChord1Pattern, synthChord1Octave, synthChord1Offset, chord1TimeSignature));
		processChordTrack(2, toneTimeFrame, beatListElement, chordListElement, new SynthChordParameters(synthChord2Source, synthChord2Measure, synthChord2Pattern, synthChord2Octave, synthChord2Offset, chord2TimeSignature));
	}
	
	private void processChordTrack(int trackNumber, ToneTimeFrame toneTimeFrame, BeatListElement beatListElement, ChordListElement chordListElement,
			SynthChordParameters synthChordParameters) {
		NoteTrack chordTrack;
		if (!chordTracks.containsKey(trackNumber)) {
			chordTrack = new NoteTrack(trackNumber);
			chordTracks.put(trackNumber, chordTrack);
		} else {
			chordTrack = chordTracks.get(trackNumber);
		}
		
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		if (synthChordParameters.chordSource == 0) {
			ChordListElement cle = toneTimeFrame.getChord();
			if (cle != null) {
				addChordNotes(chordTrack, beatListElement, toneTimeFrame.getStartTime(), cle, pitchSet, synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 1) {
			Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_POST_CHROMA.name());
			if (oc.isPresent()) {
				LOG.severe(">>NT got chord: " + toneTimeFrame.getStartTime()+ ", " + oc.get());
				addChordNotes(chordTrack, beatListElement, toneTimeFrame.getStartTime(), oc.get(), pitchSet, synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 2) {
			Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_ONSET.name() + "_SMOOTHED");
			if (oc.isPresent()) {
				addChordNotes(chordTrack, beatListElement, toneTimeFrame.getStartTime(), oc.get(), pitchSet, synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 3) {
			Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_HPS.name() + "_HARMONIC");
			if (oc.isPresent()) {
				addChordNotes(chordTrack, beatListElement, toneTimeFrame.getStartTime(), oc.get(), pitchSet, synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 4) {
			Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_PRE_CHROMA.name());
			if (oc.isPresent()) {
				addChordNotes(chordTrack, beatListElement, toneTimeFrame.getStartTime(), oc.get(), pitchSet, synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 5) {
			addChordNotes(chordTrack, beatListElement, toneTimeFrame.getStartTime(), chordListElement, pitchSet, synthChordParameters);
		}
	}
	
	private void addChordNotes(NoteTrack track, BeatListElement beatListElement, double time,
			ChordListElement chordListElement, PitchSet pitchSet, SynthChordParameters synthChordParameters) {
		NoteListElement lastNote = track.getLastNote();
		if (synthChordParameters.chordPattern > 0
			&& (beatListElement == null 
				|| beatListElement.amplitude <= ToneTimeFrame.AMPLITUDE_FLOOR 
				|| lastNote != null && beatListElement.getStartTime() * 1000 < lastNote.endTime)) {
			return;
		}
		
		NoteListElement[] currentNotes = track.getCurrentNotes(time * 1000);
		Set<Integer> currentNoteSet = new HashSet<>();
		for (NoteListElement nle: currentNotes) {
			currentNoteSet.add(nle.note);
		}
		Set<NoteListElement> newNotes = new HashSet<>();
		Set<Integer> newNoteSet = new HashSet<>();
		
		boolean isBar = track.getSize() % synthChordParameters.chordTimeSignature == 0;
		int barNote = track.getSize() % synthChordParameters.chordTimeSignature + 1;
		int barCount = track.getSize() / synthChordParameters.chordTimeSignature;

		int note = 0;
		int octave = 0;
		double startTime = beatListElement != null ? beatListElement.getStartTime() * 1000: time * 1000;
		double endTime = startTime;
		endTime += beatListElement != null && beatListElement.getTimeRange() > 0 ? beatListElement.getTimeRange() * 1000 * synthChordParameters.chordMeasure
				: synthChordParameters.chordMeasure * 200;
		double amplitude = 1.0;

		List<Double> camps = new ArrayList<>();
		List<Integer> cnotes = new ArrayList<>();

		ChordNote[] chordNotes = chordListElement.getChordNotes()
				.toArray(new ChordNote[chordListElement.getChordNotes().size()]);
		Arrays.sort(chordNotes, new Comparator<ChordNote>() {
			public int compare(ChordNote c1, ChordNote c2) {
				return Integer.valueOf(c1.getOctave()).compareTo(Integer.valueOf(c2.getOctave()));
			}
		});
		int rootNote = -1;
		int rootOctave = -1;
		double rootAmp = -1;
		for (ChordNote chordNote : chordNotes) {
			note = 0;
			amplitude = chordNote.getAmplitude();
			note = chordNote.getPitchClass();
			octave = chordNote.getOctave();
			if (rootNote < 0) {
				rootNote = note;
				rootAmp = amplitude;
				rootOctave = octave;
			}
			if (rootOctave == 0 && octave != 0) {
				rootOctave = octave;
			}
			if (rootOctave != 0 && octave > rootOctave) {
				octave = rootOctave + 1;
			}
			note += 12 * (synthChordParameters.chordOctave + (octave - rootOctave)); 
			camps.add(amplitude);
			cnotes.add(note);
			LOG.finer(">>NT ADDING chord note: " + time + ", " + currentNotes.length + ", " + octave + ", " + note + ", "
					+ rootOctave + ", " + rootNote);
			if (synthChordParameters.chordPattern == 0) {
				NoteListElement cnle = new NoteListElement(note, pitchSet.getIndex(note), startTime, endTime, 0, 0,
						amplitude, amplitude, amplitude, 0, false, incrementTime);
				newNotes.add(cnle);
				newNoteSet.add(cnle.note);
			}	
		}
		if (synthChordParameters.chordPattern > 0) {
			rootNote += 12 * synthChordParameters.chordOctave; 
			if (isBar) {
				note = rootNote;
				amplitude = rootAmp;
			} else {
				if (synthChordParameters.chordPattern == 1 || synthChordParameters.chordPattern == 2) {
					int noteIndex = 0;
					if (cnotes.size() > barNote) {
						noteIndex = barNote;
					} else {
						int r = (int) (Math.random() * (cnotes.size()));
						noteIndex = r;
					}
					note = cnotes.get(noteIndex);
					amplitude = camps.get(noteIndex);
				}
			}
	
			NoteListElement chordNote = new NoteListElement(note, pitchSet.getIndex(note), startTime, endTime, 0, 0,
					amplitude, amplitude, amplitude, 0, false, incrementTime);
			track.addNote(chordNote);
			LOG.finer(">>NT added chord arp note: " + time + ", " + chordNote.startTime + ", " + chordNote.endTime + ", " + note + ", "
					+ track.getSize() + ", " + synthChordParameters.chordTimeSignature + ", " + startTime + ", " + endTime);
		} else {
			if (!newNotes.stream().allMatch(nle -> currentNoteSet.contains(nle.note))) {
				for (NoteListElement cnle: currentNotes) {
					if (!newNoteSet.contains(cnle.note)) {
						cnle.endTime = startTime - incrementTime;
					} else {
						cnle.endTime = endTime;
					}
				}
				for (NoteListElement cnle: newNotes) {
					if (!currentNoteSet.contains(cnle.note)) {
						LOG.severe(">>NT added chord notes: " + time + ", " + currentNotes.length + ", " + cnle.startTime + ", " + cnle.endTime + ", " + note + ", "
								+ track.getSize() + ", " + synthChordParameters.chordTimeSignature + ", " + startTime + ", " + endTime+ ", " +cnle);
						track.addNote(cnle);
					}	
				}
			}	
				
		}
	}

	public void trackBeats(ToneTimeFrame toneTimeFrame) {
		processBeatTrack(1, toneTimeFrame, new SynthBeatParameters(synthBeat1Source, synthBeat1Drum, synthBeat1Offset, beat1TimeSignature));
		processBeatTrack(2, toneTimeFrame, new SynthBeatParameters(synthBeat2Source, synthBeat2Drum, synthBeat2Offset, beat2TimeSignature));
	}

	private void processBeatTrack(int trackNumber, ToneTimeFrame toneTimeFrame, SynthBeatParameters synthBeatParameters) {
		NoteTrack beatTrack;
		if (!beatTracks.containsKey(trackNumber)) {
			beatTrack = new NoteTrack(trackNumber);
			beatTracks.put(trackNumber, beatTrack);
		} else {
			beatTrack = beatTracks.get(trackNumber);
		}
		
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		NoteListElement lastNote = beatTrack.getLastNote();
		
		if (synthBeatParameters.beatSource <= 1) {
			Optional<BeatListElement> beat = toneTimeFrame.getBeat(CellTypes.AUDIO_BEAT.name() + "_CALIBRATION");
			if (beat.isPresent()) {
				BeatListElement beatListElement = beat.get();
				if (beatListElement.getAmplitude() > 0.0001) {
					if (lastNote == null || beatListElement.getStartTime() * 1000 >= lastNote.endTime) {
						addBeatNote(beatTrack, beatListElement, pitchSet, synthBeatParameters);
					}
				}
			}
		} else if (synthBeatParameters.beatSource == 2) {
			Optional<BeatListElement> beat = toneTimeFrame.getBeat(CellTypes.AUDIO_ONSET.name());
			if (beat.isPresent()) {
				BeatListElement beatListElement = beat.get();
				if (beatListElement.getAmplitude() > 0.0001) {
					if (lastNote == null || beatListElement.getStartTime() * 1000 >= lastNote.endTime) {
						addBeatNote(beatTrack, beatListElement, pitchSet, synthBeatParameters);
					}
				}
			}
		}
	}

	private NoteListElement addBeatNote(NoteTrack track, BeatListElement beatListElement, PitchSet pitchSet, SynthBeatParameters synthBeatParameters) {
		if (beatListElement == null) {
			return null;
		}
		boolean isBar = (track.getSize() + synthBeatParameters.beatOffset) % synthBeatParameters.beatTimeSignature == 0;
		int barNote = (track.getSize() + synthBeatParameters.beatOffset) % synthBeatParameters.beatTimeSignature + 1;
		int barCount = (track.getSize() + synthBeatParameters.beatOffset) / synthBeatParameters.beatTimeSignature;

		int note = 0;
		double startTime = beatListElement.getStartTime() * 1000;
		double endTime = startTime
				+ (beatListElement.getTimeRange() > 0 ? beatListElement.getTimeRange() * 1000 / 2 : incrementTime);
		double amplitude = beatListElement.getAmplitude();

		if (isBar) {
			note = synthBeatParameters.beatDrum;
		} else {
			note = synthBeatParameters.beatDrum;
		}
		
		NoteListElement beatNote = new NoteListElement(note, pitchSet.getIndex(note), startTime, endTime, 0, 0,
				amplitude, amplitude, amplitude, 0, false, incrementTime);
		track.addNote(beatNote);
		LOG.finer(">>NT added beat note: " + beatNote.startTime + ", " + beatNote.endTime + ", " + note + ", "
				+ track.getSize() + ", " + beat1TimeSignature + ", " + startTime + ", " + endTime);
		return beatNote;
	}

	private NoteTrack getPendingOverlappingSalientTrack(NoteTrack[] candidateTracks, NoteListElement noteListElement) {
		NoteTrack pitchSalientTrack = null;
		int pitchProximity = Integer.MAX_VALUE;
		for (NoteTrack track : candidateTracks) {
			NoteListElement lastNote = track.getLastNote();
			if (noteListElement.note == lastNote.note) {
				return track;
			}
			LOG.finer(">>NoteTracker getPendingOverlappingSalientTrack: " + noteListElement + ", " + lastNote);
			LOG.finer(">>NoteTracker getPendingOverlappingSalientTrack: " + overlapSalientNoteRange + ", "
					+ overlapSalientTimeRange);
			if ((Math.abs(noteListElement.note - lastNote.note) <= overlapSalientNoteRange)
					&& (lastNote.endTime >= noteListElement.startTime) && (lastNote.endTime < noteListElement.endTime)
					&& ((lastNote.endTime - noteListElement.startTime) < overlapSalientTimeRange)) {
				if (pitchProximity > Math.abs(noteListElement.note - lastNote.note)) {
					pitchProximity = Math.abs(noteListElement.note - lastNote.note);
					LOG.finer(">>NoteTracker getPendingOverlappingSalientTrack pitchProximity: " + pitchProximity);
					pitchSalientTrack = track;
				}
			}
		}
		if (pitchSalientTrack != null) {
			LOG.finer(">>NoteTracker getPendingOverlappingSalientTrack: " + noteListElement.note + ", "
					+ noteListElement.startTime + ", " + pitchSalientTrack);
			return pitchSalientTrack;
		}
		return null;
	}

	public NoteTrack getTrack(NoteListElement noteListElement) {
		NoteTrack result = null;
		for (NoteTrack track : tracks.values()) {
			if (track.hasNote(noteListElement)) {
				return track;
			}
		}
		return result;
	}

	public NoteTrack[] getTracks() {
		return tracks.values().toArray(new NoteTrack[tracks.size()]);
	}

	public NoteTrack getTrack(int number) {
		for (NoteTrack track : tracks.values()) {
			if (track.number == number) {
				return track;
			}
		}
		return null;
	}

	private NoteTrack getSalientTrack(NoteTrack[] candidateTracks, NoteTrack pendingSalientTrack,
			NoteListElement noteListElement) {
		NoteTrack pitchSalientTrack = null;
		NoteTrack timeSalientTrack = null;
		int timeSalientTrackPitchProximity = Integer.MAX_VALUE;
		double pitchSalientTrackTimeProximity = Double.MAX_VALUE;
		int pitchProximity = Integer.MAX_VALUE;
		double timeProximity = Double.MAX_VALUE;
		for (NoteTrack track : candidateTracks) {
			NoteListElement lastNote = track.getLastNote();
			LOG.finer(">>NoteTracker getSalientTrack candidateTrack: " + track + ", " + candidateTracks.length + ", "
					+ lastNote);
			if (lastNote == null) {
				LOG.finer(">>NoteTracker getSalientTrack X");
				return track;
			}
			if (noteListElement.isContinuation && noteListElement.note == lastNote.note) {
				LOG.finer(">>NoteTracker getSalientTrack Y");
				return track;
			}
			if (Math.abs(noteListElement.note - lastNote.note) <= salientNoteRange
					&& Math.abs(noteListElement.note - lastNote.note) < pitchProximity) {
				pitchProximity = Math.abs(noteListElement.note - lastNote.note);
				pitchSalientTrack = track;
				pitchSalientTrackTimeProximity = noteListElement.startTime - lastNote.endTime;
				LOG.finer(">>NoteTracker getSalientTrack PITCH A: " + pitchProximity + ", " + pitchSalientTrack + ", "
						+ noteListElement + ", " + lastNote);
			}
			if (timeProximity > noteListElement.startTime - lastNote.endTime) {
				timeProximity = noteListElement.startTime - lastNote.endTime;
				timeSalientTrack = track;
				timeSalientTrackPitchProximity = Math.abs(noteListElement.note - lastNote.note);
				LOG.finer(">>NoteTracker getSalientTrack TIME B: " + timeProximity + ", " + timeSalientTrack + ", "
						+ noteListElement + ", " + lastNote);
			}
			// double timbreFactor = noteListElement.noteTimbre. - lastNote.endTime;
			// }
		}
		if (pendingSalientTrack != null) {
			if (pitchProximity > 0 || timeProximity > 0) {
				LOG.finer(">>NoteTracker use pendingSalientTrack: " + timeProximity + ", " + timeSalientTrack + ", "
						+ noteListElement);
				return pendingSalientTrack;
			}
		}

		if (pitchSalientTrack != null || timeSalientTrack != null) {
			if (pitchSalientTrack == timeSalientTrack) {
				return pitchSalientTrack;
			}
			if (timeSalientTrack == null) {
				return pitchSalientTrack;
			}
			if (pitchSalientTrack == null) {
				return timeSalientTrack;
			}
			if (Math.abs(noteListElement.note - timeSalientTrack.getLastNote().note) > salientNoteRange) {
				return pitchSalientTrack;
			}
			if (pitchSalientTrackTimeProximity > salientTimeRange
					&& timeSalientTrackPitchProximity <= salientTimeNoteFactor) {
				return timeSalientTrack;
			}
			if (pitchSalientTrackTimeProximity <= salientTimeRange
					&& timeSalientTrackPitchProximity > salientTimeNoteFactor) {
				return timeSalientTrack;
			}
			if (Math.abs(noteListElement.note - timeSalientTrack.getLastNote().note) > salientTimeNoteFactor
					* Math.abs(noteListElement.note - pitchSalientTrack.getLastNote().note)) {
				return pitchSalientTrack;
			}
			return timeSalientTrack;
		}
		return null;
	}

	private NoteTrack getPendingSalientTrack(NoteTrack[] candidateTracks, NoteListElement noteListElement) {

		NoteTrack salientTrack = null;
		for (NoteTrack track : candidateTracks) {
			NoteListElement lastNote = track.getLastNote();
			NoteListElement penultimateNote = track.getPenultimateNote();
			if (penultimateNote != null && Math.abs(penultimateNote.note - noteListElement.note) <= salientNoteRange) {
				if (compareSalience(noteListElement, lastNote, penultimateNote)) {
					salientTrack = track;
				}
			}
		}
		return salientTrack;
	}

	private boolean compareSalience(NoteListElement newNote, NoteListElement lastNote,
			NoteListElement penultimateNote) {
		int pitchProximity = Integer.MAX_VALUE;
		double timeProximity = Double.MAX_VALUE;
		pitchProximity = Math.abs(newNote.note - penultimateNote.note);
		timeProximity = newNote.startTime - penultimateNote.endTime;
		if (pitchProximity == Math.abs(lastNote.note - penultimateNote.note)
				&& (timeProximity == (lastNote.startTime - penultimateNote.endTime))) {
			return false;
		}
		if (pitchProximity > salientNoteRange || timeProximity > salientTimeRange) {
			return false;
		}
		if (pitchProximity == Math.abs(lastNote.note - penultimateNote.note)) {
			if (timeProximity < (lastNote.startTime - penultimateNote.endTime)) {
				return true;
			} else {
				return false;
			}
		}
		if (timeProximity == (lastNote.startTime - penultimateNote.endTime)) {
			if (pitchProximity < Math.abs(lastNote.note - penultimateNote.note)) {
				return true;
			} else {
				return false;
			}
		}
		if (pitchProximity > Math.abs(lastNote.note - penultimateNote.note)) {
			if (timeProximity < 0.5 * (lastNote.startTime - penultimateNote.endTime)) {
				return true;
			} else {
				return false;
			}
		} else {
			if (timeProximity > 0.5 * (lastNote.startTime - penultimateNote.endTime)) {
				return false;
			} else {
				return true;
			}
		}
	}

	private NoteTrack[] getPendingTracks(NoteListElement noteListElement) {
		List<NoteTrack> result = new ArrayList<>();
		for (NoteTrack track : tracks.values()) {
			NoteListElement lastNote = track.getLastNote();
			if (lastNote != null && (lastNote.endTime >= noteListElement.startTime)) {
				result.add(track);
			}
		}
		return result.toArray(new NoteTrack[result.size()]);
	}

	private NoteTrack[] getNonPendingTracks(NoteListElement noteListElement) {
		List<NoteTrack> result = new ArrayList<>();
		for (NoteTrack track : tracks.values()) {
			NoteListElement lastNote = track.getLastNote();
			if (lastNote != null && (lastNote.endTime < noteListElement.startTime)) {
				LOG.finer(">>NoteTracker getNonPendingTracks ADD: " + track);
				result.add(track);
			}
		}
		LOG.finer(">>NoteTracker getNonPendingTracks size: " + result.size());
		return result.toArray(new NoteTrack[result.size()]);
	}

	private NoteTrack createTrack() {
		for (int i = 1; i < maxTracksUpper + maxTracksLower; i++) {
			if (!tracks.containsKey(i)) {
				NoteTrack track = new NoteTrack(i);
				tracks.put(track.getNumber(), track);
				LOG.finer(">>NoteTracker createTrack: " + track.getNumber());
				return track;
			}
		}
		LOG.finer(">>NoteTracker createTrack NULL!!");
		return null;
	}

	public Set<NoteListElement> cleanTracks(double fromTime) {
		double lastTime = Double.NEGATIVE_INFINITY;
		Set<NoteListElement> discardedNotes = new HashSet<>();
		Set<NoteTrack> discardedTracks = new HashSet<>();
		if (tracks.size() > maxTracksLower) {
			for (NoteTrack track : tracks.values()) {
				boolean hasDiscarded = false;
				do {
					List<NoteListElement> notes = track.getNotes();
					Set<NoteListElement> notesToDelete = new HashSet<>();
					NoteListElement lastNote = null;
					hasDiscarded = false;
					for (NoteListElement nle : notes) {
						if (lastNote != null) {
							lastTime = lastNote.endTime;
						}
						if ((nle.endTime - nle.startTime) < clearRangeLower
								&& (nle.startTime - lastTime) > clearRangeUpper
								&& (fromTime - nle.endTime) > clearRangeUpper) {
							discardedNotes.add(nle);
							notesToDelete.add(nle);
							hasDiscarded = true;
							LOG.finer(">>NoteTracker cleanTracks note A: " + nle);
							if (track.getNotes().size() == notesToDelete.size()) {
								break;
							}
						}
						if (lastNote != null
								&& ((nle.startTime == lastNote.startTime) || (nle.endTime == lastNote.endTime))) {
							if ((nle.startTime == lastNote.startTime) && (nle.endTime == lastNote.endTime)) {
								if (nle.maxAmp > lastNote.maxAmp) {
									discardedNotes.add(lastNote);
									notesToDelete.add(lastNote);
									hasDiscarded = true;
									LOG.finer(">>NoteTracker cleanTracks note B: " + nle);
									if (track.getNotes().size() == notesToDelete.size()) {
										break;
									}
								} else {
									discardedNotes.add(nle);
									notesToDelete.add(nle);
									hasDiscarded = true;
									LOG.finer(">>NoteTracker cleanTracks note C: " + nle);
									if (track.getNotes().size() == notesToDelete.size()) {
										break;
									}
								}
							} else {
								if ((nle.endTime - nle.startTime) > (lastNote.endTime - lastNote.startTime)) {
									discardedNotes.add(lastNote);
									notesToDelete.add(lastNote);
									hasDiscarded = true;
									LOG.finer(">>NoteTracker cleanTracks note B: " + nle);
									if (track.getNotes().size() == notesToDelete.size()) {
										break;
									}
								} else {
									discardedNotes.add(nle);
									notesToDelete.add(nle);
									hasDiscarded = true;
									LOG.finer(">>NoteTracker cleanTracks note C: " + nle);
									if (track.getNotes().size() == notesToDelete.size()) {
										break;
									}
								}
							}
						}
						lastNote = nle;
					}
					for (NoteListElement nle : notesToDelete) {
						LOG.finer(">>NT clean remove: " + track.getNumber() + ", " + fromTime + ", " + nle.startTime);
						track.removeNote(nle);
						if (track.getNotes().size() == 0) {
							LOG.finer(">>NoteTracker cleanTracks track: " + track);
							discardedTracks.add(track);
						}
					}
				} while (hasDiscarded && track.getNotes().size() > 0);
			}
		}
		for (NoteTrack track : discardedTracks) {
			removeTrack(track);
		}
		return discardedNotes;
	}

	private void removeTrack(NoteTrack track) {
		LOG.finer(">>NoteTracker removeTrack: " + track.getNumber());
		tracks.remove(track.getNumber());
	}

	public NoteTrack getBaseTrack() {
		return baseTrack;
	}

	public NoteTrack getBeatTrack(int trackNumber) {
		return beatTracks.get(trackNumber);
	}

	public NoteTrack getChordTrack(int trackNumber) {
		return chordTracks.get(trackNumber);
	}

	public ChordListElement getChord(double startTime, double endTime) {
		Set<ChordNote> notes = new HashSet<>();
		for (NoteTrack track : tracks.values()) {
			NoteListElement nle = track.getNote(startTime * 1000);
			if (nle != null) {
				int pitchClass = (int) nle.note % 12;
				ChordNote cn = new ChordNote(pitchClass, pitchClass, nle.maxAmp, nle.note / 12);
				notes.add(cn);
			}
		}
		if (notes.size() > 0) {
			ChordListElement cle = new ChordListElement(startTime, endTime, notes.toArray(new ChordNote[notes.size()]));
			return cle;
		} else {
			return null;
		}

	}

}
