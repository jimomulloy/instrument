package jomu.instrument.control;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.Organ;
import jomu.instrument.actuation.Voice;
import jomu.instrument.cognition.Cortex;
import jomu.instrument.perception.Hearing;

@ApplicationScoped
public class Coordinator implements Organ {

	@Inject
	Cortex cortex;

	@Inject
	Hearing hearing;

	@Inject
	Voice voice;

	public Cortex getCortex() {
		return cortex;
	}

	public Hearing getHearing() {
		return hearing;
	}

	public Voice getVoice() {
		return voice;
	}

	@Override
	public void initialise() {
		cortex.initialise();
		hearing.initialise();
		voice.initialise();
	}

	@Override
	public void start() {
		cortex.start();
		hearing.start();
		voice.start();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
