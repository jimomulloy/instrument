package jomu.instrument.workspace.tonemap;

import java.util.Objects;

/**
 * @author User
 *
 */
public class ChordNote implements Comparable<ChordNote> {
	double amplitiude;
	int pitchClass;

	public ChordNote(int pitchClass, double amplitude) {
		this.pitchClass = pitchClass;
		this.amplitiude = amplitude;
	}

	public double getAmplitiude() {
		return amplitiude;
	}

	public int getPitchClass() {
		return pitchClass;
	}

	@Override
	public int hashCode() {
		return Objects.hash(pitchClass);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChordNote other = (ChordNote) obj;
		return pitchClass == other.pitchClass;
	}

	@Override
	public String toString() {
		return "ChordNote [amplitiude=" + amplitiude + ", pitchClass=" + pitchClass + "]";
	}

	@Override
	public int compareTo(ChordNote o) {
		if (this.pitchClass > o.pitchClass)
			return 1;
		if (this.pitchClass < o.pitchClass)
			return -1;
		return 0;
	}

}
