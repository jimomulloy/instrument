package jomu.instrument.tonemap;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
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
 * This class handles the User Interface functions for the Audio SubSystem of
 * the ToneMap
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class AudioPanel extends JPanel implements ToneMapConstants {

	class TimeControlListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			audioModel.timeStart = (double) timeControl.getTimeStart();
			audioModel.timeEnd = (double) timeControl.getTimeEnd();

		}

	}

	class PitchControlListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			audioModel.pitchLow = pitchControl.getPitchLow();
			audioModel.pitchHigh = pitchControl.getPitchHigh();

		}

	}

	class SampleSizeControl extends JPanel {

		public SampleSizeControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			sampleSizeSlider = new TmSlider(JSlider.HORIZONTAL, MIN_SAMPLE_SIZE, MAX_SAMPLE_SIZE, INIT_SAMPLE_SIZE,
					"SampleSize ms.", new SampleSizeListener());
			add(sampleSizeSlider);

		}

		class SampleSizeListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				JSlider slider = (JSlider) e.getSource();
				int value = slider.getValue();
				audioModel.sampleTimeSize = (double) value;
			}

		}

	}

	class ResolutionControl extends JPanel {

		public ResolutionControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			resolutionSlider = new TmSlider(JSlider.HORIZONTAL, 1, 10, 1, "Sample Resolution",
					new ResolutionListener());
			add(resolutionSlider);

		}

		class ResolutionListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				JSlider slider = (JSlider) e.getSource();
				int value = slider.getValue();
				audioModel.resolution = value;
			}

		}

	}

	class PowerControl extends JPanel {

		public PowerControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			// TitledBorder tb = new TitledBorder(new EtchedBorder());
			// tb.setTitle("Mode");
			// setBorder(tb);

			lowPTS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 0, "Low Threshold", new PowerSListener());
			add(lowPTS);

			highPTS = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "High Threshold", new PowerSListener());
			add(highPTS);

			logCB = new JCheckBox("Log");
			logCB.addItemListener(new PowerCBListener());
			add(logCB);

		}

		class PowerSListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String s = slider.getName();
				if (s.startsWith("Low")) {
					audioModel.powerLow = value;
				} else if (s.startsWith("High")) {
					audioModel.powerHigh = value;
				}
			}
		}

		class PowerCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("Log")) {
						audioModel.logSwitch = cb.isSelected();
					}
				}

			}
		}
	}

	class OscillatorControl extends JPanel {

		public OscillatorControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			// TitledBorder tb = new TitledBorder(new EtchedBorder());
			// tb.setTitle("Mode");
			// setBorder(tb);

			osc1S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 0, "Oscillator 1", new OscSListener());
			add(osc1S);

			osc2S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 0, "Oscillator 2", new OscSListener());
			add(osc2S);

			osc1CB = new JCheckBox("Osc1");
			osc1CB.addItemListener(new OscCBListener());
			add(osc1CB);

			osc2CB = new JCheckBox("Osc2");
			osc2CB.addItemListener(new OscCBListener());
			add(osc2CB);

			osct1B = new JRadioButton("T1");
			osct1B.setActionCommand("T1");
			osct1B.setSelected(true);

			osct2B = new JRadioButton("T2");
			osct2B.setActionCommand("T2");
			osct2B.setSelected(false);
			osct2B.setEnabled(true);

			osct3B = new JRadioButton("T3");
			osct3B.setActionCommand("T3");
			osct3B.setSelected(false);
			osct3B.setEnabled(true);

			osct4B = new JRadioButton("T4");
			osct4B.setActionCommand("T4");
			osct4B.setSelected(false);
			osct4B.setEnabled(true);

			ButtonGroup group = new ButtonGroup();
			group.add(osct1B);
			group.add(osct2B);
			group.add(osct3B);
			group.add(osct4B);

			OscTypeListener oscTypeListener = new OscTypeListener();
			osct1B.addActionListener(oscTypeListener);
			osct2B.addActionListener(oscTypeListener);
			osct3B.addActionListener(oscTypeListener);
			osct4B.addActionListener(oscTypeListener);

			JPanel radioPanel = new JPanel();
			radioPanel.setLayout(new GridLayout(1, 0));
			radioPanel.add(osct1B);
			radioPanel.add(osct2B);
			radioPanel.add(osct3B);
			radioPanel.add(osct4B);

			add(radioPanel);

		}

		class OscTypeListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {

				String s = e.getActionCommand();
				if (s.startsWith("T1")) {
					audioModel.oscType = Oscillator.NOISE;
				} else if (s.startsWith("T2")) {
					audioModel.oscType = Oscillator.SINEWAVE;
				} else if (s.startsWith("T3")) {
					audioModel.oscType = Oscillator.TRIANGLEWAVE;
				} else if (s.startsWith("T4")) {
					audioModel.oscType = Oscillator.SQUAREWAVE;
				}
			}
		}

		class OscSListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String s = slider.getName();
				if (s.startsWith("Oscillator 1")) {
					audioModel.osc1Setting = value;
				} else if (s.startsWith("Oscillator 2")) {
					audioModel.osc2Setting = value;
				}
			}
		}

		class OscCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("Osc1")) {
						audioModel.osc1Switch = cb.isSelected();
					} else if (name.startsWith("Osc2")) {
						audioModel.osc2Switch = cb.isSelected();
					}

				}

			}
		}
	}

	class POffsetControl extends JPanel {

		public POffsetControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			pOffsetSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, 0, "Pitch Offset", new POffsetListener());
			add(pOffsetSlider);

		}

		class POffsetListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				JSlider slider = (JSlider) e.getSource();
				int value = slider.getValue();
				audioModel.pOffset = value;
			}

		}

	}

	class PFactorControl extends JPanel {

		public PFactorControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			pFactorSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, 100, "Pitch Factor", new PFactorListener());
			add(pFactorSlider);

		}

		class PFactorListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				JSlider slider = (JSlider) e.getSource();
				int value = slider.getValue();
				audioModel.pFactor = value;
			}

		}

	}

	class TFactorControl extends JPanel {

		public TFactorControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			tFactorSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, 80, "Time Factor", new TFactorListener());
			add(tFactorSlider);

		}

		class TFactorListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				JSlider slider = (JSlider) e.getSource();
				int value = slider.getValue();
				audioModel.tFactor = value;
			}

		}

	}

	class GainControl extends JPanel {

		public GainControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			gainSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, INIT_VOLUME_SETTING, "Gain",
					new GainSliderListener());
			add(gainSlider);

		}

		class GainSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				audioModel.gainSetting = value;
				audioModel.setGain();
			}
		}

	}

	class ReverbSControl extends JPanel {

		public ReverbSControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			reverbSSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, 0, "Reverb Send", new ReverbSSliderListener());
			add(reverbSSlider);
		}

		class ReverbSSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String name = slider.getName();
				audioModel.reverbSSetting = value;
				audioModel.setReverbSend();
			}

		}

	}

	class ReverbRControl extends JPanel {

		public ReverbRControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			reverbRSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, 0, "Reverb Return", new ReverbRSliderListener());
			add(reverbRSlider);
		}

		class ReverbRSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String name = slider.getName();
				audioModel.reverbRSetting = value;
				audioModel.setReverbReturn();
			}

		}

	}

	class PanControl extends JPanel {

		public PanControl() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			panSlider = new TmSlider(JSlider.HORIZONTAL, 0, 100, INIT_PAN_SETTING, "Pan", new PanSliderListener());
			add(panSlider);
		}

		class PanSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String name = slider.getName();
				audioModel.panSetting = value;
				audioModel.setPan();
			}

		}

	}

	public boolean openFile() {

		try {
			File file = null;
			JFileChooser fc = new JFileChooser(audioModel.toneMapFrame.getDirectory());
			fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					String name = f.getName();
					if (name.endsWith(".au") || name.endsWith(".wav") || name.endsWith(".WAV") || name.endsWith(".aiff")
							|| name.endsWith(".aif")) {
						return true;
					}
					return false;
				}

				public String getDescription() {
					return ".au, .wav, .aif";
				}
			});

			if (fc.showOpenDialog(audioModel.toneMapFrame.mainFrame) == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();

				audioModel.toneMapFrame.setDirectory(file);
				if (!audioModel.load(file))
					return false;

				fileNameField.setText(file.getParent() + "\\" + audioModel.getFileName());
				durationField.setText(String.valueOf(audioModel.getDuration()));
				sampleRateField.setText(String.valueOf(audioModel.getSampleRate()));
				bitSizeField.setText(String.valueOf(audioModel.getSampleBitSize()));
				channelsField.setText(String.valueOf(audioModel.getNumChannels()));
				timeControl.setTimeMax((int) (audioModel.getDuration() * 1000));
				return true;

			} else {
				return false;
			}
		} catch (SecurityException ex) {
			audioModel.toneMapFrame.reportStatus(EC_AUDIO_OPEN_BADFILE);
			return false;
		} catch (Exception ex) {
			audioModel.toneMapFrame.reportStatus(EC_AUDIO_OPEN_BADFILE);
			return false;
		}
	}

	class FileControl extends JPanel {

		public FileControl() {

			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("File");
			setBorder(tb);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setPreferredSize(new Dimension(200, 150));
			JPanel bp = new JPanel();
			JPanel ip = new JPanel();
			ip.setLayout(new BoxLayout(ip, BoxLayout.X_AXIS));
			JPanel ipHeadings = new JPanel();
			ipHeadings.setLayout(new GridLayout(5, 1));
			JPanel ipFields = new JPanel();
			ipFields.setLayout(new GridLayout(5, 1));

			openB = new JButton("Open");
			openB.setEnabled(true);
			bp.add(openB);

			saveB = new JButton("Save");
			saveB.setEnabled(false);
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

			add(bp, BorderLayout.NORTH);
			add(ip, BorderLayout.SOUTH);

		}

		class OpenBAction implements ActionListener {

			public void actionPerformed(ActionEvent evt) {

				try {
					File file = null;
					JFileChooser fc = new JFileChooser(audioModel.toneMapFrame.getDirectory());
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

					if (fc.showOpenDialog(audioModel.toneMapFrame.mainFrame) == JFileChooser.APPROVE_OPTION) {
						file = fc.getSelectedFile();
						audioModel.toneMapFrame.setDirectory(file);
						if (!audioModel.load(file))
							return;

						fileNameField.setText(file.getParent() + "\\" + audioModel.getFileName());
						durationField.setText(String.valueOf(audioModel.getDuration()));
						sampleRateField.setText(String.valueOf(audioModel.getSampleRate()));
						bitSizeField.setText(String.valueOf(audioModel.getSampleBitSize()));
						channelsField.setText(String.valueOf(audioModel.getNumChannels()));
						timeControl.setTimeMax((int) (audioModel.getDuration() * 1000));

					}
				} catch (SecurityException ex) {
					audioModel.toneMapFrame.reportStatus(EC_AUDIO_PANEL);
				} catch (Exception ex) {
					audioModel.toneMapFrame.reportStatus(EC_AUDIO_PANEL);
				}
			}

		}

		class SaveBAction implements ActionListener {

			public void actionPerformed(ActionEvent evt) {
			}
		}
	}

	class TransControl extends JPanel {

		public TransControl() {

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Transform");
			setBorder(tb);

			setPreferredSize(new Dimension(200, 150));

			JPanel cp = new JPanel();
			cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));

			javaB = new JRadioButton("Java");
			javaB.setActionCommand("Java");
			javaB.setSelected(true);

			jniB = new JRadioButton("JNI");
			jniB.setActionCommand("JNI");
			jniB.setEnabled(false);
			jniB.setSelected(false);

			ButtonGroup group = new ButtonGroup();
			group.add(javaB);
			group.add(jniB);

			TransformBListener transformBListener = new TransformBListener();
			javaB.addActionListener(transformBListener);
			jniB.addActionListener(transformBListener);

			JPanel radioPanel = new JPanel();
			radioPanel.setLayout(new GridLayout(1, 0));
			radioPanel.add(javaB);
			radioPanel.add(jniB);

			add(cp, BorderLayout.NORTH);
			add(radioPanel, BorderLayout.SOUTH);

		}

		class TransformBListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {

				String s = e.getActionCommand();
				if (s.startsWith("Java")) {
					audioModel.transformMode = TRANSFORM_MODE_JAVA;
				} else if (s.startsWith("JNI")) {
					audioModel.transformMode = TRANSFORM_MODE_JNI;
				}
			}
		}

	}

	class FilterControl extends JPanel {

		public FilterControl() {

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			// setPreferredSize(new Dimension(200, 150));

			JPanel cp = new JPanel();

			t1S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 50, "t1", new FilterSListener());
			add(t1S);

			t1CB = new JCheckBox("t1");
			t1CB.addItemListener(new FilterCBListener());
			add(t1CB);

			t2S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 50, "t2", new FilterSListener());
			add(t2S);

			t2CB = new JCheckBox("t2");
			t2CB.addItemListener(new FilterCBListener());
			add(t2CB);

			t3S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 50, "t3", new FilterSListener());
			add(t3S);

			t3CB = new JCheckBox("t3");
			t3CB.addItemListener(new FilterCBListener());
			add(t3CB);

			t4S = new TmSlider(JSlider.HORIZONTAL, 0, 100, 50, "t4", new FilterSListener());
			add(t4S);

			t4CB = new JCheckBox("t4");
			t4CB.addItemListener(new FilterCBListener());
			add(t4CB);

		}

		class FilterSListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {

				TmSlider slider = (TmSlider) e.getSource();
				int value = slider.getValue();
				String s = slider.getName();
				if (s.startsWith("t1")) {
					audioModel.t1Setting = value;
				} else if (s.startsWith("t2")) {
					audioModel.t2Setting = value;
				} else if (s.startsWith("t3")) {
					audioModel.t3Setting = value;
				} else if (s.startsWith("t4")) {
					audioModel.t4Setting = value;
				}
			}
		}

		class FilterCBListener implements ItemListener {

			public void itemStateChanged(ItemEvent e) {
				if (e.getSource() instanceof JComboBox) {
					JComboBox combo = (JComboBox) e.getSource();
				} else {
					JCheckBox cb = (JCheckBox) e.getSource();
					String name = cb.getText();
					if (name.startsWith("t1")) {
						audioModel.t1Switch = cb.isSelected();
					} else if (name.startsWith("t2")) {
						audioModel.t2Switch = cb.isSelected();
					} else if (name.startsWith("t3")) {
						audioModel.t3Switch = cb.isSelected();
					} else if (name.startsWith("t4")) {
						audioModel.t4Switch = cb.isSelected();
					}
				}

			}
		}

	}

	private AudioModel audioModel;

	public TimeControl timeControl;
	public PitchControl pitchControl;
	public SampleSizeControl sampleSizeControl;
	public GainControl gainControl;
	public PanControl panControl;
	public FileControl fileControl;
	public TransControl transControl;
	public ResolutionControl resolutionControl;
	public TFactorControl tFactorControl;
	public PFactorControl pFactorControl;
	public JButton openB, saveB;
	public TmSlider sampleSizeSlider, lowPTS, highPTS;
	public JCheckBox logCB, osc1CB, osc2CB, t1CB, t2CB, t3CB, t4CB;
	public TmSlider gainSlider, reverbSSlider, reverbRSlider;
	public TmSlider panSlider, osc1S, osc2S, t1S, t2S, t3S, t4S;
	public TmSlider resolutionSlider;
	public TmSlider tFactorSlider;
	public TmSlider pFactorSlider, pOffsetSlider;
	public JRadioButton javaB, jniB, osct1B, osct2B, osct3B, osct4B;
	public JLabel fileNameField;
	public JLabel durationField;
	public JLabel sampleRateField;
	public JLabel bitSizeField;
	public JLabel channelsField;

	public AudioPanel(AudioModel audioModel) {

		this.audioModel = audioModel;

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
		JPanel p4 = new JPanel();
		p4.setLayout(new BoxLayout(p4, BoxLayout.X_AXIS));
		JPanel p5 = new JPanel();
		p5.setLayout(new BoxLayout(p5, BoxLayout.X_AXIS));

		JPanel p3 = new JPanel();
		p3.setLayout(new GridLayout(1, 2));

		p1.add(timeControl = new TimeControl(new TimeControlListener()));
		p1.add(pitchControl = new PitchControl(new PitchControlListener()));
		p2.add(gainControl = new GainControl());
		p2.add(panControl = new PanControl());
		p2.add(new ReverbRControl());
		p2.add(new ReverbSControl());
		p2.add(sampleSizeControl = new SampleSizeControl());
		p4.add(resolutionControl = new ResolutionControl());
		p4.add(tFactorControl = new TFactorControl());
		p4.add(pFactorControl = new PFactorControl());
		p4.add(new POffsetControl());
		p3.add(fileControl = new FileControl());
		p3.add(transControl = new TransControl());

		p5.add(new PowerControl());
		p5.add(new FilterControl());
		p0.add(p5);
		p0.add(p4);
		p0.add(p2);
		p0.add(new OscillatorControl());
		p0.add(p1);

		add(p3, BorderLayout.NORTH);
		add(p0, BorderLayout.CENTER);

	}
}// End AudioPanel