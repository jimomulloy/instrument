package jomu.instrument;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Druid;
import jomu.instrument.workspace.Workspace;

public class Instrument implements InstrumentFactory {

	private static Instrument instrument;

	private Coordinator coordinator;

	private Controller controller;

	private Druid druid;

	private Workspace workspace;

	public Controller getController() {
		return controller;
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public Druid getDruid() {
		return druid;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void initialise() {
		controller = new Controller();
		workspace = new Workspace();
		druid = new Druid();
		coordinator = new Coordinator();

		controller.initialise();
		workspace.initialise();
		druid.initialise();
		coordinator.initialise();

		controller.start();
		workspace.start();
		druid.start();
		coordinator.start();
	}

	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
	}

	public static void setInstance(Instrument injectedInstrument) {
		instrument = injectedInstrument;
	}

}
