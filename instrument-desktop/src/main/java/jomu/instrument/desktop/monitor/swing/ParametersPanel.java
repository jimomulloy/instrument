package jomu.instrument.desktop.monitor.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
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
import jomu.instrument.store.Storage;

public class ParametersPanel extends JPanel {

	private static final Logger LOG = Logger.getLogger(ParametersPanel.class.getName());

	private final static Integer[] fftSizes = { 256, 512, 1024, 2048, 4096, 8192, 16384, 22050, 32768, 65536, 131072 };
	private final static String[] styles = {
			"default", "watcher", "dawson", "yes", "sustain", "metronome", "vw-hps-peaks",
			"finzi", "vaughn-williams", "peaks", "vw",
			"genesis",
			"whiten",
			"tapes-smoother", "tapes-busy", "tapes-fast",
			"chords", "synth", "prog", "normalised", "ensemble",
			"guitar",
			"piano",
			"vocal",
			"vocal-folk", "vocal-male", "vocal-female", "tapes", "robin", "birds",
			"blackbird",
			"bird-alt1",
			"bird-best", "bird-scaled", "bird-mix", "classical", "folk", "folky", "folky-clean", "folk-pluck", "bach",
			"birds-bach", "birds-x", "beethoven", "brass", "compresschord", "piano-harp", "epiano", "epiano-arp",
			"epiano-chord",
			"epiano-chords",
			"epiano-chords-staccato", "guitarstrum", "eguitar", "hpschord", "neon", "neon-peaked", "neon-synth",
			"billevans", "inverted", "likesomeone", "likesomeone2", "likesomeone3", "jona", "beatles", "stpauls",
			"stpauls2", "holst", "abide", "loopback", "loopmidi", "user", "ha1", "BB-SQ-1", "harmonics", "ai-pc1",
			"ai-pc2", "ai-pc2", "ai-c2eg-2-g-cq", "ai-c2eg-g-cq", "ai-c2eg-2-g-tuner", "ai-c2eg-g-tuner",
			"ai-c3eg-2-ep-cq", "ai-c3eg-2-ep-tuner",
			"ai-c3eg-2-ng-cq", "ai-c3eg-2-ng-tuner", "ai-c3eg-ep-cq", "ai-c3eg-ep-tuner", "ai-c3eg-ng-cq",
			"ai-c3eg-ng-tuner",
			"ai-c3-ep", "ai-c3-ng", "ai-c3eg-2-ep-best", "ai-voice-best", "ai-voice-male-best",
			"ai-voice-choir-best", "breton", "adamski", "tapes-a10", "da47n", "nyman-tapes" };

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
	private JSlider formantLowSlider;
	private JSlider formantMidSlider;
	private JSlider formantHighSlider;
	private JSlider formantFactorSlider;
	private JSlider formantRangeSlider;
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
	private Console console;
	private InstrumentStoreService iss;
	private JCheckBox chromaChordifySharpenSwitchCB;
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
	private JTextField harmonicLowNoteInput;
	private JTextField harmonicHighNoteInput;
	private JTextField noteTimbreFrequencyRangeInput;
	private JTextField noteTimbreFrequencyRatioInput;
	private JTextField noteTimbreMedianRangeInput;
	private JTextField noteTimbreMedianRatioInput;
	private JCheckBox noteTimbreCQSwitchCB;
	private JCheckBox noteTimbreNotateSwitchCB;

	private JCheckBox notateApplyFormantsSwitchCB;

	private JCheckBox cqAdaptiveWhitenSwitchCB;

	private JTextField cqAdaptiveWhitenFactorInput;

	private JTextField cqAdaptiveWhitenThresholdInput;

	private JTextField pidPFactorInput;

	private JTextField pidDFactorInput;

	private JTextField pidIFactorInput;

	private JSlider bottomFormantFactorSlider;

	private JSlider topFormantFactorSlider;

	private JSlider bottomFormantRangeSlider;

	private JSlider topFormantRangeSlider;

	private JSlider bottomFormantHighSlider;

	private JSlider topFormantHighSlider;

	private JSlider bottomFormantLowSlider;

	private JSlider topFormantLowSlider;

	private JSlider topFormantMidSlider;

	private JSlider bottomFormantMidSlider;

	private JSlider notePeaksMaxDurationSlider;

	private JSlider notePeaksMinDurationSlider;

	private JSlider noteSpectralMaxDurationSlider;

	private JSlider noteSpectralMinDurationSlider;

	private JCheckBox cqCalibrateForwardSwitchCB;

	private JTextField noteTrackerMaxTracksUpperInput;

	private JTextField noteTrackerMaxTracksLowerInput;

	private JTextField noteTrackerClearRangeUpperInput;

	private JTextField noteTrackerClearRangeLowerInput;

	private JTextField noteTrackerDiscardTrackRangeInput;

	private JTextField noteTrackerOverlapSalientNoteRangeInput;

	private JTextField noteTrackerOverlapSalientTimeRangeInput;

	private JTextField noteTrackerSalientNoteRangeInput;

	private JTextField noteTrackerSalientTimeNoteFactorInput;

	private JSlider audioSmoothFactorSlider;

	Storage storage;

	private JTextField noteTrackerSalientTimeRangeInput;

	private JTextField tunerHarmonicSweepInput;

	private JTextField tunerClearNoteEdgeFactorInput;

	private JCheckBox tunerClearHeadNotesSwitchCB;

	private JCheckBox tunerClearTailNotesSwitchCB;

	private JCheckBox integrateCQSwitchCB;

	private JCheckBox integratePeaksSwitchCB;

	private JCheckBox integrateSpectralSwitchCB;

	private JCheckBox integratePitchSwitchCB;

	private AbstractButton integrateSPSwitchCB;

	private JCheckBox integrateTPSwitchCB;

	private JCheckBox integrateYINSwitchCB;

	private JCheckBox integrateSACFSwitchCB;

	private JCheckBox integrateMFCCSwitchCB;

	private JTextField notateSweepRangeInput;

	private JCheckBox notatePeaksSwitchCB;

	private JTextField cqBinsPerOctaveInput;

	private JCheckBox tunerClearVibratoNotesSwitchCB;

	private JCheckBox tunerClearIsolatedNotesSwitchCB;

	private JTextField tunerClearIsolatedNotesPowerFactorInput;

	private JTextField tunerClearIsolatedNotesTimeRangeInput;

	private JTextField tunerClearIsolatedNotesPitchRangeInput;

	private JTextField onsetSilenceThresholdInput;

	private JCheckBox exportDeltaSwitchCB;

	private JTextField sinkSweepRangeInput;

	private JTextField onsetPeaksThresholdInput;

	private JTextField onsetPeaksEdgeFactorInput;

	private JTextField onsetPeaksSweepInput;

	private JCheckBox cqMicroToneSwitchCB;

	private JCheckBox onsetHpsSwitchCB;

	private JCheckBox chromaHpsSwitchCB;

	private JCheckBox integrationEnvelopeWhitenSwitchCB;

	private JCheckBox integratePercussionSwitchCB;

	private JTextField noteTimbreVibratoRatioInput;

	private JTextField cqHighThresholdInput;

	private JCheckBox pidSwitchCB;

	private JCheckBox tunerClearNotesOnCreateSwitchCB;

	private JTextField tunerClearVibratoNotesTimeRangeInput;

	private JTextField tunerHysteresisWeightInput;

	private JCheckBox normaliseNotesSwitchCB;

	public ParametersPanel() {
		super(new BorderLayout());
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.console = Instrument.getInstance().getConsole();
		this.iss = Instrument.getInstance().getStorage().getInstrumentStoreService();
		this.storage = Instrument.getInstance().getStorage();

		this.setBorder(new TitledBorder("Input Parameters"));

		JPanel actionPanel = new JPanel();

		final JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				parameterManager.reset();
				updateParameters();
				console.getVisor().updateParameters();
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

		final JButton exportButton = new JButton("Export");
		exportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String baseDir = storage.getObjectStorage().getBasePath();
				String folder = Paths
						.get(baseDir,
								parameterManager.getParameter(
										InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PROJECT_DIRECTORY))
						.toString();
				String exportFileName = folder + System.getProperty("file.separator") + "instrument-user.properties";
				try (FileOutputStream fs = new FileOutputStream(exportFileName)) {
					SortedStoreProperties ssp = new SortedStoreProperties();
					if (parameterManager
							.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_EXPORT_DELTA_SWITCH)) {
						try (InputStream is = getClass().getClassLoader().getResourceAsStream("instrument.properties");
								InputStream isc = getClass().getClassLoader()
										.getResourceAsStream("instrument-client.properties");) {
							Properties props = new Properties();
							props.load(is);
							Properties clientParameters = new Properties();
							clientParameters.load(isc);
							props.putAll(clientParameters);
							ssp.putAll(parameterManager.getDeltaParameters(props));
						} catch (IOException ex) {
							LOG.log(Level.SEVERE, "Export Parameters exception", ex);
						}
					} else {
						ssp.putAll(parameterManager.getParameters());
					}
					ssp.store(fs, null);
				} catch (IOException ex) {
					LOG.log(Level.SEVERE, "Export Parameters exception", ex);
				}
			}
		});
		actionPanel.add(exportButton);

		final JButton importButton = new JButton("Import");
		importButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String baseDir = storage.getObjectStorage().getBasePath();
				String folder = Paths
						.get(baseDir,
								parameterManager.getParameter(
										InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PROJECT_DIRECTORY))
						.toString();
				String importFileName = folder + System.getProperty("file.separator") + "instrument-user.properties";
				try (FileInputStream fi = new FileInputStream(importFileName)) {
					Properties props = new Properties();
					props.load(fi);
					parameterManager.mergeProperties(props);
					updateParameters();
					console.getVisor().updateParameters();
				} catch (IOException ex) {
					LOG.log(Level.SEVERE, "Import Parameters exception", ex);
				}
			}
		});
		actionPanel.add(importButton);

		exportDeltaSwitchCB = new JCheckBox("exportDeltaSwitchCB");
		exportDeltaSwitchCB.setText("Delta");
		exportDeltaSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_EXPORT_DELTA_SWITCH,
						Boolean.toString(newValue));
			}
		});

		exportDeltaSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_EXPORT_DELTA_SWITCH));
		actionPanel.add(exportDeltaSwitchCB);

		selectStyleComboBox = new JComboBox<String>(styles);
		selectStyleComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				String value = (String) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				if (!value.equals(parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE))) {
					parameterManager.setParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE, value);
					selectStyleComboBox.setSelectedIndex(getSelectStyleIndex(
							parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));
					try {
						parameterManager.loadStyle(value, parameterManager
								.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_EXPORT_DELTA_SWITCH));
						updateParameters();
						console.getVisor().updateParameters();
					} catch (IOException e1) {
						LOG.log(Level.SEVERE, "Select Parameter Style exception", e);
						parameterManager.setParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE, "default");
						selectStyleComboBox.setSelectedIndex(getSelectStyleIndex(
								parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));
					}
				}
			}
		});

		selectStyleComboBox.setSelectedIndex(
				getSelectStyleIndex(parameterManager.getParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE)));
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

		audioSmoothFactorSlider = new JSlider(0, 20);
		final JLabel audioSmoothFactorLabel = new JLabel("Audio Smooth Factor :");
		audioSmoothFactorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				audioSmoothFactorLabel.setText(String.format("Audio Smooth Factor  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SMOOTH_FACTOR,
						Integer.toString(newValue));
			}
		});
		audioSmoothFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SMOOTH_FACTOR));
		parameterPanel.add(audioSmoothFactorLabel);
		parameterPanel.add(audioSmoothFactorSlider);

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

		tunerClearHeadNotesSwitchCB = new JCheckBox("tunerClearHeadNotesSwitchCB");
		tunerClearHeadNotesSwitchCB.setText("Tuner Clear Head Notes Switch");
		tunerClearHeadNotesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_HEAD_NOTES_SWITCH,
						Boolean.toString(newValue));
			}
		});

		tunerClearHeadNotesSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_HEAD_NOTES_SWITCH));
		tunerSwitchPanel.add(tunerClearHeadNotesSwitchCB);

		tunerClearTailNotesSwitchCB = new JCheckBox("tunerClearTailNotesSwitchCB");
		tunerClearTailNotesSwitchCB.setText("Tuner Clear Tail Notes Switch");
		tunerClearTailNotesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_TAIL_NOTES_SWITCH,
						Boolean.toString(newValue));
			}
		});

		tunerClearTailNotesSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_TAIL_NOTES_SWITCH));
		tunerSwitchPanel.add(tunerClearTailNotesSwitchCB);

		tunerClearVibratoNotesSwitchCB = new JCheckBox("tunerClearVibratoNotesSwitchCB");
		tunerClearVibratoNotesSwitchCB.setText("Tuner Clear Vibrato Notes Switch");
		tunerClearVibratoNotesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_VIBRATO_NOTES_SWITCH,
						Boolean.toString(newValue));
			}
		});

		tunerClearVibratoNotesSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_VIBRATO_NOTES_SWITCH));
		tunerSwitchPanel.add(tunerClearVibratoNotesSwitchCB);

		tunerClearIsolatedNotesSwitchCB = new JCheckBox("tunerClearIsolatedNotesSwitchCB");
		tunerClearIsolatedNotesSwitchCB.setText("Tuner Clear Isolated Notes Switch");
		tunerClearIsolatedNotesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_SWITCH,
						Boolean.toString(newValue));
			}
		});

		tunerClearIsolatedNotesSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_SWITCH));
		tunerSwitchPanel.add(tunerClearIsolatedNotesSwitchCB);

		tunerClearNotesOnCreateSwitchCB = new JCheckBox("tunerClearNotesOnCreateSwitchCB");
		tunerClearNotesOnCreateSwitchCB.setText("Tuner Clear Notes On Create Switch");
		tunerClearNotesOnCreateSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_NOTES_ON_CREATE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		tunerClearNotesOnCreateSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_NOTES_ON_CREATE_SWITCH));
		tunerSwitchPanel.add(tunerClearNotesOnCreateSwitchCB);

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

		pidSwitchCB = new JCheckBox("pidSwitchCB");
		pidSwitchCB.setText("PID Switch");
		pidSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_SWITCH,
						Boolean.toString(newValue));
			}
		});

		pidSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_SWITCH));
		tunerSwitchPanel.add(pidSwitchCB);

		parameterPanel.add(tunerSwitchPanel);

		JPanel cqSwitchPanel = new JPanel();
		cqSwitchPanel.setLayout(new GridLayout(0, 6));
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

		normaliseNotesSwitchCB = new JCheckBox("normaliseNotesSwitchCB");
		normaliseNotesSwitchCB.setText("CQ Normalise Notes");
		normaliseNotesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE_NOTES,
						Boolean.toString(newValue));
			}
		});

		normaliseNotesSwitchCB.setSelected(
				parameterManager
						.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE_NOTES));
		cqSwitchPanel.add(normaliseNotesSwitchCB);

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
		cqCalibrateSwitchCB.setText("Calibrate Switch");
		cqCalibrateSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		cqCalibrateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_SWITCH));
		cqSwitchPanel.add(cqCalibrateSwitchCB);

		cqCalibrateForwardSwitchCB = new JCheckBox("cqCalibrateForwardSwitchCB");
		cqCalibrateForwardSwitchCB.setText("Calibrate Forward Switch");
		cqCalibrateForwardSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_FORWARD_SWITCH,
						Boolean.toString(newValue));
			}
		});

		cqCalibrateForwardSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_FORWARD_SWITCH));
		cqSwitchPanel.add(cqCalibrateForwardSwitchCB);

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

		cqMicroToneSwitchCB = new JCheckBox("cqMicroToneSwitchCB");
		cqMicroToneSwitchCB.setText("MicroTone Switch");
		cqMicroToneSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_MICRO_TONE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		cqMicroToneSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_MICRO_TONE_SWITCH));
		cqSwitchPanel.add(cqMicroToneSwitchCB);

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

		cqAdaptiveWhitenSwitchCB = new JCheckBox("cqAdaptiveWhitenSwitchCB");
		cqAdaptiveWhitenSwitchCB.setText("CQ Adaptive Whiten");
		cqAdaptiveWhitenSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_ADAPTIVE_WHITEN,
						Boolean.toString(newValue));
			}
		});

		cqAdaptiveWhitenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_ADAPTIVE_WHITEN));
		cqSwitchPanel.add(cqAdaptiveWhitenSwitchCB);

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

		integrationEnvelopeWhitenSwitchCB = new JCheckBox("integrationEnvelopeWhitenSwitchCB");
		integrationEnvelopeWhitenSwitchCB.setText("Integration Envelope Whiten");
		integrationEnvelopeWhitenSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_ENVELOPE_WHITEN_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrationEnvelopeWhitenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_ENVELOPE_WHITEN_SWITCH));
		cqSwitchPanel.add(integrationEnvelopeWhitenSwitchCB);

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

		integratePercussionSwitchCB = new JCheckBox("integratePercussionSwitchCB");
		integratePercussionSwitchCB.setText("Integrate Percussion");
		integratePercussionSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PERCUSSION_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integratePercussionSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PERCUSSION_SWITCH));
		cqSwitchPanel.add(integratePercussionSwitchCB);

		integrateCQSwitchCB = new JCheckBox("integrateCQSwitchCB");
		integrateCQSwitchCB.setText("Integrate CQ");
		integrateCQSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_CQ_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrateCQSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_CQ_SWITCH));
		cqSwitchPanel.add(integrateCQSwitchCB);

		integratePeaksSwitchCB = new JCheckBox("integratePeaksSwitchCB");
		integratePeaksSwitchCB.setText("Integrate Peaks");
		integratePeaksSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PEAKS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integratePeaksSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PEAKS_SWITCH));
		cqSwitchPanel.add(integratePeaksSwitchCB);

		integrateSpectralSwitchCB = new JCheckBox("integrateSpectralSwitchCB");
		integrateSpectralSwitchCB.setText("Integrate Spectral");
		integrateSpectralSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SPECTRAL_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrateSpectralSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SPECTRAL_SWITCH));
		cqSwitchPanel.add(integrateSpectralSwitchCB);

		integratePitchSwitchCB = new JCheckBox("integratePitchSwitchCB");
		integratePitchSwitchCB.setText("Integrate Pitch");
		integratePitchSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PITCH_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integratePitchSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PITCH_SWITCH));
		cqSwitchPanel.add(integratePitchSwitchCB);

		integrateSPSwitchCB = new JCheckBox("integrateSPSwitchCB");
		integrateSPSwitchCB.setText("Integrate SP");
		integrateSPSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SP_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrateSPSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SP_SWITCH));
		cqSwitchPanel.add(integrateSPSwitchCB);

		integrateTPSwitchCB = new JCheckBox("integrateTPSwitchCB");
		integrateTPSwitchCB.setText("Integrate TP");
		integrateTPSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_TP_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrateTPSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_TP_SWITCH));
		cqSwitchPanel.add(integrateTPSwitchCB);

		integrateYINSwitchCB = new JCheckBox("integrateYINSwitchCB");
		integrateYINSwitchCB.setText("Integrate YIN");
		integrateYINSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_YIN_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrateYINSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_YIN_SWITCH));
		cqSwitchPanel.add(integrateYINSwitchCB);

		integrateSACFSwitchCB = new JCheckBox("integrateSACFSwitchCB");
		integrateSACFSwitchCB.setText("Integrate SACF");
		integrateSACFSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SACF_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrateSACFSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SACF_SWITCH));
		cqSwitchPanel.add(integrateSACFSwitchCB);

		integrateMFCCSwitchCB = new JCheckBox("integrateMFCCSwitchCB");
		integrateMFCCSwitchCB.setText("Integrate MFCC");
		integrateMFCCSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_MFCC_SWITCH,
						Boolean.toString(newValue));
			}
		});

		integrateMFCCSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_MFCC_SWITCH));
		cqSwitchPanel.add(integrateMFCCSwitchCB);

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

		notatePeaksSwitchCB = new JCheckBox("notatePeaksnSwitchCB");
		notatePeaksSwitchCB.setText("Notate Peaks");
		notatePeaksSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_PEAKS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		notatePeaksSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_PEAKS_SWITCH));
		cqSwitchPanel.add(notatePeaksSwitchCB);

		notateApplyFormantsSwitchCB = new JCheckBox("notateApplyFormantsSwitchCB");
		notateApplyFormantsSwitchCB.setText("Notate Apply Formants");
		notateApplyFormantsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_APPLY_FORMANTS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		notateApplyFormantsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_APPLY_FORMANTS_SWITCH));
		cqSwitchPanel.add(notateApplyFormantsSwitchCB);

		onsetHpsSwitchCB = new JCheckBox("onsetHpsSwitchCB");
		onsetHpsSwitchCB.setText("Onset HPS");
		onsetHpsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_HPS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		onsetHpsSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_HPS_SWITCH));
		cqSwitchPanel.add(onsetHpsSwitchCB);

		chromaHpsSwitchCB = new JCheckBox("chromaHpsSwitchCB");
		chromaHpsSwitchCB.setText("Chroma HPS");
		chromaHpsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HPS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		chromaHpsSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HPS_SWITCH));
		cqSwitchPanel.add(chromaHpsSwitchCB);

		parameterPanel.add(cqSwitchPanel);

		noiseFloorSlider = new JSlider(100, 1000);
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

		medianFilterSizeSlider = new JSlider(1, 255);
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

		minPeakSizeSlider = new JSlider(1, 10);
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

		numberOfPeaksSlider = new JSlider(1, 10);
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

		chromaRootNoteSlider = new JSlider(24, 120);
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

		formantFactorSlider = new JSlider(1, 100);
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

		bottomFormantFactorSlider = new JSlider(1, 100);
		final JLabel bottomFormantFactorLabel = new JLabel("Audio Tuner Bottom Formant Factor :");
		bottomFormantFactorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				bottomFormantFactorLabel.setText(String.format("Audio Tuner Bottom Formant Factor   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_FACTOR,
						Integer.toString(newValue));
			}
		});
		bottomFormantFactorSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_FACTOR));
		parameterPanel.add(bottomFormantFactorLabel);
		parameterPanel.add(bottomFormantFactorSlider);

		topFormantFactorSlider = new JSlider(1, 100);
		final JLabel topFormantFactorLabel = new JLabel("Audio Tuner Top Formant Factor :");
		topFormantFactorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				topFormantFactorLabel.setText(String.format("Audio Tuner Top Formant Factor   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_FACTOR,
						Integer.toString(newValue));
			}
		});
		topFormantFactorSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_FACTOR));
		parameterPanel.add(topFormantFactorLabel);
		parameterPanel.add(topFormantFactorSlider);

		formantRangeSlider = new JSlider(0, 120);
		final JLabel formantRangeLabel = new JLabel("Audio Tuner Formant Range :");
		formantRangeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantRangeLabel.setText(String.format("Audio Tuner Formant Range  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_RANGE,
						Integer.toString(newValue));
			}
		});
		formantRangeSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_RANGE));
		parameterPanel.add(formantRangeLabel);
		parameterPanel.add(formantRangeSlider);

		bottomFormantRangeSlider = new JSlider(0, 120);
		final JLabel bottomFormantRangeLabel = new JLabel("Audio Tuner Bottom Formant Range :");
		bottomFormantRangeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				bottomFormantRangeLabel.setText(String.format("Audio Tuner Bottom Formant Range  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_RANGE,
						Integer.toString(newValue));
			}
		});
		bottomFormantRangeSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_RANGE));
		parameterPanel.add(bottomFormantRangeLabel);
		parameterPanel.add(bottomFormantRangeSlider);

		topFormantRangeSlider = new JSlider(0, 120);
		final JLabel topFormantRangeLabel = new JLabel("Audio Tuner Top Formant Range :");
		topFormantRangeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				topFormantRangeLabel.setText(String.format("Audio Tuner Top Formant Range  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_RANGE,
						Integer.toString(newValue));
			}
		});
		topFormantRangeSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_RANGE));
		parameterPanel.add(topFormantRangeLabel);
		parameterPanel.add(topFormantRangeSlider);

		formantHighSlider = new JSlider(0, 120);
		final JLabel formantHighLabel = new JLabel("Audio Tuner Formant High Note :");
		formantHighSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantHighLabel.setText(String.format("Audio Tuner Formant High Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH,
						Integer.toString(newValue));
			}
		});
		formantHighSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH));
		parameterPanel.add(formantHighLabel);
		parameterPanel.add(formantHighSlider);

		bottomFormantHighSlider = new JSlider(0, 120);
		final JLabel bottomFormantHighLabel = new JLabel("Audio Tuner Bottom Formant High Note :");
		bottomFormantHighSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				bottomFormantHighLabel.setText(String.format("Audio Tuner Bottom Formant High Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_HIGH,
						Integer.toString(newValue));
			}
		});
		bottomFormantHighSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_HIGH));
		parameterPanel.add(bottomFormantHighLabel);
		parameterPanel.add(bottomFormantHighSlider);

		topFormantHighSlider = new JSlider(0, 120);
		final JLabel topFormantHighLabel = new JLabel("Audio Tuner Top Formant High Note :");
		topFormantHighSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				topFormantHighLabel.setText(String.format("Audio Tuner Top Formant High Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_HIGH,
						Integer.toString(newValue));
			}
		});
		topFormantHighSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_HIGH));
		parameterPanel.add(topFormantHighLabel);
		parameterPanel.add(topFormantHighSlider);

		formantLowSlider = new JSlider(0, 120);
		final JLabel formantLowLabel = new JLabel("Audio Tuner Formant Low Note :");
		formantLowSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				formantLowLabel.setText(String.format("Audio Tuner Formant Low Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW,
						Integer.toString(newValue));
			}
		});
		formantLowSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW));
		parameterPanel.add(formantLowLabel);
		parameterPanel.add(formantLowSlider);

		bottomFormantLowSlider = new JSlider(0, 120);
		final JLabel bottomFormantLowLabel = new JLabel("Audio Tuner Bottom Formant Low Note :");
		bottomFormantLowSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				bottomFormantLowLabel.setText(String.format("Audio Tuner Bottom Formant Low Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_LOW,
						Integer.toString(newValue));
			}
		});
		bottomFormantLowSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_LOW));
		parameterPanel.add(bottomFormantLowLabel);
		parameterPanel.add(bottomFormantLowSlider);

		topFormantLowSlider = new JSlider(0, 120);
		final JLabel topFormantLowLabel = new JLabel("Audio Tuner Top Formant Low Note :");
		topFormantLowSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				topFormantLowLabel.setText(String.format("Audio Tuner Top Formant Low Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_LOW,
						Integer.toString(newValue));
			}
		});
		topFormantLowSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_LOW));
		parameterPanel.add(topFormantLowLabel);
		parameterPanel.add(topFormantLowSlider);

		formantMidSlider = new JSlider(0, 120);
		final JLabel formantMidLabel = new JLabel("Audio Tuner Formant Middle Note :");
		formantMidSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				formantMidLabel.setText(String.format("Audio Tuner Formant Middle Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE,
						Integer.toString(newValue));
			}
		});
		formantMidSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE));
		parameterPanel.add(formantMidLabel);
		parameterPanel.add(formantMidSlider);

		bottomFormantMidSlider = new JSlider(0, 120);
		final JLabel bottomFormantMidLabel = new JLabel("Audio Tuner Bottom Formant Middle Note :");
		bottomFormantMidSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				bottomFormantMidLabel.setText(String.format("Audio Tuner Bottom Formant Middle Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_MIDDLE,
						Integer.toString(newValue));
			}
		});
		bottomFormantMidSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_MIDDLE));
		parameterPanel.add(bottomFormantMidLabel);
		parameterPanel.add(bottomFormantMidSlider);

		topFormantMidSlider = new JSlider(0, 120);
		final JLabel topFormantMidLabel = new JLabel("Audio Tuner Top Formant Middle Note :");
		topFormantMidSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				topFormantMidLabel.setText(String.format("Audio Tuner Top Formant Middle Note  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_MIDDLE,
						Integer.toString(newValue));
			}
		});
		topFormantMidSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_MIDDLE));
		parameterPanel.add(topFormantMidLabel);
		parameterPanel.add(topFormantMidSlider);

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

		noteHighSlider = new JSlider(1, 100);
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

		noteLowSlider = new JSlider(1, 100);
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

		noteMaxDurationSlider = new JSlider(100, 10000);
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

		noteMinDurationSlider = new JSlider(1, 1000);
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

		notePeaksMaxDurationSlider = new JSlider(1, 10000);
		final JLabel notePeaksMaxDurationLabel = new JLabel("Audio Tuner Max Note Peaks Duration :");
		notePeaksMaxDurationSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				notePeaksMaxDurationLabel
						.setText(String.format("Audio Tuner Max Note Peaks Duration  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_PEAKS_MAX_DURATION,
						Integer.toString(newValue));
			}
		});
		notePeaksMaxDurationSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_PEAKS_MAX_DURATION));
		parameterPanel.add(notePeaksMaxDurationLabel);
		parameterPanel.add(notePeaksMaxDurationSlider);

		notePeaksMinDurationSlider = new JSlider(1, 10000);
		final JLabel notePeaksMinDurationLabel = new JLabel("Audio Tuner Min Note Peaks Duration :");
		notePeaksMinDurationSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				notePeaksMinDurationLabel.setText(String.format("Audio Tuner Min Note PeaksDuration  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_PEAKS_MIN_DURATION,
						Integer.toString(newValue));
			}
		});
		notePeaksMinDurationSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_PEAKS_MIN_DURATION));
		parameterPanel.add(notePeaksMinDurationLabel);
		parameterPanel.add(notePeaksMinDurationSlider);

		noteSpectralMaxDurationSlider = new JSlider(1, 10000);
		final JLabel noteSpectralMaxDurationLabel = new JLabel("Audio Tuner Max Note Spectral Duration :");
		noteSpectralMaxDurationSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				noteSpectralMaxDurationLabel
						.setText(String.format("Audio Tuner Max Note Spectral Duration  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SPECTRAL_MAX_DURATION,
						Integer.toString(newValue));
			}
		});
		noteSpectralMaxDurationSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SPECTRAL_MAX_DURATION));
		parameterPanel.add(noteSpectralMaxDurationLabel);
		parameterPanel.add(noteSpectralMaxDurationSlider);

		noteSpectralMinDurationSlider = new JSlider(1, 10000);
		final JLabel noteSpectralMinDurationLabel = new JLabel("Audio Tuner Min Note Spectral Duration :");
		noteSpectralMinDurationSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				noteSpectralMinDurationLabel
						.setText(String.format("Audio Tuner Min Note Spectral Duration  (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SPECTRAL_MIN_DURATION,
						Integer.toString(newValue));
			}
		});
		noteSpectralMinDurationSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SPECTRAL_MIN_DURATION));
		parameterPanel.add(noteSpectralMinDurationLabel);
		parameterPanel.add(noteSpectralMinDurationSlider);

		noteSustainSlider = new JSlider(0, 10000);
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

		pitchHighSlider = new JSlider(12, 120);
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

		pitchLowSlider = new JSlider(12, 120);
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

		JPanel cqParamsPanel = new JPanel();
		cqParamsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		cqParamsPanel.setLayout(new GridLayout(0, 6));
		cqParamsPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		JLabel chromaNormaliseThresholdLabel = new JLabel("CHROMA Normalise Threshold: ");
		chromaNormaliseThresholdInput = new JTextField(4);
		chromaNormaliseThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = chromaNormaliseThresholdInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD, newValue);
				chromaNormaliseThresholdLabel.setText(String.format("CHROMA Normalise Threshold  (%s):", newValue));
				chromaNormaliseThresholdInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_THRESHOLD, newValue);
				chromaChordifyThresholdLabel.setText(String.format("CHROMA Chordify (%s):", newValue));
				chromaChordifyThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_LOW_NOTE,
						newValue);
				harmonicLowNoteLabel.setText(String.format("Harmonic Low Note  (%s):", newValue));
				harmonicLowNoteInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_HIGH_NOTE,
						newValue);
				harmonicHighNoteLabel.setText(String.format("Harmonic High Note  (%s):", newValue));
				harmonicHighNoteInput.setText(newValue);
			}
		});
		harmonicHighNoteInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_HIGH_NOTE));
		cqParamsPanel.add(harmonicHighNoteLabel);
		cqParamsPanel.add(harmonicHighNoteInput);

		JLabel pidPFactorLabel = new JLabel("PID P Factor: ");
		pidPFactorInput = new JTextField(4);
		pidPFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = pidPFactorInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_P_FACTOR,
						newValue);
				pidPFactorLabel.setText(String.format("PID P Factor  (%s):", newValue));
				pidPFactorInput.setText(newValue);
			}
		});
		pidPFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_P_FACTOR));
		cqParamsPanel.add(pidPFactorLabel);
		cqParamsPanel.add(pidPFactorInput);

		JLabel pidDFactorLabel = new JLabel("PID D Factor: ");
		pidDFactorInput = new JTextField(4);
		pidDFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = pidDFactorInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_D_FACTOR,
						newValue);
				pidDFactorLabel.setText(String.format("PID D Factor  (%s):", newValue));
				pidDFactorInput.setText(newValue);
			}
		});
		pidDFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_D_FACTOR));
		cqParamsPanel.add(pidDFactorLabel);
		cqParamsPanel.add(pidDFactorInput);

		JLabel pidIFactorLabel = new JLabel("PID I Factor: ");
		pidIFactorInput = new JTextField(4);
		pidIFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = pidIFactorInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_I_FACTOR,
						newValue);
				pidIFactorLabel.setText(String.format("PID I Factor  (%s):", newValue));
				pidIFactorInput.setText(newValue);
			}
		});
		pidIFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_I_FACTOR));
		cqParamsPanel.add(pidIFactorLabel);
		cqParamsPanel.add(pidIFactorInput);

		JLabel cqBinsPerOctaveLabel = new JLabel("CQ Bins Per Octave: ");
		cqBinsPerOctaveInput = new JTextField(4);
		cqBinsPerOctaveInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqBinsPerOctaveInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_BINS_PER_OCTAVE,
						newValue);
				cqBinsPerOctaveLabel.setText(String.format("CQ Bins Per Octave  (%s):", newValue));
				cqBinsPerOctaveInput.setText(newValue);
			}
		});
		cqBinsPerOctaveInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_BINS_PER_OCTAVE));
		cqParamsPanel.add(cqBinsPerOctaveLabel);
		cqParamsPanel.add(cqBinsPerOctaveInput);

		JLabel cqLowThresholdLabel = new JLabel("CQ Low Threshold: ");
		cqLowThresholdInput = new JTextField(4);
		cqLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqLowThresholdInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD,
						newValue);
				cqLowThresholdLabel.setText(String.format("CQ Low Threshold  (%s):", newValue));
				cqLowThresholdInput.setText(newValue);
			}
		});
		cqLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD));
		cqParamsPanel.add(cqLowThresholdLabel);
		cqParamsPanel.add(cqLowThresholdInput);

		JLabel cqHighThresholdLabel = new JLabel("CQ High Threshold: ");
		cqHighThresholdInput = new JTextField(4);
		cqHighThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqHighThresholdInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_HIGH_THRESHOLD,
						newValue);
				cqHighThresholdLabel.setText(String.format("CQ High Threshold  (%s):", newValue));
				cqHighThresholdInput.setText(newValue);
			}
		});
		cqHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_HIGH_THRESHOLD));
		cqParamsPanel.add(cqHighThresholdLabel);
		cqParamsPanel.add(cqHighThresholdInput);

		JLabel cqThresholdFactorLabel = new JLabel("CQ Threshold Factor: ");
		cqThresholdFactorInput = new JTextField(4);
		cqThresholdFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqThresholdFactorInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR, newValue);
				cqThresholdFactorLabel.setText(String.format("CQ Threshold Factor  (%s):", newValue));
				cqThresholdFactorInput.setText(newValue);
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
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_THRESHOLD, newValue);
				cqEnvelopeWhitenThresholdLabel.setText(String.format("CQ Envelope Whiten Threshold (%s):", newValue));
				cqEnvelopeWhitenThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_ATTACK_FACTOR, newValue);
				cqEnvelopeWhitenAttackFactorLabel
						.setText(String.format("CQ Envelope Whiten Attack Factor (%s):", newValue));
				cqEnvelopeWhitenAttackFactorInput.setText(newValue);
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
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_DECAY_FACTOR, newValue);
				cqEnvelopeWhitenDecayFactorLabel
						.setText(String.format("CQ Envelope Whiten Decay Factor (%s):", newValue));
				cqEnvelopeWhitenDecayFactorInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM,
						newValue);
				cqSignalMinimumLabel.setText(String.format("CQ Signal Minimum  (%s):", newValue));
				cqSignalMinimumInput.setText(newValue);
			}
		});
		cqSignalMinimumInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM));
		cqParamsPanel.add(cqSignalMinimumLabel);
		cqParamsPanel.add(cqSignalMinimumInput);

		JLabel cqCalibrateRangeLabel = new JLabel("Calibrate Range: ");
		cqCalibrateRangeInput = new JTextField(4);
		cqCalibrateRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqCalibrateRangeInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE,
						newValue);
				cqCalibrateRangeLabel.setText(String.format("Calibrate Range  (%s):", newValue));
				cqCalibrateRangeInput.setText(newValue);
			}
		});
		cqCalibrateRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE));
		cqParamsPanel.add(cqCalibrateRangeLabel);
		cqParamsPanel.add(cqCalibrateRangeInput);

		JLabel cqNormaliseThresholdLabel = new JLabel("CQ Normalise Threshold: ");
		cqNormaliseThresholdInput = new JTextField(4);
		cqNormaliseThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqNormaliseThresholdInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD, newValue);
				cqNormaliseThresholdLabel.setText(String.format("CQ Normalise Threshold  (%s):", newValue));
				cqNormaliseThresholdInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SHARPEN_THRESHOLD, newValue);
				cqSharpenThresholdLabel.setText(String.format("CQ Sharpen Threshold  (%s):", newValue));
				cqSharpenThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL,
						newValue);
				cqDecibelLevelLabel.setText(String.format("CQ Decibel Level  (%s):", newValue));
				cqDecibelLevelInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION,
						newValue);
				cqCompressionLevelLabel.setText(String.format("CQ Compression Level  (%s):", newValue));
				cqCompressionLevelInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_FACTOR,
						newValue);
				cqWhitenFactorLabel.setText(String.format("CQ Whiten Factor  (%s):", newValue));
				cqWhitenFactorInput.setText(newValue);
			}
		});
		cqWhitenFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_FACTOR));
		cqParamsPanel.add(cqWhitenFactorLabel);
		cqParamsPanel.add(cqWhitenFactorInput);

		JLabel cqAdaptiveWhitenFactorLabel = new JLabel("CQ Adaptive Whiten Factor: ");
		cqAdaptiveWhitenFactorInput = new JTextField(4);
		cqAdaptiveWhitenFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqAdaptiveWhitenFactorInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ADAPTIVE_WHITEN_FACTOR, newValue);
				cqAdaptiveWhitenFactorLabel.setText(String.format("CQ Adaptive Whiten Factor  (%s):", newValue));
				cqAdaptiveWhitenFactorInput.setText(newValue);
			}
		});
		cqAdaptiveWhitenFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ADAPTIVE_WHITEN_FACTOR));
		cqParamsPanel.add(cqAdaptiveWhitenFactorLabel);
		cqParamsPanel.add(cqAdaptiveWhitenFactorInput);

		JLabel cqAdaptiveWhitenThresholdLabel = new JLabel("CQ Adaptive Whiten Threshold: ");
		cqAdaptiveWhitenThresholdInput = new JTextField(4);
		cqAdaptiveWhitenThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqAdaptiveWhitenThresholdInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_CQ_ADAPTIVE_WHITEN_THRESHOLD, newValue);
				cqAdaptiveWhitenThresholdLabel.setText(String.format("CQ Adaptive Whiten Threshold  (%s):", newValue));
				cqAdaptiveWhitenThresholdInput.setText(newValue);
			}
		});
		cqAdaptiveWhitenThresholdInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ADAPTIVE_WHITEN_THRESHOLD));
		cqParamsPanel.add(cqAdaptiveWhitenThresholdLabel);
		cqParamsPanel.add(cqAdaptiveWhitenThresholdInput);

		JLabel spLowThresholdLabel = new JLabel("SP Low Threshold: ");
		spLowThresholdInput = new JTextField(4);
		spLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = spLowThresholdInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_LOW_THRESHOLD,
						newValue);
				spLowThresholdLabel.setText(String.format("SP Low Threshold  (%s):", newValue));
				spLowThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SIGNAL_MINIMUM,
						newValue);
				spSignalMinimumLabel.setText(String.format("SP Signal Minimum  (%s):", newValue));
				spSignalMinimumInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_NORMALISE_THRESHOLD, newValue);
				spNormaliseThresholdLabel.setText(String.format("SP Normalise Threshold  (%s):", newValue));
				spNormaliseThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_DECIBEL_LEVEL,
						newValue);
				spDecibelLevelLabel.setText(String.format("SP Decibel Level  (%s):", newValue));
				spDecibelLevelInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_COMPRESSION,
						newValue);
				spCompressionLevelLabel.setText(String.format("SP Compression Level  (%s):", newValue));
				spCompressionLevelInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_COMPRESSION, newValue);
				pdCompressionLevelLabel.setText(String.format("Pitch Detect Compression Level  (%s):", newValue));
				pdCompressionLevelInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_LOW_THRESHOLD, newValue);
				pdLowThresholdLabel.setText(String.format("Pitch Detect Low Threshold Factor  (%s):", newValue));
				pdLowThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_BEATS_THRESHOLD,
						newValue);
				beatsThresholdLabel.setText(String.format("Beats Threshold Factor  (%s):", newValue));
				beatsThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_BEATS_SENSITIVITY,
						newValue);
				beatsSensitivityLabel.setText(String.format("Beats Sensitivity Factor  (%s):", newValue));
				beatsSensitivityInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_THRESHOLD,
						newValue);
				onsetThresholdLabel.setText(String.format("Onset Threshold Factor  (%s):", newValue));
				onsetThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SENSITIVITY,
						newValue);
				onsetSensitivityLabel.setText(String.format("Onset Sensitivity Factor  (%s):", newValue));
				onsetSensitivityInput.setText(newValue);
			}
		});
		onsetSensitivityInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SENSITIVITY));
		cqParamsPanel.add(onsetSensitivityLabel);
		cqParamsPanel.add(onsetSensitivityInput);

		JLabel onsetPeaksSweepLabel = new JLabel("Onset Peaks Sweep Factor: ");
		onsetPeaksSweepInput = new JTextField(4);
		onsetPeaksSweepInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = onsetPeaksSweepInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_SWEEP,
						newValue);
				onsetPeaksSweepLabel.setText(String.format("Onset Peaks Sweep Factor  (%s):", newValue));
				onsetPeaksSweepInput.setText(newValue);
			}
		});
		onsetPeaksSweepInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_SWEEP));
		cqParamsPanel.add(onsetPeaksSweepLabel);
		cqParamsPanel.add(onsetPeaksSweepInput);

		JLabel onsetPeaksThresholdLabel = new JLabel("Onset Peaks Threshold Factor: ");
		onsetPeaksThresholdInput = new JTextField(4);
		onsetPeaksThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = onsetPeaksThresholdInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_THRESHOLD, newValue);
				onsetPeaksThresholdLabel.setText(String.format("Onset Peaks Threshold Factor  (%s):", newValue));
				onsetPeaksThresholdInput.setText(newValue);
			}
		});
		onsetPeaksThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_THRESHOLD));
		cqParamsPanel.add(onsetPeaksThresholdLabel);
		cqParamsPanel.add(onsetPeaksThresholdInput);

		JLabel onsetPeaksEdgeFactorLabel = new JLabel("Onset Peaks Edge Factor: ");
		onsetPeaksEdgeFactorInput = new JTextField(4);
		onsetPeaksEdgeFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = onsetPeaksEdgeFactorInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_EDGE_FACTOR, newValue);
				onsetPeaksEdgeFactorLabel.setText(String.format("Onset Peaks Edge Factor  (%s):", newValue));
				onsetPeaksEdgeFactorInput.setText(newValue);
			}
		});
		onsetPeaksEdgeFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_EDGE_FACTOR));
		cqParamsPanel.add(onsetPeaksEdgeFactorLabel);
		cqParamsPanel.add(onsetPeaksEdgeFactorInput);

		JLabel percussionThresholdLabel = new JLabel("Percussion Threshold Factor: ");
		percussionThresholdInput = new JTextField(4);
		percussionThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = percussionThresholdInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_THRESHOLD, newValue);
				percussionThresholdLabel.setText(String.format("Percussion Threshold Factor  (%s):", newValue));
				percussionThresholdInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_PERCUSSION_SENSITIVITY, newValue);
				percussionSensitivityLabel.setText(String.format("Percussion Sensitivity Factor  (%s):", newValue));
				percussionSensitivityInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_INTERVAL,
						newValue);
				onsetIntervalLabel.setText(String.format("Onset Interval Factor  (%s):", newValue));
				onsetIntervalInput.setText(newValue);
			}
		});
		onsetIntervalInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_INTERVAL));
		cqParamsPanel.add(onsetIntervalLabel);
		cqParamsPanel.add(onsetIntervalInput);

		JLabel onsetSilenceThresholdLabel = new JLabel("Onset Silence Threshold: ");
		onsetSilenceThresholdInput = new JTextField(4);
		onsetSilenceThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = onsetSilenceThresholdInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SILENCE_THRESHOLD, newValue);
				onsetSilenceThresholdLabel.setText(String.format("Onset Silence Threshold  (%s):", newValue));
				onsetSilenceThresholdInput.setText(newValue);
			}
		});
		onsetSilenceThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SILENCE_THRESHOLD));
		cqParamsPanel.add(onsetSilenceThresholdLabel);
		cqParamsPanel.add(onsetSilenceThresholdInput);

		JLabel tunerThresholdFactorLabel = new JLabel("Tuner Threshold Factor: ");
		tunerThresholdFactorInput = new JTextField(4);
		tunerThresholdFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerThresholdFactorInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_FACTOR, newValue);
				tunerThresholdFactorLabel.setText(String.format("Tuner Threshold Factor  (%s):", newValue));
				tunerThresholdFactorInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_MINIMUM, newValue);
				tunerSignalMinimumLabel.setText(String.format("Tuner Signal Minimum  (%s):", newValue));
				tunerSignalMinimumInput.setText(newValue);
			}
		});
		tunerSignalMinimumInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_MINIMUM));
		cqParamsPanel.add(tunerSignalMinimumLabel);
		cqParamsPanel.add(tunerSignalMinimumInput);

		JLabel tunerHarmonicSweepLabel = new JLabel("Tuner Harmonic Sweep: ");
		tunerHarmonicSweepInput = new JTextField(4);
		tunerHarmonicSweepInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerHarmonicSweepInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_SWEEP, newValue);
				tunerHarmonicSweepLabel.setText(String.format("Tuner Harmonic Sweep  (%s):", newValue));
				tunerHarmonicSweepInput.setText(newValue);
			}
		});
		tunerHarmonicSweepInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_SWEEP));
		cqParamsPanel.add(tunerHarmonicSweepLabel);
		cqParamsPanel.add(tunerHarmonicSweepInput);

		JLabel tunerClearNoteEdgeFactorLabel = new JLabel("Tuner Clear Note Edge Factor: ");
		tunerClearNoteEdgeFactorInput = new JTextField(4);
		tunerClearNoteEdgeFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerClearNoteEdgeFactorInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_NOTE_EDGE_FACTOR,
						newValue);
				tunerClearNoteEdgeFactorLabel.setText(String.format("Tuner Clear Note Edge Factor (%s):", newValue));
				tunerClearNoteEdgeFactorInput.setText(newValue);
			}
		});
		tunerClearNoteEdgeFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_NOTE_EDGE_FACTOR));
		cqParamsPanel.add(tunerClearNoteEdgeFactorLabel);
		cqParamsPanel.add(tunerClearNoteEdgeFactorInput);

		JLabel tunerClearIsolatedNotesPowerFactorLabel = new JLabel("Tuner Clear Isolated Notes Power Factor: ");
		tunerClearIsolatedNotesPowerFactorInput = new JTextField(4);
		tunerClearIsolatedNotesPowerFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerClearIsolatedNotesPowerFactorInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_POWER_FACTOR, newValue);
				tunerClearIsolatedNotesPowerFactorLabel
						.setText(String.format("Tuner Clear Isolated Notes Power Factor (%s):", newValue));
				tunerClearIsolatedNotesPowerFactorInput.setText(newValue);
			}
		});
		tunerClearIsolatedNotesPowerFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_POWER_FACTOR));
		cqParamsPanel.add(tunerClearIsolatedNotesPowerFactorLabel);
		cqParamsPanel.add(tunerClearIsolatedNotesPowerFactorInput);

		JLabel tunerClearIsolatedNotesTimeRangeLabel = new JLabel("Tuner Clear Isolated Notes Time Range: ");
		tunerClearIsolatedNotesTimeRangeInput = new JTextField(4);
		tunerClearIsolatedNotesTimeRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerClearIsolatedNotesTimeRangeInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_TIME_RANGE, newValue);
				tunerClearIsolatedNotesTimeRangeLabel
						.setText(String.format("Tuner Clear Isolated Notes Time Range (%s):", newValue));
				tunerClearIsolatedNotesTimeRangeInput.setText(newValue);
			}
		});
		tunerClearIsolatedNotesTimeRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_TIME_RANGE));
		cqParamsPanel.add(tunerClearIsolatedNotesTimeRangeLabel);
		cqParamsPanel.add(tunerClearIsolatedNotesTimeRangeInput);

		JLabel tunerClearIsolatedNotesPitchRangeLabel = new JLabel("Tuner Clear Isolated Notes Pitch Range: ");
		tunerClearIsolatedNotesPitchRangeInput = new JTextField(4);
		tunerClearIsolatedNotesPitchRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerClearIsolatedNotesPitchRangeInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_PITCH_RANGE, newValue);
				tunerClearIsolatedNotesPitchRangeLabel
						.setText(String.format("Tuner Clear Isolated Notes Pitch Range (%s):", newValue));
				tunerClearIsolatedNotesPitchRangeInput.setText(newValue);
			}
		});
		tunerClearIsolatedNotesPitchRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_PITCH_RANGE));
		cqParamsPanel.add(tunerClearIsolatedNotesPitchRangeLabel);
		cqParamsPanel.add(tunerClearIsolatedNotesPitchRangeInput);

		JLabel noteTimbreFrequencyRangeLabel = new JLabel("Note Timbre Frequency Range: ");
		noteTimbreFrequencyRangeInput = new JTextField(4);
		noteTimbreFrequencyRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTimbreFrequencyRangeInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RANGE, newValue);
				noteTimbreFrequencyRangeLabel.setText(String.format("Note Timbre Frequency Range (%s):", newValue));
				noteTimbreFrequencyRangeInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RATIO, newValue);
				noteTimbreFrequencyRatioLabel.setText(String.format("Note Timbre Frequency Ratio (%s):", newValue));
				noteTimbreFrequencyRatioInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RANGE,
						newValue);
				noteTimbreMedianRangeLabel.setText(String.format("Note Timbre Median Range (%s):", newValue));
				noteTimbreMedianRangeInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RATIO,
						newValue);
				noteTimbreMedianRatioLabel.setText(String.format("Note Timbre Median Ratio (%s):", newValue));
				noteTimbreMedianRatioInput.setText(newValue);
			}
		});
		noteTimbreMedianRatioInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RATIO));
		cqParamsPanel.add(noteTimbreMedianRatioLabel);
		cqParamsPanel.add(noteTimbreMedianRatioInput);

		JLabel noteTimbreVibratoRatioLabel = new JLabel("Note Timbre Vibrato Ratio: ");
		noteTimbreVibratoRatioInput = new JTextField(4);
		noteTimbreVibratoRatioInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTimbreVibratoRatioInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_VIBRATO_RATIO,
						newValue);
				noteTimbreVibratoRatioLabel.setText(String.format("Note Timbre Vibrato Ratio (%s):", newValue));
				noteTimbreVibratoRatioInput.setText(newValue);
			}
		});
		noteTimbreVibratoRatioInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_VIBRATO_RATIO));
		cqParamsPanel.add(noteTimbreVibratoRatioLabel);
		cqParamsPanel.add(noteTimbreVibratoRatioInput);

		JLabel tunerClearVibratoNotesTimeRangeLabel = new JLabel("Tuner Clear Vibrato Notes Time Range: ");
		tunerClearVibratoNotesTimeRangeInput = new JTextField(4);
		tunerClearVibratoNotesTimeRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerClearVibratoNotesTimeRangeInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_VIBRATO_NOTES_TIME_RANGE, newValue);
				tunerClearVibratoNotesTimeRangeLabel
						.setText(String.format("Tuner Clear Vibrato Notes Time Range (%s):", newValue));
				tunerClearVibratoNotesTimeRangeInput.setText(newValue);
			}
		});
		tunerClearVibratoNotesTimeRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_VIBRATO_NOTES_TIME_RANGE));
		cqParamsPanel.add(tunerClearVibratoNotesTimeRangeLabel);
		cqParamsPanel.add(tunerClearVibratoNotesTimeRangeInput);

		JLabel notateCompressionLevelLabel = new JLabel("Notate Compression Level: ");
		notateCompressionLevelInput = new JTextField(4);
		notateCompressionLevelInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = notateCompressionLevelInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION,
						newValue);
				notateCompressionLevelLabel.setText(String.format("Notate Compression Level  (%s):", newValue));
				notateCompressionLevelInput.setText(newValue);
			}
		});
		notateCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION));
		cqParamsPanel.add(notateCompressionLevelLabel);
		cqParamsPanel.add(notateCompressionLevelInput);

		JLabel notateSweepRangeLabel = new JLabel("Notate Sweep Range: ");
		notateSweepRangeInput = new JTextField(4);
		notateSweepRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = notateSweepRangeInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWEEP_RANGE,
						newValue);
				notateSweepRangeLabel.setText(String.format("Notate Sweep Range  (%s):", newValue));
				notateSweepRangeInput.setText(newValue);
			}
		});
		notateSweepRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWEEP_RANGE));
		cqParamsPanel.add(notateSweepRangeLabel);
		cqParamsPanel.add(notateSweepRangeInput);

		JLabel sinkSweepRangeLabel = new JLabel("Sink Sweep Range: ");
		sinkSweepRangeInput = new JTextField(4);
		sinkSweepRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = sinkSweepRangeInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SINK_SWEEP_RANGE,
						newValue);
				sinkSweepRangeLabel.setText(String.format("Sink Sweep Range  (%s):", newValue));
				sinkSweepRangeInput.setText(newValue);
			}
		});
		sinkSweepRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SINK_SWEEP_RANGE));
		cqParamsPanel.add(sinkSweepRangeLabel);
		cqParamsPanel.add(sinkSweepRangeInput);

		JLabel noteTrackerMaxTracksUpperLabel = new JLabel("Note Tracker Max Tracks Upper Limit: ");
		noteTrackerMaxTracksUpperInput = new JTextField(4);
		noteTrackerMaxTracksUpperInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerMaxTracksUpperInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_MAX_TRACKS_UPPER, newValue);
				noteTrackerMaxTracksUpperLabel
						.setText(String.format("Note Tracker Max Tracks Upper Limit  (%s):", newValue));
				noteTrackerMaxTracksUpperInput.setText(newValue);
			}
		});
		noteTrackerMaxTracksUpperInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_MAX_TRACKS_UPPER));
		cqParamsPanel.add(noteTrackerMaxTracksUpperLabel);
		cqParamsPanel.add(noteTrackerMaxTracksUpperInput);

		JLabel noteTrackerMaxTracksLowerLabel = new JLabel("Note Tracker Max Tracks Lower Limit: ");
		noteTrackerMaxTracksLowerInput = new JTextField(4);
		noteTrackerMaxTracksLowerInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerMaxTracksLowerInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_MAX_TRACKS_LOWER, newValue);
				noteTrackerMaxTracksLowerLabel
						.setText(String.format("Note Tracker Max Tracks Lower Limit  (%s):", newValue));
				noteTrackerMaxTracksLowerInput.setText(newValue);
			}
		});
		noteTrackerMaxTracksLowerInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_MAX_TRACKS_LOWER));
		cqParamsPanel.add(noteTrackerMaxTracksLowerLabel);
		cqParamsPanel.add(noteTrackerMaxTracksLowerInput);

		JLabel noteTrackerClearRangeUpperLabel = new JLabel("Note Tracker Clear Range Upper Limit: ");
		noteTrackerClearRangeUpperInput = new JTextField(4);
		noteTrackerClearRangeUpperInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerClearRangeUpperInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_CLEAR_RANGE_UPPER, newValue);
				noteTrackerClearRangeUpperLabel
						.setText(String.format("Note Tracker Clear Range Upper Limit  (%s):", newValue));
				noteTrackerClearRangeUpperInput.setText(newValue);
			}
		});
		noteTrackerClearRangeUpperInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_CLEAR_RANGE_UPPER));
		cqParamsPanel.add(noteTrackerClearRangeUpperLabel);
		cqParamsPanel.add(noteTrackerClearRangeUpperInput);

		JLabel noteTrackerClearRangeLowerLabel = new JLabel("Note Tracker Clear Range Lower Limit: ");
		noteTrackerClearRangeLowerInput = new JTextField(4);
		noteTrackerClearRangeLowerInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerClearRangeLowerInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_CLEAR_RANGE_LOWER, newValue);
				noteTrackerClearRangeLowerLabel
						.setText(String.format("Note Tracker Clear Range Lower Limit  (%s):", newValue));
				noteTrackerClearRangeLowerInput.setText(newValue);
			}
		});
		noteTrackerClearRangeLowerInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_CLEAR_RANGE_LOWER));
		cqParamsPanel.add(noteTrackerClearRangeLowerLabel);
		cqParamsPanel.add(noteTrackerClearRangeLowerInput);

		JLabel noteTrackerDiscardTrackRangeLabel = new JLabel("Note Tracker Discard Track Range: ");
		noteTrackerDiscardTrackRangeInput = new JTextField(4);
		noteTrackerDiscardTrackRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerDiscardTrackRangeInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_DISCARD_TRACK_RANGE, newValue);
				noteTrackerDiscardTrackRangeLabel
						.setText(String.format("Note Tracker Discard Track Range  (%s):", newValue));
				noteTrackerDiscardTrackRangeInput.setText(newValue);
			}
		});
		noteTrackerDiscardTrackRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_DISCARD_TRACK_RANGE));
		cqParamsPanel.add(noteTrackerDiscardTrackRangeLabel);
		cqParamsPanel.add(noteTrackerDiscardTrackRangeInput);

		JLabel noteTrackerOverlapSalientNoteRangeLabel = new JLabel("Note Tracker Overlap Salient Note Range: ");
		noteTrackerOverlapSalientNoteRangeInput = new JTextField(4);
		noteTrackerOverlapSalientNoteRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerOverlapSalientNoteRangeInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_OVERLAP_SALIENT_NOTE_RANGE, newValue);
				noteTrackerOverlapSalientNoteRangeLabel
						.setText(String.format("Note Tracker Overlap Salient Note Range  (%s):", newValue));
				noteTrackerOverlapSalientNoteRangeInput.setText(newValue);
			}
		});
		noteTrackerOverlapSalientNoteRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_OVERLAP_SALIENT_NOTE_RANGE));
		cqParamsPanel.add(noteTrackerOverlapSalientNoteRangeLabel);
		cqParamsPanel.add(noteTrackerOverlapSalientNoteRangeInput);

		JLabel noteTrackerOverlapSalientTimeRangeLabel = new JLabel("Note Tracker Overlap Salient Time Range: ");
		noteTrackerOverlapSalientTimeRangeInput = new JTextField(4);
		noteTrackerOverlapSalientTimeRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerOverlapSalientTimeRangeInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_OVERLAP_SALIENT_TIME_RANGE, newValue);
				noteTrackerOverlapSalientTimeRangeLabel
						.setText(String.format("Note Tracker Overlap Salient Time Range  (%s):", newValue));
				noteTrackerOverlapSalientTimeRangeInput.setText(newValue);
			}
		});
		noteTrackerOverlapSalientTimeRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_OVERLAP_SALIENT_TIME_RANGE));
		cqParamsPanel.add(noteTrackerOverlapSalientTimeRangeLabel);
		cqParamsPanel.add(noteTrackerOverlapSalientTimeRangeInput);

		JLabel noteTrackerSalientNoteRangeLabel = new JLabel("Note Tracker Salient Note Range: ");
		noteTrackerSalientNoteRangeInput = new JTextField(4);
		noteTrackerSalientNoteRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerSalientNoteRangeInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_NOTE_RANGE, newValue);
				noteTrackerSalientNoteRangeLabel
						.setText(String.format("Note Tracker Salient Note Range  (%s):", newValue));
				noteTrackerSalientNoteRangeInput.setText(newValue);
			}
		});
		noteTrackerSalientNoteRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_NOTE_RANGE));
		cqParamsPanel.add(noteTrackerSalientNoteRangeLabel);
		cqParamsPanel.add(noteTrackerSalientNoteRangeInput);

		JLabel noteTrackerSalientTimeRangeLabel = new JLabel("Note Tracker Salient Time Range: ");
		noteTrackerSalientTimeRangeInput = new JTextField(4);
		noteTrackerSalientTimeRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerSalientTimeRangeInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_TIME_RANGE, newValue);
				noteTrackerSalientTimeRangeLabel
						.setText(String.format("Note Tracker Salient Time Range  (%s):", newValue));
				noteTrackerSalientTimeRangeInput.setText(newValue);
			}
		});
		noteTrackerSalientTimeRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_TIME_RANGE));
		cqParamsPanel.add(noteTrackerSalientTimeRangeLabel);
		cqParamsPanel.add(noteTrackerSalientTimeRangeInput);

		JLabel noteTrackerSalientTimeNoteFactorLabel = new JLabel("Note Tracker Salient Time Note Factor: ");
		noteTrackerSalientTimeNoteFactorInput = new JTextField(4);
		noteTrackerSalientTimeNoteFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = noteTrackerSalientNoteRangeInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_TIME_NOTE_FACTOR, newValue);
				noteTrackerSalientTimeNoteFactorLabel
						.setText(String.format("Note Tracker Salient Time Note Factor  (%s):", newValue));
				noteTrackerSalientTimeNoteFactorInput.setText(newValue);
			}
		});
		noteTrackerSalientTimeNoteFactorInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_TIME_NOTE_FACTOR));
		cqParamsPanel.add(noteTrackerSalientTimeNoteFactorLabel);
		cqParamsPanel.add(noteTrackerSalientTimeNoteFactorInput);

		JLabel yinLowPassLabel = new JLabel("YIN Low Pass: ");
		yinLowPassInput = new JTextField(4);
		yinLowPassInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = yinLowPassInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_YIN_LOW_PASS,
						newValue);
				yinLowPassLabel.setText(String.format("YIN Low Pass (%s):", newValue));
				yinLowPassInput.setText(newValue);
			}
		});
		yinLowPassInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_YIN_LOW_PASS));
		cqParamsPanel.add(yinLowPassLabel);
		cqParamsPanel.add(yinLowPassInput);

		parameterPanel.add(cqParamsPanel);

		JPanel tunerParamsPanel = new JPanel();
		tunerParamsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		tunerParamsPanel.setLayout(new GridLayout(0, 6));
		tunerParamsPanel
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		JLabel tunerNormaliseThresholdLabel = new JLabel("Audio Tuner Normalise Threshold: ");
		tunerNormaliseThresholdInput = new JTextField(4);
		tunerNormaliseThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerNormaliseThresholdInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_THRESHOLD,
						newValue);
				tunerNormaliseThresholdLabel.setText(String.format("Audio Tuner Normalise Threshold  (%s):", newValue));
				tunerNormaliseThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_TROUGH,
						newValue);
				tunerNormaliseTroughLabel.setText(String.format("Audio Tuner Normalise Trough  (%s):", newValue));
				tunerNormaliseTroughInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_PEAK, newValue);
				tunerNormalisePeakLabel.setText(String.format("Audio Tuner Normalise Peak  (%s):", newValue));
				tunerNormalisePeakInput.setText(newValue);
			}
		});
		tunerNormalisePeakInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_PEAK));
		tunerParamsPanel.add(tunerNormalisePeakLabel);
		tunerParamsPanel.add(tunerNormalisePeakInput);

		JLabel tunerHysteresisWeightLabel = new JLabel("Audio Tuner Hysteresis Weight: ");
		tunerHysteresisWeightInput = new JTextField(4);
		tunerHysteresisWeightInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerHysteresisWeightInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HYSTERESIS_WEIGHT,
						newValue);
				tunerHysteresisWeightLabel.setText(String.format("Audio Tuner Hysteresis Weight  (%s):", newValue));
				tunerHysteresisWeightInput.setText(newValue);
			}
		});
		tunerHysteresisWeightInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_HYSTERESIS_WEIGHT));
		tunerParamsPanel.add(tunerHysteresisWeightLabel);
		tunerParamsPanel.add(tunerHysteresisWeightInput);

		JLabel tunerHarmonicDriftFactorLabel = new JLabel("Audio Tuner Harmonic Drift Factor: ");
		tunerHarmonicDriftFactorInput = new JTextField(4);
		tunerHarmonicDriftFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = tunerHarmonicDriftFactorInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_DRIFT_FACTOR,
						newValue);
				tunerHarmonicDriftFactorLabel
						.setText(String.format("Audio Tuner Harmonic Drift Factor  (%s):", newValue));
				tunerHarmonicDriftFactorInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_MASK_FACTOR,
						newValue);
				hpsMaskFactorLabel.setText(String.format("HPS Mask Factor  (%s):", newValue));
				hpsMaskFactorInput.setText(newValue);
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
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_MAX_LAG, newValue);
				acMaxLagLabel.setText(String.format("Autocorrelation Max Lag  (%s):", newValue));
				acMaxLagInput.setText(newValue);
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
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_THRESHOLD, newValue);
				acUndertoneThresholdLabel
						.setText(String.format("Autocorrelation Undertone Threshold  (%s):", newValue));
				acUndertoneThresholdInput.setText(newValue);

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
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_UNDERTONE_RANGE, newValue);
				acUndertoneRangeLabel.setText(String.format("Autocorrelation Undertone Range  (%s):", newValue));
				acUndertoneRangeInput.setText(newValue);
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
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_AUTOCORRELATION_CORRELATION_THRESHOLD, newValue);
				acCorrelationThresholdLabel.setText(String.format("Autocorrelation Threshold  (%s):", newValue));
				acCorrelationThresholdInput.setText(newValue);
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
		audioSmoothFactorSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SMOOTH_FACTOR));
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
		pidSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_SWITCH));
		compressionSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS));
		notateCompressionSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWITCH_COMPRESS));
		notatePeaksSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_PEAKS_SWITCH));
		notateApplyFormantsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_APPLY_FORMANTS_SWITCH));
		squareSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE));
		lowThresholdSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD));
		decibelSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL));
		normaliseSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE));
		normaliseNotesSwitchCB.setSelected(
				parameterManager
						.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE_NOTES));
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
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_SWITCH));
		cqMicroToneSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_MICRO_TONE_SWITCH));
		cqCalibrateForwardSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_FORWARD_SWITCH));
		cqPreSharpenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_SHARPEN));
		cqPostSharpenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_SHARPEN));
		cqSharpenHarmonicSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SHARPEN_HARMONIC));
		cqWhitenSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN));
		cqAdaptiveWhitenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_ADAPTIVE_WHITEN));
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
		bottomFormantFactorSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_FACTOR));
		topFormantFactorSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_FACTOR));
		formantRangeSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_RANGE));
		bottomFormantRangeSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_RANGE));
		topFormantRangeSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_RANGE));
		bottomFormantLowSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_LOW));
		topFormantLowSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_LOW));
		formantMidSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE));
		bottomFormantMidSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_MIDDLE));
		topFormantMidSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_MIDDLE));
		formantHighSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH));
		bottomFormantHighSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_HIGH));
		topFormantHighSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_HIGH));

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
		notePeaksMaxDurationSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_PEAKS_MAX_DURATION));
		notePeaksMinDurationSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_PEAKS_MIN_DURATION));
		noteSpectralMaxDurationSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SPECTRAL_MAX_DURATION));
		noteSpectralMinDurationSlider.setValue(
				parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SPECTRAL_MIN_DURATION));
		noteSustainSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SUSTAIN));
		pitchHighSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_HIGH));
		pitchLowSlider.setValue(parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_LOW));
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
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE));
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
		cqAdaptiveWhitenFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ADAPTIVE_WHITEN_FACTOR));
		cqAdaptiveWhitenThresholdInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ADAPTIVE_WHITEN_THRESHOLD));
		cqLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD));
		cqHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_HIGH_THRESHOLD));
		cqBinsPerOctaveInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_BINS_PER_OCTAVE));

		harmonicLowNoteInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_LOW_NOTE));
		harmonicHighNoteInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_HIGH_NOTE));

		pidPFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_P_FACTOR));
		pidDFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_D_FACTOR));
		pidIFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_I_FACTOR));

		notateCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION));
		notateSweepRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWEEP_RANGE));
		sinkSweepRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SINK_SWEEP_RANGE));

		noteTrackerMaxTracksUpperInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_MIN_TIME_INCREMENT));
		noteTrackerMaxTracksLowerInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_MAX_TRACKS_LOWER));
		noteTrackerClearRangeUpperInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_CLEAR_RANGE_UPPER));
		noteTrackerClearRangeLowerInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_CLEAR_RANGE_LOWER));
		noteTrackerDiscardTrackRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_DISCARD_TRACK_RANGE));
		noteTrackerOverlapSalientNoteRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_OVERLAP_SALIENT_NOTE_RANGE));
		noteTrackerOverlapSalientTimeRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_OVERLAP_SALIENT_TIME_RANGE));
		noteTrackerSalientNoteRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_NOTE_RANGE));
		noteTrackerSalientTimeRangeInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_TIME_RANGE));
		noteTrackerSalientTimeNoteFactorInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_SALIENT_TIME_NOTE_FACTOR));

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
		tunerHysteresisWeightInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_HYSTERESIS_WEIGHT));
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
		integratePercussionSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PERCUSSION_SWITCH));
		onsetHpsSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_HPS_SWITCH));
		chromaHpsSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HPS_SWITCH));
		integrateCQSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_CQ_SWITCH));
		integratePeaksSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PEAKS_SWITCH));
		integrateSpectralSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SPECTRAL_SWITCH));
		integratePitchSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PITCH_SWITCH));
		integrateSPSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SP_SWITCH));
		integrateTPSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_TP_SWITCH));
		integrateYINSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_YIN_SWITCH));
		integrateSACFSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SACF_SWITCH));
		integrateMFCCSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_MFCC_SWITCH));
		integrationEnvelopeWhitenSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_ENVELOPE_WHITEN_SWITCH));

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
		onsetPeaksSweepInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_SWEEP));
		onsetPeaksThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_THRESHOLD));
		onsetPeaksEdgeFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_EDGE_FACTOR));
		onsetSilenceThresholdInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SILENCE_THRESHOLD));
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
		noteTimbreVibratoRatioInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_VIBRATO_RATIO));
		noteTimbreCQSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_CQ_SWITCH));
		noteTimbreNotateSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_NOTATE_SWITCH));
		tunerClearHeadNotesSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_HEAD_NOTES_SWITCH));
		tunerClearTailNotesSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_TAIL_NOTES_SWITCH));
		tunerClearVibratoNotesSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_VIBRATO_NOTES_SWITCH));
		tunerClearIsolatedNotesSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_SWITCH));
		tunerClearNotesOnCreateSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_NOTES_ON_CREATE_SWITCH));

		tunerHarmonicSweepInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_SWEEP));
		tunerClearNoteEdgeFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_NOTE_EDGE_FACTOR));
		tunerClearIsolatedNotesPowerFactorInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_POWER_FACTOR));
		tunerClearIsolatedNotesPitchRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_PITCH_RANGE));
		tunerClearIsolatedNotesTimeRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_ISOLATED_NOTES_TIME_RANGE));
		tunerClearVibratoNotesTimeRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.AUDIO_TUNER_CLEAR_VIBRATO_NOTES_TIME_RANGE));
	}

	class SortedStoreProperties extends Properties {

		@Override
		public void store(OutputStream out, String comments) throws IOException {
			Properties sortedProps = new Properties() {
				@Override
				public Set<Map.Entry<Object, Object>> entrySet() {
					/*
					 * Using comparator to avoid the following exception on jdk >=9:
					 * java.lang.ClassCastException:
					 * java.base/java.util.concurrent.ConcurrentHashMap$MapEntry cannot be cast to
					 * java.base/java.lang.Comparable
					 */
					Set<Map.Entry<Object, Object>> sortedSet = new TreeSet<Map.Entry<Object, Object>>(
							new Comparator<Map.Entry<Object, Object>>() {
								@Override
								public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {
									return o1.getKey().toString().compareTo(o2.getKey().toString());
								}
							});
					sortedSet.addAll(super.entrySet());
					return sortedSet;
				}

				@Override
				public Set<Object> keySet() {
					return new TreeSet<Object>(super.keySet());
				}

				@Override
				public synchronized Enumeration<Object> keys() {
					return Collections.enumeration(new TreeSet<Object>(super.keySet()));
				}

			};
			sortedProps.putAll(this);
			sortedProps.store(out, comments);
		}
	}
}
