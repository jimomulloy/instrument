package jomu.instrument.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

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
import be.tarsos.dsp.ui.layers.LegendLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.VerticalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.audio.features.ConstantQSource;
import jomu.instrument.audio.features.PitchDetectorSource;
import jomu.instrument.audio.features.SpectralInfo;
import jomu.instrument.audio.features.SpectralPeaksSource;
import jomu.instrument.audio.features.SpectrogramInfo;
import jomu.instrument.cognition.cell.Cell;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class Visor extends JPanel implements OscilloscopeEventHandler, AudioFeatureFrameObserver {

	private static String defaultAudioFileFolder = "D:/audio";
	private static String defaultAudioRecordFileFolder = "D:/audio/record";
	private static String defaultAudioFile = "3notescale.wav";

	private LinkedPanel constantQPanel;

	private CQLayer cqLayer;

	private LinkedPanel cqPanel;

	private SpectrumLayer noiseFloorLayer;

	private PitchDetectLayer pdLayer;
	private LinkedPanel pitchDetectPanel;
	private SpectrumLayer spectrumLayer;
	private LinkedPanel spectrumPanel;
	private ToneMapView toneMapView;
	private String currentToneMapViewType;

	private Map<String, ToneMap> toneMapViews = new HashMap<>();

	private String fileName;

	private static final Integer[] fftSizes = { 256, 512, 1024, 2048, 4096, 8192, 16384, 22050, 32768, 65536, 131072 };
	private static final Integer[] inputSampleRate = { 8000, 22050, 44100, 192000 };

	private File inputFile;

	private JPanel diagnosticsPanel;

	private OscilloscopePanel oscilloscopePanel;

	private ParameterManager parameterManager;

	private ChromaView chromaPreView;

	private ChromaView chromaPostView;

	private BeatsView beatsView;

	private BeatsView percussionView;

	private JFrame mainframe;

	private JPanel beatsPanel;

	private InstrumentStoreService iss;
	private JTextField audioFeatureIntervalInput;
	private JTextField timeAxisOffsetInput;
	private JTextField pitchAxisOffsetInput;
	private JTextField timeAxisRangeInput;
	private JTextField pitchAxisRangeInput;
	private JComboBox toneMapViewComboBox;
	private JCheckBox playMidiSwitchCB;
	private JCheckBox playAudioSwitchCB;
	private JTextField voicePlayerLowThresholdInput;
	private JTextField voicePlayerHighThresholdInput;
	private JTextField voicePlayerDelayInput;
	private JTextField frameNumberInput;
	private JTextField toneMapViewLowThresholdInput;
	private JTextField toneMapViewHighThresholdInput;
	private JTextField audioOffsetInput;
	private JTextField audioRangeInput;
	private JCheckBox recordSwitchCB;
	private Workspace workspace;
	private JCheckBox midiPlayBaseSwitchCB;
	private JCheckBox playResynthSwitchCB;
	private JCheckBox midiPlayVoice1SwitchCB;
	private AbstractButton midiPlayVoice2SwitchCB;
	private AbstractButton midiPlayVoice3SwitchCB;
	private AbstractButton midiPlayVoice4SwitchCB;
	private AbstractButton midiPlayChord1SwitchCB;
	private AbstractButton midiPlayChord2SwitchCB;
	private AbstractButton midiPlayPad1SwitchCB;
	private JCheckBox midiPlayPad2SwitchCB;
	private JCheckBox midiPlayBeat1SwitchCB;
	private JCheckBox midiPlayBeat2SwitchCB;
	private JCheckBox midiPlayBeat3SwitchCB;
	private JCheckBox midiPlayBeat4SwitchCB;
	private TimeFramePanel timeFramePanel;
	private JButton chooseFileButton;
	private JButton startFileProcessingButton;
	private JButton startListeningButton;
	private JButton stopListeningButton;
	private JCheckBox playPeaksSwitchCB;
	private AbstractButton showPeaksSwitchCB;

	public Visor(JFrame mainframe) {
		this.mainframe = mainframe;
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.iss = Instrument.getInstance().getStorage().getInstrumentStoreService();
		this.workspace = Instrument.getInstance().getWorkspace();
		this.setLayout(new BorderLayout());
		JPanel topPanel = buildTopPanel();
		JPanel bottomPanel = buildBottomPanel();
		JScrollPane graphPanel = buildGraphPanel();
		this.add(topPanel, BorderLayout.NORTH);
		this.add(graphPanel, BorderLayout.CENTER);
		this.add(bottomPanel, BorderLayout.SOUTH);
	}

	private JPanel buildTopPanel() {

		JPanel panel = new JPanel(new GridLayout(1, 1));

		JPanel controlPanel = buildControlPanel();
		panel.add(controlPanel);

		return panel;
	}

	private JPanel buildBottomPanel() {

		JPanel panel = new JPanel(new GridLayout(1, 1));

		JPanel viewControlPanel = buildViewControlPanel();
		panel.add(viewControlPanel);

		return panel;
	}

	private JScrollPane buildGraphPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 1));

		JPanel leftGraphPanel = new JPanel();
		leftGraphPanel.setLayout(new BorderLayout());

		JPanel rightGraphPanel = new JPanel();
		rightGraphPanel.setLayout(new BorderLayout());

		JSplitPane splitPane = new JSplitPane(SwingConstants.VERTICAL, new JScrollPane(leftGraphPanel),
				new JScrollPane(rightGraphPanel));

		Toolkit myScreen = Toolkit.getDefaultToolkit();
		Dimension screenSize = myScreen.getScreenSize();
		int screenHeight = screenSize.height;
		int screenWidth = screenSize.width;

		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation((int) ((double) screenWidth * 0.7));

		JPanel chromaPanel = buildChromaPanel();
		JPanel spectrumsPanel = buildSpectrumsPanel();
		JPanel beatsPanel = buildBeatsPanel();
		JSplitPane leftTopPane = new JSplitPane(SwingConstants.HORIZONTAL, new JScrollPane(chromaPanel),
				new JScrollPane(beatsPanel));
		JSplitPane leftBottomPane = new JSplitPane(SwingConstants.HORIZONTAL, new JScrollPane(leftTopPane),
				new JScrollPane(spectrumsPanel));
		leftTopPane.setOneTouchExpandable(true);
		leftTopPane.setDividerLocation(230);
		leftBottomPane.setOneTouchExpandable(true);
		leftBottomPane.setDividerLocation(370);

		leftGraphPanel.add(leftBottomPane, BorderLayout.CENTER);

		diagnosticsPanel = buildDiagnosticsPanel();
		rightGraphPanel.add(diagnosticsPanel, BorderLayout.CENTER);
		Dimension minimumSize = new Dimension(100, 1000);
		panel.setMinimumSize(minimumSize);
		panel.add(splitPane);
		return new JScrollPane(panel);
	}

	private JPanel buildSpectrumsPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane toneMapTabbedPane = new JTabbedPane();
		toneMapTabbedPane
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder())); // BorderFactory.createLineBorder(Color.black));
		JPanel toneMapLayerPanel = createToneMapLayerPanel();
		toneMapTabbedPane.addTab("ToneMap1", toneMapLayerPanel);
		cqPanel = createCQPanel();
		toneMapTabbedPane.addTab("CQ", cqPanel);
		pitchDetectPanel = createPitchDetectPanel();
		toneMapTabbedPane.addTab("Pitch", pitchDetectPanel);
		panel.add(toneMapTabbedPane, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildChromaPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane chromaTabbedPane = new JTabbedPane();
		chromaTabbedPane
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder())); // BorderFactory.createLineBorder(Color.black));
		chromaPreView = new ChromaView();
		JPanel chromaPrePanel = new JPanel(new BorderLayout());
		chromaPrePanel.add(chromaPreView, BorderLayout.CENTER);
		chromaPrePanel.setBackground(Color.BLACK);
		chromaTabbedPane.addTab("Chroma Pre", chromaPrePanel);
		chromaPostView = new ChromaView();
		JPanel chromaPostPanel = new JPanel(new BorderLayout());
		chromaPostPanel.add(chromaPostView, BorderLayout.CENTER);
		chromaPostPanel.setBackground(Color.BLACK);
		chromaTabbedPane.addTab("Chroma Post", chromaPostPanel);
		panel.add(chromaTabbedPane, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildBeatsPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane beatsTabbedPane = new JTabbedPane();
		beatsTabbedPane
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder())); // BorderFactory.createLineBorder(Color.black));

		beatsView = new BeatsView();
		JPanel beatsPanel = new JPanel(new BorderLayout());
		beatsPanel.add(beatsView, BorderLayout.CENTER);
		beatsPanel.setBackground(Color.BLACK);
		beatsTabbedPane.addTab("Beats", beatsPanel);

		percussionView = new BeatsView();
		JPanel percussionPanel = new JPanel(new BorderLayout());
		percussionPanel.add(percussionView, BorderLayout.CENTER);
		percussionPanel.setBackground(Color.BLACK);
		beatsTabbedPane.addTab("Percussion", percussionPanel);

		panel.add(beatsTabbedPane, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildDiagnosticsPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane diagnosticsTabbedPane = new JTabbedPane();
		diagnosticsTabbedPane
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder())); // BorderFactory.createLineBorder(Color.black));
		timeFramePanel = new TimeFramePanel();
		diagnosticsTabbedPane.addTab("ToneMap Frame", timeFramePanel);
		oscilloscopePanel = new OscilloscopePanel();
		diagnosticsTabbedPane.addTab("Oscilloscope", oscilloscopePanel);
		spectrumPanel = createSpectrumPanel();
		diagnosticsTabbedPane.addTab("Spectrum", spectrumPanel);
		panel.add(diagnosticsTabbedPane, BorderLayout.CENTER);
		return panel;
	}

	public String getCurrentLocalDateTimeStamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS"));
	}

	private JPanel buildViewControlPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		JPanel graphControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JLabel frameNumberLabel = new JLabel("Frame #: ");
		frameNumberInput = new JTextField(4);
		frameNumberInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				int frame = Integer.valueOf(newValue);
				if (frame < 1) {
					frameNumberInput.setText("1");
				}
				showFrame(Integer.valueOf(newValue));
				// parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET,
				// newValue);
			}
		});
		frameNumberInput.setText("0");
		graphControlPanel.add(frameNumberLabel);
		graphControlPanel.add(frameNumberInput);

		toneMapViewComboBox = new JComboBox<>();

		Arrays.asList(new String[] { Cell.CellTypes.AUDIO_CQ.name(), Cell.CellTypes.AUDIO_CQ_ORIGIN.name(),
				Cell.CellTypes.AUDIO_TUNER_PEAKS.name(), Cell.CellTypes.AUDIO_SPECTRAL_PEAKS.name(),
				Cell.CellTypes.AUDIO_PITCH.name(), Cell.CellTypes.AUDIO_YIN.name(), Cell.CellTypes.AUDIO_NOTATE.name(),
				Cell.CellTypes.AUDIO_INTEGRATE.name(), Cell.CellTypes.AUDIO_ONSET.name(),
				Cell.CellTypes.AUDIO_PERCUSSION.name(), Cell.CellTypes.AUDIO_ONSET.name() + "_SMOOTHED",
				Cell.CellTypes.AUDIO_HPS.name(), Cell.CellTypes.AUDIO_HPS.name() + "_HARMONIC_MASK",
				Cell.CellTypes.AUDIO_HPS.name() + "_PERCUSSION_MASK", Cell.CellTypes.AUDIO_HPS.name() + "_HARMONIC",
				Cell.CellTypes.AUDIO_HPS.name() + "_PERCUSSION" }).stream()
				.forEach(entry -> toneMapViewComboBox.addItem(entry));

		toneMapViewComboBox.setEnabled(false);
		toneMapViewComboBox.setSelectedItem(Cell.CellTypes.AUDIO_CQ.name());
		toneMapViewComboBox.setEnabled(true);
		currentToneMapViewType = Cell.CellTypes.AUDIO_CQ.name();

		toneMapViewComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String value = String.valueOf(toneMapViewComboBox.getSelectedItem());
				if (value != null) {
					currentToneMapViewType = value;
					Visor.this.resetToneMapView();
				}
			}
		});

		graphControlPanel.add(new JLabel("ToneMap View:  "));
		graphControlPanel.add(toneMapViewComboBox);

		JLabel timeAxisOffsetLabel = new JLabel("Time Axis Offset ms: ");
		timeAxisOffsetInput = new JTextField(4);
		timeAxisOffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
				Visor.this.resetToneMapView();
			}
		});

		timeAxisOffsetInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
				Visor.this.resetToneMapView();
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});

		timeAxisOffsetInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET));
		graphControlPanel.add(timeAxisOffsetLabel);
		graphControlPanel.add(timeAxisOffsetInput);

		JLabel pitchAxisOffsetLabel = new JLabel("Pitch Axis Offset ms: ");
		pitchAxisOffsetInput = new JTextField(4);
		pitchAxisOffsetInput.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();

			}
		});

		pitchAxisOffsetInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});

		pitchAxisOffsetInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET));
		graphControlPanel.add(pitchAxisOffsetLabel);
		graphControlPanel.add(pitchAxisOffsetInput);

		JLabel timeAxisRangeLabel = new JLabel("Time Axis Range ms: ");
		timeAxisRangeInput = new JTextField(4);
		timeAxisRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();

			}
		});
		timeAxisRangeInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});
		timeAxisRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE));
		graphControlPanel.add(timeAxisRangeLabel);
		graphControlPanel.add(timeAxisRangeInput);

		JLabel pitchAxisRangeLabel = new JLabel("Pitch Axis Range ms: ");
		pitchAxisRangeInput = new JTextField(4);
		pitchAxisRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}
		});
		pitchAxisRangeInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});
		pitchAxisRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE));
		graphControlPanel.add(pitchAxisRangeLabel);
		graphControlPanel.add(pitchAxisRangeInput);

		showPeaksSwitchCB = new JCheckBox("showPeaksSwitchCB");
		showPeaksSwitchCB.setText("Peaks");
		showPeaksSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_PEAKS,
						Boolean.toString(newValue));
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}
		});
		showPeaksSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_PEAKS));
		graphControlPanel.add(showPeaksSwitchCB);

		JLabel toneMapViewLowThresholdLabel = new JLabel("View Low Threshold: ");
		toneMapViewLowThresholdInput = new JTextField(4);
		toneMapViewLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}
		});
		toneMapViewLowThresholdInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});
		toneMapViewLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD));
		graphControlPanel.add(toneMapViewLowThresholdLabel);
		graphControlPanel.add(toneMapViewLowThresholdInput);

		JLabel toneMapViewHighThresholdLabel = new JLabel("View High Threshold: ");
		toneMapViewHighThresholdInput = new JTextField(4);
		toneMapViewHighThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}
		});
		toneMapViewHighThresholdInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.percussionView.updateAxis();
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});
		toneMapViewHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
		graphControlPanel.add(toneMapViewHighThresholdLabel);
		graphControlPanel.add(toneMapViewHighThresholdInput);

		panel.add(graphControlPanel, BorderLayout.CENTER);

		return panel;
	}

	private JPanel buildControlPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JPanel voicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		final JFileChooser fileChooser = new JFileChooser(new File(defaultAudioFileFolder));
		chooseFileButton = new JButton("Open");
		startFileProcessingButton = new JButton("Start");
		startListeningButton = new JButton("Listen");
		stopListeningButton = new JButton("Stop");

		fileChooser.setSelectedFile(new File(defaultAudioFileFolder, defaultAudioFile));
		chooseFileButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(Visor.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					inputFile = fileChooser.getSelectedFile();
					System.out.println(inputFile.toString());
					fileName = inputFile.getAbsolutePath();
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
					frameNumberInput.setEnabled(false);
					timeAxisOffsetInput.setText("0");
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
					Visor.this.toneMapView.updateAxis();
					Visor.this.chromaPreView.updateAxis();
					Visor.this.chromaPostView.updateAxis();
					Visor.this.beatsView.updateAxis();
					Visor.this.percussionView.updateAxis();
					Visor.this.resetToneMapView();
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

		startFileProcessingButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					inputFile = fileChooser.getSelectedFile();
					fileName = inputFile.getAbsolutePath();
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
					frameNumberInput.setEnabled(false);
					timeAxisOffsetInput.setText("0");
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
					Visor.this.toneMapView.updateAxis();
					Visor.this.chromaPreView.updateAxis();
					Visor.this.chromaPostView.updateAxis();
					Visor.this.beatsView.updateAxis();
					Visor.this.percussionView.updateAxis();
					Visor.this.resetToneMapView();
					Instrument.getInstance().getCoordinator().getHearing().startAudioFileStream(fileName);
				} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		startListeningButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					// String fileName = "D:/audio/record/instrument_recording_" +
					// getCurrentLocalDateTimeStamp();
					String fileName = defaultAudioRecordFileFolder + "/" + "instrument_recording_"
							+ System.currentTimeMillis() + ".wav";
					// File recordFile = new File(defaultAudioRecordFileFolder, fileName);
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
					frameNumberInput.setEnabled(false);
					timeAxisOffsetInput.setText("0");
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
					Visor.this.toneMapView.updateAxis();
					Visor.this.chromaPreView.updateAxis();
					Visor.this.chromaPostView.updateAxis();
					Visor.this.beatsView.updateAxis();
					Visor.this.percussionView.updateAxis();
					Visor.this.resetToneMapView();
					Instrument.getInstance().getCoordinator().getHearing().startAudioLineStream(fileName);

				} catch (LineUnavailableException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		stopListeningButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Instrument.getInstance().getCoordinator().getHearing().stopAudioStream();
				startFileProcessingButton.setEnabled(true);
				startListeningButton.setEnabled(true);
				stopListeningButton.setEnabled(false);
				chooseFileButton.setEnabled(true);
				frameNumberInput.setEnabled(true);
			}
		});

		actionPanel.add(startFileProcessingButton);
		actionPanel.add(startListeningButton);
		actionPanel.add(stopListeningButton);

		recordSwitchCB = new JCheckBox("recordSwitchCB");
		recordSwitchCB.setText("Record");
		recordSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_SWITCH,
						Boolean.toString(newValue));
			}
		});

		recordSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_SWITCH));
		actionPanel.add(recordSwitchCB);

		JComboBox<Integer> fftSizeComboBox = new JComboBox<>(fftSizes);
		fftSizeComboBox.addActionListener(new ActionListener() {
			private Integer fftsize;

			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Integer value = (Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem();
				fftsize = value;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW,
						Integer.toString(fftsize));
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
				Integer sampleRate = value;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE,
						Integer.toString(sampleRate));
			}
		});
		inputSampleRateCombobox.setSelectedIndex(2);
		actionPanel.add(new JLabel("Sample rate:  "));
		actionPanel.add(inputSampleRateCombobox);

		JLabel audioFeatureIntervalLabel = new JLabel("Interval ms: ");
		audioFeatureIntervalInput = new JTextField(4);
		audioFeatureIntervalInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL,
						newValue);
			}
		});
		audioFeatureIntervalInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL,
						newValue);
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});
		audioFeatureIntervalInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL));
		actionPanel.add(audioFeatureIntervalLabel);
		actionPanel.add(audioFeatureIntervalInput);

		JLabel audioOffsetLabel = new JLabel("Offset ms: ");
		audioOffsetInput = new JTextField(4);
		audioOffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET, newValue);
			}
		});
		audioOffsetInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET, newValue);
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});
		audioOffsetInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET));
		actionPanel.add(audioOffsetLabel);
		actionPanel.add(audioOffsetInput);

		JLabel audioRangeLabel = new JLabel("Range ms: ");
		audioRangeInput = new JTextField(4);
		audioRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE, newValue);
			}
		});
		audioRangeInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE, newValue);
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});
		audioRangeInput.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE));
		actionPanel.add(audioRangeLabel);
		actionPanel.add(audioRangeInput);

		final JButton parametersButton = new JButton("Parameters");

		parametersButton.addActionListener(new ActionListener() {

			private boolean parameterDialogOpen;

			@Override
			public void actionPerformed(ActionEvent e) {
				String s = e.getActionCommand();
				if (s.equals("Parameters")) {

					if (!parameterDialogOpen) {
						// create a dialog Box
						JDialog d = new JDialog(mainframe, "Parameters");

						d.addWindowListener(new WindowAdapter() {
							public void windowClosed(WindowEvent e) {
								parameterDialogOpen = false;
							}

							public void windowClosing(WindowEvent e) {
								parameterDialogOpen = false;
							}
						});

						JPanel dialogPanel = new JPanel(new BorderLayout());

						JPanel parameterPanel = new ParametersPanel(Visor.this.parameterManager, Visor.this.iss);
						dialogPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(20, 20, 20, 20),
								new EtchedBorder()));

						dialogPanel.add(new JScrollPane(parameterPanel), BorderLayout.CENTER);

						d.add(dialogPanel);

						Toolkit myScreen = Toolkit.getDefaultToolkit();
						Dimension screenSize = myScreen.getScreenSize();
						int screenHeight = screenSize.height;
						int screenWidth = screenSize.width;

						// setsize of dialog
						d.setSize((int) ((double) screenWidth * 0.7), (int) ((double) screenHeight * 0.7));

						// set visibility of dialog
						d.setVisible(true);

						parameterDialogOpen = true;

					}
				}
			}
		});

		JLabel spacer2Label = new JLabel("  ");
		actionPanel.add(spacer2Label);

		actionPanel.add(parametersButton);

		panel.add(actionPanel, BorderLayout.CENTER);

		JLabel playerTitleLabel = new JLabel(" Play ");
		voicePanel.add(playerTitleLabel);

		playMidiSwitchCB = new JCheckBox("playMidiSwitchCB");
		playMidiSwitchCB.setText("MIDI");
		playMidiSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY,
						Boolean.toString(newValue));
			}
		});

		playMidiSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY));
		voicePanel.add(playMidiSwitchCB);

		playAudioSwitchCB = new JCheckBox("playAudioSwitchCB");
		playAudioSwitchCB.setText("Audio");
		playAudioSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_AUDIO_PLAY,
						Boolean.toString(newValue));
			}
		});

		playAudioSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_AUDIO_PLAY));
		voicePanel.add(playAudioSwitchCB);

		playResynthSwitchCB = new JCheckBox("playResynthSwitchCB");
		playResynthSwitchCB.setText("Resynth");
		playResynthSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_RESYNTH_PLAY,
						Boolean.toString(newValue));
			}
		});

		playResynthSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_RESYNTH_PLAY));
		voicePanel.add(playResynthSwitchCB);

		playPeaksSwitchCB = new JCheckBox("playPeaksSwitchCB");
		playPeaksSwitchCB.setText("PEAKS");
		playPeaksSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS,
						Boolean.toString(newValue));
			}
		});

		playPeaksSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS));
		voicePanel.add(playPeaksSwitchCB);

		JLabel spacer1Label = new JLabel("  ");
		voicePanel.add(spacer1Label);

		JLabel voicePlayerDelayLabel = new JLabel("Delay: ");
		voicePlayerDelayInput = new JTextField(4);
		voicePlayerDelayInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = voicePlayerDelayInput.getText();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY, newValue);

			}
		});
		voicePlayerDelayInput.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		voicePanel.add(voicePlayerDelayLabel);
		voicePanel.add(voicePlayerDelayInput);

		JLabel voicePlayerLowThresholdLabel = new JLabel("Low Threshold: ");
		voicePlayerLowThresholdInput = new JTextField(4);
		voicePlayerLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = voicePlayerLowThresholdInput.getText();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD, newValue);

			}
		});
		voicePlayerLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD));
		voicePanel.add(voicePlayerLowThresholdLabel);
		voicePanel.add(voicePlayerLowThresholdInput);

		JLabel voicePlayerHighThresholdLabel = new JLabel("High Threshold: ");
		voicePlayerHighThresholdInput = new JTextField(4);
		voicePlayerHighThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = voicePlayerHighThresholdInput.getText();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD, newValue);

			}
		});
		voicePlayerHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD));
		voicePanel.add(voicePlayerHighThresholdLabel);
		voicePanel.add(voicePlayerHighThresholdInput);

		midiPlayVoice1SwitchCB = new JCheckBox("midiPlayVoice1SwitchCB");
		midiPlayVoice1SwitchCB.setText("Voice1");
		midiPlayVoice1SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE1_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayVoice1SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE1_SWITCH));
		voicePanel.add(midiPlayVoice1SwitchCB);

		midiPlayVoice2SwitchCB = new JCheckBox("midiPlayVoice2SwitchCB");
		midiPlayVoice2SwitchCB.setText("Voice2");
		midiPlayVoice2SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE2_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayVoice2SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE2_SWITCH));
		voicePanel.add(midiPlayVoice2SwitchCB);

		midiPlayVoice3SwitchCB = new JCheckBox("midiPlayVoice3SwitchCB");
		midiPlayVoice3SwitchCB.setText("Voice3");
		midiPlayVoice3SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE3_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayVoice3SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE3_SWITCH));
		voicePanel.add(midiPlayVoice3SwitchCB);

		midiPlayVoice4SwitchCB = new JCheckBox("midiPlayVoice4SwitchCB");
		midiPlayVoice4SwitchCB.setText("Voice4");
		midiPlayVoice4SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE4_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayVoice4SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE4_SWITCH));
		voicePanel.add(midiPlayVoice4SwitchCB);

		midiPlayChord1SwitchCB = new JCheckBox("midiPlayChord1SwitchCB");
		midiPlayChord1SwitchCB.setText("Chord1");
		midiPlayChord1SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD1_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayChord1SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD1_SWITCH));
		voicePanel.add(midiPlayChord1SwitchCB);

		midiPlayChord2SwitchCB = new JCheckBox("midiPlayChord2SwitchCB");
		midiPlayChord2SwitchCB.setText("Chord2");
		midiPlayChord2SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD2_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayChord2SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD2_SWITCH));
		voicePanel.add(midiPlayChord2SwitchCB);

		midiPlayPad1SwitchCB = new JCheckBox("midiPlayPad1SwitchCB");
		midiPlayPad1SwitchCB.setText("Pad1");
		midiPlayPad1SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD1_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayPad1SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD1_SWITCH));
		voicePanel.add(midiPlayPad1SwitchCB);

		midiPlayPad2SwitchCB = new JCheckBox("midiPlayPad2SwitchCB");
		midiPlayPad2SwitchCB.setText("Pad2");
		midiPlayPad2SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD2_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayPad2SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD2_SWITCH));
		voicePanel.add(midiPlayPad2SwitchCB);

		midiPlayBeat1SwitchCB = new JCheckBox("midiPlayBeat1SwitchCB");
		midiPlayBeat1SwitchCB.setText("Beat1");
		midiPlayBeat1SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT1_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayBeat1SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT1_SWITCH));
		voicePanel.add(midiPlayBeat1SwitchCB);

		midiPlayBeat2SwitchCB = new JCheckBox("midiPlayBeat2SwitchCB");
		midiPlayBeat2SwitchCB.setText("Beat2");
		midiPlayBeat2SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT2_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayBeat2SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT2_SWITCH));
		voicePanel.add(midiPlayBeat2SwitchCB);

		midiPlayBeat3SwitchCB = new JCheckBox("midiPlayBeat3SwitchCB");
		midiPlayBeat3SwitchCB.setText("Beat3");
		midiPlayBeat3SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT3_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayBeat3SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT3_SWITCH));
		voicePanel.add(midiPlayBeat3SwitchCB);

		midiPlayBeat4SwitchCB = new JCheckBox("midiPlayBeat4SwitchCB");
		midiPlayBeat4SwitchCB.setText("Beat4");
		midiPlayBeat4SwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT4_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayBeat4SwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT4_SWITCH));
		voicePanel.add(midiPlayBeat4SwitchCB);

		midiPlayBaseSwitchCB = new JCheckBox("midiPlayBaseSwitchCB");
		midiPlayBaseSwitchCB.setText("Base");
		midiPlayBaseSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BASE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayBaseSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BASE_SWITCH));
		voicePanel.add(midiPlayBaseSwitchCB);

		panel.add(voicePanel, BorderLayout.SOUTH);

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
		toneMapViews.clear();
		cqLayer.clear();
		pdLayer.clear();
		this.cqPanel.repaint();
		this.pitchDetectPanel.repaint();
		this.frameNumberInput.setText("0");
	}

	public void showFrame(int frame) {
		if (frame > 0) {
			if (toneMapViews.containsKey(currentToneMapViewType)) {
				ToneMap currentToneMap = toneMapViews.get(currentToneMapViewType);
				ToneTimeFrame toneTimeFrame = currentToneMap.getTimeFrame(frame);
				if (toneTimeFrame != null) {
					updateTimeFrameView(toneTimeFrame);
					oscilloscopePanel.paint(toneTimeFrame
							.extractFFTSpectrum(parameterManager
									.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_CQ_WINDOW))
							.getSpectrum());
					oscilloscopePanel.repaint();
					int timeOffset = (int) (toneTimeFrame.getStartTime() * 1000.0);
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET,
							Integer.toString(timeOffset));
					timeAxisOffsetInput.setText(Integer.toString(timeOffset));
					Visor.this.toneMapView.updateAxis();
					Visor.this.chromaPreView.updateAxis();
					Visor.this.chromaPostView.updateAxis();
					Visor.this.beatsView.updateAxis();
					Visor.this.percussionView.updateAxis();
					updateSpectrumView(toneTimeFrame, parameterManager
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SP_WINDOW));
				}
			}
		}
	}

	@Override
	public void handleEvent(float[] data, AudioEvent event) {
		oscilloscopePanel.paint(data);
		oscilloscopePanel.repaint();
	}

	public void updateTimeFrameView(ToneTimeFrame data) {
		timeFramePanel.paint(data);
		timeFramePanel.repaint();
	}

	@SuppressWarnings("unchecked")
	public void updateToneMapView(ToneMap toneMap, String toneMapViewType) {

		List<ToneTimeFrame> timeFrames = new ArrayList<>();
		ToneTimeFrame ttf = toneMap.getTimeFrame();
		double fromTime = (ttf.getStartTime() - 1.0) >= 0 ? ttf.getStartTime() - 1.0 : 0;

		while (ttf != null && ttf.getStartTime() >= fromTime) {
			timeFrames.add(ttf);
			ttf = toneMap.getPreviousTimeFrame(ttf.getStartTime());
		}
		for (ToneTimeFrame ttfv : timeFrames) {
			updateToneMapView(toneMap, ttfv, toneMapViewType);
		}
	}

	@SuppressWarnings("unchecked")
	public void updateToneMapView(ToneMap toneMap, ToneTimeFrame ttf, String toneMapViewType) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!toneMapViews.containsKey(toneMapViewType)) {
					toneMapViews.put(toneMapViewType, toneMap);
					if (toneMapViewType.equals(currentToneMapViewType)) {
						toneMapView.renderToneMap(toneMap);
					}
				} else if (!toneMapViews.get(toneMapViewType).getKey().equals(toneMapView.getToneMap().getKey())) {
					toneMapViews.put(toneMapViewType, toneMap);
					if (toneMapViewType.equals(currentToneMapViewType)) {
						toneMapView.renderToneMap(toneMap);
					}
				} else if (toneMapViewType.equals(currentToneMapViewType)) {
					toneMapView.updateToneMap(ttf);
				}
				updateTimeFrameView(ttf);
			}
		});
	}

	public void resetToneMapView() {
		if (toneMapViews.containsKey(currentToneMapViewType)) {
			toneMapView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			updateTimeFrameView(toneMapViews.get(currentToneMapViewType).getTimeFrame());
			// chromaPreView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			// chromaPostView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			// beatsView.renderToneMap(toneMapViews.get(currentToneMapViewType));
		}
	}

	public void updateBeatsView(ToneMap toneMap) {
		beatsView.updateToneMap(toneMap);
	}

	public void updatePercussionView(ToneMap toneMap) {
		percussionView.updateToneMap(toneMap);
	}

	public void updateChromaPreView(ToneMap toneMap, ToneTimeFrame ttf) {
		chromaPreView.updateToneMap(ttf);
	}

	public void updateChromaPreView(ToneMap toneMap) {
		chromaPreView.updateToneMap(toneMap);
	}

	public void updateChromaPostView(ToneMap toneMap, ToneTimeFrame ttf) {
		chromaPostView.updateToneMap(ttf);
	}

	public void updateChromaPostView(ToneMap toneMap) {
		chromaPostView.updateToneMap(toneMap);
	}

	public void updateSpectrumView(ToneTimeFrame toneTimeFrame, int windowSize) {
		float[] spectrum = toneTimeFrame.extractFFTSpectrum(windowSize).getSpectrum();
		for (int i = 0; i < spectrum.length; i++) {
			spectrum[i] *= 100;
		}
		spectrumLayer.clearPeaks();
		spectrumLayer.setSpectrum(spectrum);
		spectrumPanel.repaint();
	}

	private LinkedPanel createCQPanel() {
		CoordinateSystem constantQCS = getCoordinateSystem(AxisUnit.FREQUENCY);
		constantQCS.setMax(Axis.X, 20000);
		constantQPanel = new LinkedPanel(constantQCS);
		cqLayer = new CQLayer(constantQCS);
		constantQPanel.addLayer(new BackgroundLayer(constantQCS));
		constantQPanel.addLayer(cqLayer);
		constantQPanel.addLayer(new VerticalFrequencyAxisLayer(constantQCS));
		constantQPanel.addLayer(new ZoomMouseListenerLayer());
		constantQPanel.addLayer(new DragMouseListenerLayer(constantQCS));
		constantQPanel.addLayer(new SelectionLayer(constantQCS));
		constantQPanel.addLayer(new TimeAxisLayer(constantQCS));

		LegendLayer legend = new LegendLayer(constantQCS, 110);
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

	private LinkedPanel createPitchDetectPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		pitchDetectPanel = new LinkedPanel(cs);
		pitchDetectPanel.addLayer(new BackgroundLayer(cs));
		pdLayer = new PitchDetectLayer(cs);
		pitchDetectPanel.addLayer(pdLayer);
		pitchDetectPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		pitchDetectPanel.addLayer(new ZoomMouseListenerLayer());
		pitchDetectPanel.addLayer(new DragMouseListenerLayer(cs));
		pitchDetectPanel.addLayer(new SelectionLayer(cs));
		pitchDetectPanel.addLayer(new TimeAxisLayer(cs));

		LegendLayer legend = new LegendLayer(cs, 110);
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

	private LinkedPanel createSpectrumPanel() {
		CoordinateSystem cs = new CoordinateSystem(AxisUnit.FREQUENCY, AxisUnit.AMPLITUDE, 0, 1000, false);
		cs.setMax(Axis.X, 4800);
		cs.setMax(Axis.X, 13200);
		spectrumLayer = new SpectrumLayer(cs,
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SP_WINDOW),
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE),
				Color.black);
		// noiseFloorLayer = new SpectrumLayer(cs,
		// parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SP_WINDOW),
		// parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE),
		// Color.gray);

		spectrumPanel = new LinkedPanel(cs);
		spectrumPanel.addLayer(new ZoomMouseListenerLayer());
		spectrumPanel.addLayer(new DragMouseListenerLayer(cs));
		spectrumPanel.addLayer(new BackgroundLayer(cs));
		spectrumPanel.addLayer(new AmplitudeAxisLayer(cs));

		spectrumPanel.addLayer(new SelectionLayer(cs));
		spectrumPanel.addLayer(new HorizontalFrequencyAxisLayer(cs));
		spectrumPanel.addLayer(spectrumLayer);
		// spectrumPanel.addLayer(noiseFloorLayer);

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

	private JPanel createToneMapLayerPanel() {
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
		cqLayer.update(audioFeatureFrame);
		pdLayer.update(audioFeatureFrame);
		cqPanel.repaint();
		pitchDetectPanel.repaint();
		this.frameNumberInput.setText(Integer.toString(audioFeatureFrame.getFrameSequence()));
	}

	public void audioStopped() {
		startFileProcessingButton.setEnabled(true);
		startListeningButton.setEnabled(true);
		stopListeningButton.setEnabled(false);
		chooseFileButton.setEnabled(true);
		frameNumberInput.setEnabled(true);
	}

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

		public void clear() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					cqFeatures = new TreeMap<>();
				}
			});
		}

		@Override
		public String getName() {
			return "CQ Layer";
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ConstantQSource cqs = audioFeatureFrame.getConstantQFeatures().getSource();
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

	private static class OscilloscopePanel extends JPanel {

		/**
		 *
		 */
		private static final long serialVersionUID = 4969781241442094359L;

		float data[];

		public OscilloscopePanel() {
			setMinimumSize(new Dimension(80, 60));
		}

		public void paint(float[] data) {
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
					if ((i + 3) < data.length) {
						g.drawLine((int) (data[i] * width), (int) (halfHeight - data[i + 1] * height),
								(int) (data[i + 2] * width), (int) (halfHeight - data[i + 3] * height));
					}
				}
			}
		}
	}

	private static class TimeFramePanel extends JPanel {

		private ToneTimeFrame toneTimeFrame;

		public TimeFramePanel() {
			setMinimumSize(new Dimension(80, 60));
		}

		public void paint(ToneTimeFrame toneTimeFrame) {
			this.toneTimeFrame = toneTimeFrame;
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g); // paint background
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.WHITE);
			if (toneTimeFrame != null) {
				float width = getWidth();
				float height = getHeight();
				ToneMapElement[] elements = toneTimeFrame.getElements();
				double xInc = width / elements.length;
				double yInc = height / 1.0;
				int x = 0;
				int y = 0;
				for (int i = 0; i < elements.length; i++) {
					ToneMapElement element = elements[i];
					x = (int) (i * xInc);
					y = (int) height - 1;
					if ((toneTimeFrame.getMaxAmplitude() - toneTimeFrame.getMinAmplitude()) > 0.1) {
						y = (int) (height - (yInc * ((element.amplitude - toneTimeFrame.getMinAmplitude())
								/ (toneTimeFrame.getMaxAmplitude() - toneTimeFrame.getMinAmplitude()))));
					}
					g.drawLine(x, (int) height - 1, x, y);
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

		public void clear() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					features = new TreeMap<>();
				}
			});
		}

		public void update(AudioFeatureFrame audioFeatureFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					TreeMap<Double, SpectrogramInfo> fs;
					PitchDetectorSource pds;
					pds = audioFeatureFrame.getPitchDetectorFeatures().getSource();
					binStartingPointsInCents = pds.getBinStartingPointsInCents();
					binWidth = pds.getBinWidth();
					binHeight = pds.getBinHeight();
					fs = audioFeatureFrame.getPitchDetectorFeatures().getFeatures();
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

	protected void repaintSpectralInfo(AudioFeatureFrame audioFeatureFrame) {
		TreeMap<Double, SpectralInfo> fs;
		SpectralPeaksSource sps = audioFeatureFrame.getSpectralPeaksFeatures().getSource();
		fs = audioFeatureFrame.getSpectralPeaksFeatures().getFeatures();
		spectrumLayer.clearPeaks();
		for (Entry<Double, SpectralInfo> entry : fs.entrySet()) {
			spectrumLayer.setSpectrum(entry.getValue().getMagnitudes());
			spectrumPanel.repaint();
		}
	}

}
