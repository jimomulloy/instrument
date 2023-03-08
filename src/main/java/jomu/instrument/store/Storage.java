package jomu.instrument.store;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import jomu.instrument.Organ;

@ApplicationScoped
@Component
public class Storage implements Organ {

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
		// TODO Auto-generated method stub
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
