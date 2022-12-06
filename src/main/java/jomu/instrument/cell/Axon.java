package jomu.instrument.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Axon implements Serializable {
	private static final long serialVersionUID = 1003;

	private List<NuCell> outputList = new ArrayList<>();

	private NuCell source;

	public Axon(NuCell source) {
		this.source = source;
	}

	public void connect(NuCell n) {
		outputList.add(n);
	}

	public void disconnect(NuCell n) {
		outputList.remove(n);
	}

	public void disconnectAll() {
		outputList.clear();
	}

	public List<NuCell> getConnections() {
		return outputList;
	}

	public int getCount() {
		return outputList.size();
	}

	public Double getOutput(Double d) {
		// Piece-Wise Linear Function
		// maps input to output from 0 to 1.
		// Input less than 0, output = 0
		// Input more than 1, output = 1
		if (d < 0.0d)
			return 0.0d;
		else if (d > 1.0)
			return 1.0d;
		else
			return d;
	}

	public NuCell getSource() {
		return source;
	}

	public boolean isConnectedTo(NuCell n) {
		// return outputMap.get(n);
		return outputList.contains(n);
	}

	public void send(NuMessage message) {
		for (NuCell target : outputList) {
			target.receive(message);
		}
	}

	public void send(String streamId, int sequence) {
		for (NuCell target : outputList) {
			NuMessage qe = new NuMessage(source, streamId, sequence);
			target.receive(qe);
		}
	}

}
