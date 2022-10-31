package jomu.instrument.organs;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import be.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;
import jomu.instrument.ui.Visor;

public class Druid {

	private Visor visor;

	public OscilloscopeEventHandler getOscilloscopeHandler() {
		return visor;
	}

	public Visor getVisor() {
		return visor;
	}

	public void initialise() {
		visor = new Visor();
	}

	public void start() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				/**
				 *
				 */
				@Override
				public void run() {

					JFrame frame = new JFrame();
					/*try {
					    UIManager.setLookAndFeel(new NimbusLookAndFeel());
					    UIManager.put("control", new Color(128, 128, 128));
					    UIManager.put("info", new Color(128, 128, 128));
					    UIManager.put("nimbusBase", new Color(18, 30, 49));
					    UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
					    UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
					    UIManager.put("nimbusFocus", new Color(115, 164, 209));
					    UIManager.put("nimbusGreen", new Color(176, 179, 50));
					    UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
					    UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
					    UIManager.put("nimbusOrange", new Color(191, 98, 4));
					    UIManager.put("nimbusRed", new Color(169, 46, 34));
					    UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
					    UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
					    UIManager.put("text", new Color(230, 230, 230));
					    SwingUtilities.updateComponentTreeUI(frame);
					} catch (UnsupportedLookAndFeelException exc) {
					    System.err.println("Nimbus: Unsupported Look and feel!");
					}*/

					frame.setDefaultCloseOperation(
							WindowConstants.EXIT_ON_CLOSE);
					frame.setTitle("Druid");
					frame.add(visor);
					frame.pack();
					Dimension screenSize = Toolkit.getDefaultToolkit()
							.getScreenSize();
					frame.setSize(screenSize.width / 2, screenSize.height / 2);
					frame.setVisible(true);
					frame.setExtendedState(
							frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
