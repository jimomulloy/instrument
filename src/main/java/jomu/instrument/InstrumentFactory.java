package jomu.instrument;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

public interface InstrumentFactory {

	public Controller getController();

	public Coordinator getCoordinator();

	public Console getConsole();

	public Workspace getWorkspace();

	public Storage getStorage();

}
