package jomu.instrument.cognition.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Dendrites implements Serializable {
	private static final long serialVersionUID = 1002;

	// Map used to correlate the weight for each dendrite input
	// to the output signal of a presynapitic NuCell
	// key presynapitic NuCell can be null - yes
	// value Double , weight can be null - yes
	private Map<NuCell, Double> dendriteInputMap = new HashMap<>();

	private ArrayList<Dendrite> dendriteList = new ArrayList<>();

	private NuCell target;

	public Dendrites(NuCell target) {
		this.target = target;
	}

	public void addDendrite(NuCell source) {
		Dendrite dendrite = new Dendrite(source, target);
		dendriteList.add(dendrite);
	}

	public double computeNetInput() {
		double sum = 0d;

		Set<NuCell> set = dendriteInputMap.keySet();

		// loop through the dendriteInputMap and calculate the
		// summation of the product of incoming signals and weights
		for (NuCell NuCell : set) {
			if (NuCell == null)
				continue;

			// get the output signal from the the sending NuCell
			Double NuCellOutput = NuCell.computeTransferFunction();

			// get the weight associated with
			// the Dendrite for this NuCell from the map
			Double weight = dendriteInputMap.get(NuCell);

			if (weight == null)
				continue;

			// compute the product of the output from
			// the input NuCell and the weight of the
			// Dendrite
			double product = NuCellOutput * weight;
			sum += product;
		}
		return sum;
	} // end computeNetInput

	public void connect(NuCell source, Double d) {
		addDendrite(source);
		dendriteInputMap.put(source, d);
	}

	public void disconnect(NuCell n) {
		dendriteInputMap.remove(n);
	}

	public void disconnectAll() {
		dendriteInputMap.clear();
	}

	public Set<NuCell> getConnections() {
		java.util.Set<NuCell> keySet = dendriteInputMap.keySet();
		return keySet;
	}

	public int getCount() {
		return dendriteInputMap.size();
	}

	public void getDendrite(NuCell source) {
		Dendrite dendrite = new Dendrite(source, target);
		dendriteList.add(dendrite);
		connect(source, 0D);
	}

	/**
	 * @param NuCell
	 * @return
	 */
	public Double getWeight(NuCell NuCell) {
		return dendriteInputMap.get(NuCell);
	}

	/**
	 * @param NuCell
	 * @param d
	 */
	public void setWeight(NuCell NuCell, Double d) {
		dendriteInputMap.put(NuCell, d);
	}

}
