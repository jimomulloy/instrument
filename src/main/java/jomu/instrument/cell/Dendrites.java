package jomu.instrument.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Dendrites implements Serializable {
	private static final long serialVersionUID = 1002;

	// Map used to correlate the weight for each dendrite input
	// to the output signal of a presynapitic NuCell
	// key presynapitic NuCell can be null - yes
	// value Double , weight can be null - yes
	private Map<NuCell, Double> dendriteInputMap = new HashMap<NuCell, Double>();

	private ArrayList<Dendrite> dendriteList = new ArrayList<Dendrite>();

	private NuCell target;

	public Dendrites(NuCell target) {
		this.target = target;
	}

	public void addDendrite(NuCell source) {
		Dendrite dendrite = new Dendrite(source, target);
		dendriteList.add(dendrite);
	}

	public void getDendrite(NuCell source) {
		Dendrite dendrite = new Dendrite(source, target);
		dendriteList.add(dendrite);
		connect(source, 0D);
	}

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

	public int getCount() {
		return dendriteInputMap.size();
	}

	public Set<NuCell> getConnections() {
		java.util.Set<NuCell> keySet = dendriteInputMap.keySet();
		return keySet;
	}

	/**
	 * 
	 * @param NuCell
	 * @return
	 */
	public Double getWeight(NuCell NuCell) {
		return dendriteInputMap.get(NuCell);
	}

	/**
	 * 
	 * @param NuCell
	 * @param d
	 */
	public void setWeight(NuCell NuCell, Double d) {
		dendriteInputMap.put(NuCell, d);
	}

	public double computeNetInput() {
		double sum = 0d;

		Set<NuCell> set = dendriteInputMap.keySet();
		Iterator<NuCell> iterator = set.iterator();

		// loop through the dendriteInputMap and calculate the
		// summation of the product of incoming signals and weights
		while (iterator.hasNext()) {
			NuCell NuCell = iterator.next();
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
			Double product = NuCellOutput * weight;
			sum += product;
		}
		return sum;
	} // end computeNetInput

}
