package jomu.instrument;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Druid;
import jomu.instrument.workspace.WorldModel;

public class Instrument {
	private static Instrument instrument;

	private Coordinator coordinator;

	private Controller controller;

	private Druid druid;

	private WorldModel worldModel;

	public Controller getController() {
		return controller;
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public Druid getDruid() {
		return druid;
	}

	public WorldModel getWorldModel() {
		return worldModel;
	}

	public void initialise() {
		controller = new Controller();
		controller.initialise();
		controller.start();
		worldModel = new WorldModel();
		worldModel.initialise();
		worldModel.start();
		druid = new Druid();
		druid.initialise();
		druid.start();
		coordinator = new Coordinator();
		coordinator.initialise();
		coordinator.start();
	}

	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
	}

}
