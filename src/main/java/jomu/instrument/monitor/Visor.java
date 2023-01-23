package jomu.instrument.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import be.tarsos.dsp.ui.layers.SpectrumLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.VerticalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.audio.features.ConstantQSource;
import jomu.instrument.audio.features.PitchDetectorSource;
import jomu.instrument.audio.features.SpectrogramInfo;
import jomu.instrument.cognition.cell.Cell;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.workspace.tonemap.ToneMap;

public class Visor extends JPanel implements OscilloscopeEventHandler, AudioFeatureFrameObserver {

	private static String defaultAudioFileFolder = "D:/audio";
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

	private JFrame mainframe;

	private JPanel beatsPanel;

	private InstrumentStoreService iss;
	private JTextField audioFeatureIntervalInput;
	private JTextField timeAxisOffsetInput;
	private JTextField pitchAxisOffsetInput;
	private JTextField timeAxisRangeInput;
	private JTextField pitchAxisRangeInput;
	private JComboBox toneMapViewComboBox;

	public Visor(JFrame mainframe) {
		this.mainframe = mainframe;
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.iss = Instrument.getInstance().getStorage().getInstrumentStoreService();
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
		panel.add(beatsTabbedPane, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildDiagnosticsPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane diagnosticsTabbedPane = new JTabbedPane();
		diagnosticsTabbedPane
				.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder())); // BorderFactory.createLineBorder(Color.black));
		spectrumPanel = createSpectrumPanel();
		diagnosticsTabbedPane.addTab("Spectrum", spectrumPanel);
		oscilloscopePanel = new OscilloscopePanel();
		diagnosticsTabbedPane.addTab("Oscilloscope", oscilloscopePanel);
		panel.add(diagnosticsTabbedPane, BorderLayout.CENTER);
		return panel;
	}

	public String getCurrentLocalDateTimeStamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS"));
	}

	private JPanel buildViewControlPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		JPanel graphControlPanel = new JPanel();

		toneMapViewComboBox = new JComboBox<>();

		Arrays.asList(new String[] { Cell.CellTypes.AUDIO_CQ.name(), Cell.CellTypes.AUDIO_TUNER_PEAKS.name(),
				Cell.CellTypes.AUDIO_SPECTRAL_PEAKS.name(), Cell.CellTypes.AUDIO_PITCH.name(),
				Cell.CellTypes.AUDIO_NOTATE.name(), Cell.CellTypes.AUDIO_INTEGRATE.name(),
				Cell.CellTypes.AUDIO_ONSET.name(), Cell.CellTypes.AUDIO_ONSET.name() + "_SMOOTHED",
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
					System.out.println(">>SET TM VIEW: " + value);
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
				String newValue = timeAxisOffsetInput.getText();
				timeAxisOffsetLabel.setText(String.format("Time Axis Offset ms (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
				Visor.this.resetToneMapView();
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
				String newValue = pitchAxisOffsetInput.getText();
				pitchAxisOffsetLabel.setText(String.format("Pitch Axis Offset ms (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();

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
				String newValue = timeAxisRangeInput.getText();
				timeAxisRangeLabel.setText(String.format("Time Axis Range ms (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();

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
				String newValue = pitchAxisRangeInput.getText();
				pitchAxisRangeLabel.setText(String.format("Pitch Axis Range ms (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE, newValue);
				Visor.this.toneMapView.updateAxis();
				Visor.this.chromaPreView.updateAxis();
				Visor.this.chromaPostView.updateAxis();
				Visor.this.beatsView.updateAxis();
			}
		});
		pitchAxisRangeInput
				.setText(parameterManager.getParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE));
		graphControlPanel.add(pitchAxisRangeLabel);
		graphControlPanel.add(pitchAxisRangeInput);

		panel.add(graphControlPanel, BorderLayout.CENTER);

		return panel;
	}

	private JPanel buildControlPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new EtchedBorder()));

		JPanel actionPanel = new JPanel();

		final JFileChooser fileChooser = new JFileChooser(new File(defaultAudioFileFolder));
		final JButton chooseFileButton = new JButton("Open a file");
		final JButton startFileProcessingButton = new JButton("Start File Processing");
		final JButton startListeningButton = new JButton("Start Listening");
		final JButton stopListeningButton = new JButton("Stop Listening");

		fileChooser.setSelectedFile(new File(defaultAudioFileFolder, defaultAudioFile));
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

		startFileProcessingButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					inputFile = fileChooser.getSelectedFile();
					System.out.println(inputFile.toString());
					fileName = inputFile.getAbsolutePath();
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
					String fileName = "instrument_recording.wav";
					Instrument.getInstance().getCoordinator().getHearing().startAudioLineStream(fileName);
					startListeningButton.setEnabled(false);
					startFileProcessingButton.setEnabled(false);
					stopListeningButton.setEnabled(true);
					chooseFileButton.setEnabled(false);
				} catch (LineUnavailableException | IOException e) {
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
					startFileProcessingButton.setEnabled(true);
					startListeningButton.setEnabled(true);
					stopListeningButton.setEnabled(false);
					chooseFileButton.setEnabled(true);
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		actionPanel.add(startFileProcessingButton);
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
				int noiseFloorMedianFilterLength = fftsize / 117;
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
				Integer sampleRate = value;
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE,
						Integer.toString(sampleRate));
			}
		});
		inputSampleRateCombobox.setSelectedIndex(2);
		actionPanel.add(new JLabel("Input sample rate:  "));
		actionPanel.add(inputSampleRateCombobox);

		JLabel audioFeatureIntervalLabel = new JLabel("Audio Feature Interval ms: ");
		audioFeatureIntervalInput = new JTextField(4);
		audioFeatureIntervalInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = audioFeatureIntervalInput.getText();
				audioFeatureIntervalLabel.setText(String.format("Audio Feature Interval ms (%s):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL,
						newValue);

			}
		});
		audioFeatureIntervalInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL));
		actionPanel.add(audioFeatureIntervalLabel);
		actionPanel.add(audioFeatureIntervalInput);

		final JButton parametersButton = new JButton("Parameters");

		parametersButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String s = e.getActionCommand();
				if (s.equals("Parameters")) {
					// create a dialog Box
					JDialog d = new JDialog(mainframe, "Parameters");

					JPanel dialogPanel = new JPanel(new BorderLayout());

					JPanel parameterPanel = new ParametersPanel(Visor.this.parameterManager, Visor.this.iss);
					dialogPanel.setBorder(
							BorderFactory.createCompoundBorder(new EmptyBorder(20, 20, 20, 20), new EtchedBorder()));

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
				}
			}
		});
		actionPanel.add(parametersButton);

		panel.add(actionPanel, BorderLayout.CENTER);

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

	@SuppressWarnings("unchecked")
	public void updateToneMapView(ToneMap toneMap, String toneMapViewType) {
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
					toneMapView.updateToneMap(toneMap);
				}
			}
		});
	}

	public void resetToneMapView() {
		if (toneMapViews.containsKey(currentToneMapViewType)) {
			toneMapView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			chromaPreView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			chromaPostView.renderToneMap(toneMapViews.get(currentToneMapViewType));
			beatsView.renderToneMap(toneMapViews.get(currentToneMapViewType));
		}
	}

	public void updateBeatsView(ToneMap toneMap) {
		beatsView.updateToneMap(toneMap);
	}

	public void updateChromaPreView(ToneMap toneMap) {
		chromaPreView.updateToneMap(toneMap);
	}

	public void updateChromaPostView(ToneMap toneMap) {
		chromaPostView.updateToneMap(toneMap);
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
		spectrumLayer = new SpectrumLayer(cs, 1024, 44000, Color.red);
		noiseFloorLayer = new SpectrumLayer(cs, 1024, 44000, Color.gray);

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
		this.cqPanel.repaint();
		this.pitchDetectPanel.repaint();
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
					TreeMap<Double, SpectrogramInfo> fs;
					PitchDetectorSource pds;
					pds = audioFeatureFrame.getPitchDetectorFeatures().getPds();
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
}
