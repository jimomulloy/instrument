package jomu.instrument.tonemap.old;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class handles the User Interface functions for the Midi Sub system of
 * the ToneMap
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public class MidiPanel extends JPanel implements ToneMapConstants {

	class BPMControl extends JPanel {

		class BPMSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				midiModel.bpmSetting = value;

			}

		}

		public BPMControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			bpmSlider = new TmSlider(JSlider.HORIZONTAL, 0, MAX_BPM_SETTING, INIT_BPM_SETTING, "BPM",
					new BPMSliderListener());
			add(bpmSlider);
		}

	}

	class ChannelControl extends JPanel {

		class ControlBoxItemListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
					midiModel.cc = midiModel.channels[combo.getSelectedIndex()];
					midiModel.cc.setComponentStates();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("Mute")) {
						midiModel.cc.channel.setMute(midiModel.cc.mute = cb.isSelected());
					} else if (name.startsWith("Solo")) {
						midiModel.cc.channel.setSolo(midiModel.cc.solo = cb.isSelected());
					} else if (name.startsWith("Mono")) {
						midiModel.cc.channel.setMono(midiModel.cc.mono = cb.isSelected());
					} else if (name.startsWith("Sustain")) {
						midiModel.cc.sustain = cb.isSelected();
						midiModel.cc.channel.controlChange(midiModel.SUSTAIN, midiModel.cc.sustain ? 127 : 0);
					}
				}
			}

		}

		class ControlButtonListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				if (button.getText().startsWith("Off")) {
					for (int i = 0; i < midiModel.channels.length; i++) {
						midiModel.channels[i].channel.allNotesOff();
					}

				}
			}
		}

		class ControlSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {
				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String name = slider.getName();
				if (name.startsWith("Velocity")) {
					midiModel.cc.velocity = value;
				} else if (name.startsWith("Pressure")) {
					midiModel.cc.channel.setChannelPressure(midiModel.cc.pressure = value);
				} else if (name.startsWith("Bend")) {
					midiModel.cc.channel.setPitchBend(midiModel.cc.bend = value);
				} else if (name.startsWith("Reverb")) {
					midiModel.cc.channel.controlChange(midiModel.REVERB, midiModel.cc.reverb = value);
				}
				slider.repaint();
			}
		}

		class InstrumentCBListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				int value = instrumentCB.getSelectedIndex();
				midiModel.programChange(value);
			}

		}

		public ChannelControl() {

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

			veloS = new TmSlider(JSlider.HORIZONTAL, 0, 127, INIT_VELOCITY_SETTING, "Velocity",
					new ControlSliderListener());
			presS = new TmSlider(JSlider.HORIZONTAL, 0, 127, INIT_PRESSURE_SETTING, "Pressure",
					new ControlSliderListener());
			revbS = new TmSlider(JSlider.HORIZONTAL, 0, 127, INIT_REVERB_SETTING, "Reverb",
					new ControlSliderListener());
			bendS = new TmSlider(JSlider.HORIZONTAL, 0, 16383, INIT_BEND_SETTING, "Bend", new ControlSliderListener());
			p.add(veloS);
			p.add(presS);
			p.add(revbS);
			p.add(bendS);
			add(p);

			p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

			JComboBox combo = new JComboBox();
			combo.setPreferredSize(new Dimension(120, 25));
			combo.setMaximumSize(new Dimension(120, 25));
			for (int i = 1; i <= 16; i++) {
				combo.addItem("Channel " + String.valueOf(i));
			}
			combo.addItemListener(new ControlBoxItemListener());
			p.add(combo);
			muteCB = createCheckBox("Mute", p);
			soloCB = createCheckBox("Solo", p);
			monoCB = createCheckBox("Mono", p);
			sustCB = createCheckBox("Sustain", p);

			createButton("Off", p);

			String[] instrumentNames = null;
			if (midiModel.instruments != null && midiModel.instruments.length != 0) {
				instrumentNames = new String[midiModel.instruments.length];
				for (int i = 0; i < midiModel.instruments.length; i++) {
					instrumentNames[i] = midiModel.instruments[i].getName();
				}
			} else {
				instrumentNames = new String[128];
				for (int i = 0; i < 128; i++) {
					instrumentNames[i] = Integer.toString(i);
				}
			}
			instrumentCB = new JComboBox(instrumentNames);
			instrumentCB.setPreferredSize(new Dimension(120, 25));
			instrumentCB.setMaximumSize(new Dimension(120, 25));
			instrumentCB.setSelectedIndex(0);
			instrumentCB.addActionListener(new InstrumentCBListener());
			p.add(instrumentCB);
			JLabel instrumentLabel = new JLabel("Instruments", JLabel.LEFT);
			p.add(instrumentLabel);
			add(p);
		}

		public JButton createButton(String name, JPanel p) {
			JButton b = new JButton(name);
			b.addActionListener(new ControlButtonListener());
			p.add(b);
			return b;
		}

		private JCheckBox createCheckBox(String name, JPanel p) {
			JCheckBox cb = new JCheckBox(name);
			cb.addItemListener(new ControlBoxItemListener());
			p.add(cb);
			return cb;
		}

	}

	class FileControl extends JPanel {

		class OpenBAction implements ActionListener {

			public void actionPerformed(ActionEvent evt) {

				try {
					File file = new File(System.getProperty("user.dir"));
					JFileChooser fc = new JFileChooser(file);
					fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory()) {
								return true;
							}
							String name = f.getName();
							if (name.endsWith(".au") || name.endsWith(".wav") || name.endsWith(".aiff")
									|| name.endsWith(".aif")) {
								return true;
							}
							return false;
						}

						public String getDescription() {
							return ".au, .wav, .aif";
						}
					});

					if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						file = fc.getSelectedFile();
						// load();
					}
				} catch (SecurityException ex) {
					// JavaSound.showInfoDialog();
					ex.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		}

		class SaveBAction implements ActionListener {

			public void actionPerformed(ActionEvent evt) {
				if (!save())
					return;
			}
		}

		public FileControl() {

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setPreferredSize(new Dimension(200, 150));
			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("File");
			setBorder(tb);

			JPanel bp = new JPanel();
			JPanel ip = new JPanel();
			ip.setLayout(new BoxLayout(ip, BoxLayout.X_AXIS));
			JPanel ipHeadings = new JPanel();
			ipHeadings.setLayout(new GridLayout(5, 1));
			JPanel ipFields = new JPanel();
			ipFields.setLayout(new GridLayout(5, 1));

			openB = new JButton("Open");
			openB.setEnabled(false);
			bp.add(openB);

			saveB = new JButton("Save");
			saveB.setEnabled(true);
			bp.add(saveB);

			openB.addActionListener(new OpenBAction());
			saveB.addActionListener(new SaveBAction());

			JLabel fileNameHeading = new JLabel("Name:", JLabel.LEFT);
			JLabel durationHeading = new JLabel("Duration:", JLabel.LEFT);
			JLabel sampleRateHeading = new JLabel("SampleRate:", JLabel.LEFT);
			JLabel bitSizeHeading = new JLabel("Sample Bit Size:", JLabel.LEFT);
			JLabel channelsHeading = new JLabel("Channels:", JLabel.LEFT);

			fileNameField = new JLabel();
			fileNameField.setHorizontalAlignment(JLabel.RIGHT);
			durationField = new JLabel();
			durationField.setHorizontalAlignment(JLabel.RIGHT);
			sampleRateField = new JLabel();
			sampleRateField.setHorizontalAlignment(JLabel.RIGHT);
			bitSizeField = new JLabel();
			bitSizeField.setHorizontalAlignment(JLabel.RIGHT);
			channelsField = new JLabel();
			channelsField.setHorizontalAlignment(JLabel.RIGHT);

			ipHeadings.add(fileNameHeading);
			ipHeadings.add(durationHeading);
			ipHeadings.add(sampleRateHeading);
			ipHeadings.add(bitSizeHeading);
			ipHeadings.add(channelsHeading);

			ipFields.add(fileNameField);
			ipFields.add(durationField);
			ipFields.add(sampleRateField);
			ipFields.add(bitSizeField);
			ipFields.add(channelsField);

			ip.add(ipHeadings);
			ip.add(ipFields);

			add(bp);
			// add(ip);

		}
	}

	class PitchControlListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			midiModel.pitchLow = pitchControl.getPitchLow();
			midiModel.pitchHigh = pitchControl.getPitchHigh();

		}

	}

	class QuantizeControl extends JPanel {

		class QuantizeBCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
					if (combo.getSelectedIndex() == 0) {
						midiModel.quantizeBeatSetting = 0;
					} else {
						midiModel.quantizeBeatSetting = 1.0 / (double) (combo.getSelectedIndex());
					}
				}
			}

		}

		class QuantizeDCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
					if (combo.getSelectedIndex() == 0) {
						midiModel.quantizeDurationSetting = 0;
					} else {
						midiModel.quantizeDurationSetting = 1.0 / (double) (combo.getSelectedIndex());
					}
				}
			}

		}

		public QuantizeControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Quantize Beat");
			p.setBorder(tb);

			String s = null;
			JComboBox combo = new JComboBox();
			combo.setPreferredSize(new Dimension(120, 25));
			combo.setMaximumSize(new Dimension(120, 25));
			combo.addItem("0");
			for (int i = 16; i >= 1; i--) {
				s = "1/" + i;
				combo.addItem(s);
			}
			combo.addItemListener(new QuantizeBCBListener());
			p.add(combo);
			add(p);

			p = new JPanel();

			tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Quantize Duration");
			p.setBorder(tb);
			p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

			combo = new JComboBox();
			combo.setPreferredSize(new Dimension(120, 25));
			combo.setMaximumSize(new Dimension(120, 25));
			combo.addItem("0");
			for (int i = 16; i >= 1; i--) {
				s = "1/" + i;
				combo.addItem(s);
			}
			combo.addItemListener(new QuantizeDCBListener());
			p.add(combo);
			add(p);

		}

	}

	class StatusControl extends JPanel {

		class StatusBAction implements ActionListener {

			public void actionPerformed(ActionEvent evt) {

				midiModel.clear();
				midiModel.open();
			}

		}

		class StatusCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("Level")) {
						midiModel.levelSwitch = cb.isSelected();
					}
				}

			}
		}

		StatusControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			clearB = new JButton("Clear");
			clearB.setEnabled(true);
			clearB.addActionListener(new StatusBAction());
			add(clearB);

			levelCB = new JCheckBox("level");
			levelCB.addItemListener(new StatusCBListener());
			add(levelCB);

		}
	}

	class TimeControlListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			midiModel.timeStart = (double) timeControl.getTimeStart();
			midiModel.timeEnd = (double) timeControl.getTimeEnd();

		}

	}

	class TransControl extends JPanel {

		public TransControl() {

			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Transform");
			setBorder(tb);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setPreferredSize(new Dimension(200, 150));

		}

	}

	private MidiModel midiModel;

	public TimeControl timeControl;
	public PitchControl pitchControl;
	public FileControl fileControl;
	public TransControl transControl;
	public ChannelControl channelControl;
	public BPMControl bpmControl;
	public QuantizeControl quantizeControl;
	public JButton openB, saveB, clearB;
	public TmSlider timeStartSlider;
	public TmSlider timeEndSlider;
	public TmSlider timeIncSlider;
	public TmSlider pitchLowSlider;
	public TmSlider pitchHighSlider;
	public TmSlider volumeSlider;
	public TmSlider panSlider;
	public TmSlider bpmSlider;
	public TmSlider quantizeSlider;
	public JComboBox instrumentCB;
	public TmSlider veloS, presS, bendS, revbS;
	public JCheckBox soloCB, monoCB, muteCB, sustCB, levelCB;
	public JLabel fileNameField;
	public JLabel durationField;
	public JLabel sampleRateField;
	public JLabel bitSizeField;
	public JLabel channelsField;

	public MidiPanel(MidiModel midiModel) {

		this.midiModel = midiModel;

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
		p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));

		JPanel p3 = new JPanel();
		p3.setLayout(new GridLayout(1, 2));

		JPanel p4 = new JPanel();
		p4.setLayout(new BoxLayout(p4, BoxLayout.X_AXIS));

		JPanel p5 = new JPanel();
		p5.setLayout(new BoxLayout(p5, BoxLayout.X_AXIS));

		JPanel p6 = new JPanel();
		p6.setLayout(new BoxLayout(p6, BoxLayout.X_AXIS));

		p1.add(timeControl = new TimeControl(new TimeControlListener()));
		p1.add(pitchControl = new PitchControl(new PitchControlListener()));
		p6.add(new StatusControl());
		p5.add(bpmControl = new BPMControl());
		p3.add(fileControl = new FileControl());
		p3.add(transControl = new TransControl());
		p4.add(channelControl = new ChannelControl());
		p5.add(quantizeControl = new QuantizeControl());

		p0.add(p3);
		p0.add(p4);
		p0.add(p5);
		p0.add(p6);
		p0.add(p1);

		add(p3, BorderLayout.NORTH);
		add(p0, BorderLayout.CENTER);

	}

	public boolean save() {

		try {
			File file = null;
			JFileChooser fc = new JFileChooser(midiModel.toneMapFrame.getDirectory());
			fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					String name = f.getName();
					if (name.endsWith(".mid")) {
						return true;
					}
					return false;
				}

				public String getDescription() {
					return "Save as .mid file.";
				}
			});
			if (fc.showSaveDialog(midiModel.toneMapFrame.mainFrame) == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
				if (!midiModel.saveMidiFile(file))
					return false;
				midiModel.toneMapFrame.setDirectory(fc.getSelectedFile());
				return true;
			} else {
				return false;
			}
		} catch (SecurityException ex) {
			midiModel.toneMapFrame.reportStatus(EC_MIDI_SAVE_BADFILE);
			return false;
		} catch (Exception ex) {
			midiModel.toneMapFrame.reportStatus(EC_MIDI_SAVE_BADFILE);
			return false;
		}
	}
} // End MidiPanel