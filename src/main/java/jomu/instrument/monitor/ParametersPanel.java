package jomu.instrument.monitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentStoreService;

public class ParametersPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTextField tunerHarmonicDriftFactorInput;
	private ParameterManager parameterManager;
	private JTextField tunerNormalisePeakInput;
	private JTextField tunerNormaliseTroughInput;
	private JSlider audioLowPassSlider;
	private JSlider audioHighPassSlider;
	private JCheckBox n1SwitchCB;
	private JCheckBox n2SwitchCB;
	private JCheckBox n3SwitchCB;
	private AbstractButton n6SwitchCB;
	private AbstractButton n5SwitchCB;
	private AbstractButton n4SwitchCB;
	private AbstractButton harmonicWeightingSwitchCB;
	private JCheckBox harmonicGuitarSwitchCB;
	private AbstractButton peakSwitchCB;
	private JCheckBox compressionSwitchCB;
	private JCheckBox squareSwitchCB;
	private JCheckBox lowThresholdSwitchCB;
	private JCheckBox decibelSwitchCB;
	private JCheckBox normaliseSwitchCB;
	private AbstractButton tunerSwitchCB;
	private AbstractButton peaksSwitchCB;
	private JSlider noiseFloorSlider;
	private JSlider medianFilterSizeSlider;
	private JSlider minPeakSizeSlider;
	private JSlider numberOfPeaksSlider;
	private JSlider formantFactorSlider;
	private JSlider formantHighSettingSlider;
	private JSlider formantLowSettingSlider;
	private JSlider formantMiddleSettingSlider;
	private JSlider n2SettingSlider;
	private JSlider n1SettingSlider;
	private JSlider n3SettingSlider;
	private JSlider n4SettingSlider;
	private JSlider n5SettingSlider;
	private JSlider n6SettingSlider;
	private JSlider harmonic6SettingSlider;
	private JSlider harmonic5SettingSlider;
	private JSlider harmonic4SettingSlider;
	private JSlider harmonic3SettingSlider;
	private JSlider harmonic2SettingSlider;
	private JSlider harmonic1SettingSlider;
	private JSlider normalizeSettingSlider;
	private JSlider noteHighSlider;
	private JSlider noteLowSlider;
	private JSlider noteMaxDurationSlider;
	private JSlider noteMinDurationSlider;
	private JSlider noteSustainSlider;
	private JSlider pitchHighSlider;
	private JSlider pitchLowSlider;
	private JSlider formantLowFreqSlider;
	private JSlider formantMidFreqSlider;
	private JSlider formantHighFreqSlider;
	private JTextField cqLowThresholdInput;
	private JTextField cqThresholdFactorInput;
	private JTextField cqSignalMinimumInput;
	private JTextField cqNormaliseThresholdInput;
	private JTextField cqDecibelLevelInput;
	private JTextField cqCompressionLevelInput;
	private JTextField tunerNormaliseThresholdInput;
	private JTextField tunerThresholdFactorInput;
	private JTextField tunerSignalMinimumInput;
	private JTextField toneMapViewLowThresholdInput;
	private JTextField toneMapViewHighThresholdInput;
	private JSlider pitchHarmonicsSlider;
	private JTextField pdCompressionLevelInput;
	private JCheckBox pdCompressionSwitchCB;
	private JCheckBox pdWhitenerSwitchCB;
	private JCheckBox pdKlapuriSwitchCB;
	private JCheckBox pdTarsosSwitchCB;
	private JTextField pdLowThresholdInput;

	private final Integer[] fftSizes = { 256, 512, 1024, 2048, 4096, 8192, 16384, 22050, 32768, 65536, 131072 };
	private JComboBox cqWindowComboBox;
	private JComboBox pdWindowComboBox;
	private JComboBox spWindowComboBox;

	public ParametersPanel(ParameterManager parameterManager, InstrumentStoreService iss) {
		super(new BorderLayout());
		this.parameterManager = parameterManager;

		this.setBorder(new TitledBorder("Input Parameters"));

		JPanel actionPanel = new JPanel();

		final JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					parameterManager.reset();
					updateParameters();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		actionPanel.add(resetButton);

		final JButton loadButton = new JButton("Load");
		loadButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Properties params = iss.getParameters();
				if (params != null && !params.isEmpty()) {
					parameterManager.setParameters(iss.getParameters());
					updateParameters();
				}
			}
		});
		actionPanel.add(loadButton);

		final JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				iss.setParameters(parameterManager.getParameters());
			}
		});
		actionPanel.add(saveButton);

		this.add(actionPanel, BorderLayout.NORTH);

		JPanel parameterPanel = new JPanel();
		parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
		parameterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		audioLowPassSlider = new JSlider(0, 20000);
		final JLabel audioLowPassLabel = new JLabel("Audio Low Pass :");
		audioLowPassSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				audioLowPassLabel.setText(String.format("Audio Low Pass   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS,
						Integer.toString(newValue));
			}
		});
		audioLowPassSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS));
		parameterPanel.add(audioLowPassLabel);
		parameterPanel.add(audioLowPassSlider);

		audioHighPassSlider = new JSlider(0, 20000);
		final JLabel audioHighPassLabel = new JLabel("Audio High Pass :");
		audioHighPassSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				audioHighPassLabel.setText(String.format("Audio High Pass   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS,
						Integer.toString(newValue));
			}
		});
		audioHighPassSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS));
		parameterPanel.add(audioHighPassLabel);
		parameterPanel.add(audioHighPassSlider);

		JPanel audioComboPanel = new JPanel();
		// switchPanel.setLayout(new BoxLayout(switchPanel, BoxLayout.X_AXIS));
		audioComboPanel.setLayout(new GridLayout(1, 0));
		audioComboPanel
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		pdWindowComboBox = new JComboBox<>(fftSizes);
		pdWindowComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Integer value = (Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PD_WINDOW,
						Integer.toString(value));
			}
		});

		pdWindowComboBox.setSelectedIndex(getFFTWindowIndex(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PD_WINDOW)));
		audioComboPanel.add(new JLabel("Pitch Detector Window Size:  "));
		audioComboPanel.add(pdWindowComboBox);

		cqWindowComboBox = new JComboBox<>(fftSizes);
		cqWindowComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Integer value = (Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_CQ_WINDOW,
						Integer.toString(value));
			}
		});

		cqWindowComboBox.setSelectedIndex(getFFTWindowIndex(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_CQ_WINDOW)));
		audioComboPanel.add(new JLabel("CQ Window Size:  "));
		audioComboPanel.add(cqWindowComboBox);

		spWindowComboBox = new JComboBox<>(fftSizes);
		spWindowComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Integer value = (Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SP_WINDOW,
						Integer.toString(value));
			}
		});

		spWindowComboBox.setSelectedIndex(getFFTWindowIndex(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SP_WINDOW)));
		audioComboPanel.add(new JLabel("Spectral Peaks Window Size:  "));
		audioComboPanel.add(spWindowComboBox);

		parameterPanel.add(audioComboPanel);

		JPanel tunerSwitchPanel = new JPanel();
		// switchPanel.setLayout(new BoxLayout(switchPanel, BoxLayout.X_AXIS));
		tunerSwitchPanel.setLayout(new GridLayout(2, 0));
		// switchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		tunerSwitchPanel
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		n1SwitchCB = new JCheckBox("n1SwitchCB");
		n1SwitchCB.setText("Audio Tuner N1 Switch");
		n1SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SWITCH,
						Boolean.toString(newValue));
			}
		});

		n1SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SWITCH));
		tunerSwitchPanel.add(n1SwitchCB);

		n2SwitchCB = new JCheckBox("n2SwitchCB");
		n2SwitchCB.setText("Audio Tuner N2 Switch");
		n2SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SWITCH,
						Boolean.toString(newValue));
			}
		});

		n2SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SWITCH));
		tunerSwitchPanel.add(n2SwitchCB);

		n3SwitchCB = new JCheckBox("n3SwitchCB");
		n3SwitchCB.setText("Audio Tuner N3 Switch");
		n3SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N3_SWITCH,
						Boolean.toString(newValue));
			}
		});

		n3SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N3_SWITCH));
		tunerSwitchPanel.add(n3SwitchCB);

		n4SwitchCB = new JCheckBox("n4SwitchCB");
		n4SwitchCB.setText("Audio Tuner N4 Switch");
		n4SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N4_SWITCH,
						Boolean.toString(newValue));
			}
		});

		n4SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N4_SWITCH));
		tunerSwitchPanel.add(n4SwitchCB);

		n5SwitchCB = new JCheckBox("n5SwitchCB");
		n5SwitchCB.setText("Audio Tuner N5 Switch");
		n5SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N5_SWITCH,
						Boolean.toString(newValue));
			}
		});

		n5SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N5_SWITCH));
		tunerSwitchPanel.add(n5SwitchCB);

		n6SwitchCB = new JCheckBox("n6SwitchCB");
		n6SwitchCB.setText("Audio Tuner N6 Switch");
		n6SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N6_SWITCH,
						Boolean.toString(newValue));
			}
		});

		n6SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N6_SWITCH));
		tunerSwitchPanel.add(n6SwitchCB);

		JCheckBox harmonicOperatorSwitchCB = new JCheckBox("harmonicOperatorSwitchCB");
		harmonicOperatorSwitchCB.setText("Audio Tuner Harmonic Operator Switch");
		harmonicOperatorSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_OPERATOR_SWITCH,
						Boolean.toString(newValue));
			}
		});

		harmonicOperatorSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_OPERATOR_SWITCH));
		tunerSwitchPanel.add(harmonicOperatorSwitchCB);

		harmonicWeightingSwitchCB = new JCheckBox("harmonicWeightingSwitchCB");
		harmonicWeightingSwitchCB.setText("Audio Tuner Harmonic Weighting Switch");
		harmonicWeightingSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_WEIGHTING_SWITCH,
						Boolean.toString(newValue));
			}
		});

		harmonicWeightingSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_WEIGHTING_SWITCH));
		tunerSwitchPanel.add(harmonicWeightingSwitchCB);

		harmonicGuitarSwitchCB = new JCheckBox("harmonicGuitarSwitchCB");
		harmonicGuitarSwitchCB.setText("Audio Tuner Harmonic Guitar Switch");
		harmonicGuitarSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_GUITAR_SWITCH,
						Boolean.toString(newValue));
			}
		});

		harmonicGuitarSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_GUITAR_SWITCH));
		tunerSwitchPanel.add(harmonicGuitarSwitchCB);

		peakSwitchCB = new JCheckBox("peakSwitchCB");
		peakSwitchCB.setText("Audio Tuner Peak Switch");
		peakSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_PEAK_SWITCH,
						Boolean.toString(newValue));
			}
		});

		peakSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_PEAK_SWITCH));
		tunerSwitchPanel.add(peakSwitchCB);

		parameterPanel.add(tunerSwitchPanel);

		JPanel cqSwitchPanel = new JPanel();
		cqSwitchPanel.setLayout(new GridLayout(2, 0));
		cqSwitchPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		compressionSwitchCB = new JCheckBox("compressionSwitchCB");
		compressionSwitchCB.setText("CQ Compression");
		compressionSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS,
						Boolean.toString(newValue));
			}
		});

		compressionSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS));
		cqSwitchPanel.add(compressionSwitchCB);

		squareSwitchCB = new JCheckBox("squareSwitchCB");
		squareSwitchCB.setText("CQ Square");
		squareSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE,
						Boolean.toString(newValue));
			}
		});

		squareSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE));
		cqSwitchPanel.add(squareSwitchCB);

		lowThresholdSwitchCB = new JCheckBox("squareSwitchCB");
		lowThresholdSwitchCB.setText("CQ Low Threshold");
		lowThresholdSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD,
						Boolean.toString(newValue));
			}
		});

		lowThresholdSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD));
		cqSwitchPanel.add(lowThresholdSwitchCB);

		decibelSwitchCB = new JCheckBox("decibelSwitchCB");
		decibelSwitchCB.setText("CQ Decibel");
		decibelSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL,
						Boolean.toString(newValue));
			}
		});

		decibelSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL));
		cqSwitchPanel.add(decibelSwitchCB);

		normaliseSwitchCB = new JCheckBox("normaliseSwitchCB");
		normaliseSwitchCB.setText("CQ Normalise");
		normaliseSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE,
						Boolean.toString(newValue));
			}
		});

		normaliseSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE));
		cqSwitchPanel.add(normaliseSwitchCB);

		tunerSwitchCB = new JCheckBox("tunerSwitchCB");
		tunerSwitchCB.setText("Tuner");
		tunerSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_TUNER,
						Boolean.toString(newValue));
			}
		});

		tunerSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_TUNER));
		cqSwitchPanel.add(tunerSwitchCB);

		peaksSwitchCB = new JCheckBox("peaksSwitchCB");
		peaksSwitchCB.setText("Peaks");
		peaksSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_PEAKS,
						Boolean.toString(newValue));
			}
		});

		peaksSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_PEAKS));
		cqSwitchPanel.add(peaksSwitchCB);

		pdCompressionSwitchCB = new JCheckBox("pdCompressionSwitchCB");
		pdCompressionSwitchCB.setText("Pitch Detect Compression");
		pdCompressionSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_COMPRESS,
						Boolean.toString(newValue));
			}
		});

		pdCompressionSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_COMPRESS));
		cqSwitchPanel.add(pdCompressionSwitchCB);

		pdWhitenerSwitchCB = new JCheckBox("pdWhitenerSwitchCB");
		pdWhitenerSwitchCB.setText("Pitch Detect Whitener");
		pdWhitenerSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_WHITENER,
						Boolean.toString(newValue));
			}
		});

		pdWhitenerSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_WHITENER));
		cqSwitchPanel.add(pdWhitenerSwitchCB);

		pdKlapuriSwitchCB = new JCheckBox("pdKlapuriSwitchCB");
		pdKlapuriSwitchCB.setText("Pitch Detect Klapuri");
		pdKlapuriSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_KLAPURI,
						Boolean.toString(newValue));
			}
		});

		pdKlapuriSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_KLAPURI));
		cqSwitchPanel.add(pdKlapuriSwitchCB);

		pdTarsosSwitchCB = new JCheckBox("pdTarsosSwitchCB");
		pdTarsosSwitchCB.setText("Pitch Detect Tarsos");
		pdTarsosSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_TARSOS,
						Boolean.toString(newValue));
			}
		});

		pdTarsosSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_TARSOS));
		cqSwitchPanel.add(pdTarsosSwitchCB);

		parameterPanel.add(cqSwitchPanel);

		noiseFloorSlider = new JSlider(100, 250);
		final JLabel noiseFloorFactorLabel = new JLabel("Noise floor factor    :");
		noiseFloorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				double actualValue = newValue / 100.0;
				noiseFloorFactorLabel.setText(String.format("Noise floor factor (%.2f):", actualValue));

				System.out.println("New noise floor factor: " + actualValue);
				float noiseFloorFactor = (float) actualValue;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR,
						Float.toString(noiseFloorFactor));
				// TODO repaintSpectalInfo();

			}
		});
		float noiseFloorFactor = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR);
		noiseFloorSlider.setValue((int) noiseFloorFactor);
		parameterPanel.add(noiseFloorFactorLabel);
		parameterPanel.add(noiseFloorSlider);

		medianFilterSizeSlider = new JSlider(3, 255);
		final JLabel medianFilterSizeLabel = new JLabel("Median Filter Size   :");
		medianFilterSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				medianFilterSizeLabel.setText(String.format("Median Filter Size (%d):", newValue));
				System.out.println("New Median filter size: " + newValue);
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH,
						Integer.toString(newValue));
			}
		});
		int noiseFloorMedianFilterLength = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH);
		medianFilterSizeSlider.setValue(noiseFloorMedianFilterLength);
		parameterPanel.add(medianFilterSizeLabel);
		parameterPanel.add(medianFilterSizeSlider);

		minPeakSizeSlider = new JSlider(5, 255);
		final JLabel minPeakSizeLabel = new JLabel("Min Peak Size   :");
		minPeakSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				minPeakSizeLabel.setText(String.format("Min Peak Size    (%d):", newValue));
				System.out.println("Min Peak Sizee: " + newValue);
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE,
						Integer.toString(newValue));
				// TODO repaintSpectalInfo();
			}
		});
		int minPeakSize = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE);
		minPeakSizeSlider.setValue(minPeakSize);
		parameterPanel.add(minPeakSizeLabel);
		parameterPanel.add(minPeakSizeSlider);

		numberOfPeaksSlider = new JSlider(1, 40);
		final JLabel numberOfPeaksLabel = new JLabel("Number of peaks  :");
		numberOfPeaksSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				numberOfPeaksLabel.setText("Number of peaks (" + newValue + "):");

				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS,
						Integer.toString(newValue));
				// TODO repaintSpectalInfo();

			}
		});
		int numberOfSpectralPeaks = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS);
		numberOfPeaksSlider.setValue(numberOfSpectralPeaks);
		parameterPanel.add(numberOfPeaksLabel);
		parameterPanel.add(numberOfPeaksSlider);

		pitchHarmonicsSlider = new JSlider(1, 10);
		final JLabel pitchHarmonicsLabel = new JLabel("Pitch Harmonics :");
		pitchHarmonicsSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				pitchHarmonicsLabel.setText("Pitch Harmonics  (" + newValue + "):");

				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_HARMONICS,
						Integer.toString(newValue));
				// TODO repaintSpectalInfo();

			}
		});
		pitchHarmonicsSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_HARMONICS));
		parameterPanel.add(pitchHarmonicsLabel);
		parameterPanel.add(pitchHarmonicsSlider);

		formantFactorSlider = new JSlider(0, 100);
		final JLabel formantFactorLabel = new JLabel("Audio Tuner Formant Factor :");
		formantFactorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantFactorLabel.setText(String.format("Audio Tuner Formant Factor   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_FACTOR,
						Integer.toString(newValue));
			}
		});
		formantFactorSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_FACTOR));
		parameterPanel.add(formantFactorLabel);
		parameterPanel.add(formantFactorSlider);

		formantHighSettingSlider = new JSlider(0, 100);
		final JLabel formantHighSettingLabel = new JLabel("Audio Tuner Formant High :");
		formantHighSettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantHighSettingLabel.setText(String.format("Audio Tuner Formant High   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH,
						Integer.toString(newValue));
			}
		});
		formantHighSettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH));
		parameterPanel.add(formantHighSettingLabel);
		parameterPanel.add(formantHighSettingSlider);

		formantLowSettingSlider = new JSlider(0, 100);
		final JLabel formantLowSettingLabel = new JLabel("Audio Tuner Formant Low :");
		formantLowSettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantLowSettingLabel.setText(String.format("Audio Tuner Formant Low   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW,
						Integer.toString(newValue));
			}
		});
		formantLowSettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW));
		parameterPanel.add(formantLowSettingLabel);
		parameterPanel.add(formantLowSettingSlider);

		formantMiddleSettingSlider = new JSlider(0, 100);
		final JLabel formantMiddleSettingLabel = new JLabel("Audio Tuner Formant Middle :");
		formantMiddleSettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantMiddleSettingLabel.setText(String.format("Audio Tuner Formant Middle   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE,
						Integer.toString(newValue));
			}
		});
		formantMiddleSettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE));
		parameterPanel.add(formantMiddleSettingLabel);
		parameterPanel.add(formantMiddleSettingSlider);

		n1SettingSlider = new JSlider(0, 100);
		final JLabel n1SettingLabel = new JLabel("Audio Tuner N1 Setting :");
		n1SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				n1SettingLabel.setText(String.format("Audio Tuner N1 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SETTING,
						Integer.toString(newValue));
			}
		});
		n1SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SETTING));
		parameterPanel.add(n1SettingLabel);
		parameterPanel.add(n1SettingSlider);

		n2SettingSlider = new JSlider(0, 100);
		final JLabel n2SettingLabel = new JLabel("Audio Tuner N2 Setting :");
		n2SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				n2SettingLabel.setText(String.format("Audio Tuner N2 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SETTING,
						Integer.toString(newValue));
			}
		});
		n2SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SETTING));
		parameterPanel.add(n2SettingLabel);
		parameterPanel.add(n2SettingSlider);

		n3SettingSlider = new JSlider(0, 100);
		final JLabel n3SettingLabel = new JLabel("Audio Tuner N3 Setting :");
		n3SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				n3SettingLabel.setText(String.format("Audio Tuner N3 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N3_SETTING,
						Integer.toString(newValue));
			}
		});
		n3SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N3_SETTING));
		parameterPanel.add(n3SettingLabel);
		parameterPanel.add(n3SettingSlider);

		n4SettingSlider = new JSlider(0, 100);
		final JLabel n4SettingLabel = new JLabel("Audio Tuner N4 Setting :");
		n4SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				n4SettingLabel.setText(String.format("Audio Tuner N4 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N4_SETTING,
						Integer.toString(newValue));
			}
		});
		n4SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N4_SETTING));
		parameterPanel.add(n4SettingLabel);
		parameterPanel.add(n4SettingSlider);

		n5SettingSlider = new JSlider(0, 100);
		final JLabel n5SettingLabel = new JLabel("Audio Tuner N5 Setting :");
		n5SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				n5SettingLabel.setText(String.format("Audio Tuner N5 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N5_SETTING,
						Integer.toString(newValue));
			}
		});
		n5SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N5_SETTING));
		parameterPanel.add(n5SettingLabel);
		parameterPanel.add(n5SettingSlider);

		n6SettingSlider = new JSlider(0, 100);
		final JLabel n6SettingLabel = new JLabel("Audio Tuner N6 Setting :");
		n6SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				n6SettingLabel.setText(String.format("Audio Tuner N6 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N6_SETTING,
						Integer.toString(newValue));
			}
		});
		n6SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N6_SETTING));
		parameterPanel.add(n6SettingLabel);
		parameterPanel.add(n6SettingSlider);

		harmonic1SettingSlider = new JSlider(0, 100);
		final JLabel harmonic1SettingLabel = new JLabel("Audio Tuner Harmonic 1 Setting :");
		harmonic1SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				harmonic1SettingLabel.setText(String.format("Audio Tuner Harmonic 1 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC1_SETTING,
						Integer.toString(newValue));
			}
		});
		harmonic1SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC1_SETTING));
		parameterPanel.add(harmonic1SettingLabel);
		parameterPanel.add(harmonic1SettingSlider);

		harmonic2SettingSlider = new JSlider(0, 100);
		final JLabel harmonic2SettingLabel = new JLabel("Audio Tuner Harmonic 2 Setting :");
		harmonic2SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				harmonic2SettingLabel.setText(String.format("Audio Tuner Harmonic 2 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC2_SETTING,
						Integer.toString(newValue));
			}
		});
		harmonic2SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC2_SETTING));
		parameterPanel.add(harmonic2SettingLabel);
		parameterPanel.add(harmonic2SettingSlider);

		harmonic3SettingSlider = new JSlider(0, 100);
		final JLabel harmonic3SettingLabel = new JLabel("Audio Tuner Harmonic 3 Setting :");
		harmonic3SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				harmonic3SettingLabel.setText(String.format("Audio Tuner Harmonic 3 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC3_SETTING,
						Integer.toString(newValue));
			}
		});
		harmonic3SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC3_SETTING));
		parameterPanel.add(harmonic3SettingLabel);
		parameterPanel.add(harmonic3SettingSlider);

		harmonic4SettingSlider = new JSlider(0, 100);
		final JLabel harmonic4SettingLabel = new JLabel("Audio Tuner Harmonic 4 Setting :");
		harmonic4SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				harmonic4SettingLabel.setText(String.format("Audio Tuner Harmonic 4 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC4_SETTING,
						Integer.toString(newValue));
			}
		});
		harmonic4SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC4_SETTING));
		parameterPanel.add(harmonic4SettingLabel);
		parameterPanel.add(harmonic4SettingSlider);

		harmonic5SettingSlider = new JSlider(0, 100);
		final JLabel harmonic5SettingLabel = new JLabel("Audio Tuner Harmonic 5 Setting :");
		harmonic5SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				harmonic5SettingLabel.setText(String.format("Audio Tuner Harmonic 5 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC5_SETTING,
						Integer.toString(newValue));
			}
		});
		harmonic5SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC5_SETTING));
		parameterPanel.add(harmonic5SettingLabel);
		parameterPanel.add(harmonic5SettingSlider);

		harmonic6SettingSlider = new JSlider(0, 100);
		final JLabel harmonic6SettingLabel = new JLabel("Audio Tuner Harmonic 6 Setting :");
		harmonic6SettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				harmonic6SettingLabel.setText(String.format("Audio Tuner Harmonic 6 Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC6_SETTING,
						Integer.toString(newValue));
			}
		});
		harmonic6SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC6_SETTING));
		parameterPanel.add(harmonic6SettingLabel);
		parameterPanel.add(harmonic6SettingSlider);

		normalizeSettingSlider = new JSlider(0, 100);
		final JLabel normalizeSettingLabel = new JLabel("Audio Tuner Normalise Setting :");
		normalizeSettingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				normalizeSettingLabel.setText(String.format("Audio Tuner Normalise Setting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_SETTING,
						Integer.toString(newValue));
			}
		});
		normalizeSettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_SETTING));
		parameterPanel.add(normalizeSettingLabel);
		parameterPanel.add(normalizeSettingSlider);

		noteHighSlider = new JSlider(0, 100);
		final JLabel noteHighLabel = new JLabel("Audio Tuner High Note :");
		noteHighSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				noteHighLabel.setText(String.format("Audio Tuner High Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_HIGH,
						Integer.toString(newValue));
			}
		});
		noteHighSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_HIGH));
		parameterPanel.add(noteHighLabel);
		parameterPanel.add(noteHighSlider);

		noteLowSlider = new JSlider(0, 100);
		final JLabel noteLowLabel = new JLabel("Audio Tuner Low Note :");
		noteLowSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				noteLowLabel.setText(String.format("Audio Tuner Low Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_LOW,
						Integer.toString(newValue));
			}
		});
		noteLowSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_LOW));
		parameterPanel.add(noteLowLabel);
		parameterPanel.add(noteLowSlider);

		noteMaxDurationSlider = new JSlider(0, 10000);
		final JLabel noteMaxDurationLabel = new JLabel("Audio Tuner Max Note Duration :");
		noteMaxDurationSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				noteMaxDurationLabel.setText(String.format("Audio Tuner Max Note Duration  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MAX_DURATION,
						Integer.toString(newValue));
			}
		});
		noteMaxDurationSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MAX_DURATION));
		parameterPanel.add(noteMaxDurationLabel);
		parameterPanel.add(noteMaxDurationSlider);

		noteMinDurationSlider = new JSlider(0, 1000);
		final JLabel noteMinDurationLabel = new JLabel("Audio Tuner Min Note Duration :");
		noteMinDurationSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				noteMinDurationLabel.setText(String.format("Audio Tuner Min Note Duration  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MIN_DURATION,
						Integer.toString(newValue));
			}
		});
		noteMinDurationSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MIN_DURATION));
		parameterPanel.add(noteMinDurationLabel);
		parameterPanel.add(noteMinDurationSlider);

		noteSustainSlider = new JSlider(0, 1000);
		final JLabel noteSustainLabel = new JLabel("Audio Tuner Min Note Sustain :");
		noteSustainSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				noteSustainLabel.setText(String.format("Audio Tuner Min Note Sustain  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SUSTAIN,
						Integer.toString(newValue));
			}
		});
		noteSustainSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SUSTAIN));
		parameterPanel.add(noteSustainLabel);
		parameterPanel.add(noteSustainSlider);

		pitchHighSlider = new JSlider(36, 72);
		final JLabel pitchHighLabel = new JLabel("Audio Tuner High Pitch :");
		pitchHighSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				pitchHighLabel.setText(String.format("Audio Tuner High Pitch  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_HIGH,
						Integer.toString(newValue));
			}
		});
		pitchHighSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_HIGH));
		parameterPanel.add(pitchHighLabel);
		parameterPanel.add(pitchHighSlider);

		pitchLowSlider = new JSlider(36, 72);
		final JLabel pitchLowLabel = new JLabel("Audio Tuner Low Pitch :");
		pitchLowSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				pitchLowLabel.setText(String.format("Audio Tuner Low Pitch  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_LOW,
						Integer.toString(newValue));
			}
		});
		pitchLowSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_LOW));
		parameterPanel.add(pitchLowLabel);
		parameterPanel.add(pitchLowSlider);

		formantHighFreqSlider = new JSlider(0, 20000);
		final JLabel formantHighFreqLabel = new JLabel("Audio Tuner Formant High Frequency :");
		formantHighFreqSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantHighFreqLabel.setText(String.format("Audio Tuner Formant High Frequency  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH_FREQUENCY,
						Integer.toString(newValue));
			}
		});
		formantHighFreqSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH_FREQUENCY));
		parameterPanel.add(formantHighFreqLabel);
		parameterPanel.add(formantHighFreqSlider);

		formantLowFreqSlider = new JSlider(0, 20000);
		final JLabel formantLowFreqLabel = new JLabel("Audio Tuner Formant Low Frequency :");
		formantLowFreqSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantLowFreqLabel.setText(String.format("Audio Tuner Formant Low Frequency  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW_FREQUENCY,
						Integer.toString(newValue));
			}
		});
		formantLowFreqSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW_FREQUENCY));
		parameterPanel.add(formantLowFreqLabel);
		parameterPanel.add(formantLowFreqSlider);

		formantMidFreqSlider = new JSlider(0, 20000);
		final JLabel formantMidFreqLabel = new JLabel("Audio Tuner Formant Middle Frequency :");
		formantMidFreqSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantMidFreqLabel.setText(String.format("Audio Tuner Formant Middle Frequency  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE_FREQUENCY,
						Integer.toString(newValue));
			}
		});
		formantMidFreqSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE_FREQUENCY));
		parameterPanel.add(formantMidFreqLabel);
		parameterPanel.add(formantMidFreqSlider);

		JPanel cqParamsPanel = new JPanel();
		cqParamsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		cqParamsPanel.setLayout(new GridLayout(0, 2));
		cqParamsPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		JLabel toneMapViewLowThresholdLabel = new JLabel("ToneMap View Low Threshold: ");
		toneMapViewLowThresholdInput = new JTextField(4);
		toneMapViewLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = toneMapViewLowThresholdInput.getText();
				toneMapViewLowThresholdLabel.setText(String.format("ToneMap View Low Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD, newValue);

			}
		});
		toneMapViewLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD));
		cqParamsPanel.add(toneMapViewLowThresholdLabel);
		cqParamsPanel.add(toneMapViewLowThresholdInput);

		JLabel toneMapViewHighThresholdLabel = new JLabel("ToneMap View High Threshold: ");
		toneMapViewHighThresholdInput = new JTextField(4);
		toneMapViewHighThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = toneMapViewHighThresholdInput.getText();
				toneMapViewHighThresholdLabel.setText(String.format("ToneMap View High Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD, newValue);

			}
		});
		toneMapViewHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
		cqParamsPanel.add(toneMapViewHighThresholdLabel);
		cqParamsPanel.add(toneMapViewHighThresholdInput);

		JLabel cqLowThresholdLabel = new JLabel("CQ Low Threshold: ");
		cqLowThresholdInput = new JTextField(4);
		cqLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqLowThresholdInput.getText();
				cqLowThresholdLabel.setText(String.format("CQ Low Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD, newValue);

			}
		});
		cqLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD));
		cqParamsPanel.add(cqLowThresholdLabel);
		cqParamsPanel.add(cqLowThresholdInput);

		JLabel cqThresholdFactorLabel = new JLabel("CQ Threshold Factor: ");
		cqThresholdFactorInput = new JTextField(4);
		cqThresholdFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqThresholdFactorInput.getText();
				cqThresholdFactorLabel.setText(String.format("CQ Threshold Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR,
						newValue);

			}
		});
		cqThresholdFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR));
		cqParamsPanel.add(cqThresholdFactorLabel);
		cqParamsPanel.add(cqThresholdFactorInput);

		JLabel cqSignalMinimumLabel = new JLabel("CQ Signal Minimum: ");
		cqSignalMinimumInput = new JTextField(4);
		cqSignalMinimumInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqSignalMinimumInput.getText();
				cqSignalMinimumLabel.setText(String.format("CQ Signal Minimum  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM, newValue);

			}
		});
		cqSignalMinimumInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM));
		cqParamsPanel.add(cqSignalMinimumLabel);
		cqParamsPanel.add(cqSignalMinimumInput);

		JLabel cqNormaliseThresholdLabel = new JLabel("CQ Normalise Threshold: ");
		cqNormaliseThresholdInput = new JTextField(4);
		cqNormaliseThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqNormaliseThresholdInput.getText();
				cqNormaliseThresholdLabel.setText(String.format("CQ Normalise Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD,
						newValue);

			}
		});
		cqNormaliseThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD));
		cqParamsPanel.add(cqNormaliseThresholdLabel);
		cqParamsPanel.add(cqNormaliseThresholdInput);

		JLabel cqDecibelLevelLabel = new JLabel("CQ Decibel Level: ");
		cqDecibelLevelInput = new JTextField(4);
		cqDecibelLevelInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqDecibelLevelInput.getText();
				cqDecibelLevelLabel.setText(String.format("CQ Decibel Level  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL, newValue);

			}
		});
		cqDecibelLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL));
		cqParamsPanel.add(cqDecibelLevelLabel);
		cqParamsPanel.add(cqDecibelLevelInput);

		JLabel cqCompressionLevelLabel = new JLabel("CQ Compression Level: ");
		cqCompressionLevelInput = new JTextField(4);
		cqCompressionLevelInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqCompressionLevelInput.getText();
				cqCompressionLevelLabel.setText(String.format("CQ Compression Level  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION, newValue);

			}
		});
		cqCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION));
		cqParamsPanel.add(cqCompressionLevelLabel);
		cqParamsPanel.add(cqCompressionLevelInput);

		JLabel pdCompressionLevelLabel = new JLabel("Pitch Detect Compression Level: ");
		pdCompressionLevelInput = new JTextField(4);
		pdCompressionLevelInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = pdCompressionLevelInput.getText();
				pdCompressionLevelLabel.setText(String.format("Pitch Detect Compression Level  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_COMPRESSION,
						newValue);

			}
		});
		pdCompressionLevelInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_COMPRESSION));
		cqParamsPanel.add(pdCompressionLevelLabel);
		cqParamsPanel.add(pdCompressionLevelInput);

		JLabel pdLowThresholdLabel = new JLabel("Pitch Detect Low Threshold Factor: ");
		pdLowThresholdInput = new JTextField(4);
		pdLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = pdLowThresholdInput.getText();
				pdLowThresholdLabel.setText(String.format("Pitch Detect Low Threshold Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_LOW_THRESHOLD,
						newValue);

			}
		});
		pdLowThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_LOW_THRESHOLD));
		cqParamsPanel.add(pdLowThresholdLabel);
		cqParamsPanel.add(pdLowThresholdInput);

		JLabel tunerThresholdFactorLabel = new JLabel("Tuner Threshold Factor: ");
		tunerThresholdFactorInput = new JTextField(4);
		tunerThresholdFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerThresholdFactorInput.getText();
				tunerThresholdFactorLabel.setText(String.format("Tuner Threshold Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_FACTOR,
						newValue);

			}
		});
		tunerThresholdFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_FACTOR));
		cqParamsPanel.add(tunerThresholdFactorLabel);
		cqParamsPanel.add(tunerThresholdFactorInput);

		JLabel tunerSignalMinimumLabel = new JLabel("Tuner Signal Minimum: ");
		tunerSignalMinimumInput = new JTextField(4);
		tunerSignalMinimumInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerSignalMinimumInput.getText();
				tunerSignalMinimumLabel.setText(String.format("Tuner Signal Minimum  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_MINIMUM,
						newValue);

			}
		});
		tunerSignalMinimumInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_MINIMUM));
		cqParamsPanel.add(tunerSignalMinimumLabel);
		cqParamsPanel.add(tunerSignalMinimumInput);

		parameterPanel.add(cqParamsPanel);

		JPanel tunerParamsPanel = new JPanel();
		tunerParamsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		tunerParamsPanel.setLayout(new GridLayout(0, 2));
		tunerParamsPanel
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		JLabel tunerNormaliseThresholdLabel = new JLabel("Audio Tuner Normalise Threshold: ");
		tunerNormaliseThresholdInput = new JTextField(4);
		tunerNormaliseThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerNormaliseThresholdInput.getText();
				tunerNormaliseThresholdLabel.setText(String.format("Audio Tuner Normalise Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_THRESHOLD, newValue);

			}
		});
		tunerNormaliseThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_THRESHOLD));
		tunerParamsPanel.add(tunerNormaliseThresholdLabel);
		tunerParamsPanel.add(tunerNormaliseThresholdInput);

		JLabel tunerNormaliseTroughLabel = new JLabel("Audio Tuner Normalise Trough: ");
		tunerNormaliseTroughInput = new JTextField(4);
		tunerNormaliseTroughInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerNormaliseTroughInput.getText();
				tunerNormaliseTroughLabel.setText(String.format("Audio Tuner Normalise Trough  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_TROUGH, newValue);

			}
		});
		tunerNormaliseTroughInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_TROUGH));
		tunerParamsPanel.add(tunerNormaliseTroughLabel);
		tunerParamsPanel.add(tunerNormaliseTroughInput);

		JLabel tunerNormalisePeakLabel = new JLabel("Audio Tuner Normalise Peak: ");
		tunerNormalisePeakInput = new JTextField(4);
		tunerNormalisePeakInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerNormalisePeakInput.getText();
				tunerNormalisePeakLabel.setText(String.format("Audio Tuner Normalise Peak  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_PEAK, newValue);

			}
		});
		tunerNormalisePeakInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_PEAK));
		tunerParamsPanel.add(tunerNormalisePeakLabel);
		tunerParamsPanel.add(tunerNormalisePeakInput);

		JLabel tunerHarmonicDriftFactorLabel = new JLabel("Audio Tuner Harmonic Drift Factor: ");
		tunerHarmonicDriftFactorInput = new JTextField(4);
		tunerHarmonicDriftFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerHarmonicDriftFactorInput.getText();
				tunerHarmonicDriftFactorLabel
						.setText(String.format("Audio Tuner Harmonic Drift Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_DRIFT_FACTOR, newValue);

			}
		});
		tunerHarmonicDriftFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_DRIFT_FACTOR));
		tunerParamsPanel.add(tunerHarmonicDriftFactorLabel);
		tunerParamsPanel.add(tunerHarmonicDriftFactorInput);

		Dimension minimumSize = new Dimension(1000, 1000);
		parameterPanel.setMinimumSize(minimumSize);
		this.add(parameterPanel, BorderLayout.CENTER);
	}

	private int getFFTWindowIndex(String parameter) {
		for (int i = 0; i < fftSizes.length; i++) {
			if (fftSizes[i].toString().equals(parameter)) {
				return i;
			}
		}
		return 2;
	}

	public void updateParameters() {
		audioLowPassSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS));
		audioHighPassSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS));
		n1SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SWITCH));
		n2SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SWITCH));
		n3SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N3_SWITCH));
		n4SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N4_SWITCH));
		n5SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N5_SWITCH));
		n6SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N6_SWITCH));
		harmonicWeightingSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_WEIGHTING_SWITCH));
		harmonicGuitarSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_GUITAR_SWITCH));
		peakSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_PEAK_SWITCH));
		compressionSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS));
		squareSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE));
		lowThresholdSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD));
		decibelSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL));

		normaliseSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE));
		tunerSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_TUNER));
		peaksSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_PEAKS));

		float noiseFloorFactor = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR);
		noiseFloorSlider.setValue((int) noiseFloorFactor);
		int noiseFloorMedianFilterLength = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH);
		medianFilterSizeSlider.setValue(noiseFloorMedianFilterLength);
		int minPeakSize = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE);
		minPeakSizeSlider.setValue(minPeakSize);
		int numberOfSpectralPeaks = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS);
		numberOfPeaksSlider.setValue(numberOfSpectralPeaks);
		formantFactorSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_FACTOR));
		formantHighSettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH));
		formantLowSettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW));
		formantMiddleSettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE));
		n1SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SETTING));
		n2SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SETTING));
		n3SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N3_SETTING));
		n4SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N4_SETTING));
		n5SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N5_SETTING));
		n6SettingSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N6_SETTING));
		harmonic1SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC1_SETTING));
		harmonic2SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC2_SETTING));
		harmonic3SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC3_SETTING));
		harmonic4SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC4_SETTING));
		harmonic5SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC5_SETTING));
		harmonic6SettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC6_SETTING));
		normalizeSettingSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_SETTING));
		noteHighSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_HIGH));
		noteLowSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_LOW));
		noteMaxDurationSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MAX_DURATION));
		noteMinDurationSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MIN_DURATION));
		noteSustainSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SUSTAIN));
		pitchHighSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_HIGH));
		pitchLowSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_LOW));
		formantLowFreqSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW_FREQUENCY));
		formantMidFreqSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE_FREQUENCY));
		formantHighFreqSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH_FREQUENCY));
		toneMapViewLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD));
		toneMapViewHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
		cqLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
		cqThresholdFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR));
		cqSignalMinimumInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM));
		cqNormaliseThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD));
		cqDecibelLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL));
		cqCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION));
		tunerNormaliseThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_THRESHOLD));
		tunerNormaliseTroughInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_TROUGH));
		tunerHarmonicDriftFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_DRIFT_FACTOR));
		tunerNormalisePeakInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_PEAK));
		tunerThresholdFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_FACTOR));
		tunerSignalMinimumInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_MINIMUM));
		pitchHarmonicsSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_HARMONICS));
		pdCompressionLevelInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_COMPRESSION));
		pdLowThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_LOW_THRESHOLD));
		pdCompressionSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_COMPRESS));
		pdWhitenerSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_WHITENER));
		pdKlapuriSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_KLAPURI));
		pdTarsosSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_TARSOS));
		pdWindowComboBox.setSelectedIndex(getFFTWindowIndex(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PD_WINDOW)));
		cqWindowComboBox.setSelectedIndex(getFFTWindowIndex(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_CQ_WINDOW)));
		spWindowComboBox.setSelectedIndex(getFFTWindowIndex(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SP_WINDOW)));

	}
}
