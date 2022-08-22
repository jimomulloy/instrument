package jomu.instrument.tonemap.old;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.text.NumberFormat;

import javax.swing.JComponent;

/**
 * This class is a GUI component that acts as a Ruler displaying the vertical
 * Time coordinate values for the Tone Map Scrollable View Pane on the
 * ToneMapPanel UI.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public class TimeRuler extends JComponent {

	private int thickness;
	private int units;
	private double increment;
	private int minTime, maxTime;
	private int maxLength;

	public TimeRuler(int length, int thickness) {
		this.thickness = thickness;
		setPreferredSize(new Dimension(length, thickness));
	}

	public void paintComponent(Graphics g) {

		Rectangle drawHere = g.getClipBounds();

		double end = 0;
		double start = 0;
		int tickLength = 0;
		String text = null;
		NumberFormat nf = NumberFormat.getNumberInstance();

		if ((maxTime - minTime) > 0) {

			double screenFactor = 100.0;
			double timeFactor = 1000.0;
			nf.setMaximumFractionDigits(0);
			nf.setMinimumFractionDigits(0);
			nf.setMaximumIntegerDigits(2);
			nf.setMinimumIntegerDigits(1);

			increment = (double) getPreferredSize().width / (double) ((maxTime - minTime) / screenFactor);
			if (increment < 10) {
				screenFactor = 1000.0;
				timeFactor = 1000.0;
				nf.setMaximumFractionDigits(0);
				nf.setMinimumFractionDigits(0);
				nf.setMaximumIntegerDigits(2);
				nf.setMinimumIntegerDigits(1);
				increment = (double) getPreferredSize().width / (double) ((maxTime - minTime) / screenFactor);
			}

			// Use clipping bounds to calculate first tick and last tick location.
			start = (drawHere.x / increment) * increment;
			end = (((drawHere.x + drawHere.width) / increment) + 1) * increment;

			if (start == 0) {
				text = Integer.toString(0) + (" s.");
				tickLength = 10;
				g.drawLine(0, thickness - 1, 0, thickness - tickLength - 1);
				g.drawString(text, 1, 15);
				text = null;
				start = increment;
			}

			// ticks and labels

			double time = 0;
			for (double i = start; i < end; i += increment) {

				int position = (int) Math.ceil(i);
				time = minTime + (Math.ceil(i / increment) * screenFactor);

				if ((int) time % timeFactor == 0) {
					tickLength = 5;
					text = nf.format(time / timeFactor);

				} else {
					tickLength = 10;
					text = null;
				}

				if (tickLength != 0) {
					g.drawLine(position, thickness - 1, position, thickness - tickLength - 1);
					if (text != null) {
						g.drawString(text, position + 1, 15);
					}
				}
			}
		}
	}

	public void setLimits(double minTime, double maxTime) {
		this.minTime = (int) (minTime);
		this.maxTime = (int) (maxTime);
	}
}