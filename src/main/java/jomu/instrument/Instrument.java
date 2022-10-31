package jomu.instrument;

import jomu.instrument.organs.Controller;
import jomu.instrument.organs.Coordinator;
import jomu.instrument.organs.Druid;
import jomu.instrument.world.WorldModel;

public class Instrument {
	private static Instrument instrument;

	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
	}

	private Coordinator coordinator;

	private Controller controller;

	private Druid druid;

	private WorldModel worldModel;

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public Controller getController() {
		return controller;
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

}
