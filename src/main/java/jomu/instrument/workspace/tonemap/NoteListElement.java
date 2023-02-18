package jomu.instrument.workspace.tonemap;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class defines the fields of the elements contained in the NoteList
 * object which represent Notes derived from the ToneMap Processing function
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteListElement implements Serializable {

	public double avgAmp; // Average amplitude (loudness)

	public double endTime; // Note End Time
	public int endTimeIndex; // End Time Index in ToneMapMatrix
	public double maxAmp; // Maximum amplitude
	public double minAmp; // Minumum amplitude
	public int note; // Midi note pitch
	public boolean overTone; // Flag note as overtone
	public double percentMin; // Percentage # entries below minimum
	public int pitchIndex; // Pitch index in ToneMapMatrix
	public double startTime; // Note Start Time
	public int startTimeIndex; // Start Time Index in ToneMapMatrix
	public boolean underTone; // Flag note as undertone
	public NoteHarmonics noteHarmonics;
	public NoteTimbre noteTimbre;

	public NoteListElement(int note, int pitchIndex, double startTime, double endTime, int startTimeIndex,
			int endTimeIndex, double avgAmp, double maxAmp, double minAmp, double percentMin) {
		this.note = note;
		this.pitchIndex = pitchIndex;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startTimeIndex = startTimeIndex;
		this.endTimeIndex = endTimeIndex;
		this.avgAmp = avgAmp;
		this.maxAmp = maxAmp;
		this.minAmp = minAmp;
		this.percentMin = percentMin;
		this.noteHarmonics = new NoteHarmonics();
		this.noteTimbre = new NoteTimbre(this);
	}

	public NoteListElement clone() {
		NoteListElement clone = new NoteListElement(this.note, this.pitchIndex, this.startTime, this.endTime,
				this.startTimeIndex, this.endTimeIndex, this.avgAmp, this.maxAmp, this.minAmp, this.percentMin);
		clone.noteHarmonics = noteHarmonics.clone();
		clone.noteTimbre = noteTimbre.clone(clone);
		return clone;
	}

	@Override
	public int hashCode() {
		return Objects.hash(note, pitchIndex, startTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NoteListElement other = (NoteListElement) obj;
		return note == other.note && pitchIndex == other.pitchIndex
				&& Double.doubleToLongBits(startTime) == Double.doubleToLongBits(other.startTime);
	}

	@Override
	public String toString() {
		return "NoteListElement [avgAmp=" + avgAmp + ", endTime=" + endTime + ", endTimeIndex=" + endTimeIndex
				+ ", maxAmp=" + maxAmp + ", minAmp=" + minAmp + ", note=" + note + ", overTone=" + overTone
				+ ", percentMin=" + percentMin + ", pitchIndex=" + pitchIndex + ", startTime=" + startTime
				+ ", startTimeIndex=" + startTimeIndex + ", underTone=" + underTone + "]";
	}

} // End NoteListElement