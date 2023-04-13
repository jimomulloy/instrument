package jomu.instrument.desktop.monitor.swing;

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
import java.util.logging.Level;
import java.util.logging.Logger;

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

import jomu.instrument.Instrument;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.InstrumentStoreService;

public class ParametersPanel extends JPanel {

	private static final Logger LOG = Logger.getLogger(ParametersPanel.class.getName());

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
	private AbstractButton n7SwitchCB;
	private AbstractButton n5SwitchCB;
	private AbstractButton n4SwitchCB;
	private AbstractButton harmonicWeightingSwitchCB;
	private JCheckBox harmonicGuitarSwitchCB;
	private JCheckBox harmonicAttenuateSwitchCB;
	private JCheckBox harmonicAccumulateSwitchCB;
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
	private JSlider pitchHarmonicsSlider;
	private JTextField pdCompressionLevelInput;
	private JCheckBox pdCompressionSwitchCB;
	private JCheckBox pdWhitenerSwitchCB;
	private JCheckBox pdKlapuriSwitchCB;
	private JCheckBox pdTarsosSwitchCB;
	private JTextField pdLowThresholdInput;

	private final static Integer[] fftSizes = { 256, 512, 1024, 2048, 4096, 8192, 16384, 22050, 32768, 65536, 131072 };
	private final static String[] styles = { "default", "ensemble", "guitar", "piano", "vocal" };
	private JTextField cqMinFreqCentsInput;
	private JTextField cqMaxFreqCentsInput;
	private JSlider hpsHarmonicWeightingSlider;
	private JSlider hpsPercussionWeightingSlider;
	private JSlider hpsHarmonicMedianSlider;
	private JSlider hpsPercussionMedianSlider;
	private JCheckBox hpsMedianSwitchCB;
	private JTextField hpsMaskFactorInput;
	private JSlider onsetSmoothingFactorSlider;
	private JSlider onsetEdgeFactorSlider;
	private JTextField chromaNormaliseThresholdInput;
	private JSlider chromaSmoothingFactorSlider;
	private JSlider chromaRootNoteSlider;
	private JSlider chromaDownSamplingFactorSlider;
	private JCheckBox chromaHarmonicsSwitchCB;
	private JCheckBox chromaCeilingSwitchCB;
	private JCheckBox noteScanAttenuateHarmonicsSwitchCB;
	private JCheckBox noteScanAttenuateUndertonesSwitchCB;
	private JCheckBox noteScanAttenuateSemitonesSwitchCB;
	private JTextField spLowThresholdInput;
	private JTextField spThresholdFactorInput;
	private JTextField spSignalMinimumInput;
	private JTextField spNormaliseThresholdInput;
	private JTextField spDecibelLevelInput;
	private JTextField spCompressionLevelInput;
	private JCheckBox spCompressionSwitchCB;
	private JCheckBox spSquareSwitchCB;
	private JCheckBox spLowThresholdSwitchCB;
	private JCheckBox spDecibelSwitchCB;
	private JCheckBox spNormaliseSwitchCB;
	private JCheckBox cqPreHarmonicsSwitchCB;
	private JCheckBox cqPostHarmonicsSwitchCB;
	private JCheckBox cqPreSharpenSwitchCB;
	private JCheckBox cqPostSharpenSwitchCB;
	private AbstractButton chromaChordifySwitchCB;
	private AbstractButton integrateHpsSwitchCB;
	private AbstractButton cqSharpenHarmonicSwitchCB;
	private JTextField notateCompressionLevelInput;
	private JCheckBox notateCompressionSwitchCB;
	private JTextField yinLowPassInput;
	private AbstractButton chromaCQOriginSwitchCB;
	private JCheckBox hpsCQOriginSwitchCB;
	private JCheckBox onsetCQOriginSwitchCB;
	private JCheckBox cqWhitenSwitchCB;
	private JTextField beatsThresholdInput;
	private JTextField beatsSensitivityInput;
	private JTextField onsetThresholdInput;
	private JTextField onsetSensitivityInput;
	private JTextField onsetIntervalInput;
	private JTextField percussionThresholdInput;
	private JTextField percussionSensitivityInput;
	private JTextField chromaChordifyThresholdInput;
	private JCheckBox normaliseMaxSwitchCB;
	private JCheckBox cqScaleSwitchCB;
	private AbstractButton cqCompressMaxSwitchCB;
	private JCheckBox cqCompressLogSwitchCB;
	private JCheckBox cqWhitenCompensateSwitchCB;
	private JTextField cqWhitenFactorInput;
	private JTextField cqWhitenThresholdInput;
	private Console console;
	private InstrumentStoreService iss;
	private JCheckBox chromaChordifySharpenSwitchCB;
	private JCheckBox synthesisChordsSwitchCB;
	private JCheckBox powerSquareSwitchCB;
	private JCheckBox acUndertoneRemoveSwitchCB;
	private JCheckBox acSACFSwitchCB;
	private JTextField acMaxLagInput;
	private JTextField acUndertoneThresholdInput;
	private JTextField acUndertoneRangeInput;
	private JTextField acCorrelationThresholdInput;
	private JTextField cqEnvelopeWhitenThresholdInput;
	private JTextField cqEnvelopeWhitenAttackFactorInput;
	private JTextField cqEnvelopeWhitenDecayFactorInput;
	private JCheckBox cqEnvelopeWhitenPreSwitchCB;
	private JCheckBox cqEnvelopeWhitenPostSwitchCB;
	private JTextField cqSharpenThresholdInput;
	private AbstractButton cqCalibrateSwitchCB;
	private JTextField cqCalibrateRangeInput;
	private JComboBox<String> selectStyleComboBox;

	private JTextField synthesisQuantizeRangeInput;

	private JTextField synthesisQuantizePercentInput;

	private JTextField harmonicLowNoteInput;

	private JTextField harmonicHighNoteInput;

	private JTextField noteTimbreFrequencyRangeInput;

	private JTextField noteTimbreFrequencyRatioInput;

	private JTextField noteTimbreMedianRangeInput;

	private JTextField noteTimbreMedianRatioInput;

	private JCheckBox noteTimbreCQSwitchCB;

	private JCheckBox noteTimbreNotateSwitchCB;

	public ParametersPanel() {
		super(new BorderLayout());
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.console = Instrument.getInstance().getConsole();
		this.iss = Instrument.getInstance().getStorage().getInstrumentStoreService();

		this.setBorder(new TitledBorder("Input Parameters"));

		JPanel actionPanel = new JPanel();

		final JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					parameterManager.reset();
					updateParameters();
					console.getVisor().updateParameters();

				} catch (IOException e1) {
					LOG.log(Level.SEVERE, "Reset Parameter exception", e);
					parameterManager.setParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE, "default");
					selectStyleComboBox.setSelectedIndex(getSelectStyleIndex(
							parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));
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
					console.getVisor().updateParameters();
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

		selectStyleComboBox = new JComboBox<String>(styles);
		selectStyleComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				String value = (String) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				parameterManager.setParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE, value);
				selectStyleComboBox.setSelectedIndex(getSelectStyleIndex(
						parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));
				LOG.severe(">> selectStyleComboBox: " + value + " ," + getSelectStyleIndex(
						parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));
				try {
					parameterManager.loadStyle(value);
					updateParameters();
					console.getVisor().updateParameters();
				} catch (IOException e1) {
					LOG.log(Level.SEVERE, "Select Parameter Style exception", e);
					parameterManager.setParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE, "default");
					selectStyleComboBox.setSelectedIndex(getSelectStyleIndex(
							parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));
				}
			}
		});

		// selectStyleComboBox.setSelectedIndex(
		// getSelectStyleIndex(parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));
		actionPanel.add(new JLabel("Select Style: "));
		actionPanel.add(selectStyleComboBox);

		this.add(actionPanel, BorderLayout.NORTH);

		JPanel parameterPanel = new JPanel();
		parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
		parameterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		audioLowPassSlider = new JSlider(0, 12000);
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

		audioHighPassSlider = new JSlider(0, 12000);
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
		audioComboPanel.setLayout(new GridLayout(0, 4));
		audioComboPanel
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		JLabel cqMinFreqCentsLabel = new JLabel("CQ Min Cents: ");
		cqMinFreqCentsInput = new JTextField(4);
		cqMinFreqCentsInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqMinFreqCentsInput.getText();
				cqMinFreqCentsLabel.setText(String.format("CQ Min Cents (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_MINIMUM_FREQUENCY_CENTS,
						newValue);

			}
		});
		cqMinFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_MINIMUM_FREQUENCY_CENTS));
		audioComboPanel.add(cqMinFreqCentsLabel);
		audioComboPanel.add(cqMinFreqCentsInput);

		JLabel cqMaxFreqCentsLabel = new JLabel("CQ Max Cents: ");
		cqMaxFreqCentsInput = new JTextField(4);
		cqMaxFreqCentsInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqMaxFreqCentsInput.getText();
				cqMaxFreqCentsLabel.setText(String.format("CQ Max Cents (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_MAXIMUM_FREQUENCY_CENTS,
						newValue);

			}
		});
		cqMaxFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_MAXIMUM_FREQUENCY_CENTS));
		audioComboPanel.add(cqMaxFreqCentsLabel);
		audioComboPanel.add(cqMaxFreqCentsInput);

		parameterPanel.add(audioComboPanel);

		JPanel tunerSwitchPanel = new JPanel();
		// switchPanel.setLayout(new BoxLayout(switchPanel, BoxLayout.X_AXIS));
		tunerSwitchPanel.setLayout(new GridLayout(0, 4));
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

		n7SwitchCB = new JCheckBox("n7SwitchCB");
		n7SwitchCB.setText("Audio Tuner N7 Switch");
		n7SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_N7_SWITCH,
						Boolean.toString(newValue));
			}
		});

		n7SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N7_SWITCH));
		tunerSwitchPanel.add(n7SwitchCB);

		noteScanAttenuateHarmonicsSwitchCB = new JCheckBox("noteScanAttenuateHarmonicsCB");
		noteScanAttenuateHarmonicsSwitchCB.setText("Audio Tuner Note Scan Attenuate Harmonics Switch");
		noteScanAttenuateHarmonicsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_HARMONICS,
						Boolean.toString(newValue));
			}
		});

		noteScanAttenuateHarmonicsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_HARMONICS));
		tunerSwitchPanel.add(noteScanAttenuateHarmonicsSwitchCB);

		noteScanAttenuateUndertonesSwitchCB = new JCheckBox("noteScanAttenuateUndertonesCB");
		noteScanAttenuateUndertonesSwitchCB.setText("Audio Tuner Note Scan Attenuate Undertones Switch");
		noteScanAttenuateUndertonesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_UNDERTONES,
						Boolean.toString(newValue));
			}
		});

		noteScanAttenuateUndertonesSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_UNDERTONES));
		tunerSwitchPanel.add(noteScanAttenuateUndertonesSwitchCB);

		noteScanAttenuateSemitonesSwitchCB = new JCheckBox("noteScanAttenuateSemitonesCB");
		noteScanAttenuateSemitonesSwitchCB.setText("Audio Tuner Note Scan Attenuate Semitones Switch");
		noteScanAttenuateSemitonesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_SEMITONES,
						Boolean.toString(newValue));
			}
		});

		noteScanAttenuateSemitonesSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_SEMITONES));
		tunerSwitchPanel.add(noteScanAttenuateSemitonesSwitchCB);

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

		harmonicAttenuateSwitchCB = new JCheckBox("harmonicAttenuateSwitchCB");
		harmonicAttenuateSwitchCB.setText("Audio Tuner Harmonic Attenuate Switch");
		harmonicAttenuateSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_ATTENUATE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		harmonicAttenuateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_ATTENUATE_SWITCH));
		tunerSwitchPanel.add(harmonicAttenuateSwitchCB);

		harmonicAccumulateSwitchCB = new JCheckBox("harmonicAccumulateSwitchCB");
		harmonicAccumulateSwitchCB.setText("Audio Tuner Harmonic Accumulate Switch");
		harmonicAccumulateSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_ACCUMULATE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		harmonicAccumulateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_ACCUMULATE_SWITCH));
		tunerSwitchPanel.add(harmonicAccumulateSwitchCB);

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

		noteTimbreCQSwitchCB = new JCheckBox("noteTimbreCQSwitchCB");
		noteTimbreCQSwitchCB.setText("Note Timbre CQ Switch");
		noteTimbreCQSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_CQ_SWITCH,
						Boolean.toString(newValue));
			}
		});

		noteTimbreCQSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_CQ_SWITCH));
		tunerSwitchPanel.add(noteTimbreCQSwitchCB);

		noteTimbreNotateSwitchCB = new JCheckBox("noteTimbreNotateSwitchCB");
		noteTimbreNotateSwitchCB.setText("Note Timbre Notate Switch");
		noteTimbreNotateSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_NOTATE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		noteTimbreNotateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_NOTATE_SWITCH));
		tunerSwitchPanel.add(noteTimbreNotateSwitchCB);

		hpsMedianSwitchCB = new JCheckBox("hpsMedianSwitchCB");
		hpsMedianSwitchCB.setText("HPS Median Switch");
		hpsMedianSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_SWITCH_MEDIAN,
						Boolean.toString(newValue));
			}
		});

		hpsMedianSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_SWITCH_MEDIAN));
		tunerSwitchPanel.add(hpsMedianSwitchCB);

		acUndertoneRemoveSwitchCB = new JCheckBox("acUndertoneRemoveSwitchCB");
		acUndertoneRemoveSwitchCB.setText("AutoCorrelation Undertone Remove Switch");
		acUndertoneRemoveSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_REMOVE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		acUndertoneRemoveSwitchCB.setSelected(parameterManager.getBooleanParameter(
				InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_REMOVE_SWITCH));
		tunerSwitchPanel.add(acUndertoneRemoveSwitchCB);

		acSACFSwitchCB = new JCheckBox("acSACFSwitchCB");
		acSACFSwitchCB.setText("AutoCorrelation SACF Switch");
		acSACFSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_SACF_SWITCH,
						Boolean.toString(newValue));
			}
		});

		acSACFSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_SACF_SWITCH));
		tunerSwitchPanel.add(acSACFSwitchCB);

		parameterPanel.add(tunerSwitchPanel);

		JPanel cqSwitchPanel = new JPanel();
		cqSwitchPanel.setLayout(new GridLayout(0, 4));
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

		lowThresholdSwitchCB = new JCheckBox("lowTSwitchCB");
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

		normaliseMaxSwitchCB = new JCheckBox("normaliseMaxSwitchCB");
		normaliseMaxSwitchCB.setText("CQ Normalise Max");
		normaliseMaxSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE_MAX,
						Boolean.toString(newValue));
			}
		});

		normaliseMaxSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE_MAX));
		cqSwitchPanel.add(normaliseMaxSwitchCB);

		cqScaleSwitchCB = new JCheckBox("cqScaleSwitchCB");
		cqScaleSwitchCB.setText("CQ Scale");
		cqScaleSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SCALE,
						Boolean.toString(newValue));
			}
		});

		cqScaleSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SCALE));
		cqSwitchPanel.add(cqScaleSwitchCB);

		cqCompressMaxSwitchCB = new JCheckBox("cqCompressMaxCB");
		cqCompressMaxSwitchCB.setText("CQ Compress Max");
		cqCompressMaxSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_MAX,
						Boolean.toString(newValue));
			}
		});

		cqCompressMaxSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_MAX));
		cqSwitchPanel.add(cqCompressMaxSwitchCB);

		cqCompressLogSwitchCB = new JCheckBox("cqCompressLogCB");
		cqCompressLogSwitchCB.setText("CQ Compress Log");
		cqCompressLogSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_LOG,
						Boolean.toString(newValue));
			}
		});

		cqCompressLogSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_LOG));
		cqSwitchPanel.add(cqCompressLogSwitchCB);

		cqPreHarmonicsSwitchCB = new JCheckBox("cqPreHarmonicsSwitchCB");
		cqPreHarmonicsSwitchCB.setText("CQ Pre Harmonics");
		cqPreHarmonicsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_HARMONICS,
						Boolean.toString(newValue));
			}
		});

		cqPreHarmonicsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_HARMONICS));
		cqSwitchPanel.add(cqPreHarmonicsSwitchCB);

		cqPostHarmonicsSwitchCB = new JCheckBox("cqPostHarmonicsSwitchCB");
		cqPostHarmonicsSwitchCB.setText("CQ Post Harmonics");
		cqPostHarmonicsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_HARMONICS,
						Boolean.toString(newValue));
			}
		});

		cqPostHarmonicsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_HARMONICS));
		cqSwitchPanel.add(cqPostHarmonicsSwitchCB);

		cqEnvelopeWhitenPreSwitchCB = new JCheckBox("cqEnvelopeWhitenPreSwitchCB");
		cqEnvelopeWhitenPreSwitchCB.setText("CQ Pre Envelope Whiten");
		cqEnvelopeWhitenPreSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_PRE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		cqEnvelopeWhitenPreSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_PRE_SWITCH));
		cqSwitchPanel.add(cqEnvelopeWhitenPreSwitchCB);

		cqEnvelopeWhitenPostSwitchCB = new JCheckBox("cqEnvelopeWhitenPostSwitchCB");
		cqEnvelopeWhitenPostSwitchCB.setText("CQ Post Envelope Whiten");
		cqEnvelopeWhitenPostSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_POST_SWITCH,
						Boolean.toString(newValue));
			}
		});

		cqEnvelopeWhitenPostSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_POST_SWITCH));
		cqSwitchPanel.add(cqEnvelopeWhitenPostSwitchCB);

		cqCalibrateSwitchCB = new JCheckBox("cqCalibrateSwitchCB");
		cqCalibrateSwitchCB.setText("CQ Calibrate");
		cqCalibrateSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		cqCalibrateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_SWITCH));
		cqSwitchPanel.add(cqCalibrateSwitchCB);

		cqPreSharpenSwitchCB = new JCheckBox("cqPreSharpenSwitchCB");
		cqPreSharpenSwitchCB.setText("CQ Pre Sharpen");
		cqPreSharpenSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_SHARPEN,
						Boolean.toString(newValue));
			}
		});

		cqPreSharpenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_SHARPEN));
		cqSwitchPanel.add(cqPreSharpenSwitchCB);

		cqPostSharpenSwitchCB = new JCheckBox("cqPostSharpenSwitchCB");
		cqPostSharpenSwitchCB.setText("CQ Post Sharpen");
		cqPostSharpenSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_SHARPEN,
						Boolean.toString(newValue));
			}
		});

		cqPostSharpenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_SHARPEN));
		cqSwitchPanel.add(cqPostSharpenSwitchCB);

		cqSharpenHarmonicSwitchCB = new JCheckBox("cqSharpenHarmonicSwitchCB");
		cqSharpenHarmonicSwitchCB.setText("CQ Sharpen Harmonic");
		cqSharpenHarmonicSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SHARPEN_HARMONIC,
						Boolean.toString(newValue));
			}
		});

		cqSharpenHarmonicSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SHARPEN_HARMONIC));
		cqSwitchPanel.add(cqSharpenHarmonicSwitchCB);

		cqWhitenSwitchCB = new JCheckBox("cqWhitenSwitchCB");
		cqWhitenSwitchCB.setText("CQ Whiten");
		cqWhitenSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN,
						Boolean.toString(newValue));
			}
		});

		cqWhitenSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN));
		cqSwitchPanel.add(cqWhitenSwitchCB);

		cqWhitenCompensateSwitchCB = new JCheckBox("cqWhitenCompensateSwitchCB");
		cqWhitenCompensateSwitchCB.setText("CQ Whiten Compensate");
		cqWhitenCompensateSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN_COMPENSATE,
						Boolean.toString(newValue));
			}
		});

		cqWhitenCompensateSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN_COMPENSATE));
		cqSwitchPanel.add(cqWhitenCompensateSwitchCB);

		spCompressionSwitchCB = new JCheckBox("spCompressionSwitchCB");
		spCompressionSwitchCB.setText("SP Compression");
		spCompressionSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_COMPRESS,
						Boolean.toString(newValue));
			}
		});

		spCompressionSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_COMPRESS));
		cqSwitchPanel.add(spCompressionSwitchCB);

		spSquareSwitchCB = new JCheckBox("spSquareSwitchCB");
		spSquareSwitchCB.setText("SP Square");
		spSquareSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_SQUARE,
						Boolean.toString(newValue));
			}
		});

		spSquareSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_SQUARE));
		cqSwitchPanel.add(spSquareSwitchCB);

		spLowThresholdSwitchCB = new JCheckBox("spLowSwitchCB");
		spLowThresholdSwitchCB.setText("SP Low Threshold");
		spLowThresholdSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_LOW_THRESHOLD,
						Boolean.toString(newValue));
			}
		});

		spLowThresholdSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_LOW_THRESHOLD));
		cqSwitchPanel.add(spLowThresholdSwitchCB);

		spDecibelSwitchCB = new JCheckBox("spDecibelSwitchCB");
		spDecibelSwitchCB.setText("SP Decibel");
		spDecibelSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_DECIBEL,
						Boolean.toString(newValue));
			}
		});

		spDecibelSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_DECIBEL));
		cqSwitchPanel.add(spDecibelSwitchCB);

		spNormaliseSwitchCB = new JCheckBox("spNormaliseSwitchCB");
		spNormaliseSwitchCB.setText("SP Normalise");
		spNormaliseSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_NORMALISE,
						Boolean.toString(newValue));
			}
		});

		spNormaliseSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_NORMALISE));
		cqSwitchPanel.add(spNormaliseSwitchCB);

		powerSquareSwitchCB = new JCheckBox("powerSquareSwitchCB");
		powerSquareSwitchCB.setText("Power Square");
		powerSquareSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_POWER_SQUARED_SWITCH,
						Boolean.toString(newValue));
			}
		});

		powerSquareSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_POWER_SQUARED_SWITCH));
		cqSwitchPanel.add(powerSquareSwitchCB);

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

		chromaHarmonicsSwitchCB = new JCheckBox("chromaHarmonicsSwitchCB");
		chromaHarmonicsSwitchCB.setText("Chroma Harmonics Switch");
		chromaHarmonicsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HARMONICS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		chromaHarmonicsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HARMONICS_SWITCH));
		cqSwitchPanel.add(chromaHarmonicsSwitchCB);

		chromaCeilingSwitchCB = new JCheckBox("chromaCeilingSwitchCB");
		chromaCeilingSwitchCB.setText("Chroma Ceiling Switch");
		chromaCeilingSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CEILING_SWITCH,
						Boolean.toString(newValue));
			}
		});

		chromaCeilingSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CEILING_SWITCH));
		cqSwitchPanel.add(chromaCeilingSwitchCB);

		chromaChordifySwitchCB = new JCheckBox("chromaChordifySwitchCB");
		chromaChordifySwitchCB.setText("Chroma Chordify Switch");
		chromaChordifySwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SWITCH,
						Boolean.toString(newValue));
			}
		});

		chromaChordifySwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SWITCH));
		cqSwitchPanel.add(chromaChordifySwitchCB);

		chromaChordifySharpenSwitchCB = new JCheckBox("chromaChordifySharpenSwitchCB");
		chromaChordifySharpenSwitchCB.setText("Chroma Chordify Sharpen Switch");
		chromaChordifySharpenSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SHARPEN_SWITCH,
						Boolean.toString(newValue));
			}
		});

		chromaChordifySharpenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SHARPEN_SWITCH));
		cqSwitchPanel.add(chromaChordifySharpenSwitchCB);

		chromaCQOriginSwitchCB = new JCheckBox("chromaCQOriginSwitchCB");
		chromaCQOriginSwitchCB.setText("Chroma CQ Origin Switch");
		chromaCQOriginSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CQ_ORIGIN_SWITCH,
						Boolean.toString(newValue));
			}
		});

		chromaCQOriginSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CQ_ORIGIN_SWITCH));
		cqSwitchPanel.add(chromaCQOriginSwitchCB);

		hpsCQOriginSwitchCB = new JCheckBox("hpsCQOriginSwitchCB");
		hpsCQOriginSwitchCB.setText("HPS CQ Origin Switch");
		hpsCQOriginSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_CQ_ORIGIN_SWITCH,
						Boolean.toString(newValue));
			}
		});

		hpsCQOriginSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_CQ_ORIGIN_SWITCH));
		cqSwitchPanel.add(hpsCQOriginSwitchCB);

		onsetCQOriginSwitchCB = new JCheckBox("onsetCQOriginSwitchCB");
		onsetCQOriginSwitchCB.setText("Onset CQ Origin Switch");
		onsetCQOriginSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_CQ_ORIGIN_SWITCH,
						Boolean.toString(newValue));
			}
		});

		onsetCQOriginSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_CQ_ORIGIN_SWITCH));
		cqSwitchPanel.add(onsetCQOriginSwitchCB);

		integrateHpsSwitchCB = new JCheckBox("integrateHpsSwitchCB");
		integrateHpsSwitchCB.setText("Integrate HPS");
		integrateHpsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_HPS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrateHpsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_HPS_SWITCH));
		cqSwitchPanel.add(integrateHpsSwitchCB);

		synthesisChordsSwitchCB = new JCheckBox("synthesisChordsSwitchCB");
		synthesisChordsSwitchCB.setText("Synthesis Chords");
		synthesisChordsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORDS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		synthesisChordsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORDS_SWITCH));
		cqSwitchPanel.add(synthesisChordsSwitchCB);

		notateCompressionSwitchCB = new JCheckBox("notateCompressionSwitchCB");
		notateCompressionSwitchCB.setText("Notate Compression");
		notateCompressionSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWITCH_COMPRESS,
						Boolean.toString(newValue));
			}
		});

		notateCompressionSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWITCH_COMPRESS));
		cqSwitchPanel.add(notateCompressionSwitchCB);

		parameterPanel.add(cqSwitchPanel);

		noiseFloorSlider = new JSlider(100, 500);
		final JLabel noiseFloorFactorLabel = new JLabel("Noise floor factor    :");
		noiseFloorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				double actualValue = newValue / 100.0;
				noiseFloorFactorLabel.setText(String.format("Noise floor factor (%.2f):", actualValue));
				float noiseFloorFactor = (float) actualValue;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR,
						Float.toString(noiseFloorFactor));
				// TODO repaintSpectalInfo();

			}
		});
		float noiseFloorFactor = 100.0F
				* parameterManager.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR);

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
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE,
						Integer.toString(newValue));
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

		hpsHarmonicWeightingSlider = new JSlider(0, 100);
		final JLabel hpsHarmonicWeightingLabel = new JLabel("HPS Harmonic Weighting :");
		hpsHarmonicWeightingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				hpsHarmonicWeightingLabel.setText(String.format("HPS Harmonic Weighting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_HARMONIC_WEIGHTING,
						Integer.toString(newValue));
			}
		});
		hpsHarmonicWeightingSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_HARMONIC_WEIGHTING));
		parameterPanel.add(hpsHarmonicWeightingLabel);
		parameterPanel.add(hpsHarmonicWeightingSlider);

		hpsPercussionWeightingSlider = new JSlider(0, 100);
		final JLabel hpsPercussionWeightingLabel = new JLabel("HPS PercussionWWeighting :");
		hpsPercussionWeightingSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				hpsPercussionWeightingLabel.setText(String.format("HPS PercussionW Weighting  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_PERCUSSION_WEIGHTING,
						Integer.toString(newValue));
			}
		});
		hpsPercussionWeightingSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_PERCUSSION_WEIGHTING));
		parameterPanel.add(hpsPercussionWeightingLabel);
		parameterPanel.add(hpsPercussionWeightingSlider);

		hpsHarmonicMedianSlider = new JSlider(1, 10);
		final JLabel hpsHarmonicMedianLabel = new JLabel("HPS Harmonic Median :");
		hpsHarmonicMedianSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				hpsHarmonicMedianLabel.setText(String.format("HPS Harmonic Median  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_HARMONIC_MEDIAN,
						Integer.toString(newValue));
			}
		});
		hpsHarmonicMedianSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_HARMONIC_MEDIAN));
		parameterPanel.add(hpsHarmonicMedianLabel);
		parameterPanel.add(hpsHarmonicMedianSlider);

		hpsPercussionMedianSlider = new JSlider(1, 50);
		final JLabel hpsPercussionMedianLabel = new JLabel("HPS Percussion Median :");
		hpsPercussionMedianSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				hpsPercussionMedianLabel.setText(String.format("HPS Percussion Median  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_PERCUSSION_MEDIAN,
						Integer.toString(newValue));
			}
		});
		hpsPercussionMedianSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_PERCUSSION_MEDIAN));
		parameterPanel.add(hpsPercussionMedianLabel);
		parameterPanel.add(hpsPercussionMedianSlider);

		onsetSmoothingFactorSlider = new JSlider(0, 100);
		final JLabel onsetSmoothingFactorLabel = new JLabel("ONSET Smoothing Factor :");
		onsetSmoothingFactorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				onsetSmoothingFactorLabel.setText(String.format("ONSET Smoothing Factor  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SMOOTHING_FACTOR,
						Integer.toString(newValue));
			}
		});
		onsetSmoothingFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SMOOTHING_FACTOR));
		parameterPanel.add(onsetSmoothingFactorLabel);
		parameterPanel.add(onsetSmoothingFactorSlider);

		onsetEdgeFactorSlider = new JSlider(0, 100);
		final JLabel onsetEdgeFactorLabel = new JLabel("ONSET Edge Factor :");
		onsetEdgeFactorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				onsetEdgeFactorLabel.setText(String.format("ONSET Edge Factor  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_EDGE_FACTOR,
						Integer.toString(newValue));
			}
		});
		onsetEdgeFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_EDGE_FACTOR));
		parameterPanel.add(onsetEdgeFactorLabel);
		parameterPanel.add(onsetEdgeFactorSlider);

		chromaSmoothingFactorSlider = new JSlider(1, 20);
		final JLabel chromaSmoothingFactorLabel = new JLabel("CHROMA Smoothing Factor :");
		chromaSmoothingFactorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				chromaSmoothingFactorLabel.setText(String.format("CHROMA Smoothing Factor  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_SMOOTH_FACTOR,
						Integer.toString(newValue));
			}
		});
		chromaSmoothingFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_SMOOTH_FACTOR));
		parameterPanel.add(chromaSmoothingFactorLabel);
		parameterPanel.add(chromaSmoothingFactorSlider);

		chromaDownSamplingFactorSlider = new JSlider(1, 20);
		final JLabel chromaDownSamplingFactorLabel = new JLabel("CHROMA DownS ampling Factor :");
		chromaDownSamplingFactorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				chromaDownSamplingFactorLabel.setText(String.format("CHROMA Down Sampling Factor  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_DOWNSAMPLE_FACTOR,
						Integer.toString(newValue));
			}
		});
		chromaDownSamplingFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_DOWNSAMPLE_FACTOR));
		parameterPanel.add(chromaDownSamplingFactorLabel);
		parameterPanel.add(chromaDownSamplingFactorSlider);

		chromaRootNoteSlider = new JSlider(12, 156);
		final JLabel chromaRootNoteLabel = new JLabel("CHROMA Root Note :");
		chromaRootNoteSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				chromaRootNoteLabel.setText(String.format("CHROMA Root Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_ROOT_NOTE,
						Integer.toString(newValue));
			}
		});
		chromaRootNoteSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_ROOT_NOTE));
		parameterPanel.add(chromaRootNoteLabel);
		parameterPanel.add(chromaRootNoteSlider);

		formantFactorSlider = new JSlider(1, 20);
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

		JLabel chromaNormaliseThresholdLabel = new JLabel("CHROMA Normalise Threshold: ");
		chromaNormaliseThresholdInput = new JTextField(4);
		chromaNormaliseThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = chromaNormaliseThresholdInput.getText();
				chromaNormaliseThresholdLabel.setText(String.format("CHROMA Normalise Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD,
						newValue);

			}
		});
		chromaNormaliseThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD));
		cqParamsPanel.add(chromaNormaliseThresholdLabel);
		cqParamsPanel.add(chromaNormaliseThresholdInput);

		JLabel chromaChordifyThresholdLabel = new JLabel("CHROMA Chordify Threshold: ");
		chromaChordifyThresholdInput = new JTextField(4);
		chromaChordifyThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = chromaChordifyThresholdInput.getText();
				chromaChordifyThresholdLabel.setText(String.format("CHROMA Chordify (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_THRESHOLD,
						newValue);

			}
		});
		chromaChordifyThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_THRESHOLD));
		cqParamsPanel.add(chromaChordifyThresholdLabel);
		cqParamsPanel.add(chromaChordifyThresholdInput);

		JLabel harmonicLowNoteLabel = new JLabel("Harmonic Low Note: ");
		harmonicLowNoteInput = new JTextField(4);
		harmonicLowNoteInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = harmonicLowNoteInput.getText();
				harmonicLowNoteLabel.setText(String.format("Harmonic Low Note  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_LOW_NOTE, newValue);

			}
		});
		harmonicLowNoteInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_LOW_NOTE));
		cqParamsPanel.add(harmonicLowNoteLabel);
		cqParamsPanel.add(harmonicLowNoteInput);

		JLabel harmonicHighNoteLabel = new JLabel("Harmonic High Note: ");
		harmonicHighNoteInput = new JTextField(4);
		harmonicHighNoteInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = harmonicHighNoteInput.getText();
				harmonicHighNoteLabel.setText(String.format("Harmonic High Note  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_HIGH_NOTE, newValue);

			}
		});
		harmonicHighNoteInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_HIGH_NOTE));
		cqParamsPanel.add(harmonicHighNoteLabel);
		cqParamsPanel.add(harmonicHighNoteInput);

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

		JLabel cqEnvelopeWhitenThresholdLabel = new JLabel("CQ Envelope Whiten Threshold: ");
		cqEnvelopeWhitenThresholdInput = new JTextField(4);
		cqEnvelopeWhitenThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqEnvelopeWhitenThresholdInput.getText();
				cqEnvelopeWhitenThresholdLabel.setText(String.format("CQ Envelope Whiten Threshold (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_THRESHOLD,
						newValue);

			}
		});
		cqEnvelopeWhitenThresholdInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_THRESHOLD));
		cqParamsPanel.add(cqEnvelopeWhitenThresholdLabel);
		cqParamsPanel.add(cqEnvelopeWhitenThresholdInput);

		JLabel cqEnvelopeWhitenAttackFactorLabel = new JLabel("CQ Envelope Whiten Attack Factor: ");
		cqEnvelopeWhitenAttackFactorInput = new JTextField(4);
		cqEnvelopeWhitenAttackFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqEnvelopeWhitenAttackFactorInput.getText();
				cqEnvelopeWhitenAttackFactorLabel
						.setText(String.format("CQ Envelope Whiten Attack Factor (%s):", newValue));
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_ATTACK_FACTOR, newValue);

			}
		});
		cqEnvelopeWhitenAttackFactorInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_ATTACK_FACTOR));
		cqParamsPanel.add(cqEnvelopeWhitenAttackFactorLabel);
		cqParamsPanel.add(cqEnvelopeWhitenAttackFactorInput);

		JLabel cqEnvelopeWhitenDecayFactorLabel = new JLabel("CQ Envelope Whiten Decay Factor: ");
		cqEnvelopeWhitenDecayFactorInput = new JTextField(4);
		cqEnvelopeWhitenDecayFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqEnvelopeWhitenDecayFactorInput.getText();
				cqEnvelopeWhitenDecayFactorLabel
						.setText(String.format("CQ Envelope Whiten Decay Factor (%s):", newValue));
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_DECAY_FACTOR, newValue);

			}
		});
		cqEnvelopeWhitenDecayFactorInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_DECAY_FACTOR));
		cqParamsPanel.add(cqEnvelopeWhitenDecayFactorLabel);
		cqParamsPanel.add(cqEnvelopeWhitenDecayFactorInput);

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

		JLabel cqCalibrateRangeLabel = new JLabel("CQ Calibrate Range: ");
		cqCalibrateRangeInput = new JTextField(4);
		cqCalibrateRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqCalibrateRangeInput.getText();
				cqCalibrateRangeLabel.setText(String.format("CQ Calibrate Range  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_RANGE, newValue);

			}
		});
		cqCalibrateRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_RANGE));
		cqParamsPanel.add(cqCalibrateRangeLabel);
		cqParamsPanel.add(cqCalibrateRangeInput);

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

		JLabel cqSharpenThresholdLabel = new JLabel("CQ Sharpen Threshold: ");
		cqSharpenThresholdInput = new JTextField(4);
		cqSharpenThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqSharpenThresholdInput.getText();
				cqSharpenThresholdLabel.setText(String.format("CQ Sharpen Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SHARPEN_THRESHOLD,
						newValue);

			}
		});
		cqSharpenThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SHARPEN_THRESHOLD));
		cqParamsPanel.add(cqSharpenThresholdLabel);
		cqParamsPanel.add(cqSharpenThresholdInput);

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

		JLabel cqWhitenFactorLabel = new JLabel("CQ Whiten Factor: ");
		cqWhitenFactorInput = new JTextField(4);
		cqWhitenFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqWhitenFactorInput.getText();
				cqWhitenFactorLabel.setText(String.format("CQ Whiten Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_FACTOR, newValue);

			}
		});
		cqWhitenFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_FACTOR));
		cqParamsPanel.add(cqWhitenFactorLabel);
		cqParamsPanel.add(cqWhitenFactorInput);

		JLabel cqWhitenThresholdLabel = new JLabel("CQ Whiten Threshold: ");
		cqWhitenThresholdInput = new JTextField(4);
		cqWhitenThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqWhitenThresholdInput.getText();
				cqWhitenThresholdLabel.setText(String.format("CQ Whiten Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_THRESHOLD,
						newValue);

			}
		});
		cqWhitenThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_THRESHOLD));
		cqParamsPanel.add(cqWhitenThresholdLabel);
		cqParamsPanel.add(cqWhitenThresholdInput);

		JLabel spLowThresholdLabel = new JLabel("SP Low Threshold: ");
		spLowThresholdInput = new JTextField(4);
		spLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = spLowThresholdInput.getText();
				spLowThresholdLabel.setText(String.format("SP Low Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_LOW_THRESHOLD, newValue);

			}
		});
		spLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_LOW_THRESHOLD));
		cqParamsPanel.add(spLowThresholdLabel);
		cqParamsPanel.add(spLowThresholdInput);

		JLabel spSignalMinimumLabel = new JLabel("SP Signal Minimum: ");
		spSignalMinimumInput = new JTextField(4);
		spSignalMinimumInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = spSignalMinimumInput.getText();
				spSignalMinimumLabel.setText(String.format("SP Signal Minimum  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SIGNAL_MINIMUM, newValue);

			}
		});
		spSignalMinimumInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SIGNAL_MINIMUM));
		cqParamsPanel.add(spSignalMinimumLabel);
		cqParamsPanel.add(spSignalMinimumInput);

		JLabel spNormaliseThresholdLabel = new JLabel("SP Normalise Threshold: ");
		spNormaliseThresholdInput = new JTextField(4);
		spNormaliseThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = spNormaliseThresholdInput.getText();
				spNormaliseThresholdLabel.setText(String.format("SP Normalise Threshold  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_NORMALISE_THRESHOLD,
						newValue);

			}
		});
		spNormaliseThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_NORMALISE_THRESHOLD));
		cqParamsPanel.add(spNormaliseThresholdLabel);
		cqParamsPanel.add(spNormaliseThresholdInput);

		JLabel spDecibelLevelLabel = new JLabel("SP Decibel Level: ");
		spDecibelLevelInput = new JTextField(4);
		spDecibelLevelInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = spDecibelLevelInput.getText();
				spDecibelLevelLabel.setText(String.format("SP Decibel Level  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_DECIBEL_LEVEL, newValue);

			}
		});
		spDecibelLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_DECIBEL_LEVEL));
		cqParamsPanel.add(spDecibelLevelLabel);
		cqParamsPanel.add(spDecibelLevelInput);

		JLabel spCompressionLevelLabel = new JLabel("SP Compression Level: ");
		spCompressionLevelInput = new JTextField(4);
		spCompressionLevelInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = spCompressionLevelInput.getText();
				spCompressionLevelLabel.setText(String.format("SP Compression Level  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_COMPRESSION, newValue);

			}
		});
		spCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_COMPRESSION));
		cqParamsPanel.add(spCompressionLevelLabel);
		cqParamsPanel.add(spCompressionLevelInput);

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

		JLabel beatsThresholdLabel = new JLabel("Beats Threshold Factor: ");
		beatsThresholdInput = new JTextField(4);
		beatsThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = beatsThresholdInput.getText();
				beatsThresholdLabel.setText(String.format("Beats Threshold Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_BEATS_THRESHOLD, newValue);

			}
		});
		beatsThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_BEATS_THRESHOLD));
		cqParamsPanel.add(beatsThresholdLabel);
		cqParamsPanel.add(beatsThresholdInput);

		JLabel beatsSensitivityLabel = new JLabel("Beats Sensitivity Factor: ");
		beatsSensitivityInput = new JTextField(4);
		beatsSensitivityInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = beatsSensitivityInput.getText();
				beatsSensitivityLabel.setText(String.format("Beats Sensitivity Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_BEATS_SENSITIVITY, newValue);

			}
		});
		beatsSensitivityInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_BEATS_SENSITIVITY));
		cqParamsPanel.add(beatsSensitivityLabel);
		cqParamsPanel.add(beatsSensitivityInput);

		JLabel onsetThresholdLabel = new JLabel("Onset Threshold Factor: ");
		onsetThresholdInput = new JTextField(4);
		onsetThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = onsetThresholdInput.getText();
				onsetThresholdLabel.setText(String.format("Onset Threshold Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_THRESHOLD, newValue);

			}
		});
		onsetThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_THRESHOLD));
		cqParamsPanel.add(onsetThresholdLabel);
		cqParamsPanel.add(onsetThresholdInput);

		JLabel onsetSensitivityLabel = new JLabel("Onset Sensitivity Factor: ");
		onsetSensitivityInput = new JTextField(4);
		onsetSensitivityInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = onsetSensitivityInput.getText();
				onsetSensitivityLabel.setText(String.format("Onset Sensitivity Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SENSITIVITY, newValue);

			}
		});
		onsetSensitivityInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SENSITIVITY));
		cqParamsPanel.add(onsetSensitivityLabel);
		cqParamsPanel.add(onsetSensitivityInput);

		JLabel percussionThresholdLabel = new JLabel("Percussion Threshold Factor: ");
		percussionThresholdInput = new JTextField(4);
		percussionThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = percussionThresholdInput.getText();
				percussionThresholdLabel.setText(String.format("Percussion Threshold Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_THRESHOLD,
						newValue);

			}
		});
		percussionThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_THRESHOLD));
		cqParamsPanel.add(percussionThresholdLabel);
		cqParamsPanel.add(percussionThresholdInput);

		JLabel percussionSensitivityLabel = new JLabel("Percussion Sensitivity Factor: ");
		percussionSensitivityInput = new JTextField(4);
		percussionSensitivityInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = percussionSensitivityInput.getText();
				percussionSensitivityLabel.setText(String.format("Percussion Sensitivity Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_SENSITIVITY,
						newValue);

			}
		});
		percussionSensitivityInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_SENSITIVITY));
		cqParamsPanel.add(percussionSensitivityLabel);
		cqParamsPanel.add(percussionSensitivityInput);

		JLabel onsetIntervalLabel = new JLabel("Onset Interval Factor: ");
		onsetIntervalInput = new JTextField(4);
		onsetIntervalInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = onsetIntervalInput.getText();
				onsetIntervalLabel.setText(String.format("Onset Interval Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_INTERVAL, newValue);

			}
		});
		onsetIntervalInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_INTERVAL));
		cqParamsPanel.add(onsetIntervalLabel);
		cqParamsPanel.add(onsetIntervalInput);

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

		JLabel noteTimbreFrequencyRangeLabel = new JLabel("Note Timbre Frequency Range: ");
		noteTimbreFrequencyRangeInput = new JTextField(4);
		noteTimbreFrequencyRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTimbreFrequencyRangeInput.getText();
				noteTimbreFrequencyRangeLabel.setText(String.format("Note Timbre Frequency Range (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RANGE,
						newValue);

			}
		});
		noteTimbreFrequencyRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RANGE));
		cqParamsPanel.add(noteTimbreFrequencyRangeLabel);
		cqParamsPanel.add(noteTimbreFrequencyRangeInput);

		JLabel noteTimbreFrequencyRatioLabel = new JLabel("Note Timbre Frequency Ratio: ");
		noteTimbreFrequencyRatioInput = new JTextField(4);
		noteTimbreFrequencyRatioInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTimbreFrequencyRatioInput.getText();
				noteTimbreFrequencyRatioLabel.setText(String.format("Note Timbre Frequency Ratio (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RATIO,
						newValue);

			}
		});
		noteTimbreFrequencyRatioInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RATIO));
		cqParamsPanel.add(noteTimbreFrequencyRatioLabel);
		cqParamsPanel.add(noteTimbreFrequencyRatioInput);

		JLabel noteTimbreMedianRangeLabel = new JLabel("Note Timbre Median Range: ");
		noteTimbreMedianRangeInput = new JTextField(4);
		noteTimbreMedianRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTimbreMedianRangeInput.getText();
				noteTimbreMedianRangeLabel.setText(String.format("Note Timbre Median Range (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RANGE, newValue);

			}
		});
		noteTimbreMedianRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RANGE));
		cqParamsPanel.add(noteTimbreMedianRangeLabel);
		cqParamsPanel.add(noteTimbreMedianRangeInput);

		JLabel noteTimbreMedianRatioLabel = new JLabel("Note Timbre Median Ratio: ");
		noteTimbreMedianRatioInput = new JTextField(4);
		noteTimbreMedianRatioInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTimbreMedianRatioInput.getText();
				noteTimbreMedianRatioLabel.setText(String.format("Note Timbre Median Ratio (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RATIO, newValue);

			}
		});
		noteTimbreMedianRatioInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RATIO));
		cqParamsPanel.add(noteTimbreMedianRatioLabel);
		cqParamsPanel.add(noteTimbreMedianRatioInput);

		JLabel notateCompressionLevelLabel = new JLabel("Notate Compression Level: ");
		notateCompressionLevelInput = new JTextField(4);
		notateCompressionLevelInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = notateCompressionLevelInput.getText();
				notateCompressionLevelLabel.setText(String.format("Notate Compression Level  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION, newValue);

			}
		});
		notateCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION));
		cqParamsPanel.add(notateCompressionLevelLabel);
		cqParamsPanel.add(notateCompressionLevelInput);

		JLabel synthesisQuantizeRangeLabel = new JLabel("Synthesis Quantize Range: ");
		synthesisQuantizeRangeInput = new JTextField(4);
		synthesisQuantizeRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisQuantizeRangeInput.getText();
				synthesisQuantizeRangeLabel.setText(String.format("Synthesis Quantize Range  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE,
						newValue);

			}
		});
		synthesisQuantizeRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE));
		cqParamsPanel.add(synthesisQuantizeRangeLabel);
		cqParamsPanel.add(synthesisQuantizeRangeInput);

		JLabel synthesisQuantizePercentLabel = new JLabel("Synthesis Quantize Pecent: ");
		synthesisQuantizePercentInput = new JTextField(4);
		synthesisQuantizePercentInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisQuantizePercentInput.getText();
				synthesisQuantizePercentLabel.setText(String.format("Synthesis Quantize Pecent  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT,
						newValue);

			}
		});
		synthesisQuantizePercentInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT));
		cqParamsPanel.add(synthesisQuantizePercentLabel);
		cqParamsPanel.add(synthesisQuantizePercentInput);

		JLabel yinLowPassLabel = new JLabel("YIN Low Pass: ");
		yinLowPassInput = new JTextField(4);
		yinLowPassInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = yinLowPassInput.getText();
				yinLowPassLabel.setText(String.format("YIN Low Pass (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_YIN_LOW_PASS, newValue);

			}
		});
		yinLowPassInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_YIN_LOW_PASS));
		cqParamsPanel.add(yinLowPassLabel);
		cqParamsPanel.add(yinLowPassInput);

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

		JLabel hpsMaskFactorLabel = new JLabel("HPS Mask Factor: ");
		hpsMaskFactorInput = new JTextField(4);
		hpsMaskFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = hpsMaskFactorInput.getText();
				hpsMaskFactorLabel.setText(String.format("HPS Mask Factor  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_MASK_FACTOR, newValue);

			}
		});
		hpsMaskFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_MASK_FACTOR));
		tunerParamsPanel.add(hpsMaskFactorLabel);
		tunerParamsPanel.add(hpsMaskFactorInput);

		JLabel acMaxLagLabel = new JLabel("Autocorrelation Max Lag: ");
		acMaxLagInput = new JTextField(4);
		acMaxLagInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = acMaxLagInput.getText();
				acMaxLagLabel.setText(String.format("Autocorrelation Max Lag  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_MAX_LAG,
						newValue);

			}
		});
		acMaxLagInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_MAX_LAG));
		tunerParamsPanel.add(acMaxLagLabel);
		tunerParamsPanel.add(acMaxLagInput);

		JLabel acUndertoneThresholdLabel = new JLabel("Autocorrelation Undertone Threshold: ");
		acUndertoneThresholdInput = new JTextField(4);
		acUndertoneThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = acUndertoneThresholdInput.getText();
				acUndertoneThresholdLabel
						.setText(String.format("Autocorrelation Undertone Threshold  (%s):", newValue));
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_THRESHOLD, newValue);

			}
		});
		acUndertoneThresholdInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_THRESHOLD));
		tunerParamsPanel.add(acUndertoneThresholdLabel);
		tunerParamsPanel.add(acUndertoneThresholdInput);

		JLabel acUndertoneRangeLabel = new JLabel("Autocorrelation Undertone Range: ");
		acUndertoneRangeInput = new JTextField(4);
		acUndertoneRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = acUndertoneRangeInput.getText();
				acUndertoneRangeLabel.setText(String.format("Autocorrelation Undertone Range  (%s):", newValue));
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_RANGE, newValue);

			}
		});
		acUndertoneRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_RANGE));
		tunerParamsPanel.add(acUndertoneRangeLabel);
		tunerParamsPanel.add(acUndertoneRangeInput);

		JLabel acCorrelationThresholdLabel = new JLabel("Autocorrelation Threshold: ");
		acCorrelationThresholdInput = new JTextField(4);
		acCorrelationThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = acCorrelationThresholdInput.getText();
				acCorrelationThresholdLabel.setText(String.format("Autocorrelation Threshold  (%s):", newValue));
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_CORRELATION_THRESHOLD, newValue);

			}
		});
		acCorrelationThresholdInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_CORRELATION_THRESHOLD));
		tunerParamsPanel.add(acCorrelationThresholdLabel);
		tunerParamsPanel.add(acCorrelationThresholdInput);

		parameterPanel.add(tunerParamsPanel);

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

	private int getSelectStyleIndex(String parameter) {
		for (int i = 0; i < styles.length; i++) {
			if (styles[i].toString().equals(parameter)) {
				return i;
			}
		}
		return 0;
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
		n7SwitchCB.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N7_SWITCH));

		harmonicWeightingSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_WEIGHTING_SWITCH));
		harmonicGuitarSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_GUITAR_SWITCH));
		harmonicAttenuateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_ATTENUATE_SWITCH));
		harmonicAccumulateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_ACCUMULATE_SWITCH));
		peakSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_PEAK_SWITCH));
		compressionSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS));
		notateCompressionSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWITCH_COMPRESS));
		squareSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE));
		lowThresholdSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD));
		decibelSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL));
		normaliseSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE));
		normaliseMaxSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE_MAX));
		cqScaleSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SCALE));
		cqCompressMaxSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_MAX));
		cqCompressLogSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_LOG));
		cqPreHarmonicsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_HARMONICS));
		cqPostHarmonicsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_HARMONICS));
		cqEnvelopeWhitenPreSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_PRE_SWITCH));
		cqEnvelopeWhitenPostSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_POST_SWITCH));
		cqCalibrateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_SWITCH));
		cqPreSharpenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_SHARPEN));
		cqPostSharpenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_SHARPEN));
		cqSharpenHarmonicSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SHARPEN_HARMONIC));
		cqWhitenSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN));
		cqWhitenCompensateSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN_COMPENSATE));

		chromaCQOriginSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CQ_ORIGIN_SWITCH));
		hpsCQOriginSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_CQ_ORIGIN_SWITCH));
		onsetCQOriginSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_CQ_ORIGIN_SWITCH));

		spCompressionSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_COMPRESS));
		spSquareSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_SQUARE));
		spLowThresholdSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_LOW_THRESHOLD));
		spDecibelSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_DECIBEL));
		spNormaliseSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_NORMALISE));

		powerSquareSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_POWER_SQUARED_SWITCH));

		noteScanAttenuateHarmonicsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_HARMONICS));
		noteScanAttenuateUndertonesSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_UNDERTONES));
		noteScanAttenuateSemitonesSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_SEMITONES));

		tunerSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_TUNER));
		peaksSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_PEAKS));
		float noiseFloorFactor = 100.0F
				* parameterManager.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR);
		noiseFloorSlider.setValue((int) noiseFloorFactor);
		medianFilterSizeSlider.setValue(parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH));
		minPeakSizeSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE));
		numberOfPeaksSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS));
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
		cqLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
		cqThresholdFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR));
		cqEnvelopeWhitenThresholdInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_THRESHOLD));
		cqEnvelopeWhitenAttackFactorInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_ATTACK_FACTOR));
		cqEnvelopeWhitenDecayFactorInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_DECAY_FACTOR));
		cqSignalMinimumInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM));
		cqCalibrateRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_RANGE));
		cqNormaliseThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD));
		cqSharpenThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SHARPEN_THRESHOLD));
		cqDecibelLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL));
		cqCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION));
		cqWhitenFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_FACTOR));
		cqWhitenThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_THRESHOLD));
		cqLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD));

		harmonicLowNoteInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_LOW_NOTE));
		harmonicHighNoteInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_HIGH_NOTE));

		notateCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION));

		synthesisQuantizeRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE));
		synthesisQuantizePercentInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT));

		yinLowPassInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_YIN_LOW_PASS));

		spSignalMinimumInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SIGNAL_MINIMUM));
		spNormaliseThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_NORMALISE_THRESHOLD));
		spDecibelLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_DECIBEL_LEVEL));
		spCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_COMPRESSION));
		spLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_LOW_THRESHOLD));

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
		cqMaxFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS));
		cqMinFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS));
		hpsHarmonicWeightingSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_HARMONIC_WEIGHTING));
		hpsHarmonicMedianSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_HARMONIC_MEDIAN));
		hpsPercussionWeightingSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_PERCUSSION_WEIGHTING));
		hpsPercussionMedianSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_PERCUSSION_MEDIAN));
		hpsMedianSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_SWITCH_MEDIAN));
		hpsMaskFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_MASK_FACTOR));

		acCorrelationThresholdInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_CORRELATION_THRESHOLD));
		acMaxLagInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_MAX_LAG));
		acUndertoneRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_RANGE));
		acUndertoneThresholdInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_THRESHOLD));
		acSACFSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_SACF_SWITCH));
		acUndertoneRemoveSwitchCB.setSelected(parameterManager.getBooleanParameter(
				InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_REMOVE_SWITCH));

		onsetSmoothingFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SMOOTHING_FACTOR));
		onsetEdgeFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_EDGE_FACTOR));
		chromaNormaliseThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD));
		chromaChordifyThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_THRESHOLD));
		chromaSmoothingFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_SMOOTH_FACTOR));
		chromaRootNoteSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_ROOT_NOTE));
		chromaDownSamplingFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_DOWNSAMPLE_FACTOR));
		chromaHarmonicsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HARMONICS_SWITCH));
		chromaCeilingSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CEILING_SWITCH));
		chromaChordifySwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SWITCH));
		integrateHpsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_HPS_SWITCH));
		synthesisChordsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORDS_SWITCH));

		beatsThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_BEATS_THRESHOLD));
		beatsSensitivityInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_BEATS_SENSITIVITY));
		onsetThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_THRESHOLD));
		onsetSensitivityInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SENSITIVITY));
		onsetIntervalInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_INTERVAL));
		percussionSensitivityInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_SENSITIVITY));
		percussionThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_THRESHOLD));
		selectStyleComboBox.setSelectedIndex(
				getSelectStyleIndex(parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));

		noteTimbreFrequencyRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RANGE));
		noteTimbreFrequencyRatioInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RATIO));
		noteTimbreMedianRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RANGE));
		noteTimbreMedianRatioInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RATIO));
		noteTimbreCQSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_CQ_SWITCH));
		noteTimbreNotateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_NOTATE_SWITCH));
	}
}
