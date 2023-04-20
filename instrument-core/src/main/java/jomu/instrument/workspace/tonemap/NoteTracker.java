package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class NoteTracker {

	private static final Logger LOG = Logger.getLogger(NoteTracker.class.getName());

	Set<NoteTrack> tracks = ConcurrentHashMap.newKeySet();
	ToneMap toneMap;

	public class NoteTrack {

		int number;
		double salience;
		LinkedList<NoteListElement> notes = new LinkedList<>();

		public NoteTrack(int number) {
			this.number = number;
		}

		public int getNumber() {
			return this.number;
		}

		public void addNote(NoteListElement note) {
			notes.add(note);
		}

		public NoteListElement getLastNote() {
			return notes.getLast();
		}

		public LinkedList<NoteListElement> getNotes() {
			return notes;
		}

		public boolean hasNote(NoteListElement note) {
			return notes.contains(note);
		}

		public NoteListElement getPenultimateNote() {
			if (notes.size() > 1) {
				return notes.get(notes.indexOf(notes.getLast()) - 1);
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

		public void insertNote(NoteListElement nle, NoteListElement previousNle) {
			int addIndex = notes.indexOf(previousNle) + 1;
			notes.add(addIndex, nle);
		}
	}

	public NoteTracker(ToneMap toneMap) {
		this.toneMap = toneMap;
	}

	public void trackNote(NoteListElement noteListElement) {
		NoteTrack salientTrack = null;
		if (tracks.isEmpty()) {
			salientTrack = createTrack();
		} else {
			NoteTrack[] pendingTracks = getPendingTracks(noteListElement);
			NoteTrack[] nonPendingTracks = getNonPendingTracks(noteListElement);

			if (pendingTracks.length > 0) {
				salientTrack = getPendingOverlappingSalientTrack(pendingTracks, noteListElement);
				if (salientTrack != null) {
					salientTrack.getLastNote().addOverlap(noteListElement);
				}
			}

			if (salientTrack == null && nonPendingTracks.length > 0) {
				salientTrack = getSalientTrack(nonPendingTracks, noteListElement);
			}
			if (salientTrack == null && pendingTracks.length > 0) {
				salientTrack = getPendingSalientTrack(pendingTracks, noteListElement);
				if (salientTrack != null) {
					NoteListElement disconnectedNote = salientTrack.getLastNote();
					salientTrack.removeNote(disconnectedNote);
				}
			}
		}
		if (salientTrack == null) {
			if (tracks.size() > 6 && ((noteListElement.endTime - noteListElement.startTime) < 200)) {
				return;
			} else {
				salientTrack = createTrack();
			}
		}
		salientTrack.addNote(noteListElement);
	}

	private NoteTrack getPendingOverlappingSalientTrack(NoteTrack[] candidateTracks, NoteListElement noteListElement) {
		NoteTrack pitchSalientTrack = null;
		int pitchProximity = Integer.MAX_VALUE;
		for (NoteTrack track : candidateTracks) {
			NoteListElement lastNote = track.getLastNote();
			if (noteListElement.note == lastNote.note) {
				return track;
			}
			if ((Math.abs(noteListElement.note - lastNote.note) <= 2)
					&& ((lastNote.endTime - noteListElement.startTime < 201))) {
				if (pitchProximity > noteListElement.note - lastNote.note) {
					pitchProximity = noteListElement.note - lastNote.note;
					pitchSalientTrack = track;
				}
			}
		}
		if (pitchSalientTrack != null) {
			return pitchSalientTrack;
		}
		return null;
	}

	public NoteTrack getTrack(NoteListElement noteListElement) {
		NoteTrack result = null;
		for (NoteTrack track : tracks) {
			if (track.hasNote(noteListElement)) {
				return track;
			}
		}
		return result;
	}

	private NoteTrack getSalientTrack(NoteTrack[] candidateTracks, NoteListElement noteListElement) {
		NoteTrack pitchSalientTrack = null;
		NoteTrack timeSalientTrack = null;
		int pitchProximity = Integer.MAX_VALUE;
		double timeProximity = Double.MAX_VALUE;
		for (NoteTrack track : candidateTracks) {
			NoteListElement lastNote = track.getLastNote();
			if (noteListElement.isContinuation && noteListElement.note == lastNote.note) {
				return track;
			}
			if (Math.abs(noteListElement.note - lastNote.note) <= 20) {
				if (pitchProximity > noteListElement.note - lastNote.note) {
					pitchProximity = noteListElement.note - lastNote.note;
					pitchSalientTrack = track;
				}
				if (timeProximity > noteListElement.startTime - lastNote.endTime) {
					timeProximity = noteListElement.startTime - lastNote.endTime;
					timeSalientTrack = track;
				}
				// double timbreFactor = noteListElement.noteTimbre. - lastNote.endTime;
			}
		}
		if (pitchSalientTrack != null || timeSalientTrack != null) {
			if (pitchSalientTrack == timeSalientTrack) {
				return pitchSalientTrack;
			}
			if ((noteListElement.note - timeSalientTrack.getLastNote().note) > 2
					* (noteListElement.note - pitchSalientTrack.getLastNote().note)) {
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
			if (penultimateNote != null && Math.abs(penultimateNote.note - noteListElement.note) <= 20) {
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
		pitchProximity = newNote.note - penultimateNote.note;
		timeProximity = newNote.startTime - penultimateNote.endTime;
		if (pitchProximity >= lastNote.note - penultimateNote.note) {
			if (timeProximity >= lastNote.startTime - penultimateNote.endTime) {
				return false;
			}
		} else {
			if (timeProximity < lastNote.startTime - penultimateNote.endTime) {
				return true;
			}
		}
		return true;
	}

	private NoteTrack[] getPendingTracks(NoteListElement noteListElement) {
		List<NoteTrack> result = new ArrayList<>();
		for (NoteTrack track : tracks) {
			NoteListElement lastNote = track.getLastNote();
			if (lastNote != null && (lastNote.endTime > noteListElement.startTime)) {
				result.add(track);
			}
		}
		return result.toArray(new NoteTrack[result.size()]);
	}

	private NoteTrack[] getNonPendingTracks(NoteListElement noteListElement) {
		List<NoteTrack> result = new ArrayList<>();
		for (NoteTrack track : tracks) {
			NoteListElement lastNote = track.getLastNote();
			if (lastNote != null && (lastNote.endTime <= noteListElement.startTime)) {
				result.add(track);
			}
		}
		return result.toArray(new NoteTrack[result.size()]);
	}

	private NoteTrack createTrack() {
		NoteTrack track = new NoteTrack(tracks.size() + 1);
		tracks.add(track);
		return track;
	}

	public NoteListElement[] cleanTracks(Double startTime) {
		Set<NoteListElement> discardedNotes = new HashSet<>();
		Set<NoteTrack> discardedTracks = new HashSet<>();
		if (tracks.size() > 5) {
			for (NoteTrack track : tracks) {
				LinkedList<NoteListElement> notes = track.getNotes();
				Set<NoteListElement> notesToDelete = new HashSet<>();
				NoteListElement lastNote = null;
				for (NoteListElement nle : notes) {
					double lastTime = 0;
					if (lastNote != null) {
						lastTime = lastNote.endTime;
					}
//					if (((nle.endTime - nle.startTime) < 200) && (nle.startTime - lastTime) > 2000) {
//						discardedNotes.add(nle);
//						notesToDelete.add(nle);
//						if (track.getNotes().size() == notesToDelete.size()) {
//							break;
//						}
//					}
					lastNote = nle;
				}
				for (NoteListElement nle : notesToDelete) {
					track.removeNote(nle);
					if (track.getNotes().size() == 0) {
						discardedTracks.add(track);
					}
				}
			}
		}
		for (NoteTrack track : discardedTracks) {
			removeTrack(track);
		}
		return discardedNotes.toArray(new NoteListElement[discardedNotes.size()]);
	}

	private void removeTrack(NoteTrack track) {
		tracks.remove(track);
	}

}
