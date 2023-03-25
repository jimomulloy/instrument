package jomu.instrument.desktop.monitor.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
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
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;

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
import jomu.instrument.control.Coordinator;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.monitor.Visor;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

@ApplicationScoped
public class SwingDesktopVisor implements Visor, AudioFeatureFrameObserver {

	private static final Logger LOG = Logger.getLogger(SwingDesktopVisor.class.getName());

	private static String defaultAudioFile = "NOTETRACK49sec.wav";

	LinkedPanel constantQPanel;

	CQLayer cqLayer;

	LinkedPanel cqPanel;

	SpectrumLayer noiseFloorLayer;

	PitchDetectLayer pdLayer;
	LinkedPanel pitchDetectPanel;
	SpectrumLayer spectrumLayer;
	LinkedPanel spectrumPanel;
	ToneMapView toneMapView;
	String currentToneMapViewType;

	Map<String, ToneMap> toneMapViews = new HashMap<>();

	String fileName;

	static final Integer[] fftSizes = { 256, 512, 1024, 2048, 4096, 8192, 16384, 22050, 32768, 65536, 131072 };
	static final Integer[] inputSampleRate = { 8000, 11025, 22050, 44100, 192000 };

	File inputFile;

	JPanel diagnosticsPanel;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Storage storage;

	ChromaView chromaPreView;

	ChromaView chromaPostView;

	BeatsView beatsView;

	BeatsView percussionView;

	JFrame mainframe;

	JPanel beatsPanel;

	@Inject
	InstrumentStoreService iss;

	JTextField audioFeatureIntervalInput;
	JTextField timeAxisOffsetInput;
	JTextField pitchAxisOffsetInput;
	JTextField timeAxisRangeInput;
	JTextField pitchAxisRangeInput;
	JComboBox toneMapViewComboBox;
	JCheckBox playMidiSwitchCB;
	JCheckBox playAudioSwitchCB;
	JTextField voicePlayerLowThresholdInput;
	JTextField voicePlayerHighThresholdInput;
	JTextField voicePlayerDelayInput;
	JTextField frameNumberInput;
	JTextField toneMapViewLowThresholdInput;
	JTextField toneMapViewHighThresholdInput;
	JTextField audioOffsetInput;
	JTextField audioRangeInput;
	JCheckBox recordSwitchCB;

	@Inject
	Workspace workspace;

	JCheckBox midiPlayBaseSwitchCB;
	JCheckBox playResynthSwitchCB;
	JCheckBox midiPlayVoice1SwitchCB;
	AbstractButton midiPlayVoice2SwitchCB;
	AbstractButton midiPlayVoice3SwitchCB;
	AbstractButton midiPlayVoice4SwitchCB;
	AbstractButton midiPlayChord1SwitchCB;
	AbstractButton midiPlayChord2SwitchCB;
	AbstractButton midiPlayPad1SwitchCB;
	JCheckBox midiPlayPad2SwitchCB;
	JCheckBox midiPlayBeat1SwitchCB;
	JCheckBox midiPlayBeat2SwitchCB;
	JCheckBox midiPlayBeat3SwitchCB;
	JCheckBox midiPlayBeat4SwitchCB;
	TimeFramePanel timeFramePanel;
	JButton chooseFileButton;
	JButton startFileProcessingButton;
	JButton startListeningButton;
	JButton stopListeningButton;
	JCheckBox playPeaksSwitchCB;
	AbstractButton showPeaksSwitchCB;

	JCheckBox showTrackingSwitchCB;

	@Inject
	Console console;

	@Inject
	Coordinator coordinator;

	JCheckBox showLogSwitchCB;

	JCheckBox trackWriteSwitchCB;

	JPanel visorPanel;

	JFrame mainFrame;

	Container upperPane;

	JPanel contentPane;

	JLabel statusLabel;

	JCheckBox showSynthesisSwitchCB;

	JTextField hearingMinFreqCentsInput;

	JTextField hearingMaxFreqCentsInput;

	JCheckBox updateThresholdSwitchCB;

	JCheckBox showColourSwitchCB;

	JCheckBox silentWriteSwitchCB;

	@Override
	public void startUp() {
		LOG.warning(">>Using SwingDesktopVisor");
		EventQueue.invokeLater(() -> {
			buildMainFrame();
		});
	}

	public void updateStatusMessage(String message) {
		statusLabel.setText(message);
	}

	protected void buildMainFrame() {
		FlatMaterialDesignDarkIJTheme.setup();
		FlatLaf.setUseNativeWindowDecorations(true);
		mainFrame = new JFrame();
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.setTitle("The Instrument");

		buildMenus();

		buildContent();

		initialise(mainFrame);

		upperPane.add(getContentPanel(), BorderLayout.CENTER);

		final int inset = 10;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height - inset * 2);
		mainFrame.pack();
		mainFrame.setVisible(true);
		mainFrame.setExtendedState(mainFrame.getExtendedState() | Frame.MAXIMIZED_BOTH);
	}

	private void buildContent() {

		contentPane = new JPanel();

		upperPane = new JPanel();
		JPanel statusPane = new JPanel();
		JPanel lowerPane = new JPanel();
		statusLabel = new JLabel("Ready");

		contentPane.setLayout(new BorderLayout());

		final int inset1 = 45;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		contentPane.setPreferredSize(new Dimension(screenSize.width - inset1 * 2, screenSize.height - inset1 * 2));

		EmptyBorder eb = new EmptyBorder(5, 5, 5, 5);
		BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
		CompoundBorder cb = new CompoundBorder(eb, bb);
		contentPane.setBorder(cb);

		statusPane.add(statusLabel, BorderLayout.CENTER);
		EmptyBorder eb1 = new EmptyBorder(2, 2, 2, 2);
		BevelBorder bb1 = new BevelBorder(BevelBorder.LOWERED);
		CompoundBorder cb1 = new CompoundBorder(eb1, bb1);
		statusPane.setBorder(cb1);

		upperPane.setLayout(new BorderLayout());

		lowerPane.setLayout(new BorderLayout());
		lowerPane.add(statusPane, BorderLayout.CENTER);

		contentPane.add(upperPane, BorderLayout.CENTER);
		contentPane.add(lowerPane, BorderLayout.SOUTH);
		contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		mainFrame.setContentPane(contentPane);
	}

	protected void buildMenus() {

		JMenuBar menuBar = new JMenuBar();
		menuBar.setOpaque(true);
		JMenu file = buildFileMenu();
		JMenu help = buildHelpMenu();

		menuBar.add(file);
		menuBar.add(help);
		mainFrame.setJMenuBar(menuBar);
	}

	protected JMenu buildFileMenu() {

		JMenu file = new JMenu("File");
		JMenuItem open = new JMenuItem("Open");
		JMenuItem save = new JMenuItem("Save");
		JMenuItem quit = new JMenuItem("Quit");

		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// openToneMap();
			}
		});

		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// saveToneMap();
			}
		});

		quit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// stop();
			}
		});

		file.add(open);
		file.add(save);
		file.addSeparator();
		file.add(quit);
		return file;
	}

	protected JMenu buildHelpMenu() {

		JMenu help = new JMenu("Help");
		JMenuItem about = new JMenuItem("About ToneMap...");
		JMenuItem openHelp = new JMenuItem("Open Help Window");

		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// showAboutBox();
			}
		});

		openHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// openHelpWindow();
			}
		});

		help.add(about);
		help.add(openHelp);

		return help;
	}

	public void initialise(JFrame mainframe) {
		visorPanel = new JPanel();
		this.mainframe = mainframe;
		this.visorPanel.setLayout(new BorderLayout());
		JPanel topPanel = buildTopPanel();
		JPanel bottomPanel = buildBottomPanel();
		JScrollPane graphPanel = buildGraphPanel();
		this.visorPanel.add(topPanel, BorderLayout.NORTH);
		this.visorPanel.add(graphPanel, BorderLayout.CENTER);
		this.visorPanel.add(bottomPanel, BorderLayout.SOUTH);
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
				Cell.CellTypes.AUDIO_PITCH.name(), Cell.CellTypes.AUDIO_YIN.name(), Cell.CellTypes.AUDIO_SACF.name(),
				Cell.CellTypes.AUDIO_MFCC.name(), Cell.CellTypes.AUDIO_CEPSTRUM.name(),
				Cell.CellTypes.AUDIO_SYNTHESIS.name(), Cell.CellTypes.AUDIO_NOTATE.name(),
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
					resetToneMapView();
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
				if (parseIntegerTextField((JTextField) e.getSource(), 0, 60000,
						InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET)) {
					refreshMapViews();
					resetToneMapView();
				}
			}
		});

		timeAxisOffsetInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (parseIntegerTextField((JTextField) e.getSource(), 0, 60000,
						InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET)) {
					refreshMapViews();
					resetToneMapView();
				}
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
				refreshMapViews();
			}
		});

		pitchAxisOffsetInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET, newValue);
				refreshMapViews();
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
				refreshMapViews();

			}
		});
		timeAxisRangeInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE, newValue);
				refreshMapViews();
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
				refreshMapViews();
			}
		});
		pitchAxisRangeInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE, newValue);
				refreshMapViews();
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

		showColourSwitchCB = new JCheckBox("showColourSwitchCB");
		showColourSwitchCB.setText("Colour");
		showColourSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_COLOUR,
						Boolean.toString(newValue));
				refreshMapViews();
			}
		});
		showColourSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_COLOUR));
		graphControlPanel.add(showColourSwitchCB);

		showPeaksSwitchCB = new JCheckBox("showPeaksSwitchCB");
		showPeaksSwitchCB.setText("Peaks");
		showPeaksSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_PEAKS,
						Boolean.toString(newValue));
				refreshMapViews();
			}
		});
		showPeaksSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_PEAKS));
		graphControlPanel.add(showPeaksSwitchCB);

		showTrackingSwitchCB = new JCheckBox("showTrackingSwitchCB");
		showTrackingSwitchCB.setText("Tracks");
		showTrackingSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_TRACKING,
						Boolean.toString(newValue));
				refreshMapViews();
			}
		});
		showTrackingSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_TRACKING));
		graphControlPanel.add(showTrackingSwitchCB);

		showSynthesisSwitchCB = new JCheckBox("showSynthesisSwitchCB");
		showSynthesisSwitchCB.setText("Synth");
		showSynthesisSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_SYNTHESIS,
						Boolean.toString(newValue));
				refreshMapViews();
			}
		});
		showSynthesisSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_SYNTHESIS));
		graphControlPanel.add(showSynthesisSwitchCB);

		showLogSwitchCB = new JCheckBox("showLogSwitchCB");
		showLogSwitchCB.setText("Logn");
		showLogSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_LOG,
						Boolean.toString(newValue));
				refreshMapViews();
			}
		});
		showLogSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_LOG));
		graphControlPanel.add(showLogSwitchCB);

		final JButton parametersButton = new JButton("Update");
		parametersButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateViewThresholds();
				toneMapViewHighThresholdInput.setText(
						parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
			}
		});
		graphControlPanel.add(parametersButton);

		JLabel toneMapViewLowThresholdLabel = new JLabel("Low: ");
		toneMapViewLowThresholdInput = new JTextField(4);
		toneMapViewLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD, newValue);
				refreshMapViews();
			}
		});
		toneMapViewLowThresholdInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD, newValue);
				refreshMapViews();
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

		JLabel toneMapViewHighThresholdLabel = new JLabel("High: ");
		toneMapViewHighThresholdInput = new JTextField(4);
		toneMapViewHighThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD, newValue);
				refreshMapViews();
			}
		});
		toneMapViewHighThresholdInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD, newValue);
				refreshMapViews();
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

	protected void updateViewThresholds() {
		if (toneMapViews.containsKey(currentToneMapViewType)) {
			double maxAmplitude = toneMapView.getMaxAmplitude();

			Formatter formatter = new Formatter();
			formatter.format("%.2f", maxAmplitude);
			parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD,
					formatter.toString());
			formatter.close();
		}
		refreshMapViews();
	}

	protected boolean parseIntegerTextField(JTextField textField, int min, int max, String parameterName) {
		try {
			int value = Integer.parseInt(textField.getText());
			if (value >= min && value <= max) {
				parameterManager.setParameter(parameterName, Integer.toString(value));
				return true;
			} else {
				textField.setText(parameterManager.getParameter(parameterName));
			}
		} catch (Exception ex) {
			textField.setText(parameterManager.getParameter(parameterName));
		}
		return false;
	}

	private JPanel buildControlPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JPanel voicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		final JFileChooser fileChooser = new JFileChooser(new File(getAudioFileFolder()));
		chooseFileButton = new JButton("Open");
		startFileProcessingButton = new JButton("Start");
		startListeningButton = new JButton("Listen");
		stopListeningButton = new JButton("Stop");

		fileChooser.setSelectedFile(new File(getAudioFileFolder(), defaultAudioFile));
		chooseFileButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(visorPanel);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					inputFile = fileChooser.getSelectedFile();
					fileName = inputFile.getAbsolutePath();
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
					frameNumberInput.setEnabled(false);
					timeAxisOffsetInput.setText("0");
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
					LOG.severe(">>!! chosen file");
					toneMapViews.remove(currentToneMapViewType);
					refreshMapViews();
					resetToneMapView();
					try {
						Instrument.getInstance().getCoordinator().getHearing().startAudioFileStream(fileName);
						updateStatusMessage("Choosen file: " + inputFile);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Error choosing file :" + inputFile, e);
						Instrument.getInstance().getCoordinator().getHearing().stopAudioStream();
						startFileProcessingButton.setEnabled(true);
						startListeningButton.setEnabled(true);
						// stopListeningButton.setEnabled(false);
						chooseFileButton.setEnabled(true);
						frameNumberInput.setEnabled(true);
						updateStatusMessage("Error choosing file :" + inputFile + ", " + e.getMessage());
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
					toneMapViews.remove(currentToneMapViewType);
					refreshMapViews();
					resetToneMapView();
					Instrument.getInstance().getCoordinator().getHearing().startAudioFileStream(fileName);
					updateStatusMessage("Started file: " + inputFile);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Error starting file :" + inputFile, e);
					Instrument.getInstance().getCoordinator().getHearing().stopAudioStream();
					startFileProcessingButton.setEnabled(true);
					startListeningButton.setEnabled(true);
					// stopListeningButton.setEnabled(false);
					chooseFileButton.setEnabled(true);
					frameNumberInput.setEnabled(true);
					updateStatusMessage("Error starting file :" + inputFile + ", " + e.getMessage());
				}
			}
		});

		startListeningButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					String fileName = getAudioRecordFileFolder() + "/instrument_recording_" + System.currentTimeMillis()
							+ ".wav";
					LOG.finer(">>Recording Audio to fileName" + fileName);
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
					frameNumberInput.setEnabled(false);
					timeAxisOffsetInput.setText("0");
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
					toneMapViews.remove(currentToneMapViewType);
					refreshMapViews();
					resetToneMapView();
					Instrument.getInstance().getCoordinator().getHearing().startAudioLineStream(fileName);
					updateStatusMessage("Started listener: " + fileName);

				} catch (LineUnavailableException | IOException e) {
					LOG.log(Level.SEVERE, "Error starting listener", e);
					Instrument.getInstance().getCoordinator().getHearing().stopAudioStream();
					startFileProcessingButton.setEnabled(true);
					startListeningButton.setEnabled(true);
					// stopListeningButton.setEnabled(false);
					chooseFileButton.setEnabled(true);
					frameNumberInput.setEnabled(true);
					updateStatusMessage("Error starting listener :" + e.getMessage());
				}
			}
		});

		stopListeningButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Instrument.getInstance().getCoordinator().getHearing().stopAudioStream();
				startFileProcessingButton.setEnabled(true);
				startListeningButton.setEnabled(true);
				// stopListeningButton.setEnabled(false);
				chooseFileButton.setEnabled(true);
				frameNumberInput.setEnabled(true);
				updateStatusMessage("Stopped");
				coordinator.getVoice().clear();
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
		inputSampleRateCombobox.setSelectedIndex(3);
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

		JLabel hearingMinFreqCentsLabel = new JLabel("Min Cents: ");
		hearingMinFreqCentsInput = new JTextField(4);
		hearingMinFreqCentsInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = hearingMinFreqCentsInput.getText();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS,
						newValue);

			}
		});
		hearingMinFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS));
		actionPanel.add(hearingMinFreqCentsLabel);
		actionPanel.add(hearingMinFreqCentsInput);

		JLabel hearingMaxFreqCentsLabel = new JLabel("Max Cents: ");
		hearingMaxFreqCentsInput = new JTextField(4);
		hearingMaxFreqCentsInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = hearingMaxFreqCentsInput.getText();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS,
						newValue);

			}
		});
		hearingMaxFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS));
		actionPanel.add(hearingMaxFreqCentsLabel);
		actionPanel.add(hearingMaxFreqCentsInput);

		actionPanel.add(new JLabel("  "));

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

						JPanel parameterPanel = new ParametersPanel();
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

		actionPanel.add(new JLabel("  "));

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
		voicePlayerDelayInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY, newValue);
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
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
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD, newValue);

			}
		});

		voicePlayerLowThresholdInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD, newValue);
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
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
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD, newValue);
			}
		});
		voicePlayerHighThresholdInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD, newValue);
			}

			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
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

		voicePanel.add(new JLabel("  "));

		silentWriteSwitchCB = new JCheckBox("silentWriteCB");
		silentWriteSwitchCB.setText("Silent Write");
		silentWriteSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE,
						Boolean.toString(newValue));
			}
		});

		silentWriteSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE));
		voicePanel.add(silentWriteSwitchCB);

		trackWriteSwitchCB = new JCheckBox("trackWriteCB");
		trackWriteSwitchCB.setText("Track Write");
		trackWriteSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH,
						Boolean.toString(newValue));
			}
		});

		trackWriteSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH));
		voicePanel.add(trackWriteSwitchCB);

		panel.add(voicePanel, BorderLayout.SOUTH);

		return panel;
	}

	private String getAudioFileFolder() {
		String baseDir = storage.getObjectStorage().getBasePath();
		return Paths
				.get(baseDir,
						parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DIRECTORY))
				.toString();
	}

	private String getAudioRecordFileFolder() {
		String baseDir = storage.getObjectStorage().getBasePath();
		return Paths
				.get(baseDir,
						parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DIRECTORY),
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
				.toString();
	}

	@Override
	public void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame) {
		updateView(audioFeatureFrame);
	}

	@Override
	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		updateView(audioFeatureFrame);
	}

	@Override
	public void clearView() {
		toneMapViews.clear();
		cqLayer.clear();
		pdLayer.clear();
		this.cqPanel.repaint();
		this.pitchDetectPanel.repaint();
		this.frameNumberInput.setText("0");
	}

	@Override
	public void showFrame(int frame) {
		if (frame > 0) {
			if (toneMapViews.containsKey(currentToneMapViewType)) {
				ToneMap currentToneMap = toneMapViews.get(currentToneMapViewType);
				ToneTimeFrame toneTimeFrame = currentToneMap.getTimeFrame(frame);
				if (toneTimeFrame != null) {
					updateTimeFrameView(toneTimeFrame);
					int timeOffset = (int) (toneTimeFrame.getStartTime() * 1000.0);
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET,
							Integer.toString(timeOffset));
					timeAxisOffsetInput.setText(Integer.toString(timeOffset));
					refreshMapViews();
					updateSpectrumView(toneTimeFrame, parameterManager
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SP_WINDOW));
				}
			}
		}
	}

	@Override
	public void updateTimeFrameView(ToneTimeFrame data) {
		timeFramePanel.paint(data);
		timeFramePanel.repaint();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void updateToneMapView(ToneMap toneMap, String toneMapViewType) {

		List<ToneTimeFrame> timeFrames = new ArrayList<>();
		ToneTimeFrame ttf = toneMap.getTimeFrame();
		if (ttf != null) {
			double fromTime = (ttf.getStartTime() - 1.0) >= 0 ? ttf.getStartTime() - 1.0 : 0;

			while (ttf != null && ttf.getStartTime() >= fromTime) {
				timeFrames.add(ttf);
				ttf = toneMap.getPreviousTimeFrame(ttf.getStartTime());
			}
			for (ToneTimeFrame ttfv : timeFrames) {
				updateToneMapView(toneMap, ttfv, toneMapViewType);
			}
		}
	}

	@Override
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
				} else if (toneMapView.getToneMap() != null && toneMapViews.get(toneMapViewType) != null
						&& !toneMapViews.get(toneMapViewType).getKey().equals(toneMapView.getToneMap().getKey())) {
					toneMapViews.put(toneMapViewType, toneMap);
					if (toneMapViewType.equals(currentToneMapViewType)) {
						toneMapView.renderToneMap(toneMap);
					}
				} else if (toneMapViewType.equals(currentToneMapViewType) && toneMapView != null) {
					toneMapView.updateToneMap(toneMap, ttf);
				}
				updateTimeFrameView(ttf);
			}
		});
	}

	@Override
	public void resetToneMapView() {
		if (toneMapViews.containsKey(currentToneMapViewType)) {
			LOG.severe(">>!! chosen file reset TM views: " + currentToneMapViewType);
			toneMapView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			updateTimeFrameView(toneMapViews.get(currentToneMapViewType).getTimeFrame());
			chromaPreView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			chromaPostView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			beatsView.renderToneMap(toneMapViews.get(currentToneMapViewType));
		} else {
			LOG.severe(">>Visor clear TM views: " + currentToneMapViewType);
			toneMapView.clear();
			chromaPreView.clear();
			chromaPostView.clear();
			beatsView.clear();
		}
	}

	@Override
	public void updateBeatsView(ToneMap toneMap) {
		beatsView.updateToneMap(toneMap);
	}

	@Override
	public void updatePercussionView(ToneMap toneMap) {
		percussionView.updateToneMap(toneMap);
	}

	@Override
	public void updateChromaPreView(ToneMap toneMap, ToneTimeFrame ttf) {
		chromaPreView.updateToneMap(toneMap, ttf);
	}

	@Override
	public void updateChromaPreView(ToneMap toneMap) {
		chromaPreView.updateToneMap(toneMap);
	}

	@Override
	public void updateChromaPostView(ToneMap toneMap, ToneTimeFrame ttf) {
		chromaPostView.updateToneMap(toneMap, ttf);
	}

	@Override
	public void updateChromaPostView(ToneMap toneMap) {
		chromaPostView.updateToneMap(toneMap);
	}

	@Override
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

	@Override
	public void audioStopped() {
		startFileProcessingButton.setEnabled(true);
		startListeningButton.setEnabled(true);
		// stopListeningButton.setEnabled(false);
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

	private void refreshMapViews() {
		if (toneMapView != null) {
			toneMapView.updateAxis();
			chromaPreView.updateAxis();
			chromaPostView.updateAxis();
			beatsView.updateAxis();
			percussionView.updateAxis();
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

	@Override
	public void updateParameters() {
		// TODO Update main Visor display from latest parameters.
	}

	public JPanel getContentPanel() {
		return visorPanel;
	}

}
