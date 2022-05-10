package jomu.instrument.organs;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import be.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;

public class Druid {

	private Visor visor;

	public void initialise() {
		visor = new Visor();
	}

	public void start() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					JFrame frame = new JFrame();
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.setTitle("Druid");
					frame.add(visor);
					frame.pack();
					Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
					frame.setSize(screenSize.width/2, screenSize.height/2);
					frame.setVisible(true);
				    frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
					
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public OscilloscopeEventHandler getOscilloscopeHandler() {
		return visor;
	}

	public Visor getVisor() {
		return visor;
	}
}
