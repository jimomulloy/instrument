package jomu.instrument.workspace.tonemap;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class NoteTimbre {

	private static final Logger LOG = Logger.getLogger(NoteTimbre.class.getName());

	public double frequency;
	public double range;
	public double median;

	double frequencyRange = 1.0;
	double frequencyRatio = 0.5;
	double medianRange = 1.0;
	double medianRatio = 0.5;

	public NoteTimbre(double frequencyRange, double frequencyRatio, double medianRange, double medianRatio) {
		super();
		this.frequencyRange = frequencyRange;
		this.frequencyRatio = frequencyRatio;
		this.medianRange = medianRange;
		this.medianRatio = medianRatio;
	}

	public void buildTimbre(ToneTimeFrame[] timeFrames, NoteListElement noteListElement) {
		Map<Double, Double> microTones = new HashMap<>();
		for (ToneTimeFrame toneTimeFrame : timeFrames) {
			if (toneTimeFrame.getStartTime() > noteListElement.endTime / 1000.0) {
				break;
			}
			if (toneTimeFrame.getStartTime() < noteListElement.startTime / 1000.0) {
				continue;
			}
			ToneMapElement toneMapElement = toneTimeFrame.getElement(noteListElement.pitchIndex);
			microTones.putAll(toneMapElement.microTones.getMicroTones());

		}
		median = buildMedian(microTones);
		frequency = buildFrequency(microTones);
		range = buildRange(microTones);

	}

	public void buildTimbre(ToneTimeFrame timeFrames[], int pitchIndex) {
		Map<Double, Double> microTones = new HashMap<>();
		for (ToneTimeFrame toneTimeFrame : timeFrames) {
			ToneMapElement toneMapElement = toneTimeFrame.getElement(pitchIndex);
			microTones.putAll(toneMapElement.microTones.getMicroTones());
		}
		median = buildMedian(microTones);
		frequency = buildFrequency(microTones);
		range = buildRange(microTones);

	}

	public void buildTimbre(ToneMapElement toneMapElement) {
		Map<Double, Double> microTones = new HashMap<>();
		microTones.putAll(toneMapElement.microTones.getMicroTones());
		median = buildMedian(microTones);
		frequency = buildFrequency(microTones);
		range = buildRange(microTones);
	}

	private double buildRange(Map<Double, Double> microTones) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (double amplitude : microTones.values()) {
			if (max < amplitude) {
				max = amplitude;
			}
			if (min > amplitude) {
				min = amplitude;
			}
		}
		return Math.log10(1 + (1000 * (max - min)));
	}

	private double buildFrequency(Map<Double, Double> microTones) {
		int highs = 0;
		boolean highPending = false;
		double lastAmplitude = 0;
		for (double amplitude : microTones.values()) {
			if (lastAmplitude <= amplitude) {
				highPending = true;
			} else if (highPending) {
				highs++;
				highPending = false;
			}
			lastAmplitude = amplitude;
		}
		return highs;
	}

	private double buildMedian(Map<Double, Double> microTones) {
		double total = 0;
		double count = 0;
		for (double amplitude : microTones.values()) {
			count++;
			total += amplitude;
		}
		return Math.log10(1 + (1000 * (total / count)));
	}

	public NoteTimbre clone() {
		NoteTimbre clone = new NoteTimbre(frequencyRange, frequencyRatio, medianRange, medianRatio);
		clone.median = median;
		clone.frequency = frequency;
		clone.range = range;
		return clone;
	}

	public boolean matches(NoteTimbre other) {
		if (this.frequency == 0 && other.frequency == 0) {
			return true;
		}
		if (this.frequency != 0 && other.frequency == 0) {
			return false;
		}
		if (this.frequency == 0 && other.frequency != 0) {
			return false;
		}
		double fr = this.frequency / other.frequency;
		if (fr > frequencyRatio && fr < (frequencyRatio + frequencyRange)) {
			double mr = (this.range / this.median) / (other.range / other.median);
			if (mr > medianRatio && mr < (medianRatio + medianRange)) {
				return true;
			}
		}
		return false;
	}

	public boolean matches2(NoteTimbre other) {
		LOG.severe(">>!!matches2: " + this.frequency + ", " + other.frequency + ", " + this.median + ", " + other.median
				+ ", " + this.range + ", " + other.range);
		if (this.frequency == 0 && other.frequency == 0) {
			return true;
		}
		if (this.frequency != 0 && other.frequency == 0) {
			return false;
		}
		if (this.frequency == 0 && other.frequency != 0) {
			return false;
		}
		double fr = this.frequency / other.frequency;
		LOG.severe(">>!!matches2 A: " + fr + ", " + (frequencyRatio + frequencyRange));
		if (fr > frequencyRatio && fr < (frequencyRatio + frequencyRange)) {
			double mr = (this.range / this.median) / (other.range / other.median);
			LOG.severe(">>!!matches2 B: " + mr + ", " + (medianRatio + medianRange) + ", " + (this.range / this.median)
					+ ", " + (other.range / other.median));
			if (mr > medianRatio && mr < (medianRatio + medianRange)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "NoteTimbre [frequency=" + frequency + ", range=" + range + ", median=" + median + ", frequencyRange="
				+ frequencyRange + ", frequencyRatio=" + frequencyRatio + ", medianRange=" + medianRange
				+ ", medianRatio=" + medianRatio + "]";
	}

}
