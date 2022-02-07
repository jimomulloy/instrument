package jomu.instrument;

import net.beadsproject.beads.core.*;

/**
 * Hello world!
 *
 */
public class Main {
	public static void main(String[] args) {
		System.out.println("Hello World!");
		Instrument instrument = Instrument.getInstance();
		instrument.initialise();
	}
}
