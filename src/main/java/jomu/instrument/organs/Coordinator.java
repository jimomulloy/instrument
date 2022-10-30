package jomu.instrument.organs;

public class Coordinator {
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

	public void start() {
		hearing.start();
	}
}
