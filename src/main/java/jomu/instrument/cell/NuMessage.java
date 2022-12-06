package jomu.instrument.cell;

import java.util.Objects;

public class NuMessage {
	public int sequence;
	public Cell source;
	public String streamId;

	public NuMessage(Cell source, String streamId, int sequence) {
		this.source = source;
		this.sequence = sequence;
		this.streamId = streamId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (getClass() != obj.getClass()))
			return false;
		NuMessage other = (NuMessage) obj;
		return Objects.equals(streamId + sequence, other.streamId + other.sequence)
				&& Objects.equals(source, other.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(streamId + sequence, source);
	}

	@Override
	public String toString() {
		return "NuMessage [streamId=" + streamId + " source=" + source + ", sequence=" + sequence + "]";
	}
}
