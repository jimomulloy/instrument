package jomu.instrument.organs;


public class Coordinator {
	private Hearing hearing;
	private Voice voice;
	private Cortex cortex;
	
	public void initialise() {
		hearing = new Hearing();
		hearing.initialise();
		hearing.start();
		voice = new Voice();
		voice.initialise();
		cortex = new Cortex();
		cortex.initialise();
	}
	
	public void start() {
		hearing.start();
	}


}
