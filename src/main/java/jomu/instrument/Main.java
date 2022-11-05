package jomu.instrument;

import java.lang.reflect.InvocationTargetException;

import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterContrastIJTheme;

/**
 * Hello world!
 *
 */
public class Main {
	public static void main(String[] args)
			throws InvocationTargetException, InterruptedException {
		FlatMaterialDesignDarkIJTheme.setup();
		FlatMaterialLighterContrastIJTheme.setUseNativeWindowDecorations(true);
		Instrument instrument = Instrument.getInstance();
		instrument.initialise();
	}
}
