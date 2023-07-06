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
	 * Gets the console.
	 *
	 * @return the console
	 */
	Console getConsole();

	/**
	 * Gets the controller.
	 *
	 * @return the controller
	 */
	Controller getController();

	/**
	 * Gets the coordinator.
	 *
	 * @return the coordinator
	 */
	Coordinator getCoordinator();

	/**
	 * Gets the storage.
	 *
	 * @return the storage
	 */
	Storage getStorage();

	/**
	 * Gets the workspace.
	 *
	 * @return the workspace
	 */
	Workspace getWorkspace();

}
