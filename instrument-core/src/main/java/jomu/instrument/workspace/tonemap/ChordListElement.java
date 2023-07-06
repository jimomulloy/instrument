package jomu.instrument.workspace.tonemap;

import java.util.Arrays;
import java.util.Comparator;
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

	private TreeSet<ChordNote> chordNotes = new TreeSet<>();
	private double endTime;
	private double startTime;

	public ChordListElement(double startTime, double endTime, ChordNote[] chord) {
		this(startTime, endTime);
		this.chordNotes = new TreeSet<>(Arrays.asList(chord));
	}

	public ChordListElement(double startTime, double endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public ChordListElement clone() {
		ChordListElement clone = new ChordListElement(this.startTime, this.endTime,
				chordNotes.toArray(new ChordNote[chordNotes.size()]));
		return clone;
	}

	public void merge(ChordListElement source) {
		if (source != null) {
			ChordNote[] sourceNotes = source.getChordNotes()
					.toArray(new ChordNote[source.getChordNotes()
							.size()]);
			Arrays.sort(sourceNotes, new Comparator<ChordNote>() {
				public int compare(ChordNote c1, ChordNote c2) {
					return Double.valueOf(c2.getAmplitude())
							.compareTo(Double.valueOf(c1.getAmplitude()));
				}
			});

			for (ChordNote sourceNote : sourceNotes) {
				if (chordNotes.contains(sourceNote)) {
					if (chordNotes.floor(sourceNote).amplitude < sourceNote.amplitude) {
						chordNotes.floor(sourceNote).amplitude = sourceNote.amplitude;
					}
				} else {
					chordNotes.add(sourceNote);
				}
			}
		}
	}

	public TreeSet<ChordNote> getChordNotes() {
		return chordNotes;
	}

	public boolean hasChordNote(int pitchClass) {
		return chordNotes.stream()
				.anyMatch(c -> c.pitchClass == pitchClass);
	}

	public Optional<ChordNote> getChordNote(int pitchClass) {
		return chordNotes.stream()
				.filter(c -> c.pitchClass == pitchClass)
				.findAny();
	}

	public double getEndTime() {
		return endTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
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