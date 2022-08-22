package jomu.instrument.tonemap.old;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class handles the User Interface functions for the Tuner SubSystem of
 * the ToneMap
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public class TunerPanel extends JPanel implements ToneMapConstants {

	class FilterControl extends JPanel {

		public FilterControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			normalizeS = new TmSlider(JSlider.HORIZONTAL, 1, 1000, 100, "Normalisation", new FilterSliderListener());
			undertoneS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Undertone", new FilterSliderListener());
			droneS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Drone", new FilterSliderListener());
			spikeS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Spike", new FilterSliderListener());
			droneS.setEnabled(false);
			spikeS.setEnabled(false);

			add(normalizeS);
			add(undertoneS);
			add(droneS);
			add(spikeS);
		}
	}

	class FilterSliderListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			TmSlider slider = (TmSlider) e.getSource();
			int value = slider.getValue();
			String s = slider.getName();
			if (s.startsWith("Harmonic 1")) {
				tunerModel.harmonic1Setting = value;
			} else if (s.startsWith("Harmonic 2")) {
				tunerModel.harmonic2Setting = value;
			} else if (s.startsWith("Harmonic 3")) {
				tunerModel.harmonic3Setting = value;
			} else if (s.startsWith("Harmonic 4")) {
				tunerModel.harmonic4Setting = value;
			} else if (s.startsWith("Formant low")) {
				tunerModel.formantLowSetting = value;
			} else if (s.startsWith("Formant middle")) {
				tunerModel.formantMiddleSetting = value;
			} else if (s.startsWith("Formant high")) {
				tunerModel.formantHighSetting = value;
			} else if (s.startsWith("Formant Factor")) {
				tunerModel.formantFactor = value;
			} else if (s.startsWith("Drone")) {
				tunerModel.droneSetting = value;
			} else if (s.startsWith("Undertone")) {
				tunerModel.undertoneSetting = value;
			} else if (s.startsWith("Spike")) {
				tunerModel.spikeSetting = value;
			} else if (s.startsWith("Normalisation")) {
				tunerModel.normalizeSetting = value;
			}

		}
	}

	class FormantControl extends JPanel {

		class FormantCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("Add")) {
						tunerModel.formantAdd = cb.isSelected();
					}
				}
			}
		}

		public FormantControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			formantLowS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 0, "Formant low", new FilterSliderListener());
			formantMiddleS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 50, "Formant middle", new FilterSliderListener());
			formantHighS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Formant high", new FilterSliderListener());
			formantFreqS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 0, "Formant Factor", new FilterSliderListener());

			formantACB = new JCheckBox("Add");
			formantACB.addItemListener(new FormantCBListener());

			add(formantLowS);
			add(formantMiddleS);
			add(formantHighS);
			add(formantFreqS);
			add(formantACB);

		}

	}

	class HarmonicControl extends JPanel {

		class HarmonicCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("Add")) {
						tunerModel.harmonicAdd = cb.isSelected();
					}
				}
			}
		}

		public HarmonicControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			harmonic1S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Harmonic 1", new FilterSliderListener());
			harmonic2S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Harmonic 2", new FilterSliderListener());
			harmonic3S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Harmonic 3", new FilterSliderListener());
			harmonic4S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Harmonic 4", new FilterSliderListener());

			harmonicACB = new JCheckBox("Add");
			harmonicACB.addItemListener(new HarmonicCBListener());

			add(harmonic1S);
			add(harmonic2S);
			add(harmonic3S);
			add(harmonic4S);
			add(harmonicACB);

		}

	}

	class ModeControl extends JPanel {

		class FilterBoxItemListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("Harmonic")) {
						tunerModel.harmonicSwitch = cb.isSelected();
					} else if (name.startsWith("Formant")) {
						tunerModel.formantSwitch = cb.isSelected();
					} else if (name.startsWith("Undertone")) {
						tunerModel.undertoneSwitch = cb.isSelected();
					} else if (name.startsWith("Peak")) {
						tunerModel.peakSwitch = cb.isSelected();
					} else if (name.startsWith("Drone")) {
						tunerModel.droneSwitch = cb.isSelected();
					} else if (name.startsWith("Spike")) {
						tunerModel.spikeSwitch = cb.isSelected();
					} else if (name.startsWith("Normalize")) {
						tunerModel.normalizeSwitch = cb.isSelected();
					}

				}
			}

		}

		class ModeControlListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {

				String s = e.getActionCommand();
				if (s.startsWith("Note")) {
					tunerModel.processMode = NOTE_MODE;
				} else if (s.startsWith("Beat")) {
					tunerModel.processMode = BEAT_MODE;
				} else if (s.startsWith("Chirp")) {
					tunerModel.processMode = CHIRP_MODE;
				} else if (s.startsWith("Chord")) {
					tunerModel.processMode = CHORD_MODE;
				}
			}
		}

		public ModeControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Mode");
			setBorder(tb);

			noteModeB = new JRadioButton("Note");
			noteModeB.setActionCommand("Note");
			noteModeB.setSelected(true);

			beatModeB = new JRadioButton("Beat");
			beatModeB.setActionCommand("Beat");
			beatModeB.setSelected(false);
			beatModeB.setEnabled(true);

			chirpModeB = new JRadioButton("Chirp");
			chirpModeB.setActionCommand("Chirp");
			chirpModeB.setSelected(false);
			chirpModeB.setEnabled(false);

			chordModeB = new JRadioButton("Chord");
			chordModeB.setActionCommand("Chord");
			chordModeB.setSelected(false);
			chordModeB.setEnabled(false);

			ButtonGroup group = new ButtonGroup();
			group.add(noteModeB);
			group.add(beatModeB);
			group.add(chirpModeB);
			group.add(chordModeB);

			ModeControlListener modeControlListener = new ModeControlListener();
			noteModeB.addActionListener(modeControlListener);
			beatModeB.addActionListener(modeControlListener);
			chirpModeB.addActionListener(modeControlListener);
			chordModeB.addActionListener(modeControlListener);

			JPanel radioPanel = new JPanel();
			radioPanel.setLayout(new GridLayout(1, 0));
			radioPanel.add(noteModeB);
			radioPanel.add(beatModeB);
			radioPanel.add(chirpModeB);
			radioPanel.add(chordModeB);

			add(radioPanel);

			harmonicCB = new JCheckBox("Harmonic");
			harmonicCB.addItemListener(new FilterBoxItemListener());
			add(harmonicCB);

			formantCB = new JCheckBox("Formant");
			formantCB.addItemListener(new FilterBoxItemListener());
			add(formantCB);

			undertoneCB = new JCheckBox("Undertone");
			undertoneCB.addItemListener(new FilterBoxItemListener());
			add(undertoneCB);

			normalizeCB = new JCheckBox("Normalize");
			normalizeCB.addItemListener(new FilterBoxItemListener());
			add(normalizeCB);

			peakCB = new JCheckBox("Peak");
			peakCB.addItemListener(new FilterBoxItemListener());
			add(peakCB);

			droneCB = new JCheckBox("Drone");
			droneCB.addItemListener(new FilterBoxItemListener());
			droneCB.setEnabled(false);
			add(droneCB);

			spikeCB = new JCheckBox("Spike");
			spikeCB.addItemListener(new FilterBoxItemListener());
			spikeCB.setEnabled(false);
			add(spikeCB);

		}

	}

	class NormalizeControl extends JPanel {

		class NormalizeCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("N1")) {
						tunerModel.n1Switch = cb.isSelected();
					} else if (name.startsWith("N2")) {
						tunerModel.n2Switch = cb.isSelected();
					}
				}

			}
		}

		class NormalizeSListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String s = slider.getName();
				if (s.startsWith("N1")) {
					tunerModel.n1Setting = value;
				} else if (s.startsWith("N2")) {
					tunerModel.n2Setting = value;
				} else if (s.startsWith("N3")) {
					tunerModel.n3Setting = value;
				} else if (s.startsWith("N4")) {
					tunerModel.n4Setting = value;
				} else if (s.startsWith("N5")) {
					tunerModel.n5Setting = value;
				} else if (s.startsWith("N6")) {
					tunerModel.n6Setting = value;
				}
			}
		}

		public NormalizeControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			n1S = new TmSlider(JSlider.HORIZONTAL, 1, 100, 100, "N1", new NormalizeSListener());
			n2S = new TmSlider(JSlider.HORIZONTAL, 1, 100, 100, "N2", new NormalizeSListener());
			n3S = new TmSlider(JSlider.HORIZONTAL, 1, 100, 100, "N3", new NormalizeSListener());
			n4S = new TmSlider(JSlider.HORIZONTAL, 1, 100, 100, "N4", new NormalizeSListener());
			n5S = new TmSlider(JSlider.HORIZONTAL, 1, 100, 100, "N5", new NormalizeSListener());
			n6S = new TmSlider(JSlider.HORIZONTAL, 1, 100, 100, "N6", new NormalizeSListener());

			n1CB = new JCheckBox("N1");
			n1CB.addItemListener(new NormalizeCBListener());
			n2CB = new JCheckBox("N2");
			n2CB.addItemListener(new NormalizeCBListener());

			add(n1S);
			add(n2S);
			add(n3S);
			add(n4S);
			add(n5S);
			add(n6S);
			add(n1CB);
			add(n2CB);

		}

	}

	class NoteControl extends JPanel {

		class NoteSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String s = slider.getName();
				if (s.startsWith("Low")) {
					tunerModel.noteLow = value;
				} else if (s.startsWith("High")) {
					tunerModel.noteHigh = value;
				} else if (s.startsWith("Sus")) {
					tunerModel.noteSustain = value;
				} else if (s.startsWith("Min")) {
					tunerModel.noteMinDuration = value;
				} else if (s.startsWith("Max")) {
					tunerModel.noteMaxDuration = value;
				}
			}
		}

		public NoteControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Note Scan");
			setBorder(tb);

			noteLowSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, INIT_NOTE_LOW, "Low Threshhold",
					new NoteSliderListener());
			add(noteLowSlider);

			noteHighSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, INIT_NOTE_HIGH, "High Threshhold",
					new NoteSliderListener());
			add(noteHighSlider);
			noteSustainSlider = new TmSlider(JSlider.HORIZONTAL, 0, 1000, INIT_NOTE_SUSTAIN, "Sustain Time",
					new NoteSliderListener());
			add(noteSustainSlider);
			noteMinDurationSlider = new TmSlider(JSlider.HORIZONTAL, 0, 1000, INIT_NOTE_MIN_DURATION, "Min Duration",
					new NoteSliderListener());
			add(noteMinDurationSlider);
			noteMaxDurationSlider = new TmSlider(JSlider.HORIZONTAL, 100, 10000, INIT_NOTE_MAX_DURATION, "Max Duration",
					new NoteSliderListener());
			add(noteMaxDurationSlider);

		}

	}

	class PitchControlListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			tunerModel.pitchLow = pitchControl.getPitchLow();
			tunerModel.pitchHigh = pitchControl.getPitchHigh();

		}

	}

	class TimeControlListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			tunerModel.timeStart = (double) timeControl.getTimeStart();
			tunerModel.timeEnd = (double) timeControl.getTimeEnd();

		}

	}

	private TunerModel tunerModel;

	public TimeControl timeControl;
	public PitchControl pitchControl;
	public ModeControl modeControl;
	public NoteControl noteControl;
	public HarmonicControl harmonicControl;
	public FormantControl formantControl;
	public FilterControl filterControl;
	public TmSlider noteLowSlider;
	public TmSlider noteHighSlider;
	public TmSlider noteSustainSlider;
	public TmSlider noteMinDurationSlider;
	public TmSlider noteMaxDurationSlider;
	public TmSlider harmonic1S, n1S, n2S, n3S, n4S, n5S, n6S;
	public TmSlider harmonic2S;
	public TmSlider harmonic3S;
	public TmSlider harmonic4S;
	public TmSlider formantLowS;
	public TmSlider formantMiddleS;
	public TmSlider formantHighS;
	public TmSlider formantFreqS;
	public TmSlider droneS;
	public TmSlider undertoneS;
	public TmSlider normalizeS;
	public TmSlider spikeS;
	public JCheckBox harmonicCB, formantCB, droneCB, undertoneCB, peakCB, spikeCB, normalizeCB, n1CB, n2CB, harmonicACB,
			formantACB;
	public JRadioButton noteModeB, beatModeB, chirpModeB, chordModeB;

	public TunerPanel(TunerModel tunerModel) {

		this.tunerModel = tunerModel;

		setLayout(new BorderLayout());

		EmptyBorder eb = new EmptyBorder(5, 5, 5, 5);
		BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
		CompoundBorder cb = new CompoundBorder(eb, bb);
		setBorder(new CompoundBorder(cb, eb));

		JPanel p0 = new JPanel();
		p0.setLayout(new BoxLayout(p0, BoxLayout.Y_AXIS));

		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));

		JPanel p2 = new JPanel();
		// p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));

		JPanel p3 = new JPanel();
		p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));

		JPanel p4 = new JPanel();
		p4.setLayout(new BoxLayout(p4, BoxLayout.X_AXIS));

		JPanel p5 = new JPanel();
		p5.setLayout(new BoxLayout(p5, BoxLayout.X_AXIS));

		JPanel p6 = new JPanel();
		p6.setLayout(new BoxLayout(p6, BoxLayout.X_AXIS));

		p1.add(timeControl = new TimeControl(new TimeControlListener()));
		p1.add(pitchControl = new PitchControl(new PitchControlListener()));
		p2.add(modeControl = new ModeControl());
		p3.add(noteControl = new NoteControl());
		p4.add(harmonicControl = new HarmonicControl());
		p5.add(formantControl = new FormantControl());
		p6.add(filterControl = new FilterControl());

		p0.add(p3);
		p0.add(p4);
		p0.add(p5);
		p0.add(p6);
		p0.add(new NormalizeControl());

		p0.add(p1);

		add(p0, BorderLayout.CENTER);
		add(p2, BorderLayout.NORTH);

	}
}