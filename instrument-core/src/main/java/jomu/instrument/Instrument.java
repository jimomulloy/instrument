package jomu.instrument;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

/**
 * The Class Instrument.
 * <p>
 * Instrument is the main entry point, container and lifecycle manager for all
 * the component Organ parts within the body of the application.
 * </p>
 *
 * @author Jim O'Mulloy
 */
@ApplicationScoped
public class Instrument implements Organ, InstrumentFactory {

	/** The instrument. */
	static Instrument instrument;

	private static final Logger LOG = Logger.getLogger(Instrument.class.getName());

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
	 * @param injectedInstrument
	 *            the new instance
	 */
	public static void setInstance(final Instrument injectedInstrument) {
		instrument = injectedInstrument;
	}

	/** The console. */
	@Inject
	Console console;

	/** The controller. */
	@Inject
	Controller controller;

	/** The coordinator. */
	@Inject
	Coordinator coordinator;

	/** The storage. */
	@Inject
	Storage storage;

	/** The workspace. */
	@Inject
	Workspace workspace;

	private boolean isAlive = false;

	/**
	 * Gets the console.
	 *
	 * @return the console
	 */
	@Override
	public Console getConsole() {
		return this.console;
	}

	/**
	 * Gets the controller.
	 *
	 * @return the controller
	 */
	@Override
	public Controller getController() {
		return this.controller;
	}

	/**
	 * Gets the coordinator.
	 *
	 * @return the coordinator
	 */
	@Override
	public Coordinator getCoordinator() {
		return this.coordinator;
	}

	/**
	 * Gets the storage.
	 *
	 * @return the storage
	 */
	@Override
	public Storage getStorage() {
		return this.storage;
	}

	/**
	 * Gets the workspace.
	 *
	 * @return the workspace
	 */
	@Override
	public Workspace getWorkspace() {
		return this.workspace;
	}

	/**
	 * Initialise.
	 */
	@Override
	public void initialise() {
		try {
			LOG.severe(">>Initialise INSTRUMENT");
			this.controller.initialise();
			this.storage.initialise();
			this.workspace.initialise();
			this.console.initialise();
			this.coordinator.initialise();
			LOG.severe(">>Initialised INSTRUMENT");
		} catch (final Exception ex) {
			LOG.log(Level.SEVERE, ">>Initialise INSTRUMENT exception: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	/**
	 * Process exception.
	 *
	 * @param exception
	 *            the exception
	 * @throws InstrumentException
	 *             the instrument exception
	 */
	@Override
	public void processException(final InstrumentException exception) throws InstrumentException {
		LOG.log(Level.SEVERE, ">>INSTRUMENT Exception: " + exception.getMessage(), exception);
	}

	/**
	 * Reset.
	 */
	public void reset() {
		try {
			stop();
			initialise();
			start();
		} catch (final Exception ex) {
			LOG.log(Level.SEVERE, ">>Reset INSTRUMENT exception: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	/**
	 * Start.
	 */
	@Override
	public void start() {
		try {
			LOG.severe(">>Start INSTRUMENT");
			this.controller.start();
			this.storage.start();
			this.workspace.start();
			this.console.start();
			this.coordinator.start();
			this.setAlive(true);
			LOG.severe(">>Started INSTRUMENT");
		} catch (final Exception ex) {
			LOG.log(Level.SEVERE, ">>Start INSTRUMENT exception: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	/**
	 * Stop.
	 */
	@Override
	public void stop() {
		LOG.finer(">>Stop INSTRUMENT");
		this.controller.stop();
		this.storage.stop();
		this.workspace.stop();
		this.console.stop();
		this.coordinator.stop();
		this.setAlive(false);
	}

	public boolean isAlive() {
		return isAlive;

	}

	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;

	}

}
