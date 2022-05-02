package jomu.instrument.cell;

import java.util.Objects;

public class NuMessage {
	public Cell source;
	public String sequence;
	public Object input;

	public NuMessage(Cell source, String sequence, Object input) {
		this.source = source;
		this.sequence = sequence;
		this.input = input;
	}

	@Override
	public String toString() {
		return "NuMessage [source=" + source + ", sequence=" + sequence + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(sequence, source);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NuMessage other = (NuMessage) obj;
		return Objects.equals(sequence, other.sequence) && Objects.equals(source, other.source);
	}
}
