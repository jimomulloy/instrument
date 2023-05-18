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

	double amplitude;
	double startTime;
	double timeRange;

	public BeatListElement(double amplitude, double startTime, double timeRange) {
		this.amplitude = amplitude;
		this.startTime = startTime;
		this.timeRange = timeRange;
	}

	public BeatListElement clone() {
		BeatListElement clone = new BeatListElement(this.amplitude, this.startTime, this.timeRange);
		return clone;
	}

	public double getAmplitude() {
		return amplitude;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getTimeRange() {
		return timeRange;
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
		return "BeatListElement [amplitiude=" + amplitude + ", startTime=" + startTime + ", timeRange=" + timeRange
				+ "]";
	}

}