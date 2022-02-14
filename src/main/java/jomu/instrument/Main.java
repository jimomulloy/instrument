package jomu.instrument;

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
