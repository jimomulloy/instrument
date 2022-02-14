package jomu.instrument.tonemap;

/**
 * This is the main driver class for ToneMap Application
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMapMain {

	public static void main(String[] args) {

		try {
			// UIManager.setLookAndFeel(
			// "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			// javax.swing.LookAndFeel alloyLnF = new
			// com.incors.plaf.kunststoff.KunststoffLookAndFeel();
			// javax.swing.UIManager.setLookAndFeel(alloyLnF);

		} catch (Exception e) {
		}

		final ToneMapFrame TMF = new ToneMapFrame();

	}
}