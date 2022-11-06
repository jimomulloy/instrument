package jomu.instrument;

import java.lang.reflect.InvocationTargetException;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;

/**
 * Hello world!
 *
 */
public class Main {
	public static void main(String[] args)
			throws InvocationTargetException, InterruptedException {
		FlatMaterialDesignDarkIJTheme.setup();
		FlatLaf.setUseNativeWindowDecorations(true);
		Instrument instrument = Instrument.getInstance();
		instrument.initialise();
	}
}
