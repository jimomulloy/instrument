package jomu.instrument.workspace.tonemap;

import java.io.Serializable;
import java.util.Objects;

/**
 * This is a class that encapsulates parameters associated with the ToneMap Time
 * base coordinate range, and sample size settings.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class TimeSet implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double currentTime;

	private int endSample;

	// public int getRange(){
	// return ((int)((timeToSamples(endTime - startTime))/sampleIndexSize));
	// }

	private double endTime;

	private int sampleIndexSize;

	private float sampleRate;

	private double sampleTimeSize;

	private int startSample;

	private double startTime;

	public TimeSet(double startTime, double endTime, float sampleRate, double sampleTimeSize) {

		this.startTime = startTime;
		this.endTime = endTime;
		this.sampleRate = sampleRate;
		this.sampleTimeSize = sampleTimeSize;
		sampleIndexSize = timeToSamples(sampleTimeSize);
	}

	@Override
	public TimeSet clone() {
		return new TimeSet(this.startTime, this.endTime, this.sampleRate, this.sampleTimeSize);
	}

	public int getEndSample() {
		return (getStartSample() + (getRange() * sampleIndexSize));
	}

	public double getEndTime() {
		return endTime;
	}

	public int getRange() {
		return (int) ((endTime - startTime) / sampleTimeSize);
	}

	public int getSampleIndexSize() {
		return sampleIndexSize;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public double getSampleTimeSize() {
		return sampleTimeSize;
	}

	public int getSampleWindow() {
		return (int) Math.floor(sampleRate * sampleTimeSize);
	}

	public int getStartSample() {
		return (timeToSamples(startTime));
	}

	public double getStartTime() {
		return startTime;
	}

	public double getTime(int index) {
		return (index * getSampleTimeSize());
	}

	public double samplesToTime(int samples) {
		return ((samples) * 1000.0 / sampleRate);
	}

	public int timeToIndex(double time) {
		return ((int) Math.floor((time - startTime) / getSampleTimeSize()));
	}

	public int timeToSamples(double time) {
		return ((int) ((time) * sampleRate));
	}

	@Override
	public String toString() {
		return "TimeSet [startTime=" + startTime + ", endTime=" + endTime + ", currentTime=" + currentTime
				+ ", startSample=" + startSample + ", endSample=" + endSample + ", sampleRate=" + sampleRate
				+ ", sampleTimeSize=" + sampleTimeSize + ", sampleIndexSize=" + sampleIndexSize + "]";
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
		TimeSet other = (TimeSet) obj;
		return Double.doubleToLongBits(startTime) == Double.doubleToLongBits(other.startTime);
	}
} // End TimeSet