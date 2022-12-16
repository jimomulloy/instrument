package jomu.instrument;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Druid;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
@Component
public class Instrument implements Organ, InstrumentFactory {

	private static Instrument instrument;

	@Autowired
	private Coordinator coordinator;

	@Autowired
	private Controller controller;

	@Autowired
	private Druid druid;

	@Autowired
	private Storage storage;

	@Autowired
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

	public Storage getStorage() {
		return storage;
	}

	public void initialise() {
		controller.initialise();
		storage.initialise();
		workspace.initialise();
		druid.initialise();
		coordinator.initialise();
	}

	public void start() {
		controller.start();
		storage.start();
		workspace.start();
		druid.start();
		coordinator.start();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

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
