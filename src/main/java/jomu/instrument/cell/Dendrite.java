package jomu.instrument.cell;

import java.io.Serializable;

public class Dendrite implements Serializable {
	private static final long serialVersionUID = 1002;

	private NuCell source;

	private NuCell target;

	public Dendrite(NuCell source, NuCell target) {
		super();
		this.source = source;
		this.target = target;
	}

	public NuCell getSource() {
		return source;
	}

	public NuCell getTarget() {
		return target;
	}

	/**
	 *
	 * @param NuCell
	 * @param d
	 */
	public void receive(NuMessage message) {
		target.receive(message);
	}

}
