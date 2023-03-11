package jomu.instrument.cognition.cell;

/**
 * LayerClassification Denotes the layer of a given NuCell
 *
 * NuCells that receive stimuli from outside the network are called input
 * NuCells; NuCells whose outputs are externally used are called output NuCells;
 * NuCells that receive stimuli from other NuCells and whose outputs are s
 * timuli for other NuCells in the network are called hidden NuCells
 *
 * Imperial College Press, “Slawomir Koziel” Simulation-Driven Design
 * Optimization and Modeling for Microwave Engineering, ISBN-13: 978-1848169166
 *
 * @author lancedooley
 *
 */
public enum LayerClassification {
	EXTERNAL_INPUT, HIDDEN, INPUT, INPUT_OUTPUT, OUTPUT
}
