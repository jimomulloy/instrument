package jomu.instrument.organs;

public class Coordinator {
	private Hearing hearing;
	private Voice voice;
	private Cortex cortex;

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
		hearing = new Hearing();
		hearing.initialise();
		voice = new Voice();
		voice.initialise();
		cortex = new Cortex();
		cortex.initialise();
		hearing.start();
		cortex.start();
		voice.start();
	}

	public void start() {
		hearing.start();
	}
}
