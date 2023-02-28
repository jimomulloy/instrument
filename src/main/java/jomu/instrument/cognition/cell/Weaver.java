package jomu.instrument.cognition.cell;

public class Weaver {

	public static void connect(NuCell source, NuCell target) {
		target.connectInput(source);
		source.connectOutput(target);
	}
}
