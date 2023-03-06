package jomu.instrument.store;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jomu.instrument.Organ;

@ApplicationScoped
@Component
public class Storage implements Organ {

	@Autowired
	InstrumentStoreService instrumentStoreService;

	@Override
	public void initialise() {
		instrumentStoreService.initialise();
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
