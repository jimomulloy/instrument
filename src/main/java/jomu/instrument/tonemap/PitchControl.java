package jomu.instrument.tonemap;

import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class manages a pair of Pitch Range Settings "low" and "high" GUI
 * ontrols in the form of TMSlider controls
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public class PitchControl extends JPanel implements ToneMapConstants {

	class PitchSliderListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			TmSlider slider = (TmSlider) e.getSource();
			int value = slider.getValue();
			String name = slider.getName();
			if (name.startsWith("Pitch Low")) {
				pitchLow = value;
				if (pitchLow > pitchHigh)
					pitchHighSlider.setValue(pitchLow);
				if ((pitchHigh - pitchLow) > PITCH_RANGE_MAX)
					pitchHighSlider.setValue(pitchLow + PITCH_RANGE_MAX);

			} else if (name.startsWith("Pitch High")) {
				pitchHigh = value;
				if (pitchLow > pitchHigh)
					pitchLowSlider.setValue(pitchHigh);
				if ((pitchHigh - pitchLow) > PITCH_RANGE_MAX)
					pitchLowSlider.setValue(pitchHigh - PITCH_RANGE_MAX);

			}
			setTitle(slider, value);
			listener.stateChanged(e);
		}
	}

	private int pitchLow = INIT_PITCH_LOW;
	private int pitchHigh = INIT_PITCH_HIGH;
	private int pitchMin = INIT_PITCH_MIN;
	private int pitchMax = INIT_PITCH_MAX;
	private int pitchInc = INIT_PITCH_INC;
	private ChangeListener listener;
	private Hashtable labelTable;
	private TmSlider pitchLowSlider, pitchHighSlider;

	public PitchControl(ChangeListener listener) {

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.listener = listener;
		pitchLowSlider = new TmSlider(JSlider.HORIZONTAL, pitchMin, pitchMax, pitchLow, "Pitch Low",
				new PitchSliderListener());

		pitchHighSlider = new TmSlider(JSlider.HORIZONTAL, pitchMin, pitchMax, pitchHigh, "Pitch High",
				new PitchSliderListener());

		labelTable = new Hashtable();
		labelTable.put(new Integer(pitchMin), new JLabel(PitchSet.MidiNoteToSymbol(pitchMin).toString()));
		labelTable.put(new Integer(pitchMax), new JLabel(PitchSet.MidiNoteToSymbol(pitchMax).toString()));

		setTitle(pitchLowSlider, pitchLow);
		setTitle(pitchHighSlider, pitchHigh);

		pitchLowSlider.setPaintLabels(true);
		pitchLowSlider.setLabelTable(labelTable);

		pitchHighSlider.setPaintLabels(true);
		pitchHighSlider.setLabelTable(labelTable);

		add(pitchLowSlider);
		add(pitchHighSlider);

	}

	public int getPitchHigh() {
		return pitchHigh;
	}

	public int getPitchLow() {
		return pitchLow;
	}

	public void setPitchHigh(int pitchHigh) {
		this.pitchHigh = pitchHigh;
		pitchHighSlider.setValue(pitchHigh);
	}

	public void setPitchLow(int pitchLow) {
		this.pitchLow = pitchLow;
		pitchLowSlider.setValue(pitchLow);
	}

	public void setPitchRange(int min, int max) {

		pitchMax = max;
		pitchMin = min;
		pitchLowSlider.setPaintLabels(false);
		pitchLowSlider.setMaximum(pitchMax);
		pitchLowSlider.setMinimum(pitchMin);
		pitchLowSlider.setValue(pitchMin);
		pitchHighSlider.setPaintLabels(false);
		pitchHighSlider.setMaximum(pitchMax);
		pitchHighSlider.setMinimum(pitchMin);
		pitchHighSlider.setValue(pitchMax);

		labelTable = new Hashtable();
		labelTable.put(new Integer(pitchMin), new JLabel(PitchSet.MidiNoteToSymbol(pitchMin).toString()));
		labelTable.put(new Integer(pitchMax), new JLabel(PitchSet.MidiNoteToSymbol(pitchMax).toString()));
		setTitle(pitchLowSlider, pitchLow);
		setTitle(pitchHighSlider, pitchHigh);
		pitchLowSlider.setPaintLabels(true);
		pitchLowSlider.setLabelTable(labelTable);
		pitchHighSlider.setPaintLabels(true);
		pitchHighSlider.setLabelTable(labelTable);

	}

	private void setTitle(TmSlider slider, int value) {

		TitledBorder tb = (TitledBorder) slider.getBorder();
		String s = tb.getTitle();
		tb.setTitle(s.substring(0, s.indexOf('=') + 1) + PitchSet.MidiNoteToSymbol(value).toString());

	}

}