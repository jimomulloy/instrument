package jomu.instrument.organs;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import be.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;

public class Druid {

	private Visor visor;

	public void initialise() {
		visor = new Visor();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					JFrame frame = visor;
					frame.pack();
					frame.setSize(1500, 1000);
					frame.setVisible(true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void start() {
	}

	public OscilloscopeEventHandler getOscilloscopeHandler() {
		return visor;
	}

	public Visor getVisor() {
		return visor;
	}
}
