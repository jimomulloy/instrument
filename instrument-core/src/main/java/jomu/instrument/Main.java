package jomu.instrument;

/**
 *
 */
public class Main {
	public static void main(final String[] args) {
		final Instrument instrument = Instrument.getInstance();
		instrument.initialise();
		instrument.start();
	}
}
