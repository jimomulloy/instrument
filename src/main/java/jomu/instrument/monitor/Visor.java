/*
*      _______                       _____   _____ _____
*     |__   __|                     |  __ \ / ____|  __ \
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
*
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
*
*/

package jomu.instrument.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.AmplitudeAxisLayer;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.HorizontalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.ui.layers.LegendLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.SpectrumLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.VerticalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.audio.features.ConstantQSource;
import jomu.instrument.audio.features.OnsetInfo;
import jomu.instrument.audio.features.PitchDetectorSource;
import jomu.instrument.audio.features.ScalogramFeatures;
import jomu.instrument.audio.features.ScalogramFrame;
import jomu.instrument.audio.features.SpectralInfo;
import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.audio.features.SpectralPeaksSource;
import jomu.instrument.audio.features.SpectrogramInfo;
import jomu.instrument.audio.features.SpectrogramSource;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class Visor extends JPanel implements OscilloscopeEventHandler, AudioFeatureFrameObserver {

	/**
	 *
	 */
	private static final long serialVersionUID = 3501426880288136245L;

	private List<Double> amplitudes;

	private LinkedPanel bandedPitchDetectPanel;

	// private BeadsLayer beadsLayer;
	// private LinkedPanel beadsPanel;
	private BandedPitchDetectLayer bpdLayer;

	private LinkedPanel constantQPanel;

	private CQLayer cqLayer;

	private LinkedPanel cqPanel;

	// current frequencies and amplitudes of peak list, for sensory dissonance
	// curve
	private List<Double> frequencies;
	private LegendLayer legend;
	private SpectrumLayer noiseFloorLayer;
	private OnsetLayer onsetLayer;
	private LinkedPanel onsetPanel;

	private OscilloscopePanel doscilloscopePanel;

	private PitchDetectLayer pdLayer;
	private LinkedPanel pitchDetectPanel;
	private ScalogramLayer scalogramLayer;
	private LinkedPanel scalogramPanel;
	private SpectrogramLayer sLayer;
	private SpectrumPeaksLayer spectralPeaksLayer;
	private LinkedPanel spectralPeaksPanel;
	private LinkedPanel spectrogramPanel;
	private SpectrumLayer spectrumLayer;
	private LinkedPanel spectrumPanel;
	// private ToneMapLayer toneMapLayer;
	private JPanel toneMapPanel;
	private ToneMapView toneMapView;
	int counter;
	Mixer currentMixer;
	AudioDispatcher dispatcher;

	double threshold;

	private int sampleRate;

	private int fftsize;

	private int noiseFloorMedianFilterLength;// 35

	private float noiseFloorFactor;

	private String fileName;

	private int numberOfSpectralPeaks;

	private int currentFrame;

	private int minPeakSize;

	private final Integer[] fftSizes = { 256, 512, 1024, 2048, 4096, 8192, 16384, 22050, 32768, 65536, 131072 };
	private final Integer[] inputSampleRate = { 8000, 22050, 44100, 192000 };
	private File inputFile;
	private final List<SpectralInfo> spectalInfo = null;

	private JPanel controlPanel;

	private JPanel graphPanel;

	private JPanel diagnosticsPanel;

	private OscilloscopePanel oscilloscopePanel;

	private ParameterManager parameterManager;

	public Visor() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.setLayout(new BorderLayout());
		JPanel topPanel = buildTopPanel();
		graphPanel = buildGraphPanel();
		JSplitPane splitPane = new JSplitPane(SwingConstants.HORIZONTAL, new JScrollPane(topPanel),
				new JScrollPane(graphPanel));
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(150);
		this.add(splitPane, BorderLayout.CENTER);
	}

	private JPanel buildTopPanel() {

		JPanel panel = new JPanel(new GridLayout(1, 2));

		controlPanel = buildControlPanel();
		panel.add(controlPanel);
		panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		diagnosticsPanel = buildDiagnosticsPanel();
		panel.add(diagnosticsPanel);
		panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		Dimension minimumSize = new Dimension(100, 50);
		panel.setMinimumSize(minimumSize);
		return panel;
	}

	private JPanel buildGraphPanel() {

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder())); // BorderFactory.createLineBorder(Color.black));

		toneMapPanel = createToneMapPanel();
		tabbedPane.addTab("ToneMap", toneMapPanel);
		cqPanel = createCQPanel();
		tabbedPane.addTab("CQ", cqPanel);
		bandedPitchDetectPanel = createBandedPitchDetectPanel();
		tabbedPane.addTab("Banded Pitch", bandedPitchDetectPanel);
		pitchDetectPanel = createPitchDetectPanel();
		tabbedPane.addTab("Pitch", pitchDetectPanel);
		spectrogramPanel = createSpectogramPanel();
		tabbedPane.addTab("Spectogram", spectrogramPanel);
		scalogramPanel = createScalogramPanel();
		tabbedPane.addTab("Scalogram", scalogramPanel);
		onsetPanel = createOnsetPanel();
		tabbedPane.addTab("Onset", onsetPanel);
		spectralPeaksPanel = createSpectralPeaksPanel();
		tabbedPane.addTab("SP", spectralPeaksPanel);

		Dimension minimumSize = new Dimension(100, 50);
		tabbedPane.setMinimumSize(minimumSize);

		JPanel panel = new JPanel(new GridLayout(1, 1));
		panel.add(tabbedPane);
		return panel;
	}

	private JPanel buildDiagnosticsPanel() {
		JPanel panel = new JPanel(new GridLayout(1, 1));
		oscilloscopePanel = new OscilloscopePanel();
		panel.add(oscilloscopePanel);
		return panel;
	}

	private JPanel buildControlPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		// panel = new JPanel(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
		actionPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		actionPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		final JFileChooser fileChooser = new JFileChooser(new File("D:/audio"));
		final JButton chooseFileButton = new JButton("Open a file");
		chooseFileButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(Visor.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					inputFile = fileChooser.getSelectedFile();
					System.out.println(inputFile.toString());
					fileName = inputFile.getAbsolutePath();
					try {
						Instrument.getInstance().getCoordinator().getHearing().startAudioFileStream(fileName);
					} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});

		actionPanel.add(chooseFileButton);

		final JButton startListeningButton = new JButton("Start Listening");
		final JButton stopListeningButton = new JButton("Stop Listening");

		startListeningButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					Instrument.getInstance().getCoordinator().getHearing().startAudioLineStream();
					startListeningButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		stopListeningButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					Instrument.getInstance().getCoordinator().getHearing().stopAudioLineStream();
					startListeningButton.setEnabled(true);
					stopListeningButton.setEnabled(false);
					chooseFileButton.setEnabled(true);
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		actionPanel.add(startListeningButton);
		actionPanel.add(stopListeningButton);

		JComboBox<Integer> fftSizeComboBox = new JComboBox<>(fftSizes);
		fftSizeComboBox.addActionListener(new ActionListener() {
			private Integer fftsize;

			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Integer value = (Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				fftsize = value;
				noiseFloorMedianFilterLength = fftsize / 117;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW,
						Integer.toString(fftsize));
				// startProcessing();
			}
		});

		fftSizeComboBox.setSelectedIndex(2);
		actionPanel.add(new JLabel("FFT-size:  "));
		actionPanel.add(fftSizeComboBox);

		JComboBox<Integer> inputSampleRateCombobox = new JComboBox<>(inputSampleRate);
		inputSampleRateCombobox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Integer value = (Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				sampleRate = value;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE,
						Integer.toString(sampleRate));
			}
		});
		inputSampleRateCombobox.setSelectedIndex(2);
		actionPanel.add(new JLabel("Input sample rate:  "));
		actionPanel.add(inputSampleRateCombobox);

		panel.add(actionPanel, BorderLayout.NORTH);

		JPanel parameterPanel = new JPanel();
		parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
		parameterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JSlider audioLowPassSlider = new JSlider(0, 20000);
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

		JSlider audioHighPassSlider = new JSlider(0, 20000);
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

		JPanel tunerSwitchPanel = new JPanel();
		// switchPanel.setLayout(new BoxLayout(switchPanel, BoxLayout.X_AXIS));
		tunerSwitchPanel.setLayout(new GridLayout(1, 4));
		// switchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		tunerSwitchPanel
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		JCheckBox n1SwitchCB = new JCheckBox("n1SwitchCB");
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

		JCheckBox n2SwitchCB = new JCheckBox("n2SwitchCB");
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

		JCheckBox peakSwitchCB = new JCheckBox("peakSwitchCB");
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

		JSlider noiseFloorSlider = new JSlider(100, 250);
		final JLabel noiseFloorFactorLabel = new JLabel("Noise floor factor    :");
		noiseFloorSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				double actualValue = newValue / 100.0;
				noiseFloorFactorLabel.setText(String.format("Noise floor factor (%.2f):", actualValue));

				System.out.println("New noise floor factor: " + actualValue);
				noiseFloorFactor = (float) actualValue;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR,
						Float.toString(noiseFloorFactor));
				// TODO repaintSpectalInfo();

			}
		});
		noiseFloorFactor = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR);
		noiseFloorSlider.setValue((int) noiseFloorFactor);
		parameterPanel.add(noiseFloorFactorLabel);
		parameterPanel.add(noiseFloorSlider);

		JSlider medianFilterSizeSlider = new JSlider(3, 255);
		final JLabel medianFilterSizeLabel = new JLabel("Median Filter Size   :");
		medianFilterSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				medianFilterSizeLabel.setText(String.format("Median Filter Size (%d):", newValue));
				System.out.println("New Median filter size: " + newValue);
				noiseFloorMedianFilterLength = newValue;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH,
						Integer.toString(noiseFloorMedianFilterLength));
				// TODO repaintSpectalInfo();
			}
		});
		noiseFloorMedianFilterLength = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH);
		medianFilterSizeSlider.setValue(noiseFloorMedianFilterLength);
		parameterPanel.add(medianFilterSizeLabel);
		parameterPanel.add(medianFilterSizeSlider);

		JSlider minPeakSizeSlider = new JSlider(5, 255);
		final JLabel minPeakSizeLabel = new JLabel("Min Peak Size   :");
		minPeakSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				minPeakSizeLabel.setText(String.format("Min Peak Size    (%d):", newValue));
				System.out.println("Min Peak Sizee: " + newValue);
				minPeakSize = newValue;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE,
						Integer.toString(minPeakSize));
				// TODO repaintSpectalInfo();
			}
		});
		minPeakSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE);
		minPeakSizeSlider.setValue(minPeakSize);
		parameterPanel.add(minPeakSizeLabel);
		parameterPanel.add(minPeakSizeSlider);

		JSlider numberOfPeaksSlider = new JSlider(1, 40);
		final JLabel numberOfPeaksLabel = new JLabel("Number of peaks  :");
		numberOfPeaksSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();

				numberOfPeaksLabel.setText("Number of peaks (" + newValue + "):");

				numberOfSpectralPeaks = newValue;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS,
						Integer.toString(numberOfSpectralPeaks));
				// TODO repaintSpectalInfo();

			}
		});
		numberOfSpectralPeaks = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS);
		numberOfPeaksSlider.setValue(7);
		parameterPanel.add(numberOfPeaksLabel);
		parameterPanel.add(numberOfPeaksSlider);

		JSlider formantFactorSlider = new JSlider(0, 100);
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

		JSlider formantHighSettingSlider = new JSlider(0, 100);
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

		JSlider formantLowSettingSlider = new JSlider(0, 100);
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

		JSlider formantMiddleSettingSlider = new JSlider(0, 100);
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

		JSlider n1SettingSlider = new JSlider(0, 100);
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

		JSlider n2SettingSlider = new JSlider(0, 100);
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

		JSlider n3SettingSlider = new JSlider(0, 100);
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

		JSlider n4SettingSlider = new JSlider(0, 100);
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

		JSlider n5SettingSlider = new JSlider(0, 100);
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

		JSlider normalizeSettingSlider = new JSlider(0, 100);
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

		JSlider noteHighSlider = new JSlider(0, 100);
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

		JSlider noteLowSlider = new JSlider(0, 100);
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

		JSlider noteMaxDurationSlider = new JSlider(0, 10000);
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

		JSlider noteMinDurationSlider = new JSlider(0, 1000);
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

		JSlider noteSustainSlider = new JSlider(0, 1000);
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

		JSlider pitchHighSlider = new JSlider(36, 72);
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

		JSlider pitchLowSlider = new JSlider(36, 72);
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

		JSlider formantHighFreqSlider = new JSlider(0, 20000);
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

		JSlider formantLowFreqSlider = new JSlider(0, 20000);
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

		JSlider formantMidFreqSlider = new JSlider(0, 20000);
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

		JPanel cqSwitchPanel = new JPanel();
		// switchPanel.setLayout(new BoxLayout(switchPanel, BoxLayout.X_AXIS));
		cqSwitchPanel.setLayout(new GridLayout(1, 4));
		// switchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		cqSwitchPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		JCheckBox compressionSwitchCB = new JCheckBox("compressionSwitchCB");
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

		JCheckBox squareSwitchCB = new JCheckBox("squareSwitchCB");
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

		JCheckBox lowThresholdSwitchCB = new JCheckBox("squareSwitchCB");
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

		JCheckBox decibelSwitchCB = new JCheckBox("decibelSwitchCB");
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

		JCheckBox normaliseSwitchCB = new JCheckBox("normaliseSwitchCB");
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

		JCheckBox tunerSwitchCB = new JCheckBox("tunerSwitchCB");
		tunerSwitchCB.setText("CQ Tuner");
		tunerSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_TUNER,
						Boolean.toString(newValue));
			}
		});

		tunerSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_TUNER));
		cqSwitchPanel.add(tunerSwitchCB);

		JCheckBox peaksSwitchCB = new JCheckBox("peaksSwitchCB");
		peaksSwitchCB.setText("CQ Tuner");
		peaksSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PEAKS,
						Boolean.toString(newValue));
			}
		});

		peaksSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PEAKS));
		cqSwitchPanel.add(peaksSwitchCB);

		parameterPanel.add(cqSwitchPanel);

		JPanel cqParamsPanel = new JPanel();
		// cqParamsPanel.setLayout(new BoxLayout(cqParamsPanel, BoxLayout.X_AXIS));
		cqParamsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		cqParamsPanel.setLayout(new GridLayout(0, 2));
		cqParamsPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		JLabel cqLowThresholdLabel = new JLabel("CQ Low Threshold: ");
		JTextField cqLowThresholdInput = new JTextField(10);
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
		JTextField cqThresholdFactorInput = new JTextField(10);
		cqLowThresholdInput.addActionListener(new ActionListener() {
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
		JTextField cqSignalMinimumInput = new JTextField(10);
		cqLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqThresholdFactorInput.getText();
				cqSignalMinimumLabel.setText(String.format("CQ Signal Minimum  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM, newValue);

			}
		});
		cqSignalMinimumInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM));
		cqParamsPanel.add(cqSignalMinimumLabel);
		cqParamsPanel.add(cqSignalMinimumInput);

		JLabel cqNormaliseThresholdLabel = new JLabel("CQ Normalise Threshold: ");
		JTextField cqNormaliseThresholdInput = new JTextField(10);
		cqLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqThresholdFactorInput.getText();
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
		JTextField cqDecibelLevelInput = new JTextField(10);
		cqLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqThresholdFactorInput.getText();
				cqDecibelLevelLabel.setText(String.format("CQ Decibel Level  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL, newValue);

			}
		});
		cqDecibelLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL));
		cqParamsPanel.add(cqDecibelLevelLabel);
		cqParamsPanel.add(cqDecibelLevelInput);

		JLabel cqCompressionLevelLabel = new JLabel("CQ Compression Level: ");
		JTextField cqCompressionLevelInput = new JTextField(10);
		cqLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = cqThresholdFactorInput.getText();
				cqCompressionLevelLabel.setText(String.format("CQ Compression Level  (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION, newValue);

			}
		});
		cqCompressionLevelInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION));
		cqParamsPanel.add(cqCompressionLevelLabel);
		cqParamsPanel.add(cqCompressionLevelInput);

		parameterPanel.add(cqParamsPanel);

		panel.add(parameterPanel, BorderLayout.SOUTH);

		return panel;
	}

	@Override
	public void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame) {
		updateView(audioFeatureFrame);
	}

	@Override
	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		updateView(audioFeatureFrame);
	}

	public void clearView() {
		// ??toneMapLayer.clear();
		// ??this.toneMapPanel.repaint();
	}

	@Override
	public void handleEvent(float[] data, AudioEvent event) {
		oscilloscopePanel.paint(data, event);
		oscilloscopePanel.repaint();
	}

	public void repaintSpectalInfo(SpectralInfo info) {

		spectrumLayer.clearPeaks();
		spectrumLayer.setSpectrum(info.getMagnitudes());
		noiseFloorLayer.setSpectrum(info.getNoiseFloor(noiseFloorMedianFilterLength, noiseFloorFactor));

		List<SpectralPeak> peaks = info.getPeakList(noiseFloorMedianFilterLength, noiseFloorFactor,
				numberOfSpectralPeaks, minPeakSize);

		StringBuilder sb = new StringBuilder("Frequency(Hz);Step(cents);Magnitude\n");
		frequencies.clear();
		amplitudes.clear();
		for (SpectralPeak peak : peaks) {

			String message = String.format("%.2f;%.2f;%.2f\n", peak.getFrequencyInHertz(),
					peak.getRelativeFrequencyInCents(), peak.getMagnitude());
			sb.append(message);
			// float peakFrequencyInCents =(float)
			// PitchConverter.hertzToAbsoluteCent(peak.getFrequencyInHertz());
			spectrumLayer.setPeak(peak.getBin());
			frequencies.add((double) peak.getFrequencyInHertz());
			amplitudes.add((double) peak.getMagnitude());

		}
		// textArea.setText(sb.toString());
		this.spectrumPanel.repaint();
	}

	public void updateToneMap(ToneMap toneMap) {
		toneMapView.drawToneMap(toneMap);
	}

	private LinkedPanel createBandedPitchDetectPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		bandedPitchDetectPanel = new LinkedPanel(cs);
		bpdLayer = new BandedPitchDetectLayer(cs);
		bandedPitchDetectPanel.addLayer(new BackgroundLayer(cs));
		bandedPitchDetectPanel.addLayer(bpdLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		bandedPitchDetectPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		bandedPitchDetectPanel.addLayer(new ZoomMouseListenerLayer());
		bandedPitchDetectPanel.addLayer(new DragMouseListenerLayer(cs));
		bandedPitchDetectPanel.addLayer(new SelectionLayer(cs));
		bandedPitchDetectPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		bandedPitchDetectPanel.addLayer(legend);
		legend.addEntry("Pitch", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				bandedPitchDetectPanel.repaint();
			}
		};
		bandedPitchDetectPanel.getViewPort().addViewPortChangedListener(listener);
		return bandedPitchDetectPanel;
	}

	private LinkedPanel createCQPanel() {
		CoordinateSystem constantQCS = getCoordinateSystem(AxisUnit.FREQUENCY);
		constantQCS.setMax(Axis.X, 20000);
		constantQPanel = new LinkedPanel(constantQCS);
		cqLayer = new CQLayer(constantQCS);
		constantQPanel.addLayer(new BackgroundLayer(constantQCS));
		constantQPanel.addLayer(cqLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		constantQPanel.addLayer(new VerticalFrequencyAxisLayer(constantQCS));
		constantQPanel.addLayer(new ZoomMouseListenerLayer());
		constantQPanel.addLayer(new DragMouseListenerLayer(constantQCS));
		constantQPanel.addLayer(new SelectionLayer(constantQCS));
		constantQPanel.addLayer(new TimeAxisLayer(constantQCS));

		legend = new LegendLayer(constantQCS, 110);
		constantQPanel.addLayer(legend);
		legend.addEntry("ConstantQ", Color.BLACK);
		legend.addEntry("Pitch estimations", Color.RED);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				constantQPanel.repaint();
			}
		};
		constantQPanel.getViewPort().addViewPortChangedListener(listener);
		return constantQPanel;
	}

	private LinkedPanel createOnsetPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		LinkedPanel constantQPanel = new LinkedPanel(cs);
		onsetLayer = new OnsetLayer(cs);
		constantQPanel.addLayer(new BackgroundLayer(cs));
		constantQPanel.addLayer(onsetLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		constantQPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		constantQPanel.addLayer(new ZoomMouseListenerLayer());
		constantQPanel.addLayer(new DragMouseListenerLayer(cs));
		constantQPanel.addLayer(new SelectionLayer(cs));
		constantQPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		constantQPanel.addLayer(legend);
		legend.addEntry("Onset", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				constantQPanel.repaint();
			}
		};
		constantQPanel.getViewPort().addViewPortChangedListener(listener);
		return constantQPanel;
	}

	private LinkedPanel createPitchDetectPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		pitchDetectPanel = new LinkedPanel(cs);
		pdLayer = new PitchDetectLayer(cs);
		pitchDetectPanel.addLayer(new BackgroundLayer(cs));
		pitchDetectPanel.addLayer(pdLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		pitchDetectPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		pitchDetectPanel.addLayer(new ZoomMouseListenerLayer());
		pitchDetectPanel.addLayer(new DragMouseListenerLayer(cs));
		pitchDetectPanel.addLayer(new SelectionLayer(cs));
		pitchDetectPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		pitchDetectPanel.addLayer(legend);
		legend.addEntry("Pitch", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				pitchDetectPanel.repaint();
			}
		};
		pitchDetectPanel.getViewPort().addViewPortChangedListener(listener);
		return pitchDetectPanel;
	}

	private LinkedPanel createScalogramPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		scalogramPanel = new LinkedPanel(cs);
		scalogramLayer = new ScalogramLayer(cs);
		scalogramPanel.addLayer(new BackgroundLayer(cs));
		scalogramPanel.addLayer(scalogramLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		scalogramPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		scalogramPanel.addLayer(new ZoomMouseListenerLayer());
		scalogramPanel.addLayer(new DragMouseListenerLayer(cs));
		scalogramPanel.addLayer(new SelectionLayer(cs));
		scalogramPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		scalogramPanel.addLayer(legend);
		legend.addEntry("Scalogram", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				scalogramPanel.repaint();
			}
		};
		scalogramPanel.getViewPort().addViewPortChangedListener(listener);
		return scalogramPanel;
	}

	private LinkedPanel createSpectogramPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		spectrogramPanel = new LinkedPanel(cs);
		sLayer = new SpectrogramLayer(cs);
		spectrogramPanel.addLayer(new BackgroundLayer(cs));
		spectrogramPanel.addLayer(sLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		spectrogramPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		spectrogramPanel.addLayer(new ZoomMouseListenerLayer());
		spectrogramPanel.addLayer(new DragMouseListenerLayer(cs));
		spectrogramPanel.addLayer(new SelectionLayer(cs));
		spectrogramPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		spectrogramPanel.addLayer(legend);
		legend.addEntry("Pitch", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				spectrogramPanel.repaint();
			}
		};
		spectrogramPanel.getViewPort().addViewPortChangedListener(listener);
		return spectrogramPanel;
	}

	private LinkedPanel createSpectralPeaksPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		spectralPeaksPanel = new LinkedPanel(cs);
		spectralPeaksLayer = new SpectrumPeaksLayer(cs);
		spectralPeaksPanel.addLayer(new BackgroundLayer(cs));
		spectralPeaksPanel.addLayer(spectralPeaksLayer);
		spectralPeaksPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		spectralPeaksPanel.addLayer(new ZoomMouseListenerLayer());
		spectralPeaksPanel.addLayer(new DragMouseListenerLayer(cs));
		spectralPeaksPanel.addLayer(new SelectionLayer(cs));
		spectralPeaksPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		spectralPeaksPanel.addLayer(legend);
		legend.addEntry("SpectralPeaks", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				spectralPeaksPanel.repaint();
			}
		};
		spectralPeaksPanel.getViewPort().addViewPortChangedListener(listener);
		return spectralPeaksPanel;
	}

	private LinkedPanel createSpectrumPanel() {
		CoordinateSystem cs = new CoordinateSystem(AxisUnit.FREQUENCY, AxisUnit.AMPLITUDE, 0, 10000, false);
		cs.setMax(Axis.X, 4800);
		cs.setMax(Axis.X, 13200);
		spectrumLayer = new SpectrumLayer(cs, 1024, 44100, Color.red);
		noiseFloorLayer = new SpectrumLayer(cs, 1024, 44100, Color.gray);

		spectrumPanel = new LinkedPanel(cs);
		spectrumPanel.addLayer(new ZoomMouseListenerLayer());
		spectrumPanel.addLayer(new DragMouseListenerLayer(cs));
		spectrumPanel.addLayer(new BackgroundLayer(cs));
		spectrumPanel.addLayer(new AmplitudeAxisLayer(cs));

		spectrumPanel.addLayer(new SelectionLayer(cs));
		spectrumPanel.addLayer(new HorizontalFrequencyAxisLayer(cs));
		spectrumPanel.addLayer(spectrumLayer);
		spectrumPanel.addLayer(noiseFloorLayer);

		spectrumPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			boolean painting = false;

			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				if (!painting) {
					painting = true;
					spectrumPanel.repaint();
					painting = false;
				}
			}
		});
		return spectrumPanel;
	}

	private JPanel createToneMapPanel() {
		toneMapView = new ToneMapView();
		JPanel tmContainer = new JPanel(new BorderLayout());
		tmContainer.add(toneMapView, BorderLayout.CENTER);
		tmContainer.setBackground(Color.BLACK);
		return tmContainer;
	}

	private CoordinateSystem getCoordinateSystem(AxisUnit yUnits) {
		float minValue = -1000;
		float maxValue = 1000;
		if (yUnits == AxisUnit.FREQUENCY) {
			minValue = 400;
			maxValue = 12000;
		}
		return new CoordinateSystem(yUnits, minValue, maxValue);
	}

	private void updateView(AudioFeatureFrame audioFeatureFrame) {
		// toneMapLayer.update(audioFeatureFrame);
		scalogramLayer.update(audioFeatureFrame);
		// toneMapLayer.update(audioFeatureFrame);
		// beadsLayer.update(audioFeatureFrame);
		cqLayer.update(audioFeatureFrame);
		onsetLayer.update(audioFeatureFrame);
		spectralPeaksLayer.update(audioFeatureFrame);
		pdLayer.update(audioFeatureFrame);
		// bpdLayer.update(audioFeatureFrame);
		sLayer.update(audioFeatureFrame);
		// if (count % 10 == 0) {
		// this.toneMapPanel.repaint();
		this.scalogramPanel.repaint();
		this.spectrogramPanel.repaint();
		this.cqPanel.repaint();
		this.onsetPanel.repaint();
		this.spectralPeaksPanel.repaint();
		this.pitchDetectPanel.repaint();
		// this.bandedPitchDetectPanel.repaint();
		// this.beadsPanel.repaint();
		// }
		// count++;
		// SpectralPeaksFeatures specFeatures = audioFeatureFrame
		// .getSpectralPeaksFeatures();
		// repaintSpectalInfo(specFeatures.getSpectralInfo().get(0));
	}

	private static class BandedPitchDetectLayer implements Layer {

		private float binHeight;
		private float[] binStartingPointsInCents;
		private float binWidth;
		private final CoordinateSystem cs;
		TreeMap<Double, SpectrogramInfo> features;

		public BandedPitchDetectLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		@Override
		public void draw(Graphics2D graphics) {

			if (features != null) {
				System.out.println(">>PD max amp: " + binWidth + ", " + binHeight);

				Map<Double, SpectrogramInfo> spSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				double maxAmp = 0.00001;
				for (Entry<Double, SpectrogramInfo> column : spSubMap.entrySet()) {

					double timeStart = column.getKey();// in seconds
					SpectrogramInfo spectrogramInfo = column.getValue();// in
																		// cents
					float pitch = spectrogramInfo.getPitchDetectionResult().getPitch(); // -1?
					float[] amplitudes = spectrogramInfo.getAmplitudes();
					// draw the pixels
					for (int i = 0; i < amplitudes.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = binStartingPointsInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							// int greyValue = 255 - (int)
							// (Math.log1p(spectralEnergy[i])
							// / Math.log1p(currentMaxSpectralEnergy) * 255);
							if (amplitudes[i] > maxAmp) {
								maxAmp = amplitudes[i];
							}
							int greyValue = 255 - (int) (amplitudes[i] / (maxAmp) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									Math.round(binWidth * 1000), (int) Math.ceil(binHeight));
							// graphics.fillRect((int) Math.round(timeStart *
							// 1000),
							// Math.round(centsStartingPoint),
							// (int) Math.round(100), (int) Math.ceil(100));

						}
					}
					// System.out.println(">>PD max amp: " + maxAmp + ", " +
					// timeStart);

					if (pitch > -1) {
						double cents = PitchConverter.hertzToAbsoluteCent(pitch);
						Color color = Color.red;
						// only draw the visible frequency range
						if (cents >= cs.getMin(Axis.Y) && cents <= cs.getMax(Axis.Y)) {
							// int greyValue = (int) (255F * probability);
							// greyValue = Math.max(0, greyValue);
							// color = new Color(greyValue, greyValue,
							// greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), (int) cents, Math.round(40),
									(int) Math.ceil(100));
						}
					}

				}

			}
		}

		@Override
		public String getName() {
			return "Pitch Detect Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
//					BandedPitchDetectorSource bpds = audioFeatureFrame.getBandedPitchDetectorFeatures().getBpds();
//					binStartingPointsInCents = bpds.getBinStartingPointsInCents(2048);
//					binWidth = bpds.getBinWidth(2048);
//					binHeight = bpds.getBinHeight(2048);
//					Map<Integer, TreeMap<Double, SpectrogramInfo>> bfs = audioFeatureFrame
//							.getBandedPitchDetectorFeatures().getFeatures();
//					if (features == null) {
//						features = new TreeMap<>();
//					}
//					TreeMap<Double, SpectrogramInfo> fs = bfs.get(2048);
//					if (fs != null) {
//						for (Entry<Double, SpectrogramInfo> entry : fs.entrySet()) {
//							features.put(entry.getKey(), entry.getValue());
//						}
//					}
				}
			});
		}
	}
	//
	// private static class BeadsLayer implements Layer {
	//
	// private float binHeight;
	// private float[] binStartingPointsInCents;
	//
	// private float binWidth;
	// private final CoordinateSystem cs;
	// private TreeMap<Double, float[][]> features;
	//
	// public BeadsLayer(CoordinateSystem cs) {
	// this.cs = cs;
	// }
	//
	// @Override
	// public void draw(Graphics2D graphics) {
	//
	// if (features != null) {
	// Map<Double, float[][]> spectralInfoSubMap = features.subMap(
	// cs.getMin(Axis.X) / 1000.0, cs.getMax(Axis.X) / 1000.0);
	//
	// double currentMaxSpectralEnergy = 0;
	// // for (Map.Entry<Double, float[][]> column :
	// // spectralInfoSubMap.entrySet()) {
	// // float[][] spectralEnergy = column.getValue();
	// // for (int i = 0; i < spectralEnergy.length; i++) {
	// // currentMaxSpectralEnergy = Math.max(currentMaxSpectralEnergy,
	// // spectralEnergy[i]);
	// // }
	// // }
	// for (Map.Entry<Double, float[][]> column : spectralInfoSubMap
	// .entrySet()) {
	// double timeStart = column.getKey();// in seconds
	// float[][] spectralEnergy = column.getValue();// in cents
	// // draw the pixels
	// for (float[] element : spectralEnergy) {
	// Color color = Color.black;
	// float centsStartingPoint = (float) PitchConverter
	// .hertzToAbsoluteCent(element[0]);
	// // only draw the visible frequency range
	// if (centsStartingPoint >= cs.getMin(Axis.Y)
	// && centsStartingPoint <= cs.getMax(Axis.Y)) {
	// int greyValue = 255 - (int) (Math.log1p(element[1])
	// / Math.log1p(currentMaxSpectralEnergy)
	// * 255);
	// greyValue = Math.max(0, greyValue);
	// color = new Color(greyValue, greyValue, greyValue);
	// graphics.setColor(color);
	// graphics.fillRect(
	// (int) Math.round(timeStart * 1000),
	// Math.round(centsStartingPoint),
	// Math.round(binWidth * 1000),
	// (int) Math.ceil(binHeight));
	// }
	// }
	// }
	// }
	// }
	//
	// @Override
	// public String getName() {
	// return "Beads Layer";
	// }
	//
	// public void update(AudioFeatureFrame audioFeatureFrame) {
	// SwingUtilities.invokeLater(new Runnable() {
	// @Override
	// public void run() {
	// if (features == null) {
	// features = new TreeMap<>();
	// }
	// List<FeatureFrame> ffs = audioFeatureFrame
	// .getBeadsFeatures();
	// for (FeatureFrame ff : ffs) {
	// float[][] fs = (float[][]) ff
	// .get(SpectralPeaks.class.getSimpleName());
	// features.put(ff.getStartTimeMS(), fs);
	// }
	// }
	// });
	// }
	// }

	private static class CQLayer implements Layer {

		private float binHeight;
		private float[] binStartingPointsInCents;

		private float binWidth;
		private TreeMap<Double, float[]> cqFeatures;
		private final CoordinateSystem cs;

		public CQLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		@Override
		public void draw(Graphics2D graphics) {

			if (cqFeatures != null) {
				Map<Double, float[]> spectralInfoSubMap = cqFeatures.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				float minValue = 5 / 1000000.0F;
				float currentMaxSpectralEnergy = 0;
				for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
					float[] spectralEnergy = column.getValue();
					for (float element : spectralEnergy) {
						float magnitude = Math.max(minValue, element);
						magnitude = (float) Math.log10(1 + (100.0 * magnitude));
						currentMaxSpectralEnergy = Math.max(currentMaxSpectralEnergy, magnitude);
					}
				}
				for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
					double timeStart = column.getKey();// in seconds
					float[] spectralEnergy = column.getValue();// in cents
					// draw the pixels
					for (int i = 0; i < spectralEnergy.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = binStartingPointsInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							// int greyValue = 255 - (int)
							// (Math.log1p(spectralEnergy[i])
							// / Math.log1p(currentMaxSpectralEnergy) * 255);
							float magnitude = Math.max(minValue, spectralEnergy[i]);
							magnitude = (float) Math.log10(1 + (100.0 * magnitude));
							int greyValue = 255 - (int) (magnitude / (currentMaxSpectralEnergy) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									Math.round(binWidth * 1000), (int) Math.ceil(binHeight));
						}
					}
				}
			}
		}

		@Override
		public String getName() {
			return "CQ Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ConstantQSource cqs = audioFeatureFrame.getConstantQFeatures().getCqs();
					binStartingPointsInCents = cqs.getBinStartingPointsInCents();
					binWidth = cqs.getBinWidth();
					binHeight = cqs.getBinHeight();
					Map<Double, float[]> fs = audioFeatureFrame.getConstantQFeatures().getFeatures();
					if (cqFeatures == null) {
						cqFeatures = new TreeMap<>();
					}
					for (java.util.Map.Entry<Double, float[]> entry : fs.entrySet()) {
						cqFeatures.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}

	private static class OnsetLayer implements Layer {

		private final CoordinateSystem cs;
		private TreeMap<Double, OnsetInfo[]> features;

		public OnsetLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		@Override
		public void draw(Graphics2D graphics) {

			if (features != null) {
				Map<Double, OnsetInfo[]> onsetInfoSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);

				for (Map.Entry<Double, OnsetInfo[]> column : onsetInfoSubMap.entrySet()) {
					double timeStart = column.getKey();// in seconds
					OnsetInfo[] onsetInfo = column.getValue();// in cents
					// draw the pixels
					for (OnsetInfo element : onsetInfo) {
						float centsStartingPoint = (float) (((cs.getMax(Axis.Y) - cs.getMin(Axis.Y)) / 2.0)
								+ cs.getMin(Axis.Y));
						Color color = Color.red;
						graphics.setColor(color);
						graphics.fillRect((int) Math.round(element.getTime() * 1000), Math.round(centsStartingPoint),
								Math.round(100), (int) Math.ceil(100));
					}
				}
			}
		}

		@Override
		public String getName() {
			return "Onset Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					Map<Double, OnsetInfo[]> fs = audioFeatureFrame.getOnsetFeatures().getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					for (Entry<Double, OnsetInfo[]> entry : fs.entrySet()) {
						features.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}

	private static class OscilloscopePanel extends JPanel {

		/**
		 *
		 */
		private static final long serialVersionUID = 4969781241442094359L;

		float data[];

		public OscilloscopePanel() {
			setMinimumSize(new Dimension(80, 60));
		}

		public void paint(float[] data, AudioEvent event) {
			this.data = data;
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g); // paint background
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.WHITE);
			if (data != null) {
				float width = getWidth();
				float height = getHeight();
				float halfHeight = height / 2;
				for (int i = 0; i < data.length; i += 4) {
					g.drawLine((int) (data[i] * width), (int) (halfHeight - data[i + 1] * height),
							(int) (data[i + 2] * width), (int) (halfHeight - data[i + 3] * height));
				}
			}
		}
	}

	private static class PitchDetectLayer implements Layer {

		private float binHeight;
		private float[] binStartingPointsInCents;
		private float binWidth;
		private final CoordinateSystem cs;
		TreeMap<Double, SpectrogramInfo> features;

		public PitchDetectLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		@Override
		public void draw(Graphics2D graphics) {

			if (features != null) {
				System.out.println(">>PD max amp: " + binWidth + ", " + binHeight);

				Map<Double, SpectrogramInfo> spSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				double maxAmp = 0.00001;
				for (Entry<Double, SpectrogramInfo> column : spSubMap.entrySet()) {

					double timeStart = column.getKey();// in seconds
					SpectrogramInfo spectrogramInfo = column.getValue();// in
																		// cents
					float pitch = spectrogramInfo.getPitchDetectionResult().getPitch(); // -1?
					float[] amplitudes = spectrogramInfo.getAmplitudes();
					// draw the pixels
					for (int i = 0; i < amplitudes.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = binStartingPointsInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							// int greyValue = 255 - (int)
							// (Math.log1p(spectralEnergy[i])
							// / Math.log1p(currentMaxSpectralEnergy) * 255);
							if (amplitudes[i] > maxAmp) {
								maxAmp = amplitudes[i];
							}
							int greyValue = 255 - (int) (amplitudes[i] / (maxAmp) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									Math.round(binWidth * 1000), (int) Math.ceil(binHeight));

						}
					}
					// System.out.println(">>PD max amp: " + maxAmp + ", " +
					// timeStart);

					if (pitch > -1) {
						double cents = PitchConverter.hertzToAbsoluteCent(pitch);
						Color color = Color.red;
						// only draw the visible frequency range
						if (cents >= cs.getMin(Axis.Y) && cents <= cs.getMax(Axis.Y)) {
							// int greyValue = (int) (255F * probability);
							// greyValue = Math.max(0, greyValue);
							// color = new Color(greyValue, greyValue,
							// greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), (int) cents, Math.round(40),
									(int) Math.ceil(100));
						}
					}

				}

			}
		}

		@Override
		public String getName() {
			return "Pitch Detect Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					PitchDetectorSource pds = audioFeatureFrame.getPitchDetectorFeatures().getPds();
					binStartingPointsInCents = pds.getBinStartingPointsInCents();
					binWidth = pds.getBinWidth();
					binHeight = pds.getBinHeight();
					TreeMap<Double, SpectrogramInfo> fs = audioFeatureFrame.getPitchDetectorFeatures().getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					for (Entry<Double, SpectrogramInfo> entry : fs.entrySet()) {
						features.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}

	private static class ScalogramLayer implements Layer {

		private final CoordinateSystem cs;
		private TreeMap<Double, ScalogramFrame> features;

		public ScalogramLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		@Override
		public void draw(Graphics2D graphics) {
			if (features == null) {
				return;
			}
			Map<Double, ScalogramFrame> spectralInfoSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
					cs.getMax(Axis.X) / 1000.0);
			for (Map.Entry<Double, ScalogramFrame> frameEntry : spectralInfoSubMap.entrySet()) {
				double timeStart = frameEntry.getKey();// in seconds
				ScalogramFrame frame = frameEntry.getValue();// in cents

				for (int level = 0; level < frame.getDataPerScale().length; level++) {
					for (int block = 0; block < frame.getDataPerScale()[level].length; block++) {
						Color color = Color.black;
						float centsStartingPoint = frame.getStartFrequencyPerLevel()[level];
						float centsHeight = frame.getStopFrequencyPerLevel()[level] - centsStartingPoint;
						// only draw the visible frequency range
						if (centsStartingPoint + centsHeight >= cs.getMin(Axis.Y)
								&& centsStartingPoint <= cs.getMax(Axis.Y)) {
							float factor = Math.abs(frame.getDataPerScale()[level][block] / frame.getCurrentMax());

							double startTimeBlock = timeStart
									+ (block + 1) * frame.getDurationsOfBlockPerLevel()[level];
							double timeDuration = frame.getDurationsOfBlockPerLevel()[level];

							int greyValue = (int) (factor * 0.99 * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							color = Color.black;
							graphics.setColor(color);
							// graphics.fillRect((int) Math.round(startTimeBlock
							// * 1000),
							// Math.round(centsStartingPoint),
							// (int) Math.round(timeDuration * 1000), (int)
							// Math.ceil(centsHeight));
							graphics.fillRect((int) Math.round(startTimeBlock * 1000), Math.round(centsStartingPoint),
									Math.round(100), (int) Math.ceil(100));
							// System.out.println(">>scalo: " + startTimeBlock +
							// ", " + centsStartingPoint +
							// ", " + timeDuration + ", " + centsHeight);
						}
					}
				}
			}
		}

		@Override
		public String getName() {
			return "Scalogram Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ScalogramFeatures scf = audioFeatureFrame.getScalogramFeatures();
					TreeMap<Double, ScalogramFrame> fs = scf.getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					for (java.util.Map.Entry<Double, ScalogramFrame> entry : fs.entrySet()) {
						features.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}

	private class SpectrogramLayer implements Layer {

		private float binHeight;
		private float[] binHeightInCents;
		private float[] binStartingPointsInCents;
		private float binWidth;
		private final CoordinateSystem cs;
		TreeMap<Double, SpectrogramInfo> features;

		public SpectrogramLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		@Override
		public void draw(Graphics2D graphics) {

			if (features != null) {
				Map<Double, SpectrogramInfo> spSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				double maxAmp = 100;
				for (Entry<Double, SpectrogramInfo> column : spSubMap.entrySet()) {

					double timeStart = column.getKey();// in seconds
					SpectrogramInfo spectrogramInfo = column.getValue();// in
																		// cents
					float pitch = spectrogramInfo.getPitchDetectionResult().getPitch(); // -1?
					float[] amplitudes = spectrogramInfo.getAmplitudes();
					// draw the pixels
					for (int i = 0; i < amplitudes.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = binStartingPointsInCents[i];
						float binHeight = binHeightInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							// int greyValue = 255 - (int)
							// (Math.log1p(spectralEnergy[i])
							// / Math.log1p(currentMaxSpectralEnergy) * 255);
							if (amplitudes[i] > maxAmp) {
								maxAmp = amplitudes[i];
							}
							int greyValue = 255 - (int) (amplitudes[i] / (maxAmp) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									Math.round(binWidth * 1000), (int) Math.ceil(binHeight));

						}
					}

					if (pitch > -1) {
						double cents = PitchConverter.hertzToAbsoluteCent(pitch);
						Color color = Color.red;
						// only draw the visible frequency range
						if (cents >= cs.getMin(Axis.Y) && cents <= cs.getMax(Axis.Y)) {
							// int greyValue = (int) (255F * probability);
							// greyValue = Math.max(0, greyValue);
							// color = new Color(greyValue, greyValue,
							// greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), (int) cents, Math.round(40),
									(int) Math.ceil(100));
						}
					}

				}

			}
		}

		@Override
		public String getName() {
			return "Spectogram Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					SpectrogramSource ss = audioFeatureFrame.getSpectrogramFeatures().getSs();
					binStartingPointsInCents = ss.getBinStartingPointsInCents();
					binHeightInCents = ss.getBinhHeightInCents();
					binWidth = ss.getBinWidth();
					binHeight = ss.getBinHeight();
					TreeMap<Double, SpectrogramInfo> fs = audioFeatureFrame.getSpectrogramFeatures().getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					for (Entry<Double, SpectrogramInfo> entry : fs.entrySet()) {
						features.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}

	private static class SpectrumPeaksLayer implements Layer {

		private final CoordinateSystem cs;
		private int fftSize;
		private float multiplier = 10;
		private List<Integer> peaksInBins;
		private int sampleRate;
		private float[] spectrum;
		private TreeMap<Double, SpectralInfo> spFeatures;
		int minPeakSize = 5;
		float noiseFloorFactor = 1.5F;
		int noiseFloorMedianFilterLenth = 17;
		int numberOfSpectralPeaks = 7;

		public SpectrumPeaksLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		@Override
		public void draw(Graphics2D graphics) {

			if (spFeatures != null && !spFeatures.isEmpty()) {
				Map<Double, SpectralInfo> spectralInfoSubMap = spFeatures.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);

				for (Map.Entry<Double, SpectralInfo> column : spectralInfoSubMap.entrySet()) {
					double timeStart = column.getKey();// in seconds
					SpectralInfo spectralInfo = column.getValue();

					List<SpectralPeak> peaks = spectralInfo.getPeakList(noiseFloorMedianFilterLenth, noiseFloorFactor,
							numberOfSpectralPeaks, minPeakSize);

					int markerWidth = Math.round(LayerUtilities.pixelsToUnits(graphics, 7, true));
					int markerheight = Math.round(LayerUtilities.pixelsToUnits(graphics, 7, false));
					// draw the pixels
					for (SpectralPeak peak : peaks) {
						int bin = peak.getBin();
						float hertzValue = (bin * sampleRate) / (float) fftSize;
						int frequencyInCents = (int) Math
								.round(PitchConverter.hertzToAbsoluteCent(hertzValue) - markerWidth / 2.0f);

						Color color = Color.black;
						float magnitude = peak.getMagnitude();
						// only draw the visible frequency range
						if (frequencyInCents >= cs.getMin(Axis.Y) && frequencyInCents <= cs.getMax(Axis.Y)) {
							int greyValue = (int) ((magnitude / 100F) * 255F);
							// int greyValue = 255 - (int)
							// (Math.log1p(magnitude)
							// / Math.log1p(100) * 255);
							// greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), frequencyInCents, markerWidth,
									markerheight);
						}
					}
				}
			}
		}

		@Override
		public String getName() {
			return "Spectral Peaks Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					SpectralPeaksSource sps = audioFeatureFrame.getSpectralPeaksFeatures().getSps();

					noiseFloorMedianFilterLenth = sps.getNoiseFloorMedianFilterLenth();
					noiseFloorFactor = sps.getNoiseFloorFactor();
					numberOfSpectralPeaks = sps.getNumberOfSpectralPeaks();
					minPeakSize = sps.getMinPeakSize();
					fftSize = sps.getBufferSize();
					sampleRate = sps.getSampleRate();

					TreeMap<Double, SpectralInfo> fs = audioFeatureFrame.getSpectralPeaksFeatures().getFeatures();
					if (spFeatures == null) {
						spFeatures = new TreeMap<>();
					}
					for (java.util.Map.Entry<Double, SpectralInfo> entry : fs.entrySet()) {
						spFeatures.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}

	private static class ToneMapLayer implements Layer {

		private final CoordinateSystem cs;
		private TreeMap<Double, ToneMap> toneMaps;

		public ToneMapLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void clear() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					toneMaps = new TreeMap<>();
				}
			});

		}

		@Override
		public void draw(Graphics2D g) {

			if (toneMaps != null) {

				Map<Double, ToneMap> toneMapsSubMap = toneMaps.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				double timeStart = 0.0F;
				for (Map.Entry<Double, ToneMap> column : toneMapsSubMap.entrySet()) {
					timeStart = column.getKey();
					ToneMap toneMap = column.getValue();
					ToneTimeFrame[] ttfs = toneMap.getTimeFramesFrom(0.0);
					for (ToneTimeFrame ttf : ttfs) {
						TimeSet timeSet = ttf.getTimeSet();
						PitchSet pitchSet = ttf.getPitchSet();
						timeStart = timeSet.getStartTime();

						ToneMapElement[] elements = ttf.getElements();

						double ampT;
						double lowThreshhold = 0.0;
						double highThreshhold = 100.0;
						double maxAmplitude = -1;
						for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {

							ToneMapElement toneMapElement = elements[elementIndex];
							if (toneMapElement != null) {
								double amplitude = 0.0;
								if (toneMapElement.amplitude > 1.0) {
									amplitude = 100.0 * toneMapElement.amplitude / ttf.getMaxAmplitude();
								}
								int greyValue = (int) (Math.log1p(toneMapElement.amplitude / ttf.getMaxAmplitude())
										/ Math.log1p(1.0000001) * 255);

								if (amplitude > maxAmplitude) {
									maxAmplitude = amplitude;
								}
								if (amplitude <= lowThreshhold) {
									g.setColor(Color.white);
								} else if (amplitude >= highThreshhold) {
									g.setColor(Color.black);
								} else {
									ampT = (amplitude - lowThreshhold) / (highThreshhold - lowThreshhold);
									// greyValue = 255 - (int) (ampT * 255);
									greyValue = 255 - greyValue;
									greyValue = Math.max(0, greyValue);
									Color color = new Color(greyValue, greyValue, greyValue);
									g.setColor(color);
									// g.setColor(new Color(255, 0, 0));
									// g.setColor(new Color((int) (255 * ampT),
									// (int) (255 * ampT),
									// (int) (255 * ampT)));
								}
								double cents = PitchConverter.hertzToAbsoluteCent(pitchSet.getFreq(elementIndex));

								double width = timeSet.getEndTime() - timeSet.getStartTime();

								g.fillRect((int) Math.floor(timeStart * 1000), (int) Math.floor(cents),
										(int) Math.round(width * 1000), 100);
							}
						}
					}
				}
				// cs.setWrappingOrigin((float) timeStart);
			}
		}

		@Override
		public String getName() {
			return "ToneMap Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ToneMap toneMap = audioFeatureFrame.getConstantQFeatures().getToneMap();
					if (toneMap != null) {
						if (toneMaps == null) {
							toneMaps = new TreeMap<>();
						}
						toneMaps.put(audioFeatureFrame.getStart() / 1000.0, toneMap);
					}
					float timeEnd = 0.0F;
					for (Map.Entry<Double, ToneMap> column : toneMaps.entrySet()) {
						timeEnd = (float) (column.getKey() * 1000);
					}
					float csX = cs.getMax(Axis.X) - cs.getMin(Axis.X);
					if (timeEnd > cs.getMax(Axis.X)) {
						cs.setMax(Axis.X, timeEnd);
						cs.setMin(Axis.X, timeEnd - csX);
					} else {
						cs.setMax(Axis.X, csX);
						cs.setMin(Axis.X, 0);
					}
				}
			});
		}
	}
}
