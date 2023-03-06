package jomu.instrument.control;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jomu.instrument.Organ;
import jomu.instrument.actuation.Voice;
import jomu.instrument.cognition.Cortex;
import jomu.instrument.perception.Hearing;

@ApplicationScoped
@Component
public class Coordinator implements Organ {

	@Autowired
	Cortex cortex;

	@Autowired
	Hearing hearing;

	@Autowired
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
