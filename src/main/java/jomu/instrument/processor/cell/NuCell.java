package jomu.instrument.processor.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class NuCell extends Cell implements Serializable {
	private static final long serialVersionUID = 1002;

	private BlockingQueue<Object> bq;

	private HashMap<String, List<NuMessage>> sequenceMap = new HashMap<>();

	// Create ActivationFunction object
	protected ActivationFunction activationFunction = new ActivationFunction();

	// Axon represents the physical Axon of a NuCell and
	// it will hold references to all the NuCells it is
	// outputting a signal to
	protected Axon axon;

	// Class representing all of the Dendrite inputs
	// Each Dendrite can receive a signal from the axon of another NuCell,
	// and there is a weight associated with this relationship
	protected Dendrites dendrites;

	protected Double expectedOutputSignal = new Double(0.000d);

	// this field is for sending a signal into the Neural Network from
	// a source outside of the Neural Network
	protected Double externalInputSignal = new Double(0.000d);
	// Each NuCell can be a combination of the various types of
	// NetworkRole
	// LayerClassification
	// InputNuCell, HiddenNuCell, OutputNuCell
	protected LayerClassification layerClassification = LayerClassification.INPUT_OUTPUT;

	// enum, one of ...
	// UNIPOLAR, BIPOLAR, MULTIPOLAR, PSEUDOUNIPOLAR
	// this field is somewhat described by the class Dendrites
	// A NuCell "has-a" Dendrites instance
	protected MorphologyEnum morphology;

	// Processor
	protected Consumer<List<NuMessage>> processor;

	// Most NuCells receive many input signals throughout their dendritic trees.
	// A single NuCell may have more than one set of dendrites, and may receive
	// many thousands of input signals. Whether or not a NuCell is excited into
	// firing an impulse depends on the sum of all of the excitatory and
	// inhibitory
	// signals it receives. If the NuCell does end up firing, the nerve impulse,
	// or action potential, is conducted down the axon
	@SuppressWarnings("preview")
	public NuCell(CellTypes cellType) {
		super(cellType);
		dendrites = new Dendrites(this);
		axon = new Axon(this);
		bq = new LinkedBlockingQueue<>();
		Thread.startVirtualThread(new QueueConsumer());
		System.out.println(">>Loom thread started: ");
		// new Thread(new QueueConsumer()).start();
	}

	/**
	 *
	 * @param double
	 * @return double
	 */
	public double computeActivationFunction(double d) {
		// System.out.println("computeActivationFunction ... " + d );
		return activationFunction.compute(d);
	}

	/**
	 *
	 * @return Double
	 */
	public Double computeNetInput() {
		// System.out.println("NuCell.computeNetInput ..." );
		// loop through all the
		return dendrites.computeNetInput();
	}

	/**
	 *
	 * @return double
	 */
	public double computeOutputFunction(double d) {
		return axon.getOutput(d);
	}

	// ==========================================
	// Morphology
	// this section is unused for now
	// possibly remove it later
	// ==========================================

	/**
	 * Computes the Transfer Function for a NuCell
	 *
	 * @return Double
	 */
	public Double computeTransferFunction() {
		Double niD = computeNetInput();
		Double afD = computeActivationFunction(niD);
		Double ofD = computeOutputFunction(afD);
		return ofD.doubleValue();
	}

	/**
	 *
	 * @param NuCell
	 */
	public void connectInput(NuCell NuCell) {
		connectInput(NuCell, new Double(1d));
	}

	// ==========================================
	// Inputs
	// ==========================================

	/**
	 *
	 * @param NuCell
	 * @param d
	 */
	public void connectInput(NuCell NuCell, Double d) {
		dendrites.connect(NuCell, 1d);
		updateLayerClassification();
	}

	/**
	 *
	 * @param NuCell
	 */
	public void connectOutput(NuCell NuCell) {
		// axon.connect(NuCell, 1d);
		axon.connect(NuCell);
		updateLayerClassification();
	}

	/**
	 *
	 */
	public void disconnectAllInputs() {
		dendrites.disconnectAll();
	}

	/**
	 *
	 */
	public void disconnectAllOutputs() {
		axon.disconnectAll();
	}

	/**
	 *
	 * @param NuCell
	 */
	public void disconnectInput(NuCell NuCell) {
		// System.out.println(NuCellID + " disconnectInput " +
		// NuCell.getNuCellID()
		// + " success");
		dendrites.disconnect(NuCell);
		updateLayerClassification();
	}

	/**
	 *
	 * @param NuCell
	 */
	public void disconnectOutput(NuCell NuCell) {
		// System.out.println(NuCellID + " disconnectOutput " +
		// NuCell.getNuCellID() );
		axon.disconnect(NuCell);
		updateLayerClassification();
		// System.out.println(NuCellID + " disconnectOutput " +
		// NuCell.getNuCellID() + "
		// success");

	}

	/**
	 *
	 * @return ActivationFunctionEnum
	 */
	public ActivationFunctionEnum getActivationFunctionSelection() {
		return activationFunction.getSelection();
	}

	public double getActivationFunctionThreshold() {
		return activationFunction.getThreshold();
	}

	// ==========================================
	// External Input Signal
	// ==========================================

	/**
	 *
	 * @return Axon
	 */
	public Axon getAxon() {
		return axon;
	}

	/**
	 *
	 * @return
	 */
	public Dendrites getDendrites() {
		return dendrites;
	}

	// ==========================================
	// Outputs
	// ==========================================

	public double getExpectedOutputSignal() {
		return expectedOutputSignal;
	}

	/**
	 *
	 * @return
	 */
	public Double getExternalInputSignal() {
		return externalInputSignal;
	}

	/**
	 *
	 * @return
	 */
	public java.util.Set<NuCell> getInputs() {
		return dendrites.getConnections();
	}

	/**
	 *
	 * @param NuCell
	 * @return
	 */
	public Double getInputWeight(NuCell NuCell) {
		return dendrites.getWeight(NuCell);
	}

	/**
	 *
	 * @return
	 */
	public LayerClassification getLayerClassification() {
		return layerClassification;
	}

	/**
	 *
	 * @return MorphologyEnum
	 */
	public MorphologyEnum getMorphology() {
		return morphology;
	}

	/**
	 *
	 * @return Set<NuCell>
	 */
	// public java.util.Set<NuCell> getOutputs()
	public List<NuCell> getOutputs() {
		// return axon.getConnections();
		return axon.getConnections();
	}

	public Consumer<List<NuMessage>> getProcessor() {
		return processor;
	}

	/**
	 * Returns true if this is explicitly or implicitly connected to NuCellB.<br>
	 *
	 * Explicit case is if this NuCell is directly connected to NuCellB.<br>
	 * Implicit case is if this NuCell is connected to NuCellB through a feedback
	 * loop
	 * <p>
	 *
	 * @param NuCellB
	 * @return
	 */
	public boolean isCircularConnection(NuCell NuCell) {
		// System.out.println("NuCell.isCircularConnection: " +
		// NuCell.getNuCellID() );

		// If this is first iteration, then create the
		// visitedNuCells List and the bCircular object and
		// set appropriately
		List<NuCell> visitedNuCells = new ArrayList<>();
		visitedNuCells.add(this);

		return isCircularConnectionFound(visitedNuCells, NuCell);
	}

	/**
	 * isCircularConnectionFound is a recursive method used to find out if a NuCell
	 * has a circular connection in its network.<br>
	 *
	 * @param visitedNuCells type List<NuCell>
	 * @param n              type NuCell
	 * @return boolean
	 */
	public boolean isCircularConnectionFound(List<NuCell> visitedNuCells, NuCell n) {
		// System.out.println("NuCell.isCircularConnection: " +
		// n.getNuCellID() );

		// get list of outputs for NuCell n
		List<NuCell> outputs = n.getOutputs();

		if (outputs.size() == 0)
			return false;

		// loop through the list of outputs.
		// if any of the outputs are in the visitedNuCells List,
		// then return true
		for (NuCell NuCell : outputs) {
			if (visitedNuCells.contains(NuCell)) {
				return true;
			}
		}

		// add this the NuCell parameter to the visitedNuCells
		visitedNuCells.add(n);

		// loop through outputs again, calling isCircularConnectionFound for
		// each output
		// Each output NuCell, becomes the new visited NuCell
		for (NuCell NuCell : outputs) {
			if (n.isCircularConnectionFound(visitedNuCells, NuCell))
				return true;
		}

		return false;
	}

	/**
	 * isOutputConnected returns true if this NuCell has an output connected to the
	 * input parameter
	 *
	 * @param NuCell
	 */
	public boolean isOutputConnectedTo(NuCell NuCell) {
		// one of this NuCell's outputs

		return axon.isConnectedTo(NuCell);
		/*
		 * Double d = axon.isFound(NuCell);
		 *
		 * if( d == null ) return false; else return true;
		 */
	}

	// ==========================================
	// Net Input Function
	// ==========================================

	public void receive(NuMessage message) {
		bq.add(message);
	}

	// ==========================================
	// Activation Function
	// ==========================================

	public void send(NuMessage message) {
		axon.send(message);
	}

	public void send(String streamId, int sequence) {
		axon.send(streamId, sequence);
	}

	/**
	 *
	 * @param selection
	 */
	public void setActivationFunctionSelection(ActivationFunctionEnum selection) {
		activationFunction.setSelection(selection);
	}

	public void setActivationFunctionThreshold(double d) {
		// System.out.println("setActivationFunctionThreshold: " + d);
		activationFunction.setThreshold(d);
	}

	/**
	 *
	 * @param axon
	 */
	public void setAxon(Axon axon) {
		this.axon = axon;
	}

	// ==========================================
	// Output Function
	// ==========================================

	/**
	 *
	 * @param dendrites
	 */
	public void setDendrites(Dendrites dendrites) {
		this.dendrites = dendrites;
	}

	public void setExpectedOutputSignal(double d) {
		expectedOutputSignal = d;
	}

	/**
	 *
	 * @param d
	 */
	public void setExternalInputSignal(Double d) {
		externalInputSignal = d;
	}

	/**
	 *
	 * @param NuCell
	 * @param d
	 */
	public void setInputWeight(NuCell NuCell, Double d) {
		dendrites.setWeight(NuCell, d);
	}

	public void setMorphology(MorphologyEnum morphology) {
		this.morphology = morphology;
	}

	public void setProcessor(Consumer<List<NuMessage>> processor) {
		this.processor = processor;
	}

	// ==========================================
	// Layer Classification
	// ==========================================
	/**
	 *
	 */
	public void updateLayerClassification() {
		boolean bInputs = false;
		boolean bOutputs = false;

		// does this NuCell have any NuCells connected to its Dendrites
		if (dendrites.getCount() > 0)
			bInputs = true;

		// does this NuCell have any NuCells connected to its Axon
		if (axon.getCount() > 0)
			bOutputs = true;

		if (bInputs & bOutputs) {
			layerClassification = LayerClassification.HIDDEN;
			return;
		} else if (!bInputs & bOutputs) {
			layerClassification = LayerClassification.INPUT;
			return;
		} else if (bInputs & !bOutputs) {
			layerClassification = LayerClassification.OUTPUT;
			return;
		} else if (!bInputs & !bOutputs) {
			layerClassification = LayerClassification.INPUT_OUTPUT;
			return;
		}

	} // end updateLayerClassification

	private class QueueConsumer implements Runnable {

		@Override
		public void run() {
			try {
				while (true) {
					NuMessage qe = (NuMessage) bq.take();
					List<NuMessage> entries;
					if (sequenceMap.containsKey(qe.streamId + qe.sequence)) {
						entries = sequenceMap.get(qe.streamId + qe.sequence);
					} else {
						entries = new ArrayList<>();
						sequenceMap.put(qe.streamId + qe.sequence, entries);
					}
					entries.add(qe);
					if (entries.size() >= dendrites.getCount()) {
						processor.accept(entries);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
} // end NuCell
