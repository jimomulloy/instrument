package jomu.instrument;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.monitor.Druid;
import jomu.instrument.workspace.Workspace;

public interface InstrumentFactory {

	public Controller getController();

	public Coordinator getCoordinator();

	public Druid getDruid();

	public Workspace getWorkspace();

}
