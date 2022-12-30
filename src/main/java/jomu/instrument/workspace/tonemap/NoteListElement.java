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
	}

	@Override
	public int hashCode() {
		return Objects.hash(avgAmp, endTime, endTimeIndex, maxAmp, minAmp, note, overTone, percentMin, pitchIndex,
				startTime, startTimeIndex, underTone);
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
		return Double.doubleToLongBits(avgAmp) == Double.doubleToLongBits(other.avgAmp)
				&& Double.doubleToLongBits(endTime) == Double.doubleToLongBits(other.endTime)
				&& endTimeIndex == other.endTimeIndex
				&& Double.doubleToLongBits(maxAmp) == Double.doubleToLongBits(other.maxAmp)
				&& Double.doubleToLongBits(minAmp) == Double.doubleToLongBits(other.minAmp) && note == other.note
				&& overTone == other.overTone
				&& Double.doubleToLongBits(percentMin) == Double.doubleToLongBits(other.percentMin)
				&& pitchIndex == other.pitchIndex
				&& Double.doubleToLongBits(startTime) == Double.doubleToLongBits(other.startTime)
				&& startTimeIndex == other.startTimeIndex && underTone == other.underTone;
	}

	@Override
	public String toString() {
		return "NoteListElement [avgAmp=" + avgAmp + ", endTime=" + endTime + ", endTimeIndex=" + endTimeIndex
				+ ", maxAmp=" + maxAmp + ", minAmp=" + minAmp + ", note=" + note + ", overTone=" + overTone
				+ ", percentMin=" + percentMin + ", pitchIndex=" + pitchIndex + ", startTime=" + startTime
				+ ", startTimeIndex=" + startTimeIndex + ", underTone=" + underTone + "]";
	}

} // End NoteListElement