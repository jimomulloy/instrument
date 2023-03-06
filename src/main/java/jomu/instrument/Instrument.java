package jomu.instrument;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jomu.instrument.audio.AudioGenerator;
import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
@Component
public class Instrument implements Organ, InstrumentFactory {

	private static final Logger LOG = Logger.getLogger(Instrument.class.getName());

	private static Instrument instrument;

	@Autowired
	private Coordinator coordinator;

	@Autowired
	private Controller controller;

	@Autowired
	private Console console;

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
		LOG.info(">>Initialise INSTRUMENT");
		controller.initialise();
		storage.initialise();
		workspace.initialise();
		console.initialise();
		coordinator.initialise();
		LOG.info(">>Initialised INSTRUMENT");
	}

	public void start() {
		LOG.info(">>Start INSTRUMENT");
		controller.start();
		storage.start();
		workspace.start();
		console.start();
		coordinator.start();
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
