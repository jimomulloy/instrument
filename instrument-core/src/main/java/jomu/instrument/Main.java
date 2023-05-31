package jomu.instrument;

/**
 *
 */
public class Main {
	public static void main(String[] args) {
		Instrument instrument = Instrument.getInstance();
		instrument.initialise();
		instrument.start();
	}
}
