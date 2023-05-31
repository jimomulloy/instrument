package jomu.instrument;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

/**
 * The Class Instrument.
 * 
 * <p>
 * Instrument is the main entry point, container and lifecycle manager for all
 * the component Organ parts within the body of the application.
 * </p>
 * 
 * @author Jim O'Mulloy
 */
@ApplicationScoped
@Default
public class Instrument implements Organ, InstrumentFactory {

	private static final Logger LOG = Logger.getLogger(Instrument.class.getName());

	/** The instrument. */
	static Instrument instrument;

	/** The coordinator. */
	@Inject
	Coordinator coordinator;

	/** The controller. */
	@Inject
	Controller controller;

	/** The console. */
	@Inject
	Console console;

	/** The storage. */
	@Inject
	Storage storage;

	/** The workspace. */
	@Inject
	Workspace workspace;

	/**
	 * Gets the controller.
	 *
	 * @return the controller
	 */
	public Controller getController() {
		return controller;
	}

	/**
	 * Gets the coordinator.
	 *
	 * @return the coordinator
	 */
	public Coordinator getCoordinator() {
		return coordinator;
	}

	/**
	 * Gets the console.
	 *
	 * @return the console
	 */
	public Console getConsole() {
		return console;
	}

	/**
	 * Gets the workspace.
	 *
	 * @return the workspace
	 */
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * Gets the storage.
	 *
	 * @return the storage
	 */
	public Storage getStorage() {
		return storage;
	}

	/**
	 * Initialise.
	 */
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

	/**
	 * Start.
	 */
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

	/**
	 * Reset.
	 */
	public void reset() {
		try {
			stop();
			initialise();
			start();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ">>Reset INSTRUMENT exception: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	/**
	 * Stop.
	 */
	@Override
	public void stop() {
		LOG.finer(">>Stop INSTRUMENT");
		controller.stop();
		storage.stop();
		workspace.stop();
		console.stop();
		coordinator.stop();
	}

	/**
	 * Gets the single instance of Instrument.
	 *
	 * @return single instance of Instrument
	 */
	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
	}

	/**
	 * Sets the instance.
	 *
	 * @param injectedInstrument the new instance
	 */
	public static void setInstance(Instrument injectedInstrument) {
		instrument = injectedInstrument;
	}

	/**
	 * Process exception.
	 *
	 * @param exception the exception
	 * @throws InstrumentException the instrument exception
	 */
	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		LOG.log(Level.SEVERE, ">>INSTRUMENT Exception: " + exception.getMessage(), exception);
	}

}
