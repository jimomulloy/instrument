package jomu.instrument.workspace.tonemap;

import java.util.HashMap;
import java.util.Map;

public class NoteTimbre {

	public double frequency;
	public double range;
	public double median;

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
		return max - min;
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
		return total / count;
	}

	public NoteTimbre clone() {
		NoteTimbre clone = new NoteTimbre();
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
		double frequencyRatio = this.frequency / other.frequency;
		if (frequencyRatio > 0.5 && frequencyRatio < 1.5) {
			double medianRangeRatio = (this.range / this.median) / (other.range / other.median);
			if (medianRangeRatio > 0.5 && medianRangeRatio < 1.5) {
				return true;
			}
		}
		return false;
	}

}
