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

	record SynthChordParameters(int quantizeSource, int chordSource, double chordMeasure, int chordPattern,
			int chordOctave, int chordOffset, boolean chordInvert) {
	};

	record SynthBeatParameters(int beatSource, int beatDrum, int beatOffset, int beatPattern) {
	};

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

	private double synthBaseMeasure;

	private int synthTimeSignature;

	private int synthBaseOctave;

	private int incrementTime;

	private int synthBeat1Offset;

	private int synthBeat1Pattern;

	private int synthBeat1Source;

	private int synthBeat2Offset;

	private int synthBeat2Pattern;

	private int synthBeat2Source;

	private int synthBeat1Drum;

	private int synthBeat2Drum;

	private int synthBeat3Drum;

	private int synthBeat4Drum;

	private double synthChord1Measure;

	private int synthChord1Offset;

	private int synthChord1Pattern;

	private int synthChord1Octave;

	private double synthChord2Measure;

	private int synthChord2Offset;

	private int synthChord2Pattern;

	private int synthChord2Octave;

	private int synthChord1Source;

	private int synthChord1QuantizeSource;

	private boolean synthChord1Invert;

	private boolean synthChord2Invert;

	private int synthChord2Source;

	private int synthChord2QuantizeSource;

	private double synthChord3Measure;

	private int synthChord3Offset;

	private int synthChord3Pattern;

	private int synthChord3Source;

	private int synthChord3QuantizeSource;

	private boolean synthChord3Invert;

	private int synthChord3Octave;

	private double synthChord4Measure;

	private int synthChord4Offset;

	private int synthChord4Pattern;

	private int synthChord4Source;

	private int synthChord4QuantizeSource;

	private boolean synthChord4Invert;

	private int synthChord4Octave;

	private int synthBeat3Offset;

	private int synthBeat3Pattern;

	private int synthBeat3Source;

	private int synthBeat4Offset;

	private int synthBeat4Pattern;

	private int synthBeat4Source;

	private int synthBaseSource;

	private int synthBaseQuantizeSource;

	private int synthQuantizeSource;

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
				LOG.finer(
						">>NoteTracker getEndNotes: " + time + ", " + note.note + ", "
								+ (note.endTime + note.incrementTime)
								+ ", " + number + ", " + note.startTime + " ," + note.endTime + " ,"
								+ note.incrementTime);
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
				if ((nle.startTime <= startTime
						&& Math.floor(nle.endTime + nle.incrementTime) >= Math.floor(endTime))
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
		synthTimeSignature = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_TIME_SIGNATURE);
		synthBaseMeasure = toneMap.getParameterManager()
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_MEASURE);
		synthBasePattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_PATTERN);
		synthBaseOctave = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_OCTAVE);
		synthBaseSource = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_SOURCE);
		synthBaseQuantizeSource = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_QUANTIZE_SOURCE);

		synthChord1Measure = toneMap.getParameterManager()
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_MEASURE);
		synthChord1Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_OFFSET);
		synthChord1Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_PATTERN);
		synthChord1Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_SOURCE);
		synthChord1Invert = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_INVERT);
		synthChord1Octave = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_OCTAVE);
		synthChord1QuantizeSource = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_QUANTIZE_SOURCE);

		synthChord2Measure = toneMap.getParameterManager()
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_MEASURE);
		synthChord2Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_OFFSET);
		synthChord2Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_PATTERN);
		synthChord2Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_SOURCE);
		synthChord2Invert = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_INVERT);
		synthChord2Octave = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_OCTAVE);
		synthChord2QuantizeSource = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_QUANTIZE_SOURCE);

		synthChord3Measure = toneMap.getParameterManager()
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_MEASURE);
		synthChord3Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_OFFSET);
		synthChord3Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_PATTERN);
		synthChord3Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_SOURCE);
		synthChord3Invert = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_INVERT);
		synthChord3Octave = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_OCTAVE);
		synthChord3QuantizeSource = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_QUANTIZE_SOURCE);

		synthChord4Measure = toneMap.getParameterManager()
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_MEASURE);
		synthChord4Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_OFFSET);
		synthChord4Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_PATTERN);
		synthChord4Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_SOURCE);
		synthChord4Invert = toneMap.getParameterManager()
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_INVERT);
		synthChord4Octave = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_OCTAVE);
		synthChord4QuantizeSource = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_QUANTIZE_SOURCE);

		synthBeat1Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_OFFSET);
		synthBeat1Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_PATTERN);
		synthBeat1Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_SOURCE);

		synthBeat2Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_OFFSET);
		synthBeat2Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_PATTERN);
		synthBeat2Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_SOURCE);

		synthBeat3Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_OFFSET);
		synthBeat3Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_PATTERN);
		synthBeat3Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_SOURCE);

		synthBeat4Offset = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_OFFSET);
		synthBeat4Pattern = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_PATTERN);
		synthBeat4Source = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_SOURCE);

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

		synthQuantizeSource = toneMap.getParameterManager()
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_SOURCE);
	}

	public NoteTrack trackNote(NoteListElement noteListElement, Set<NoteListElement> discardedNotes, int retryCount) {
		LOG.finer(">>NoteTracker trackNote: " + noteListElement.note + ", " + noteListElement);
		int currentRetryCount = retryCount;
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
						LOG.finer(
								">>NoteTracker addLegato for nle B: "
										+ pendingOverlappingSalientTrack.getLastNote().note
										+ ", " + noteListElement + ", " + pendingOverlappingSalientTrack.getLastNote());
						pendingOverlappingSalientTrack.getLastNote()
								.addLegato(noteListElement);
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
		LOG.finer(">>NoteTracker addNote: " + salientTrack.getNumber() + ", " + noteListElement.note + ", "
				+ salientTrack + ", " + noteListElement.startTime + " ," + noteListElement.endTime);

		for (NoteTrack track : tracks.values()) {
			if (track.isEmpty()) {
				removeTrack(track);
			}
		}

		LOG.finer(">>NoteTracker trackNote ADDED note: " + noteListElement.note + ", " + salientTrack.number);
		if (disconnectedNote != null && currentRetryCount <= maxTracksLower) {
			if (currentRetryCount <= maxTracksLower) {
				LOG.severe(
						">>NoteTracker trackNote disconnected note: " + disconnectedNote + ", " + noteListElement.note);
				currentRetryCount++;
				trackNote(disconnectedNote, discardedNotes, currentRetryCount);
			} else {
				discardedNotes.add(disconnectedNote);
			}
		}
		return salientTrack;
	}

	public NoteListElement trackBase(NoteListElement synthQuantizeNote, ChordListElement chordListElement,
			ToneTimeFrame toneTimeFrame) {
		if (baseTrack == null) {
			baseTrack = new NoteTrack(1);
		}
		NoteListElement quantizeNote = synthQuantizeNote;
		if (synthBaseQuantizeSource != synthQuantizeSource) {
			NoteTrack quantizeBeatTrack = toneMap.getNoteTracker()
					.getBeatTrack(synthBaseQuantizeSource);
			quantizeNote = quantizeBeatTrack.getLastNote();
			if (quantizeNote == null) {
				quantizeNote = synthQuantizeNote;
			}
		}
		if (quantizeNote == null) {
			return null;
		}
		NoteListElement lastNote = baseTrack.getLastNote();
		if (quantizeNote == null || lastNote == null || quantizeNote.startTime >= lastNote.endTime) {
			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			if (synthBaseSource == 0) {
				ChordListElement cle = toneTimeFrame.getChord();
				if (cle != null) {
					return addBaseNote(baseTrack, quantizeNote, cle, pitchSet);
				}
			} else if (synthBaseSource == 1) {
				Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_POST_CHROMA.name());
				if (oc.isPresent()) {
					return addBaseNote(baseTrack, quantizeNote, oc.get(), pitchSet);
				}
			} else if (synthBaseSource == 2) {
				Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_ONSET.name() + "_SMOOTHED");
				if (oc.isPresent()) {
					return addBaseNote(baseTrack, quantizeNote, oc.get(), pitchSet);
				}
			} else if (synthBaseSource == 3) {
				Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_HPS.name() + "_HARMONIC");
				if (oc.isPresent()) {
					return addBaseNote(baseTrack, quantizeNote, oc.get(), pitchSet);
				}
			} else if (synthBaseSource == 4) {
				Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_PRE_CHROMA.name());
				if (oc.isPresent()) {
					return addBaseNote(baseTrack, quantizeNote, oc.get(), pitchSet);
				}
			} else if (synthBaseSource == 5) {
				return addBaseNote(baseTrack, quantizeNote, chordListElement, pitchSet);
			} else if (synthBaseSource == 6) {
				NoteTrack[] tracks = getTracks();
				Set<NoteListElement> nles = new HashSet<>();
				double startTime = Double.MAX_VALUE;
				double endTime = Double.MIN_VALUE;
				for (NoteTrack track : tracks) {
					NoteListElement note = track.getLastNote();
					if (note != null) {
						nles.add(note);
						if (note.startTime < startTime) {
							startTime = note.startTime;
						}
						if (note.endTime > endTime) {
							endTime = note.endTime;
						}
					}
				}

				if (nles.size() > 0) {
					Set<ChordNote> cns = new HashSet<>();
					for (NoteListElement nle : nles) {
						int pitchClass = (int) nle.note % 12;
						ChordNote cn = new ChordNote(pitchClass, pitchClass, nle.maxAmp, nle.note / 12, 0);
						cns.add(cn);
					}
					ChordListElement cle = new ChordListElement(startTime, endTime,
							cns.toArray(new ChordNote[cns.size()]));
					return addBaseNote(baseTrack, quantizeNote, cle, pitchSet);

				}
			}
		}
		return null;
	}

	private NoteListElement addBaseNote(NoteTrack track, NoteListElement quantizeNote,
			ChordListElement chordListElement, PitchSet pitchSet) {
		boolean isBar = track.getSize() % synthTimeSignature == 0;
		int barNote = track.getSize() % synthTimeSignature + 1;
		NoteListElement lastNote = track.getLastNote();

		int note = 0;
		double startTime = quantizeNote == null ? 0 : quantizeNote.startTime;
		double endTime = startTime;
		double range = endTime - startTime;
		endTime += range > 0 ? range * synthBaseMeasure : synthBaseMeasure * 200;
		double amplitude = 1.0;

		List<Double> camps = new ArrayList<>();
		List<Integer> cnotes = new ArrayList<>();

		ChordNote[] chordNotes = chordListElement.getChordNotes()
				.toArray(new ChordNote[chordListElement.getChordNotes()
						.size()]);
		Arrays.sort(chordNotes, new Comparator<ChordNote>() {
			public int compare(ChordNote c1, ChordNote c2) {
				return Double.valueOf(c2.getAmplitude())
						.compareTo(Double.valueOf(c1.getAmplitude()));
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

		if (synthBasePattern <= 1) {
			note = rootNote;
			amplitude = rootAmp;
		} else if (synthBasePattern == 2) {
			if (isBar) {
				note = rootNote;
				amplitude = rootAmp;
			} else {
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
		} else if (synthBasePattern == 3) {
			if (isBar) {
				if (lastNote != null && lastNote.note > rootNote) {
					note = cnotes.get(cnotes.size() - 1);
					amplitude = camps.get(cnotes.size() - 1);
				} else {
					note = rootNote;
					amplitude = rootAmp;
				}
			} else {
				int noteIndex = 0;
				NoteListElement penultimateNote = track.getPenultimateNote();
				if (lastNote != null && penultimateNote != null && penultimateNote.note > lastNote.note) {
					for (int i = 1; i < cnotes.size(); i++) {
						if (cnotes.get(i) == lastNote.note) {
							noteIndex = i - 1;
							break;
						}
					}
				} else {
					for (int i = 0; i < cnotes.size() - 1; i++) {
						if (cnotes.get(i) == lastNote.note) {
							noteIndex = i + 1;
							break;
						}
					}
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

	public void trackChords(NoteListElement quantizeNote, ChordListElement chordListElement,
			ToneTimeFrame toneTimeFrame) {
		processChordTrack(1, toneTimeFrame, quantizeNote, chordListElement,
				new SynthChordParameters(synthChord1QuantizeSource, synthChord1Source, synthChord1Measure,
						synthChord1Pattern, synthChord1Octave, synthChord1Offset, synthChord1Invert));
		processChordTrack(2, toneTimeFrame, quantizeNote, chordListElement,
				new SynthChordParameters(synthChord2QuantizeSource, synthChord2Source, synthChord2Measure,
						synthChord2Pattern, synthChord2Octave, synthChord2Offset, synthChord2Invert));
		processChordTrack(3, toneTimeFrame, quantizeNote, chordListElement,
				new SynthChordParameters(synthChord3QuantizeSource, synthChord3Source, synthChord3Measure,
						synthChord3Pattern, synthChord3Octave, synthChord3Offset, synthChord3Invert));
		processChordTrack(4, toneTimeFrame, quantizeNote, chordListElement,
				new SynthChordParameters(synthChord4QuantizeSource, synthChord4Source, synthChord4Measure,
						synthChord4Pattern, synthChord4Octave, synthChord4Offset, synthChord4Invert));
	}

	private void processChordTrack(int trackNumber, ToneTimeFrame toneTimeFrame, NoteListElement synthQuantizeNote,
			ChordListElement chordListElement, SynthChordParameters synthChordParameters) {
		NoteListElement quantizeNote = synthQuantizeNote;
		if (synthChordParameters.quantizeSource != synthQuantizeSource) {
			NoteTrack quantizeBeatTrack = toneMap.getNoteTracker()
					.getBeatTrack(synthChordParameters.quantizeSource);
			quantizeNote = quantizeBeatTrack.getLastNote();
			if (quantizeNote == null) {
				quantizeNote = synthQuantizeNote;
			}
		}
		if (quantizeNote == null) {
			return;
		}
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
				addChordNotes(chordTrack, quantizeNote, toneTimeFrame.getStartTime(), cle, pitchSet,
						synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 1) {
			Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_POST_CHROMA.name());
			if (oc.isPresent()) {
				addChordNotes(chordTrack, quantizeNote, toneTimeFrame.getStartTime(), oc.get(), pitchSet,
						synthChordParameters);
				LOG.finer(">>SYN Chord: " + toneTimeFrame.getStartTime() + " ," + oc.get());
			}
		} else if (synthChordParameters.chordSource == 2) {
			Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_ONSET.name() + "_SMOOTHED");
			if (oc.isPresent()) {
				addChordNotes(chordTrack, quantizeNote, toneTimeFrame.getStartTime(), oc.get(), pitchSet,
						synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 3) {
			Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_HPS.name() + "_HARMONIC");
			if (oc.isPresent()) {
				addChordNotes(chordTrack, quantizeNote, toneTimeFrame.getStartTime(), oc.get(), pitchSet,
						synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 4) {
			Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_PRE_CHROMA.name());
			if (oc.isPresent()) {
				addChordNotes(chordTrack, quantizeNote, toneTimeFrame.getStartTime(), oc.get(), pitchSet,
						synthChordParameters);
			}
		} else if (synthChordParameters.chordSource == 5) {
			addChordNotes(chordTrack, quantizeNote, toneTimeFrame.getStartTime(), chordListElement, pitchSet,
					synthChordParameters);
		}
	}

	private void addChordNotes(NoteTrack track, NoteListElement quantizeNote, double time,
			ChordListElement chordListElement, PitchSet pitchSet, SynthChordParameters synthChordParameters) {
		NoteListElement lastNote = track.getLastNote();
		if (synthChordParameters.chordPattern > 0 && (lastNote != null && quantizeNote.startTime < lastNote.endTime)) {
			return;
		}

		NoteListElement[] currentNotes = track.getCurrentNotes(time * 1000);
		Set<Integer> currentNoteSet = new HashSet<>();
		for (NoteListElement nle : currentNotes) {
			currentNoteSet.add(nle.note);
		}
		Set<NoteListElement> newNotes = new HashSet<>();
		Set<Integer> newNoteSet = new HashSet<>();

		boolean isBar = track.getSize() % synthTimeSignature == 0;
		int barNote = track.getSize() % synthTimeSignature + 1;

		int note = 0;
		int octave = 0;
		double startTime = quantizeNote == null ? 0 : quantizeNote.startTime;
		double endTime = startTime;
		double range = quantizeNote.endTime - quantizeNote.startTime;
		endTime += range > 0 ? range * synthChordParameters.chordMeasure : synthChordParameters.chordMeasure * 1000;

		double amplitude = 1.0;

		List<Double> camps = new ArrayList<>();
		List<Integer> cnotes = new ArrayList<>();

		ChordNote[] chordNotes = chordListElement.getChordNotes()
				.toArray(new ChordNote[chordListElement.getChordNotes()
						.size()]);
		Arrays.sort(chordNotes, new Comparator<ChordNote>() {
			public int compare(ChordNote c1, ChordNote c2) {
				return Integer.valueOf(c1.getOctave())
						.compareTo(Integer.valueOf(c2.getOctave()));
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
			// if (rootOctave != 0 && octave > rootOctave) {
			// octave = rootOctave + 1;
			// }
			// if (synthChordParameters.chordPattern > 0) {
			// note += 12 * (synthChordParameters.chordOctave + (octave - rootOctave));
			// } else {
			// note += 12 * (synthChordParameters.chordOctave + (octave - rootOctave));//
			// octave;
			// }
			if (synthChordParameters.chordInvert) {
				note += 12 * (octave + synthChordParameters.chordOctave);
			} else {
				note += 12 * synthChordParameters.chordOctave;
			}
			camps.add(amplitude);
			cnotes.add(note);
			if (synthChordParameters.chordPattern == 0 || synthChordParameters.chordPattern == 1) {
				NoteListElement cnle = new NoteListElement(note, pitchSet.getIndex(note), startTime, endTime, 0, 0,
						amplitude, amplitude, amplitude, 0, false, incrementTime);
				newNotes.add(cnle);
				newNoteSet.add(cnle.note);
			}
		}
		if (synthChordParameters.chordPattern == 0) {
			if (!newNotes.stream()
					.allMatch(nle -> currentNoteSet.contains(nle.note))) {
				for (NoteListElement cnle : currentNotes) {
					if (cnle.endTime + incrementTime >= startTime) {
						cnle.endTime = startTime - incrementTime;
					}
				}
				for (NoteListElement nnle : newNotes) {
					track.addNote(nnle);
				}
			} else {
				for (NoteListElement nnle : newNotes) {
					for (NoteListElement cnle : currentNotes) {
						if (nnle.note == cnle.note && (cnle.endTime + incrementTime >= nnle.startTime)) {
							cnle.endTime = startTime - incrementTime;
						}
					}
					track.addNote(nnle);
				}
			}
		} else if (synthChordParameters.chordPattern == 1) {
			for (NoteListElement nnle : newNotes) {
				if (!currentNoteSet.contains(nnle.note)) {
					track.addNote(nnle);
				} else {
					for (NoteListElement cnle : currentNotes) {
						if (nnle.note == cnle.note && (cnle.endTime + incrementTime >= nnle.startTime)
								&& (cnle.endTime < nnle.endTime)) {
							cnle.endTime = nnle.endTime;
						}
					}
				}
			}
		} else {
			lastNote = track.getLastNote();
			if (synthChordParameters.chordInvert) {
				rootNote += 12 * (rootOctave + synthChordParameters.chordOctave);
			} else {
				rootNote += 12 * synthChordParameters.chordOctave;
			}
			if (synthChordParameters.chordPattern == 2) {
				if (isBar) {
					note = rootNote;
					amplitude = rootAmp;
				} else {
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
			} else if (synthChordParameters.chordPattern == 3) {
				if (isBar) {
					if (lastNote != null && lastNote.note > rootNote) {
						note = cnotes.get(cnotes.size() - 1);
						amplitude = camps.get(cnotes.size() - 1);
					} else {
						note = rootNote;
						amplitude = rootAmp;
					}
				} else {
					int noteIndex = 0;
					NoteListElement penultimateNote = track.getPenultimateNote();
					if (lastNote != null && penultimateNote != null && penultimateNote.note > lastNote.note) {
						for (int i = 1; i < cnotes.size(); i++) {
							if (cnotes.get(i) == lastNote.note) {
								noteIndex = i - 1;
								break;
							}
						}
					} else {
						for (int i = 0; i < cnotes.size() - 1; i++) {
							if (cnotes.get(i) == lastNote.note) {
								noteIndex = i + 1;
								break;
							}
						}
					}
					note = cnotes.get(noteIndex);
					amplitude = camps.get(noteIndex);
				}
			}

			NoteListElement chordNote = new NoteListElement(note, pitchSet.getIndex(note), startTime, endTime, 0, 0,
					amplitude, amplitude, amplitude, 0, false, incrementTime);
			track.addNote(chordNote);
			LOG.finer(
					">>NT added chord arp note: " + time + ", " + chordNote.startTime + ", " + chordNote.endTime + ", "
							+ note + ", " + track.getSize() + ", " + synthTimeSignature + ", " + startTime + ", "
							+ endTime);
		}
	}

	public void trackBeats(ToneTimeFrame toneTimeFrame) {
		processBeatTrack(1, toneTimeFrame,
				new SynthBeatParameters(synthBeat1Source, synthBeat1Drum, synthBeat1Offset, synthBeat1Pattern));
		processBeatTrack(2, toneTimeFrame,
				new SynthBeatParameters(synthBeat2Source, synthBeat2Drum, synthBeat2Offset, synthBeat2Pattern));
		processBeatTrack(3, toneTimeFrame,
				new SynthBeatParameters(synthBeat3Source, synthBeat3Drum, synthBeat3Offset, synthBeat3Pattern));
		processBeatTrack(4, toneTimeFrame,
				new SynthBeatParameters(synthBeat4Source, synthBeat4Drum, synthBeat4Offset, synthBeat4Pattern));
	}

	private void processBeatTrack(int trackNumber, ToneTimeFrame toneTimeFrame,
			SynthBeatParameters synthBeatParameters) {
		NoteTrack beatTrack;
		if (!beatTracks.containsKey(trackNumber)) {
			beatTrack = new NoteTrack(trackNumber);
			beatTracks.put(trackNumber, beatTrack);
		} else {
			beatTrack = beatTracks.get(trackNumber);
		}

		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		NoteListElement lastNote = beatTrack.getLastNote();

		if (synthBeatParameters.beatSource == 0) {
			BeatListElement beatListElement = toneTimeFrame.getBeat();
			if (beatListElement != null && beatListElement.getAmplitude() > 0.0001) {
				if (lastNote == null || beatListElement.getStartTime() * 1000 >= lastNote.endTime) {
					addBeatNote(beatTrack, beatListElement, pitchSet, synthBeatParameters);
				}

			}
		} else if (synthBeatParameters.beatSource == 1) {
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
		} else if (synthBeatParameters.beatSource == 3) {
			Optional<BeatListElement> beat = toneTimeFrame.getBeat(CellTypes.AUDIO_ONSET.name() + "_PEAKS");
			if (beat.isPresent()) {
				BeatListElement beatListElement = beat.get();
				if (beatListElement.getAmplitude() > 0.0001) {
					if (lastNote == null || beatListElement.getStartTime() * 1000 >= lastNote.endTime) {
						addBeatNote(beatTrack, beatListElement, pitchSet, synthBeatParameters);
					}
				}
			}
		} else if (synthBeatParameters.beatSource == 4) {
			Optional<BeatListElement> beat = toneTimeFrame.getBeat(CellTypes.AUDIO_PERCUSSION.name());
			if (beat.isPresent()) {
				BeatListElement beatListElement = beat.get();
				if (beatListElement.getAmplitude() > 0.0001) {
					if (lastNote == null || beatListElement.getStartTime() * 1000 >= lastNote.endTime) {
						addBeatNote(beatTrack, beatListElement, pitchSet, synthBeatParameters);
					}
				}
			}
		} else if (synthBeatParameters.beatSource == 5) {
			Optional<BeatListElement> beat = toneTimeFrame.getBeat(CellTypes.AUDIO_HPS.name() + "_PERCUSSION");
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

	private NoteListElement addBeatNote(NoteTrack track, BeatListElement beatListElement, PitchSet pitchSet,
			SynthBeatParameters synthBeatParameters) {
		if (beatListElement == null) {
			return null;
		}
		boolean isBar = (track.getSize() + synthBeatParameters.beatOffset) % synthTimeSignature == 0;
		int barNote = (track.getSize() + synthBeatParameters.beatOffset) % synthTimeSignature + 1;
		int barCount = (track.getSize() + synthBeatParameters.beatOffset) / synthTimeSignature;

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

		if (synthBeatParameters.beatPattern == 0) {
			if (isBar) {
				note = synthBeatParameters.beatDrum;
			} else {
				note = synthBeatParameters.beatDrum;
			}
		} else if (synthBeatParameters.beatPattern == 1) {
			if (isBar) {
				note = synthBeatParameters.beatDrum;
			} else {
				return null;
			}
		} else if (synthBeatParameters.beatPattern == 2) {
			if (isBar) {
				return null;
			} else {
				note = synthBeatParameters.beatDrum;
			}
		} else if (synthBeatParameters.beatPattern == 3) {
			if (isBar) {
				note = synthBeatParameters.beatDrum;
			} else {
				note = synthBeatParameters.beatDrum;
				amplitude = amplitude / 2;
			}
		}

		NoteListElement beatNote = new NoteListElement(note, pitchSet.getIndex(note), startTime, endTime, 0, 0,
				amplitude, amplitude, amplitude, 0, false, incrementTime);
		track.addNote(beatNote);
		return beatNote;
	}

	private NoteTrack getPendingOverlappingSalientTrack(NoteTrack[] candidateTracks,
			NoteListElement noteListElement) {
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
		return tracks.values()
				.toArray(new NoteTrack[tracks.size()]);
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
		return null;
	}

	public Set<NoteListElement> cleanTracks(double fromTime) {
		double lastTime = Double.NEGATIVE_INFINITY;
		Set<NoteListElement> discardedNotes = new HashSet<>();
		Set<NoteTrack> discardedTracks = new HashSet<>();
		if (tracks.size() > 1) {
			LOG.severe(">>NT CLEAR: " + clearRangeLower + ", " + clearRangeUpper);
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
							LOG.severe(">>NoteTracker cleanTracks note A: " + nle);
							if (track.getNotes()
									.size() == notesToDelete.size()) {
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
									LOG.severe(">>NoteTracker cleanTracks note B: " + nle);
									if (track.getNotes()
											.size() == notesToDelete.size()) {
										break;
									}
								} else {
									discardedNotes.add(nle);
									notesToDelete.add(nle);
									hasDiscarded = true;
									LOG.severe(">>NoteTracker cleanTracks note C: " + nle);
									if (track.getNotes()
											.size() == notesToDelete.size()) {
										break;
									}
								}
							} else {
								if ((nle.endTime - nle.startTime) > (lastNote.endTime - lastNote.startTime)) {
									discardedNotes.add(lastNote);
									notesToDelete.add(lastNote);
									hasDiscarded = true;
									LOG.severe(">>NoteTracker cleanTracks note B: " + nle);
									if (track.getNotes()
											.size() == notesToDelete.size()) {
										break;
									}
								} else {
									discardedNotes.add(nle);
									notesToDelete.add(nle);
									hasDiscarded = true;
									LOG.severe(">>NoteTracker cleanTracks note C: " + nle);
									if (track.getNotes()
											.size() == notesToDelete.size()) {
										break;
									}
								}
							}
						}
						lastNote = nle;
					}
					for (NoteListElement nle : notesToDelete) {
						LOG.severe(
								">>NT clean remove: " + track.getNumber() + ", " + fromTime + ", " + nle.startTime);
						track.removeNote(nle);
						if (track.getNotes()
								.size() == 0) {
							LOG.severe(">>NoteTracker cleanTracks track: " + track);
							discardedTracks.add(track);
						}
					}
				} while (hasDiscarded && track.getNotes()
						.size() > 0);
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
				ChordNote cn = new ChordNote(pitchClass, pitchClass, nle.maxAmp, nle.note / 12, 0); // TODO
				notes.add(cn);
			}
		}
		if (notes.size() > 0) {
			ChordListElement cle = new ChordListElement(startTime, endTime,
					notes.toArray(new ChordNote[notes.size()]));
			return cle;
		} else {
			return null;
		}

	}

}
