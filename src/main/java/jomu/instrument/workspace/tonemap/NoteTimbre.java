package jomu.instrument.workspace.tonemap;

import java.util.HashMap;
import java.util.Map;

public class NoteTimbre {

	public double frequency;
	public double range;
	public double median;
	private NoteListElement noteListElement;

	public NoteTimbre(NoteListElement noteListElement) {
		this.noteListElement = noteListElement;
	}

	public void buildTimbre(ToneTimeFrame[] timeFrames) {
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

	public NoteTimbre clone(NoteListElement nleClone) {
		NoteTimbre clone = new NoteTimbre(nleClone);
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
		double ratio = this.frequency / other.frequency;
		if (ratio > 0.8 && ratio < 1.2) {
			return true;
		}
		return false;
	}

}
