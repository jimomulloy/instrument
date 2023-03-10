package jomu.instrument.store;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.Organ;

@ApplicationScoped
public class Storage implements Organ {

	private static final Logger LOG = Logger.getLogger(Storage.class.getName());

	@Inject
	InstrumentStoreService instrumentStoreService;

	@Override
	public void initialise() {
		System.out.println(">>STORAGE IS Init");
		instrumentStoreService.initialise();
		System.out.println(">>STORAGE IS Initted");
	}

	public InstrumentStoreService getInstrumentStoreService() {
		return instrumentStoreService;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

}
