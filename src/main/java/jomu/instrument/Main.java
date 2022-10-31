package jomu.instrument;

import java.lang.reflect.InvocationTargetException;

/**
 * Hello world!
 *
 */
public class Main {
	public static void main(String[] args)
			throws InvocationTargetException, InterruptedException {
		Instrument instrument = Instrument.getInstance();
		instrument.initialise();
	}
}
