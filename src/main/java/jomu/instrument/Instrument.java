package jomu.instrument;

import jomu.instrument.memory.Memory;
import jomu.instrument.organs.Coordinator;
import jomu.instrument.organs.Druid;

public class Instrument {
	private Coordinator coordinator;
	private Druid druid;
	private Memory memory;

	private static Instrument instrument;

	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
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

	public Memory getMemory() {
		return memory;
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public Druid getDruid() {
		return druid;
	}

}
