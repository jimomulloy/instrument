package jomu.instrument.store;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;
import jomu.instrument.control.ParameterManager;

@ApplicationScoped
public class Storage implements Organ {

	private static final Logger LOG = Logger.getLogger(Storage.class.getName());

	@Inject
	InstrumentStoreService instrumentStoreService;

	@Inject
	ObjectStorage objectStorage;

	@Inject
	ParameterManager parameterManager;

	@Override
	public void initialise() {
		System.out.println(">>STORAGE IS Init");
		instrumentStoreService.initialise();
		System.out.println(">>STORAGE IS Initted");
	}

	public InstrumentStoreService getInstrumentStoreService() {
		return instrumentStoreService;
	}

	public ObjectStorage getObjectStorage() {
		return objectStorage;
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
