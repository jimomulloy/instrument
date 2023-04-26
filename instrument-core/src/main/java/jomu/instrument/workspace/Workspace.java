package jomu.instrument.workspace;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;
import jomu.instrument.control.ParameterManager;

@ApplicationScoped
public class Workspace implements Organ {

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

	@Override
	public void stop() throws InstrumentException {
		// TODO Auto-generated method stub

	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		instrumentSessionManager.getCurrentSession().setException(exception);
	}

}
