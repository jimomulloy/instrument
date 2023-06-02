package jomu.instrument.desktop.monitor.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jomu.instrument.Instrument;
import jomu.instrument.InstrumentException;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.audio.features.ConstantQSource;
import jomu.instrument.audio.features.PitchDetectorSource;
import jomu.instrument.audio.features.SpectralInfo;
import jomu.instrument.audio.features.SpectralPeaksSource;
import jomu.instrument.audio.features.SpectrogramInfo;
import jomu.instrument.cognition.cell.Cell;
import jomu.instrument.control.Controller;
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
@Alternative
@jakarta.annotation.Priority(1)
public class SwingDesktopVisor implements Visor, AudioFeatureFrameObserver {

	private static final Logger LOG = Logger.getLogger(SwingDesktopVisor.class.getName());

	@Inject
	Instrument instrument;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Storage storage;

	@Inject
	InstrumentStoreService iss;

	@Inject
	Workspace workspace;

	@Inject
	Console console;

	@Inject
	Controller controller;

	@Inject
	Coordinator coordinator;

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

	static final Integer[] fftWindowSizes = { 256, 512, 1024, 2048, 4096, 8192 };
	static final Integer[] inputSampleRate = { 8000, 11025, 22050, 44100, 48000, 192000 };

	File inputFile;

	JPanel diagnosticsPanel;

	ChromaView chromaView;

	BeatsView beatsView;

	JFrame mainframe;

	JPanel beatsPanel;

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
	JCheckBox playResynthSwitchCB;
	TimeFramePanel timeFramePanel;
	JButton chooseFileButton;
	JButton startFileProcessingButton;
	JButton startListeningButton;
	JButton stopListeningButton;
	JButton playAudioButton;
	JButton playStreamButton;
	JCheckBox playPeaksSwitchCB;
	AbstractButton showPeaksSwitchCB;
	JCheckBox showPowerSwitchCB;
	JCheckBox showLogSwitchCB;
	JCheckBox trackWriteSwitchCB;
	JPanel visorPanel;
	JFrame mainFrame;
	Container upperPane;
	JPanel contentPane;
	JLabel statusLabel;
	JLabel fileNameLabel;
	JTextField hearingMinFreqCentsInput;
	JTextField hearingMaxFreqCentsInput;
	JCheckBox updateThresholdSwitchCB;
	JCheckBox showColourSwitchCB;
	JCheckBox silentWriteSwitchCB;
	JCheckBox pausePlaySwitchCB;
	JCheckBox midiSynthTracksSwitchCB;
	JCheckBox showStatsSwitchCB;
	JCheckBox midiPlayLogSwitchCB;
	JTextField voicePlayerLogFactorInput;
	JTextField voicePlayerGlissandoRangeInput;
	JTextField audioGainInput;
	JTextField persistenceModeInput;
	JTextField voicePlayerRepeatInput;
	JCheckBox loopSaveSwitchCB;
	JCheckBox midiPlayVolumeSwitchCB;
	JCheckBox showNotesSwitchCB;
	JCheckBox showChordsSwitchCB;
	JCheckBox showBeatsSwitchCB;
	JButton resetSystemButton;
	JButton parametersButton;
	JButton synthButton;
	JButton updateViewButton;
	JSplitPane toneMapViewTopPane;
	JSplitPane toneMapViewBottomPane;
	JComboBox fftWindowSizeComboBox;
	JComboBox inputSampleRateCombobox;

	@Override
	public void startUp() {
		LOG.severe(">>Using SwingDesktopVisor");
		EventQueue.invokeLater(() -> {
			buildMainFrame();
		});
	}

	@Override
	public void shutdown() {
		EventQueue.invokeLater(() -> {
			closeMainFrame();
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

	protected void closeMainFrame() {
		mainFrame.setVisible(false);
		mainframe.dispose();
	}

	private void showStackTraceDialog(Throwable throwable, String title, String message) {
		Window window = DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		showStackTraceDialog(throwable, window, title, message);
	}

	private void showEmailStatusInfoDialog(String title, String message) {
		Window window = DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		showEmailStatusInfoDialog(window, title, message);
	}

	private void showStatusInfoDialog(String title, String message) {
		Window window = DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		showStatusInfoDialog(window, title, message);
	}

	/**
	 * show stack trace dialog when exception throws
	 * 
	 * @param throwable
	 * @param parentComponent
	 * @param title
	 * @param message
	 */
	private void showStackTraceDialog(Throwable throwable, Component parentComponent, String title, String message) {
		final String more = "More";
		// create stack strace panel
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JLabel label = new JLabel(more + ">>");
		labelPanel.add(label);

		JTextArea straceTa = new JTextArea();
		final JScrollPane taPane = new JScrollPane(straceTa);
		taPane.setPreferredSize(new Dimension(360, 240));
		taPane.setVisible(false);
		// print stack trace into textarea
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		throwable.printStackTrace(new PrintStream(out));
		straceTa.setForeground(Color.RED);
		straceTa.setText(new String(out.toByteArray()));

		final JPanel stracePanel = new JPanel(new BorderLayout());
		stracePanel.add(labelPanel, BorderLayout.NORTH);
		stracePanel.add(taPane, BorderLayout.CENTER);

		label.setForeground(Color.BLUE);
		label.setCursor(new Cursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JLabel tmpLab = (JLabel) e.getSource();
				if (tmpLab.getText().equals(more + ">>")) {
					tmpLab.setText("<<" + more);
					taPane.setVisible(true);
				} else {
					tmpLab.setText(more + ">>");
					taPane.setVisible(false);
				}
				SwingUtilities.getWindowAncestor(taPane).pack();
			};
		});

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(message), BorderLayout.NORTH);
		panel.add(stracePanel, BorderLayout.CENTER);

		JOptionPane pane = new JOptionPane(panel, JOptionPane.ERROR_MESSAGE);
		JDialog dialog = pane.createDialog(parentComponent, title);
		int maxWidth = Toolkit.getDefaultToolkit().getScreenSize().width * 2 / 3;
		if (dialog.getWidth() > maxWidth) {
			dialog.setSize(new Dimension(maxWidth, dialog.getHeight()));
			setLocationRelativeTo(dialog, parentComponent);
		}
		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.dispose();
	}

	private void showStatusInfoDialog(Component parentComponent, String title, String message) {
		final String more = "More";
		// create stack strace panel
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JLabel label = new JLabel(more + ">>");
		labelPanel.add(label);

		JTextArea straceTa = new JTextArea();
		final JScrollPane taPane = new JScrollPane(straceTa);
		taPane.setPreferredSize(new Dimension(360, 240));
		taPane.setVisible(false);
		straceTa.setForeground(Color.RED);
		straceTa.setText("Status Info");

		final JPanel stracePanel = new JPanel(new BorderLayout());
		stracePanel.add(labelPanel, BorderLayout.NORTH);
		stracePanel.add(taPane, BorderLayout.CENTER);

		label.setForeground(Color.BLUE);
		label.setCursor(new Cursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JLabel tmpLab = (JLabel) e.getSource();
				if (tmpLab.getText().equals(more + ">>")) {
					tmpLab.setText("<<" + more);
					taPane.setVisible(true);
				} else {
					tmpLab.setText(more + ">>");
					taPane.setVisible(false);
				}
				SwingUtilities.getWindowAncestor(taPane).pack();
			};
		});

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(message), BorderLayout.NORTH);
		panel.add(stracePanel, BorderLayout.CENTER);

		JOptionPane pane = new JOptionPane(panel, JOptionPane.ERROR_MESSAGE);
		JDialog dialog = pane.createDialog(parentComponent, title);
		int maxWidth = Toolkit.getDefaultToolkit().getScreenSize().width * 2 / 3;
		if (dialog.getWidth() > maxWidth) {
			dialog.setSize(new Dimension(maxWidth, dialog.getHeight()));
			setLocationRelativeTo(dialog, parentComponent);
		}
		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.dispose();
	}

	private void showEmailStatusInfoDialog(Component parentComponent, String title, String message) {
		final String more = "More";
		// create stack strace panel
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton emailSendButton = new JButton("Send Email");

		JLabel emailSenderAddressLabel = new JLabel("Sender Email address: ");
		JTextField emailSenderAddressInput = new JTextField(4);
		emailSenderAddressInput.setPreferredSize(null);
		emailSenderAddressInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				emailSendButton.setEnabled(true);
			}
		});

		emailSendButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// String result =
				// controller.sendStatusInfoEmail(emailSenderAddressInput.getText());
				// statusLabel.setText(result);
			}
		});
		emailSendButton.setEnabled(false);

		labelPanel.add(emailSenderAddressLabel);
		labelPanel.add(emailSenderAddressInput);
		labelPanel.add(emailSendButton);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(labelPanel, BorderLayout.CENTER);

		JOptionPane pane = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE);
		JDialog dialog = pane.createDialog(parentComponent, title);
		int maxWidth = Toolkit.getDefaultToolkit().getScreenSize().width * 2 / 3;
		if (dialog.getWidth() > maxWidth) {
			dialog.setSize(new Dimension(maxWidth, dialog.getHeight()));
			setLocationRelativeTo(dialog, parentComponent);
		}
		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.dispose();
	}

	/**
	 * set c1 location relative to c2
	 * 
	 * @param c1
	 * @param c2
	 */
	private void setLocationRelativeTo(Component c1, Component c2) {
		Container root = null;

		if (c2 != null) {
			if (c2 instanceof Window) {
				root = (Container) c2;
			} else {
				Container parent;
				for (parent = c2.getParent(); parent != null; parent = parent.getParent()) {
					if (parent instanceof Window) {
						root = parent;
						break;
					}
				}
			}
		}

		if ((c2 != null && !c2.isShowing()) || root == null || !root.isShowing()) {
			Dimension paneSize = c1.getSize();

			Point centerPoint = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
			c1.setLocation(centerPoint.x - paneSize.width / 2, centerPoint.y - paneSize.height / 2);
		} else {
			Dimension invokerSize = c2.getSize();
			Point invokerScreenLocation = c2.getLocation(); // by longrm:
			// c2.getLocationOnScreen();

			Rectangle windowBounds = c1.getBounds();
			int dx = invokerScreenLocation.x + ((invokerSize.width - windowBounds.width) >> 1);
			int dy = invokerScreenLocation.y + ((invokerSize.height - windowBounds.height) >> 1);
			Rectangle ss = root.getGraphicsConfiguration().getBounds();

			// Adjust for bottom edge being offscreen
			if (dy + windowBounds.height > ss.y + ss.height) {
				dy = ss.y + ss.height - windowBounds.height;
				if (invokerScreenLocation.x - ss.x + invokerSize.width / 2 < ss.width / 2) {
					dx = invokerScreenLocation.x + invokerSize.width;
				} else {
					dx = invokerScreenLocation.x - windowBounds.width;
				}
			}

			// Avoid being placed off the edge of the screen
			if (dx + windowBounds.width > ss.x + ss.width) {
				dx = ss.x + ss.width - windowBounds.width;
			}
			if (dx < ss.x)
				dx = ss.x;
			if (dy < ss.y)
				dy = ss.y;

			c1.setLocation(dx, dy);
		}
	}

	private void buildContent() {

		contentPane = new JPanel();

		upperPane = new JPanel();
		JPanel statusPane = new JPanel();
		JPanel lowerPane = new JPanel();

		statusLabel = new JLabel("Ready");
		JButton showStatusInfoButton = new JButton("Show Status Info");

		showStatusInfoButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String s = e.getActionCommand();
				Throwable t = workspace.getInstrumentSessionManager().getCurrentSession().getException();
				if (t != null) {
					showStackTraceDialog(t, "StatusInfo", "Instrument Exception");
				} else {
					showStatusInfoDialog("StatusInfo", "Instrument OK");
				}
			}
		});

		statusLabel = new JLabel("Ready");

		JButton dumpStatusInfoButton = new JButton("Dump Status Info");

		dumpStatusInfoButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String result = controller.dumpStatusInfo();
				statusLabel.setText(result);
			}
		});

		contentPane.setLayout(new BorderLayout());

		final int inset1 = 45;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		contentPane.setPreferredSize(new Dimension(screenSize.width - inset1 * 2, screenSize.height - inset1 * 2));

		EmptyBorder eb = new EmptyBorder(5, 5, 5, 5);
		BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
		CompoundBorder cb = new CompoundBorder(eb, bb);
		contentPane.setBorder(cb);

		statusPane.add(statusLabel, BorderLayout.CENTER);
		statusPane.add(showStatusInfoButton, BorderLayout.EAST);
		statusPane.add(dumpStatusInfoButton, BorderLayout.EAST);

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
		toneMapViewTopPane = new JSplitPane(SwingConstants.HORIZONTAL, new JScrollPane(chromaPanel),
				new JScrollPane(beatsPanel));
		toneMapViewBottomPane = new JSplitPane(SwingConstants.HORIZONTAL, new JScrollPane(toneMapViewTopPane),
				new JScrollPane(spectrumsPanel));
		toneMapViewTopPane.setOneTouchExpandable(true);
		toneMapViewBottomPane.setOneTouchExpandable(true);

		leftGraphPanel.add(toneMapViewBottomPane, BorderLayout.CENTER);

		if (toneMapViewTopPane != null) {
			if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_CHORDS)) {
				if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS)) {
					toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.3));
					toneMapViewTopPane.setDividerLocation((int) ((double) screenHeight * 0.18));
				} else {
					toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.3));
					toneMapViewTopPane.setDividerLocation((int) ((double) screenHeight * 0.18));
				}
			} else {
				if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS)) {
					toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.2));
					toneMapViewTopPane.setDividerLocation(0);
				} else {
					toneMapViewBottomPane.setDividerLocation(0);
					toneMapViewTopPane.setDividerLocation(0);
				}
			}
		}
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
		toneMapTabbedPane.addTab("ToneMap", toneMapLayerPanel);
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
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));
		chromaView = new ChromaView();
		JPanel chromaSynthPanel = new JPanel(new BorderLayout());
		chromaSynthPanel.add(chromaView, BorderLayout.CENTER);
		chromaSynthPanel.setBackground(Color.BLACK);
		chromaTabbedPane.addTab("Chroma", chromaSynthPanel);
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

		JPanel graphViewControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

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
			}
		});
		frameNumberInput.setText("0");
		graphControlPanel.add(frameNumberLabel);
		graphControlPanel.add(frameNumberInput);

		toneMapViewComboBox = new JComboBox<>();

		Arrays.asList(new String[] { Cell.CellTypes.AUDIO_SYNTHESIS.name(), Cell.CellTypes.AUDIO_CQ.name(),
				Cell.CellTypes.AUDIO_CQ_ORIGIN.name(), Cell.CellTypes.AUDIO_CQ_MICRO_TONE.name(),
				Cell.CellTypes.AUDIO_TUNER_PEAKS.name(), Cell.CellTypes.AUDIO_SPECTRAL_PEAKS.name(),
				Cell.CellTypes.AUDIO_PITCH.name(), Cell.CellTypes.AUDIO_YIN.name(), Cell.CellTypes.AUDIO_SACF.name(),
				Cell.CellTypes.AUDIO_MFCC.name(), Cell.CellTypes.AUDIO_CEPSTRUM.name(),
				Cell.CellTypes.AUDIO_NOTATE.name(), Cell.CellTypes.AUDIO_NOTATE.name() + "_PEAKS",
				Cell.CellTypes.AUDIO_NOTATE.name() + "_SPECTRAL", Cell.CellTypes.AUDIO_INTEGRATE.name(),
				Cell.CellTypes.AUDIO_INTEGRATE.name() + "_PEAKS", Cell.CellTypes.AUDIO_INTEGRATE.name() + "_SPECTRAL",
				Cell.CellTypes.AUDIO_ONSET.name(), Cell.CellTypes.AUDIO_PERCUSSION.name(),
				Cell.CellTypes.AUDIO_ONSET.name() + "_SMOOTHED", Cell.CellTypes.AUDIO_HPS.name(),
				Cell.CellTypes.AUDIO_HPS.name() + "_HARMONIC_MASK",
				Cell.CellTypes.AUDIO_HPS.name() + "_PERCUSSION_MASK", Cell.CellTypes.AUDIO_HPS.name() + "_HARMONIC",
				Cell.CellTypes.AUDIO_HPS.name() + "_PERCUSSION", Cell.CellTypes.AUDIO_PRE_CHROMA.name(),
				Cell.CellTypes.AUDIO_POST_CHROMA.name(), Cell.CellTypes.AUDIO_BEAT.name() }).stream()
				.forEach(entry -> toneMapViewComboBox.addItem(entry));

		toneMapViewComboBox.setEnabled(false);
		toneMapViewComboBox.setSelectedItem(Cell.CellTypes.AUDIO_SYNTHESIS.name());
		toneMapViewComboBox.setEnabled(true);
		currentToneMapViewType = Cell.CellTypes.AUDIO_SYNTHESIS.name();

		toneMapViewComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String value = String.valueOf(toneMapViewComboBox.getSelectedItem());
				if (value != null) {
					currentToneMapViewType = value;
					switchToneMapView();
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET,
						newValue);
				textField.setText(newValue);
				refreshMapViews();
				switchToneMapView();
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET,
						newValue);
				textField.setText(newValue);
				refreshMapViews();
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE,
						newValue);
				textField.setText(newValue);
				refreshMapViews();

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
				newValue = parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE,
						newValue);
				textField.setText(newValue);
				refreshMapViews();
			}
		});

		pitchAxisRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE));
		graphControlPanel.add(pitchAxisRangeLabel);
		graphControlPanel.add(pitchAxisRangeInput);

		panel.add(graphControlPanel, BorderLayout.NORTH);

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
		graphViewControlPanel.add(showColourSwitchCB);

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
		graphViewControlPanel.add(showPeaksSwitchCB);

		showPowerSwitchCB = new JCheckBox("showPowerSwitchCB");
		showPowerSwitchCB.setText("Power");
		showPowerSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_POWER,
						Boolean.toString(newValue));
				refreshMapViews();
			}
		});
		showPowerSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_POWER));
		graphViewControlPanel.add(showPowerSwitchCB);

		showNotesSwitchCB = new JCheckBox("showNotesSwitchCB");
		showNotesSwitchCB.setText("Notes");
		showNotesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_NOTES,
						Boolean.toString(newValue));
				refreshMapViews();
			}
		});
		showNotesSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_NOTES));
		graphViewControlPanel.add(showNotesSwitchCB);

		showChordsSwitchCB = new JCheckBox("showChordsSwitchCB");
		showChordsSwitchCB.setText("Chords");
		showChordsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_CHORDS,
						Boolean.toString(newValue));
				Toolkit myScreen = Toolkit.getDefaultToolkit();
				Dimension screenSize = myScreen.getScreenSize();
				int screenHeight = screenSize.height;
				if (toneMapViewTopPane != null) {
					if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_CHORDS)) {
						if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS)) {
							toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.3));
							toneMapViewTopPane.setDividerLocation((int) ((double) screenHeight * 0.18));
						} else {
							toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.3));
							toneMapViewTopPane.setDividerLocation((int) ((double) screenHeight * 0.18));
						}
					} else {
						if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS)) {
							toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.2));
							toneMapViewTopPane.setDividerLocation(0);
						} else {
							toneMapViewBottomPane.setDividerLocation(0);
							toneMapViewTopPane.setDividerLocation(0);
						}
					}
				}
				refreshMapViews();
			}
		});
		showChordsSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_CHORDS));
		graphViewControlPanel.add(showChordsSwitchCB);

		showBeatsSwitchCB = new JCheckBox("showBeatsSwitchCB");
		showBeatsSwitchCB.setText("Beats");
		showBeatsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS,
						Boolean.toString(newValue));
				Toolkit myScreen = Toolkit.getDefaultToolkit();
				Dimension screenSize = myScreen.getScreenSize();
				int screenHeight = screenSize.height;
				if (toneMapViewTopPane != null) {
					if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_CHORDS)) {
						if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS)) {
							toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.3));
							toneMapViewTopPane.setDividerLocation((int) ((double) screenHeight * 0.18));
						} else {
							toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.3));
							toneMapViewTopPane.setDividerLocation((int) ((double) screenHeight * 0.18));
						}
					} else {
						if (parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS)) {
							toneMapViewBottomPane.setDividerLocation((int) ((double) screenHeight * 0.2));
							toneMapViewTopPane.setDividerLocation(0);
						} else {
							toneMapViewBottomPane.setDividerLocation(0);
							toneMapViewTopPane.setDividerLocation(0);
						}
					}
				}
				refreshMapViews();
			}
		});
		showBeatsSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS));
		graphViewControlPanel.add(showBeatsSwitchCB);

		showStatsSwitchCB = new JCheckBox("showStatsSwitchCB");
		showStatsSwitchCB.setText("Stats");
		showStatsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_STATS,
						Boolean.toString(newValue));
				refreshMapViews();
			}
		});
		showStatsSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_STATS));
		graphViewControlPanel.add(showStatsSwitchCB);

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
		graphViewControlPanel.add(showLogSwitchCB);

		updateViewButton = new JButton("Update");
		updateViewButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateViewThresholds();
				toneMapViewHighThresholdInput.setText(
						parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
			}
		});
		graphViewControlPanel.add(updateViewButton);

		JLabel toneMapViewLowThresholdLabel = new JLabel("Low: ");
		toneMapViewLowThresholdInput = new JTextField(4);
		toneMapViewLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD,
						newValue);
				textField.setText(newValue);
				refreshMapViews();
			}
		});

		toneMapViewLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD));
		graphViewControlPanel.add(toneMapViewLowThresholdLabel);
		graphViewControlPanel.add(toneMapViewLowThresholdInput);

		JLabel toneMapViewHighThresholdLabel = new JLabel("High: ");
		toneMapViewHighThresholdInput = new JTextField(4);
		toneMapViewHighThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD,
						newValue);
				textField.setText(newValue);
				refreshMapViews();
			}
		});

		toneMapViewHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
		graphViewControlPanel.add(toneMapViewHighThresholdLabel);
		graphViewControlPanel.add(toneMapViewHighThresholdInput);

		panel.add(graphViewControlPanel, BorderLayout.CENTER);

		return panel;
	}

	@Override
	public void updateViewThresholds() {
		if (toneMapViews.containsKey(currentToneMapViewType)) {
			double maxAmplitude = toneMapView.getMaxAmplitude();
			Formatter formatter = new Formatter();
			formatter.format("%.2f", maxAmplitude);
			parameterManager.setParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD,
					formatter.toString());
			formatter.close();
			parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
			int timeRange = toneMapView.getMaxTime();
			formatter = new Formatter();
			formatter.format("%d", timeRange);
			parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE, formatter.toString());
			formatter.close();
			int minPitchCents = toneMapView.getMinPitchCents();
			formatter = new Formatter();
			formatter.format("%d", minPitchCents);
			parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET,
					formatter.toString());
			formatter.close();
			int maxPitchCents = toneMapView.getMaxPitchCents();
			formatter = new Formatter();
			formatter.format("%d", maxPitchCents);
			LOG.severe(">>TV Max pitch: " + maxPitchCents + ", "
					+ parameterManager.getIntParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE));
			parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE, formatter.toString());
			formatter.close();
			timeAxisOffsetInput
					.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET));
			pitchAxisOffsetInput
					.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET));
			timeAxisRangeInput
					.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE));
			pitchAxisRangeInput
					.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE));

		}
		refreshMapViews();
	}

	protected boolean parseIntsegerTextField(JTextField textField, int min, int max, String parameterName) {
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

		JPanel actionPanel = new JPanel(new BorderLayout());
		// JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JPanel actionEastPanel = new JPanel();
		JPanel actionCenterPanel = new JPanel();
		JPanel actionWestPanel = new JPanel();
		actionPanel.add(actionEastPanel, BorderLayout.EAST);
		actionPanel.add(actionCenterPanel, BorderLayout.CENTER);
		actionPanel.add(actionWestPanel, BorderLayout.WEST);

		JPanel voicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JPanel instrumentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		resetSystemButton = new JButton("Reset");
		resetSystemButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				instrument.reset();
				updateStatusMessage("System Reset");
			}
		});

		actionWestPanel.add(resetSystemButton);

		final JButton helpButton = new JButton("Help");

		helpButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String s = e.getActionCommand();
				String message = """
						For information about using this application - please see - https://github.com/jimomulloy/instrument/wiki

						In case of status errors, please use the "Dump Status Info" button to create a text file containing information
						about the current status of the application.

						If you wish me to investigate please send this file as an attachment to my email address jimomulloy@gmail.com.

						Thank you for using this early prototype version, hope you have fun with it.
						""";
				if (s.equals("Help")) {
					JOptionPane.showMessageDialog(mainframe, message);
				}
			}
		});

		parametersButton = new JButton("Parameters");

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
						d.setSize((int) ((double) screenWidth * 0.9), (int) ((double) screenHeight * 0.8));

						// set visibility of dialog
						d.setVisible(true);

						parameterDialogOpen = true;

					}
				}
			}
		});
		actionWestPanel.add(helpButton);

		parametersButton = new JButton("Parameters");

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
						d.setSize((int) ((double) screenWidth * 0.9), (int) ((double) screenHeight * 0.8));

						// set visibility of dialog
						d.setVisible(true);

						parameterDialogOpen = true;

					}
				}
			}
		});

		actionWestPanel.add(parametersButton);

		synthButton = new JButton("Synth Controls");

		synthButton.addActionListener(new ActionListener() {

			private boolean synthDialogOpen;

			@Override
			public void actionPerformed(ActionEvent e) {
				String s = e.getActionCommand();
				if (s.equals("Synth Controls")) {

					if (!synthDialogOpen) {
						// create a dialog Box
						JDialog d = new JDialog(mainframe, "Synth");

						d.addWindowListener(new WindowAdapter() {
							public void windowClosed(WindowEvent e) {
								synthDialogOpen = false;
							}

							public void windowClosing(WindowEvent e) {
								synthDialogOpen = false;
							}
						});

						JPanel dialogPanel = new JPanel(new BorderLayout());

						JPanel synthPanel = new SynthPanel();
						dialogPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(20, 20, 20, 20),
								new EtchedBorder()));

						dialogPanel.add(new JScrollPane(synthPanel), BorderLayout.CENTER);

						d.add(dialogPanel);

						Toolkit myScreen = Toolkit.getDefaultToolkit();
						Dimension screenSize = myScreen.getScreenSize();
						int screenHeight = screenSize.height;
						int screenWidth = screenSize.width;

						// setsize of dialog
						d.setSize((int) ((double) screenWidth * 0.9), (int) ((double) screenHeight * 0.8));

						// set visibility of dialog
						d.setVisible(true);

						synthDialogOpen = true;

					}
				}
			}
		});
		actionWestPanel.add(synthButton);

		final JFileChooser fileChooser = new JFileChooser(new File(getAudioSourceFileFolder()));
		chooseFileButton = new JButton("Open");
		startFileProcessingButton = new JButton("Start");
		startListeningButton = new JButton("Listen");
		stopListeningButton = new JButton("Stop");
		playAudioButton = new JButton("Play Audio");
		playStreamButton = new JButton("Play Stream");
		fileChooser.setSelectedFile(new File(getAudioSourceFileFolder(), getDefaultAudioFile()));
		chooseFileButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(visorPanel);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					inputFile = fileChooser.getSelectedFile();
					String fileName = inputFile.getAbsolutePath();
					fileNameLabel.setText(inputFile.getName());
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
					frameNumberInput.setEnabled(false);
					// timeAxisOffsetInput.setText("0");
					workspace.getInstrumentSessionManager().getCurrentSession().clearException();
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
					parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DEFAULT_FILE,
							inputFile.getName());
					toneMapViews.remove(currentToneMapViewType);
					refreshMapViews();
					resetToneMapView();
					try {
						Instrument.getInstance().getCoordinator().getHearing().startAudioFileStream(fileName);
						updateStatusMessage("Choosen file: " + inputFile);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Error choosing file :" + inputFile, e);
						coordinator.getHearing().stopAudioStream();
						coordinator.getVoice().clear(coordinator.getHearing().getStreamId());
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

		actionCenterPanel.add(chooseFileButton);

		startFileProcessingButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					inputFile = fileChooser.getSelectedFile();
					String fileName = inputFile.getAbsolutePath();
					fileNameLabel.setText(inputFile.getName());
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
					frameNumberInput.setEnabled(false);
					// timeAxisOffsetInput.setText("0");
					workspace.getInstrumentSessionManager().getCurrentSession().clearException();
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
					parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DEFAULT_FILE,
							inputFile.getName());
					toneMapViews.remove(currentToneMapViewType);
					refreshMapViews();
					resetToneMapView();
					Instrument.getInstance().getCoordinator().getHearing().startAudioFileStream(fileName);
					updateStatusMessage("Started file: " + inputFile);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Error starting file :" + inputFile, e);
					coordinator.getHearing().stopAudioStream();
					coordinator.getVoice().clear(coordinator.getHearing().getStreamId());
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
					String fileName = "instrument_recording_" + System.currentTimeMillis() + ".wav";
					String filePath = getAudioRecordFileFolder() + "/" + fileName;
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					fileNameLabel.setText(fileName);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
					frameNumberInput.setEnabled(false);
					// timeAxisOffsetInput.setText("0");
					workspace.getInstrumentSessionManager().getCurrentSession().clearException();
					parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, "0");
					toneMapViews.remove(currentToneMapViewType);
					refreshMapViews();
					resetToneMapView();
					coordinator.getHearing().startAudioLineStream(filePath);
					updateStatusMessage("Started listener: " + fileName);

				} catch (LineUnavailableException | IOException e) {
					LOG.log(Level.SEVERE, "Error starting listener", e);
					coordinator.getHearing().stopAudioStream();
					coordinator.getVoice().clear(coordinator.getHearing().getStreamId());
					startFileProcessingButton.setEnabled(true);
					startListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(true);
					frameNumberInput.setEnabled(true);
					updateStatusMessage("Error starting listener :" + e.getMessage());
				}
			}
		});

		stopListeningButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				coordinator.getHearing().stopAudioStream();
				coordinator.getHearing().stopAudioPlayer();
				coordinator.getVoice().clear(coordinator.getHearing().getStreamId());
				coordinator.getVoice().stopStreamPlayer();
				startFileProcessingButton.setEnabled(true);
				startListeningButton.setEnabled(true);
				playAudioButton.setEnabled(true);
				playStreamButton.setEnabled(true);
				chooseFileButton.setEnabled(true);
				frameNumberInput.setEnabled(true);
				updateStatusMessage("Stopped");
			}
		});

		playAudioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					boolean started = coordinator.getHearing().startAudioPlayer();
					if (started) {
						stopListeningButton.setEnabled(true);
					}
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Error starting listener", e);
					coordinator.getHearing().stopAudioStream();
					updateStatusMessage("Error playing audio :" + e.getMessage());
				}
			}
		});

		playStreamButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (coordinator.getHearing().getStreamId() != null) {
					boolean started = coordinator.getVoice().startStreamPlayer(coordinator.getHearing().getStreamId());
					stopListeningButton.setEnabled(true);
				}
			}
		});

		actionCenterPanel.add(startFileProcessingButton);
		actionCenterPanel.add(startListeningButton);
		actionCenterPanel.add(stopListeningButton);
		actionCenterPanel.add(playAudioButton);
		actionCenterPanel.add(playStreamButton);

		JLabel persistenceModeLabel = new JLabel("Persistence Mode: ");
		persistenceModeInput = new JTextField(4);
		persistenceModeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = persistenceModeInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_PERSISTENCE_MODE, newValue);
				persistenceModeInput.setText(newValue);
			}
		});
		persistenceModeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_PERSISTENCE_MODE));
		actionEastPanel.add(persistenceModeLabel);
		actionEastPanel.add(persistenceModeInput);

		panel.add(actionPanel, BorderLayout.NORTH);

		fftWindowSizeComboBox = new JComboBox<>(fftWindowSizes);
		fftWindowSizeComboBox.addActionListener(new ActionListener() {
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

		fftWindowSizeComboBox.setSelectedIndex(2);
		instrumentPanel.add(new JLabel("FFT Window:  "));
		instrumentPanel.add(fftWindowSizeComboBox);

		inputSampleRateCombobox = new JComboBox<>(inputSampleRate);
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
		instrumentPanel.add(new JLabel("Sample rate:  "));
		instrumentPanel.add(inputSampleRateCombobox);

		JLabel audioFeatureIntervalLabel = new JLabel("Interval ms: ");
		audioFeatureIntervalInput = new JTextField(4);
		audioFeatureIntervalInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL, newValue);
				textField.setText(newValue);
			}
		});

		audioFeatureIntervalInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL));
		instrumentPanel.add(audioFeatureIntervalLabel);
		instrumentPanel.add(audioFeatureIntervalInput);

		JLabel audioOffsetLabel = new JLabel("Offset ms: ");
		audioOffsetInput = new JTextField(4);
		audioOffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET,
						newValue);
				textField.setText(newValue);
			}
		});

		audioOffsetInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET));
		instrumentPanel.add(audioOffsetLabel);
		instrumentPanel.add(audioOffsetInput);

		audioFeatureIntervalInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL));
		instrumentPanel.add(audioFeatureIntervalLabel);
		instrumentPanel.add(audioFeatureIntervalInput);

		JLabel audioGainLabel = new JLabel("Gain: ");
		audioGainInput = new JTextField(4);
		audioGainInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_GAIN_COMPRESS_FACTOR, newValue);
				textField.setText(newValue);
			}
		});

		audioGainInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_GAIN_COMPRESS_FACTOR));
		instrumentPanel.add(audioGainLabel);
		instrumentPanel.add(audioGainInput);

		JLabel audioRangeLabel = new JLabel("Range ms: ");
		audioRangeInput = new JTextField(4);
		audioRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE,
						newValue);
				textField.setText(newValue);
			}
		});

		audioRangeInput.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE));
		instrumentPanel.add(audioRangeLabel);
		instrumentPanel.add(audioRangeInput);

		JLabel hearingMinFreqCentsLabel = new JLabel("Min Cents: ");
		hearingMinFreqCentsInput = new JTextField(4);
		hearingMinFreqCentsInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = hearingMinFreqCentsInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS, newValue);
				hearingMinFreqCentsInput.setText(newValue);
			}
		});
		hearingMinFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS));
		instrumentPanel.add(hearingMinFreqCentsLabel);
		instrumentPanel.add(hearingMinFreqCentsInput);

		JLabel hearingMaxFreqCentsLabel = new JLabel("Max Cents: ");
		hearingMaxFreqCentsInput = new JTextField(4);
		hearingMaxFreqCentsInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = hearingMaxFreqCentsInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS, newValue);
				hearingMaxFreqCentsInput.setText(newValue);
			}
		});
		hearingMaxFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS));
		instrumentPanel.add(hearingMaxFreqCentsLabel);
		instrumentPanel.add(hearingMaxFreqCentsInput);

		instrumentPanel.add(new JLabel(" Current File:"));

		fileNameLabel = new JLabel("");

		instrumentPanel.add(fileNameLabel);

		panel.add(instrumentPanel, BorderLayout.CENTER);

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

		pausePlaySwitchCB = new JCheckBox("pausePlaySwitchCB");
		pausePlaySwitchCB.setText("Pause");
		pausePlaySwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_PAUSE_PLAY_SWITCH,
						Boolean.toString(newValue));
			}
		});

		pausePlaySwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PAUSE_PLAY_SWITCH));
		voicePanel.add(pausePlaySwitchCB);

		JLabel spacer1Label = new JLabel("  ");
		voicePanel.add(spacer1Label);

		JLabel voicePlayerDelayLabel = new JLabel("Delay: ");
		voicePlayerDelayInput = new JTextField(4);
		voicePlayerDelayInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = voicePlayerDelayInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY, newValue);
				voicePlayerDelayInput.setText(newValue);
			}
		});

		voicePlayerDelayInput.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		voicePanel.add(voicePlayerDelayLabel);
		voicePanel.add(voicePlayerDelayInput);

		JLabel voicePlayerRepeatLabel = new JLabel("Repeat: ");
		voicePlayerRepeatInput = new JTextField(4);
		voicePlayerRepeatInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = voicePlayerRepeatInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_REPEAT,
						newValue);
				voicePlayerRepeatInput.setText(newValue);
			}
		});

		voicePlayerRepeatInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_REPEAT));
		voicePanel.add(voicePlayerRepeatLabel);
		voicePanel.add(voicePlayerRepeatInput);

		loopSaveSwitchCB = new JCheckBox("loopSaveSwitchCB");
		loopSaveSwitchCB.setText("Loops");
		loopSaveSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_LOOP_SAVE,
						Boolean.toString(newValue));
			}
		});

		loopSaveSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_LOOP_SAVE));
		voicePanel.add(loopSaveSwitchCB);

		JLabel voicePlayerLowThresholdLabel = new JLabel("Low Threshold: ");
		voicePlayerLowThresholdInput = new JTextField(4);
		voicePlayerLowThresholdInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD,
						newValue);
				voicePlayerLowThresholdInput.setText(newValue);
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
				newValue = parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD,
						newValue);
				voicePlayerHighThresholdInput.setText(newValue);
			}
		});

		voicePlayerHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD));
		voicePanel.add(voicePlayerHighThresholdLabel);
		voicePanel.add(voicePlayerHighThresholdInput);

		midiPlayVolumeSwitchCB = new JCheckBox("midiPlayVolumeSwitchCB");
		midiPlayVolumeSwitchCB.setText("Volume");
		midiPlayVolumeSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOLUME_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayVolumeSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOLUME_SWITCH));
		voicePanel.add(midiPlayVolumeSwitchCB);

		midiPlayLogSwitchCB = new JCheckBox("midiPlayLogSwitchCB");
		midiPlayLogSwitchCB.setText("Log");
		midiPlayLogSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiPlayLogSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH));
		voicePanel.add(midiPlayLogSwitchCB);

		JLabel voicePlayerLogFactorLabel = new JLabel("Log Factor: ");
		voicePlayerLogFactorInput = new JTextField(4);
		voicePlayerLogFactorInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR, newValue);
				voicePlayerLogFactorInput.setText(newValue);
			}
		});

		voicePlayerLogFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR));
		voicePanel.add(voicePlayerLogFactorLabel);
		voicePanel.add(voicePlayerLogFactorInput);

		JLabel voicePlayerGlissandoRangeLabel = new JLabel("Glissando: ");
		voicePlayerGlissandoRangeInput = new JTextField(4);
		voicePlayerGlissandoRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String newValue = textField.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_GLISSANDO_RANGE,
						newValue);
				voicePlayerGlissandoRangeInput.setText(newValue);
			}
		});

		voicePlayerGlissandoRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_GLISSANDO_RANGE));
		voicePanel.add(voicePlayerGlissandoRangeLabel);
		voicePanel.add(voicePlayerGlissandoRangeInput);

		voicePanel.add(new JLabel(" "));

		midiSynthTracksSwitchCB = new JCheckBox("midiSynthTracksSwitchCB");
		midiSynthTracksSwitchCB.setText("Synth");
		midiSynthTracksSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SYNTH_TRACKS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		midiSynthTracksSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SYNTH_TRACKS_SWITCH));
		voicePanel.add(midiSynthTracksSwitchCB);

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
		voicePanel.add(recordSwitchCB);

		panel.add(voicePanel, BorderLayout.SOUTH);

		return panel;
	}

	private String getAudioSourceFileFolder() {
		return parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SOURCE_DIRECTORY);
	}

	private String getDefaultAudioFile() {
		return parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DEFAULT_FILE);
	}

	private String getAudioRecordFileFolder() {
		String baseDir = storage.getObjectStorage().getBasePath();
		return Paths
				.get(baseDir,
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
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW));
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
						beatsView.renderToneMap(toneMap);
						chromaView.renderToneMap(toneMap);
					}
				} else if (toneMapView.getToneMap() != null && toneMapViews.get(toneMapViewType) != null
						&& !toneMapViews.get(toneMapViewType).getKey().equals(toneMapView.getToneMap().getKey())) {
					toneMapViews.put(toneMapViewType, toneMap);
					if (toneMapViewType.equals(currentToneMapViewType)) {
						toneMapView.renderToneMap(toneMap);
						beatsView.renderToneMap(toneMap);
						chromaView.renderToneMap(toneMap);
					}
				} else if (toneMapViewType.equals(currentToneMapViewType) && toneMapView != null) {
					toneMapView.updateToneMap(toneMap, ttf);
					beatsView.updateToneMap(toneMap, ttf);
					chromaView.updateToneMap(toneMap, ttf);
				}
				updateTimeFrameView(ttf);
			}
		});
	}

	private void switchToneMapView() {
		if (toneMapViews.containsKey(currentToneMapViewType)) {
			toneMapView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			updateTimeFrameView(toneMapViews.get(currentToneMapViewType).getTimeFrame());
		}
	}

	private void resetToneMapView() {
		if (toneMapViews.containsKey(currentToneMapViewType)) {
			toneMapView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			updateTimeFrameView(toneMapViews.get(currentToneMapViewType).getTimeFrame());
			chromaView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			beatsView.renderToneMap(toneMapViews.get(currentToneMapViewType));
		} else {
			toneMapView.clear();
			chromaView.clear();
			beatsView.clear();
		}
	}

	@Override
	public void updateSpectrumView(ToneTimeFrame toneTimeFrame, int windowSize) {
		if (toneTimeFrame != null) {
			float[] spectrum = toneTimeFrame.extractFFTSpectrum(windowSize).getSpectrum();
			for (int i = 0; i < spectrum.length; i++) {
				spectrum[i] *= 100;
			}
			spectrumLayer.clearPeaks();
			spectrumLayer.setSpectrum(spectrum);
			spectrumPanel.repaint();
		}
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
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW),
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
			chromaView.updateAxis();
			beatsView.updateAxis();
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
		timeAxisOffsetInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET));
		pitchAxisOffsetInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET));
		timeAxisRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE));
		pitchAxisRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE));
		showColourSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_COLOUR));
		showPeaksSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_PEAKS));
		showPowerSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_POWER));
		showNotesSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_NOTES));
		showChordsSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_CHORDS));
		showBeatsSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS));
		showStatsSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_STATS));
		showLogSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_LOG));
		toneMapViewLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD));
		toneMapViewHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD));
		recordSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_SWITCH));
		audioFeatureIntervalInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL));
		audioOffsetInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET));
		audioGainInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_GAIN_COMPRESS_FACTOR));
		audioRangeInput.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE));
		hearingMinFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS));
		hearingMaxFreqCentsInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS));
		persistenceModeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_PERSISTENCE_MODE));
		playMidiSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY));
		playAudioSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_AUDIO_PLAY));
		playResynthSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_RESYNTH_PLAY));
		playPeaksSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS));
		loopSaveSwitchCB
				.setSelected(parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_LOOP_SAVE));
		voicePlayerRepeatInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_REPEAT));

		pausePlaySwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PAUSE_PLAY_SWITCH));
		voicePlayerDelayInput.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		voicePlayerLowThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD));
		voicePlayerHighThresholdInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD));
		voicePlayerLogFactorInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR));
		voicePlayerGlissandoRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_GLISSANDO_RANGE));
		midiPlayLogSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH));
		midiPlayVolumeSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOLUME_SWITCH));
		midiSynthTracksSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SYNTH_TRACKS_SWITCH));
		silentWriteSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE));
		trackWriteSwitchCB.setSelected(
				parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH));
		int sampleRateParam = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE);
		int i = 0;
		for (int sr : inputSampleRate) {
			if (sr == sampleRateParam) {
				inputSampleRateCombobox.setSelectedIndex(i);
				break;
			}
			i++;
		}
		int fftParam = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
		i = 0;
		for (int fws : fftWindowSizes) {
			if (fws == fftParam) {
				fftWindowSizeComboBox.setSelectedIndex(i);
				break;
			}
			i++;
		}
	}

	public JPanel getContentPanel() {
		return visorPanel;
	}

	@Override
	public void showException(InstrumentException exception) {
		startListeningButton.setEnabled(false);
		startFileProcessingButton.setEnabled(false);
		stopListeningButton.setEnabled(false);
		chooseFileButton.setEnabled(false);
		frameNumberInput.setEnabled(false);
		playAudioButton.setEnabled(false);
		playStreamButton.setEnabled(false);
		parametersButton.setEnabled(false);
		updateViewButton.setEnabled(false);
		synthButton.setEnabled(false);
		resetSystemButton.setForeground(Color.RED);
		updateStatusMessage("Instument System Error: " + exception.getMessage());
	}

}
