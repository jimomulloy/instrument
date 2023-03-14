package jomu.instrument.workspace.tonemap;

import java.util.Objects;

/**
 * This class defines the fields of the elements contained in the NoteList
 * object which represent Notes derived from the ToneMap Processing function
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteListElement {

	public double avgAmp;
	public double endTime;
	public int endTimeIndex;
	public double maxAmp;
	public double minAmp;
	public int note;
	public boolean overTone;
	public double percentMin;
	public int pitchIndex;
	public double startTime;
	public int startTimeIndex;
	public boolean underTone;
	public NoteHarmonics noteHarmonics;
	public NoteTimbre noteTimbre;
	public boolean isContinuation;

	public NoteListElement(int note, int pitchIndex, double startTime, double endTime, int startTimeIndex,
			int endTimeIndex, double avgAmp, double maxAmp, double minAmp, double percentMin, boolean isContinuation) {
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
		this.isContinuation = isContinuation;
	}

	public NoteListElement clone() {
		NoteListElement clone = new NoteListElement(this.note, this.pitchIndex, this.startTime, this.endTime,
				this.startTimeIndex, this.endTimeIndex, this.avgAmp, this.maxAmp, this.minAmp, this.percentMin,
				this.isContinuation);
		clone.noteHarmonics = noteHarmonics.clone();
		clone.noteTimbre = noteTimbre.clone(clone);
		return clone;
	}

	@Override
	public String toString() {
		return "NoteListElement [avgAmp=" + avgAmp + ", endTime=" + endTime + ", endTimeIndex=" + endTimeIndex
				+ ", maxAmp=" + maxAmp + ", minAmp=" + minAmp + ", note=" + note + ", overTone=" + overTone
				+ ", percentMin=" + percentMin + ", pitchIndex=" + pitchIndex + ", startTime=" + startTime
				+ ", startTimeIndex=" + startTimeIndex + ", underTone=" + underTone + "]";
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

}