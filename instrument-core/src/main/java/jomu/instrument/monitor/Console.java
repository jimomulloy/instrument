package jomu.instrument.monitor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.Organ;

@ApplicationScoped
public class Console implements Organ {

	@Inject
	Visor visor;

	// public OscilloscopeEventHandler getOscilloscopeHandler() {
	// return visor;
	// }

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
		System.exit(0);
	}
}
