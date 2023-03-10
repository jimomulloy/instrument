package jomu.instrument.workspace.tonemap;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * This class defines the fields of the elements contained in the NoteList
 * object which represent Notes derived from the ToneMap Processing function
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ChordListElement {

	TreeSet<ChordNote> chordNotes = new TreeSet<>();
	double endTime;
	double startTime;

	public ChordListElement(ChordNote[] chords, double startTime, double endTime) {
		this.chordNotes = new TreeSet<>(Arrays.asList(chords));
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public ChordListElement clone() {
		ChordListElement clone = new ChordListElement(chordNotes.toArray(new ChordNote[chordNotes.size()]),
				this.startTime, this.endTime);
		return clone;
	}

	public TreeSet<ChordNote> getChordNotes() {
		return chordNotes;
	}

	public boolean hasChordNote(int pitchClass) {
		return chordNotes.stream().anyMatch(c -> c.pitchClass == pitchClass);
	}

	public Optional<ChordNote> getChordNote(int pitchClass) {
		return chordNotes.stream().filter(c -> c.pitchClass == pitchClass).findAny();
	}

	public double getEndTime() {
		return endTime;
	}

	public double getStartTime() {
		return startTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(endTime, startTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChordListElement other = (ChordListElement) obj;
		return Double.doubleToLongBits(endTime) == Double.doubleToLongBits(other.endTime)
				&& Double.doubleToLongBits(startTime) == Double.doubleToLongBits(other.startTime);
	}

	@Override
	public String toString() {
		return "ChordListElement [chordNotes=" + chordNotes + ", size=" + chordNotes.size() + ", endTime=" + endTime
				+ ", startTime=" + startTime + "]";
	}

}