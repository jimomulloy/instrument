package jomu.instrument.monitor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;

@ApplicationScoped
public class Console implements Organ {

	@Inject
	Visor visor;

	public Visor getVisor() {
		return visor;
	}

	@Override
	public void initialise() {
	}

	@Override
	public void start() {
		visor.startUp();

	}

	@Override
	public void stop() {
		visor.shutdown();
		// System.exit(0);
	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		visor.showException(exception);
	}
}
