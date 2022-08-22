package jomu.instrument.tonemap.old;

import java.io.Serializable;

/**
 * This is a class that encapsulates parameters associated with the ToneMap Time
 * base coordinate range, and sample size settings.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class TimeSet implements Serializable {

	private double startTime;

	private double endTime;

	// public int getRange(){
	// return ((int)((timeToSamples(endTime - startTime))/sampleIndexSize));
	// }

	private double currentTime;

	private int startSample;

	private int endSample;

	private double sampleRate;

	private double timeIndexSize;

	private double sampleTimeSize;

	private int sampleIndexSize;

	public TimeSet(double startTime, double endTime, double sampleRate, double sampleTimeSize) {

		this.startTime = startTime;
		this.endTime = endTime;
		this.sampleRate = sampleRate;
		this.sampleTimeSize = sampleTimeSize;
		sampleIndexSize = timeToSamples(sampleTimeSize);
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
	public double getSampleTimeSize() {
		return sampleTimeSize;
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
		return (((double) samples) * 1000.0 / sampleRate);
	}
	public int timeToIndex(double time) {
		return ((int) Math.floor((time - startTime) / getSampleTimeSize()));
	}
	public int timeToSamples(double time) {
		return ((int) ((time / 1000.0) * sampleRate));
	}

} // End TimeSet