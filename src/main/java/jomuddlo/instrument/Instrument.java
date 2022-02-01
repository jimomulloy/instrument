package jomuddlo.instrument;

import jomuddlo.instrument.organs.Coordinator;

public class Instrument {
	private Coordinator coordinator = new Coordinator();
	private static Instrument instrument;
	
	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
	}
	
	public void initialise() {
		coordinator = new Coordinator();
		coordinator.initialise();
	}

}
