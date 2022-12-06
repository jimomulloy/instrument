package jomu.instrument.control;

import jomu.instrument.Organ;
import jomu.instrument.actuation.Voice;
import jomu.instrument.cognition.Cortex;
import jomu.instrument.perception.Hearing;

public class Coordinator implements Organ {
	private Cortex cortex;
	private Hearing hearing;
	private Voice voice;

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
		cortex = new Cortex();
		cortex.initialise();
		hearing = new Hearing();
		hearing.initialise();
		voice = new Voice();
		voice.initialise();
		cortex.start();
		hearing.start();
		voice.start();
	}

	@Override
	public void start() {
		hearing.start();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
