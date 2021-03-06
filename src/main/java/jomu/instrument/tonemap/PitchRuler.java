package jomu.instrument.tonemap;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;

/**
 * This class is a GUI component that acts as a Ruler displaying the vertical
 * Pitch coordinate values for the Tone Map Scrollable View Pane on the
 * ToneMapPanel UI.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public class PitchRuler extends JComponent {

	private int thickness;
	private int units;
	private double increment;
	private int minPitch, maxPitch;
	private int maxLength;

	public PitchRuler(int thickness, int length) {
		this.thickness = thickness;
		setPreferredSize(new Dimension(thickness, length));
	}

	public void paintComponent(Graphics g) {

		Rectangle drawHere = g.getClipBounds();

		// g.setFont(new Font("SansSerif", Font.PLAIN, 10));
		// g.setColor(Color.black);

		// Some vars we need.
		double end = 0;
		double start = 0;
		int tickLength = 0;
		String text = null;
		if ((maxPitch - minPitch) > 0) {

			increment = (double) getPreferredSize().height / (double) (maxPitch - minPitch);
			int noteFactor = 1;
			if (increment < 12)
				noteFactor = 12;

			// Use clipping bounds to calculate first tick and last tick location.
			start = (drawHere.y / increment) * increment;
			end = (((drawHere.y + drawHere.height) / increment) + 1) * increment;

			// ticks and labels

			int note = 0;
			for (double i = start; i < end; i += increment) {

				int position = (int) Math.ceil(i);
				note = maxPitch - (int) (i / increment);

				if (note % noteFactor == 0) {
					tickLength = 3;
					text = PitchSet.MidiNoteToSymbol(note).toString();

				} else {
					tickLength = 10;
					text = null;
				}

				if (tickLength != 0) {
					g.drawLine(thickness - 1, position, thickness - tickLength - 1, position);
					if (text != null) {
						g.drawString(text, 1, position + 3);
					}
				}
			}
		}
	}

	public void setLimits(int minPitch, int maxPitch) {
		this.minPitch = minPitch;
		this.maxPitch = maxPitch;
	}
}