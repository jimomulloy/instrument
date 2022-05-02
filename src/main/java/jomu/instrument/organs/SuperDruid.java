package jomu.instrument.organs;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;
import be.tarsos.dsp.ui.LinkedFrame;

public class SuperDruid implements OscilloscopeEventHandler {

	private SuperDruid oscilloscopeHandler;

	public void initialise() {
		oscilloscopeHandler = this;
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					JFrame frame = LinkedFrame.getInstance();
					frame.pack();
					frame.setSize(640, 480);
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
		return oscilloscopeHandler;
	}

	@Override
	public void handleEvent(float[] data, AudioEvent event) {
		// TODO Auto-generated method stub

	}

}
