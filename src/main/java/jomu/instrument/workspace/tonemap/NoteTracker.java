package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class NoteTracker {

	private static final Logger LOG = Logger.getLogger(NoteTracker.class.getName());

	Set<NoteTrack> tracks = new HashSet<>();
	ToneMap toneMap;

	public class NoteTrack {

		int number;
		double salience;
		LinkedList<NoteListElement> notes = new LinkedList<>();

		public NoteTrack(int number) {
			LOG.finer(">>Add Node Track: " + number);
			this.number = number;
		}

		public int getNumber() {
			return this.number;
		}

		public void addNote(NoteListElement note) {
			LOG.finer(">>Track Note: " + note);
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

	}

	public NoteTracker(ToneMap toneMap) {
		this.toneMap = toneMap;
	}

	public void trackNote(NoteListElement noteListElement) {
		NoteTrack salientTrack = null;
		if (tracks.isEmpty()) {
			salientTrack = createTrack();
		} else {
			NoteTrack[] candidateTracks = getNonPendingTracks(noteListElement);
			if (candidateTracks.length > 0) {
				salientTrack = getSalientTrack(candidateTracks, noteListElement);
			} else {
				candidateTracks = getPendingTracks(noteListElement);
				if (candidateTracks.length > 0) {
					salientTrack = getPendingSalientTrack(candidateTracks, noteListElement);
					if (salientTrack != null) {
						NoteListElement disconnectedNote = salientTrack.getLastNote();
						salientTrack.removeNote(disconnectedNote);
					}
				}
			}
		}
		if (salientTrack == null) {
			salientTrack = createTrack();
		}
		salientTrack.addNote(noteListElement);
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
		double maxSalience = -1, salience = -1;
		NoteTrack salientTrack = null;
		for (NoteTrack track : candidateTracks) {
			NoteListElement lastNote = track.getLastNote();
			salience = calculateSalience(noteListElement, lastNote);
			if (salience > maxSalience) {
				salientTrack = track;
				maxSalience = salience;
			}
		}
		return salientTrack;
	}

	private NoteTrack getPendingSalientTrack(NoteTrack[] candidateTracks, NoteListElement noteListElement) {

		double maxSalience = -1, salience = -1;
		NoteTrack salientTrack = null;
		for (NoteTrack track : candidateTracks) {
			NoteListElement lastNote = track.getLastNote();
			NoteListElement penultimateNote = track.getPenultimateNote();
			if (penultimateNote != null) {
				if (compareSalience(noteListElement, lastNote, penultimateNote)) {
					salience = calculateSalience(noteListElement, lastNote);
					if (salience > maxSalience) {
						salientTrack = track;
						maxSalience = salience;
					}
				}
			}
		}
		return salientTrack;
	}

	private boolean compareSalience(NoteListElement newNote, NoteListElement lastNote,
			NoteListElement penultimateNote) {
		double salienceNewNote = calculateSalience(newNote, penultimateNote);
		double salienceCurrentNote = calculateSalience(lastNote, penultimateNote);
		return salienceNewNote > salienceCurrentNote;
	}

	private double calculateSalience(NoteListElement noteListElement, NoteListElement lastNote) {
		return 1.0;
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

}
