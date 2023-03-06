package jomu.instrument;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
@Component
public class Instrument implements Organ, InstrumentFactory {

	private static final Logger LOG = Logger.getLogger(Instrument.class.getName());

	static Instrument instrument;

	@Inject
	Coordinator coordinator;

	@Inject
	Controller controller;

	@Inject
	Console console;

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

	public Controller getController() {
		return controller;
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public Console getConsole() {
		return console;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public Storage getStorage() {
		return storage;
	}

	public void initialise() {
		System.out.println(">>Initialise INSTRUMENT");
		LOG.info(">>Initialise INSTRUMENT");
		controller.initialise();
		storage.initialise();
		workspace.initialise();
		console.initialise();
		coordinator.initialise();
		System.out.println(">>Initialised INSTRUMENT");
		LOG.info(">>Initialised INSTRUMENT");
	}

	public void start() {
		LOG.info(">>Start INSTRUMENT");
		controller.start();
		storage.start();
		workspace.start();
		console.start();
		coordinator.start();
		System.out.println(">>Started INSTRUMENT");
		LOG.info(">>Started INSTRUMENT");
	}

	@Override
	public void stop() {
		LOG.info(">>Stop INSTRUMENT");

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
