package jomu.instrument;

import jomu.instrument.organs.Coordinator;
import jomu.instrument.organs.Druid;

public class Instrument {
	private Coordinator coordinator;
	private Druid druid;
	private static Instrument instrument;

	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
	}

	public void initialise() {
		druid = new Druid();
		druid.initialise();
		druid.start();
		coordinator = new Coordinator();
		coordinator.initialise();
		coordinator.start();
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public Druid getDruid() {
		return druid;
	}

}
