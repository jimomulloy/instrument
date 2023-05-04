package jomu.instrument;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
@Default
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
		try {
			LOG.severe(">>Initialise INSTRUMENT");
			controller.initialise();
			storage.initialise();
			workspace.initialise();
			console.initialise();
			coordinator.initialise();
			LOG.severe(">>Initialised INSTRUMENT");
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ">>Initialise INSTRUMENT exception: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	public void start() {
		try {
			LOG.severe(">>Start INSTRUMENT");
			controller.start();
			storage.start();
			workspace.start();
			console.start();
			coordinator.start();
			LOG.severe(">>Started INSTRUMENT");
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ">>Start INSTRUMENT exception: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	@Override
	public void stop() {
		LOG.finer(">>Stop INSTRUMENT");
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

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		// TODO Auto-generated method stub

	}

}
