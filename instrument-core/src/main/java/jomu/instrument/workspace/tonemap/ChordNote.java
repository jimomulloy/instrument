package jomu.instrument.workspace.tonemap;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author User
 *
 */
public class ChordNote implements Comparable<ChordNote>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	double amplitude;
	int pitchClass;
	int index;
	int octave;

	public ChordNote(int index, int pitchClass, double amplitude, int octave) {
		this.index = index;
		this.pitchClass = pitchClass;
		this.amplitude = amplitude;
		this.octave = octave;
	}

	public int getIndex() {
		return index;
	}

	public double getAmplitude() {
		return amplitude;
	}

	public int getOctave() {
		return octave;
	}

	public int getPitchClass() {
		return pitchClass;
	}

	public ChordNote clone() {
		ChordNote clone = new ChordNote(index, pitchClass, amplitude, octave);
		return clone;
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
		return "ChordNote [amplitiude=" + amplitude + ", pitchClass=" + pitchClass + "]";
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
