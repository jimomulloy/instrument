package jomu.instrument;

import jomu.instrument.model.Memory;
import jomu.instrument.organs.Coordinator;
import jomu.instrument.organs.Druid;

public class Instrument {
	private static Instrument instrument;
	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
	}
	private Coordinator coordinator;

	private Druid druid;

	private Memory memory;

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public Druid getDruid() {
		return druid;
	}

	public Memory getMemory() {
		return memory;
	}

	public void initialise() {
		memory = new Memory();
		memory.initialise();
		memory.start();
		druid = new Druid();
		druid.initialise();
		druid.start();
		coordinator = new Coordinator();
		coordinator.initialise();
		coordinator.start();
	}

}
