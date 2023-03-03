package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NoteTracker {

	Set<NoteTrack> tracks = new HashSet<>();

	class NoteTrack {
		double salience;
		LinkedList<NoteListElement> notes = new LinkedList<>();

		void addNote(NoteListElement note) {
			notes.add(note);
		}

		NoteListElement getLastNote() {
			return notes.getLast();
		}

	}

	public void trackNote(NoteListElement noteListElement) {
		NoteTrack salientTrack = null;
		if (tracks.isEmpty()) {
			salientTrack = createTrack();
		} else {
			NoteTrack[] candidateTracks = getPendingTracks(noteListElement);
			if (candidateTracks.length != 0) {
				salientTrack = getSalientTrack(candidateTracks, noteListElement);
			} else {
				candidateTracks = getNonPendingTracks(noteListElement);
				if (candidateTracks.length != 0) {
					salientTrack = getSalientTrack(candidateTracks, noteListElement);
				}
			}
		}
		salientTrack.addNote(noteListElement);
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

	private double calculateSalience(NoteListElement noteListElement, NoteListElement lastNote) {
		return 1.0;
	}

	private NoteTrack[] getPendingTracks(NoteListElement noteListElement) {
		List<NoteTrack> result = new ArrayList<>();
		for (NoteTrack track : tracks) {
			NoteListElement lastNote = track.getLastNote();
			if (lastNote != null && (lastNote.endTime >= noteListElement.startTime)) {
				result.add(track);
			}
		}
		return result.toArray(new NoteTrack[result.size()]);
	}

	private NoteTrack[] getNonPendingTracks(NoteListElement noteListElement) {
		List<NoteTrack> result = new ArrayList<>();
		for (NoteTrack track : tracks) {
			NoteListElement lastNote = track.getLastNote();
			if (lastNote != null && (lastNote.endTime < noteListElement.startTime)) {
				result.add(track);
			}
		}
		return result.toArray(new NoteTrack[result.size()]);
	}

	private NoteTrack createTrack() {
		NoteTrack track = new NoteTrack();
		tracks.add(track);
		return track;
	}

}
