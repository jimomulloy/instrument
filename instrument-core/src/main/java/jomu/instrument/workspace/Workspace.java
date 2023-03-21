package jomu.instrument.workspace;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.control.ParameterManager;

@ApplicationScoped
public class Workspace {

	@Inject
	Atlas atlas;

	@Inject
	ParameterManager parameterManager;

	@Inject
	InstrumentSessionManager instrumentSessionManager;

	public Atlas getAtlas() {
		return atlas;
	}

	public InstrumentSessionManager getInstrumentSessionManager() {
		return instrumentSessionManager;
	}

	public void initialise() {
	}

	public void start() {
	}

}
