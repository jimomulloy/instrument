package jomu.instrument;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

/**
 * A factory for creating Instrument objects.
 * 
 * @author Jim O'Mulloy
 */
public interface InstrumentFactory {

	/**
	 * Gets the controller.
	 *
	 * @return the controller
	 */
	public Controller getController();

	/**
	 * Gets the coordinator.
	 *
	 * @return the coordinator
	 */
	public Coordinator getCoordinator();

	/**
	 * Gets the console.
	 *
	 * @return the console
	 */
	public Console getConsole();

	/**
	 * Gets the workspace.
	 *
	 * @return the workspace
	 */
	public Workspace getWorkspace();

	/**
	 * Gets the storage.
	 *
	 * @return the storage
	 */
	public Storage getStorage();

}
