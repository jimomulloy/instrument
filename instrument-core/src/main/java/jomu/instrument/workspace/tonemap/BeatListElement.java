package jomu.instrument.workspace.tonemap;

import java.util.Objects;

/**
 * This class defines the fields of the elements contained in the NoteList
 * object which represent Notes derived from the ToneMap Processing function
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class BeatListElement {

	double amplitiude;
	double startTime;
	double endTime;

	public BeatListElement(double amplitude, double startTime, double endTime) {
		this.amplitiude = amplitude;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public BeatListElement clone() {
		BeatListElement clone = new BeatListElement(this.amplitiude, this.startTime, this.endTime);
		return clone;
	}

	public double getAmplitiude() {
		return amplitiude;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(startTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BeatListElement other = (BeatListElement) obj;
		return Double.doubleToLongBits(startTime) == Double.doubleToLongBits(other.startTime);
	}

	@Override
	public String toString() {
		return "BeatListElement [amplitiude=" + amplitiude + ", startTime=" + startTime + ", endTime=" + endTime + "]";
	}

}