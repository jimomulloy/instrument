package jomu.instrument.store;

import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.FrameStore;

@ApplicationScoped
public class Storage implements Organ {

	private static final Logger LOG = Logger.getLogger(Storage.class.getName());

	@Inject
	InstrumentStoreService instrumentStoreService;

	@Inject
	ObjectStorage objectStorage;

	@Inject
	FrameStore frameStore;

	@Inject
	ParameterManager parameterManager;

	@Override
	public void initialise() {
		instrumentStoreService.initialise();
	}

	public InstrumentStoreService getInstrumentStoreService() {
		return instrumentStoreService;
	}

	public ObjectStorage getObjectStorage() {
		return objectStorage;
	}

	public FrameStore getFrameStore() {
		return frameStore;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		// TODO Auto-generated method stub

	}

}
