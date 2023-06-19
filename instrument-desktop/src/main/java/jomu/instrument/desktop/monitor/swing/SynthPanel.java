package jomu.instrument.desktop.monitor.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
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

public class SynthPanel extends JPanel {

	private static final Logger LOG = Logger.getLogger(SynthPanel.class.getName());

	private final static Integer[] fftSizes = { 256, 512, 1024, 2048, 4096, 8192, 16384, 22050, 32768, 65536, 131072 };
	private final static String[] styles = { "default", "ensemble", "guitar", "piano", "vocal", "birds", "beethoven",
			"folk" };

	private ParameterManager parameterManager;

	Storage storage;

	private Console console;

	private InstrumentStoreService iss;

	private JTextField synthesisChord1MeasureInput;

	private JTextField synthesisChord1PatternInput;

	private JTextField synthesisChord1SourceInput;

	private JTextField synthesisChord1OffsetInput;

	private JTextField synthesisChord2MeasureInput;

	private JTextField synthesisChord2PatternInput;

	private JTextField synthesisChord2SourceInput;

	private JTextField synthesisChord2OffsetInput;

	private JTextField synthesisBaseTimingInput;

	private JTextField synthesisBaseMeasureInput;

	private JTextField synthesisBasePatternInput;

	private JTextField synthesisBeat1PatternInput;

	private JTextField synthesisBeat1MeasureInput;

	private JTextField synthesisBeat1OffsetInput;

	private JTextField synthesisBeat1SourceInput;

	private JTextField synthesisBeat2PatternInput;

	private JTextField synthesisBeat2MeasureInput;

	private JTextField synthesisBeat2OffsetInput;

	private JTextField synthesisBeat2SourceInput;

	private JTextField synthesisBaseOctaveInput;

	private JTextField synthesisChord1OctaveInput;

	private JTextField synthesisChord2OctaveInput;

	private JTextField synthesisPad1OctaveInput;

	private JTextField synthesisPad2OctaveInput;

	private JTextField synthesisSweepRangeInput;

	private JTextField synthesisQuantizeBeatInput;

	private JTextField synthesisQuantizeRangeInput;

	private JTextField synthesisQuantizePercentInput;

	private JTextField synthesisMinTimeIncrementInput;

	private JCheckBox synthesisChordsSwitchCB;

	private JCheckBox synthesisNotesSwitchCB;

	private JCheckBox synthesisLegatoSwitchCB;

	private JCheckBox synthesisChordFirstSwitchCB;

	private JCheckBox synthesisChord1InvertSwitchCB;

	private JCheckBox synthesisChord2InvertSwitchCB;

	private JTextField synthesisChordTimingInput;

	private JTextField synthesisBeatTimingInput;

	private JTextField synthesisChord3MeasureInput;

	private JTextField synthesisChord3PatternInput;

	private JTextField synthesisChord3SourceInput;

	private JTextField synthesisChord3OffsetInput;

	private JTextField synthesisChord3OctaveInput;

	private JTextField synthesisChord4MeasureInput;

	private JTextField synthesisChord4PatternInput;

	private JTextField synthesisChord4SourceInput;

	private JTextField synthesisChord4OffsetInput;

	private JTextField synthesisChord4OctaveInput;

	private JCheckBox synthesisChord3InvertSwitchCB;

	private JCheckBox synthesisChord4InvertSwitchCB;

	private JTextField synthesisBeat3PatternInput;

	private JTextField synthesisBeat3MeasureInput;

	private JTextField synthesisBeat3OffsetInput;

	private JTextField synthesisBeat3SourceInput;

	private JTextField synthesisBeat4PatternInput;

	private JTextField synthesisBeat4MeasureInput;

	private JTextField synthesisBeat4OffsetInput;

	private JTextField synthesisBeat4SourceInput;

	public SynthPanel() {
		super(new BorderLayout());
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.console = Instrument.getInstance().getConsole();
		this.iss = Instrument.getInstance().getStorage().getInstrumentStoreService();
		this.storage = Instrument.getInstance().getStorage();

		this.setBorder(
				BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new TitledBorder("Synth Controls")));

		JPanel parameterPanel = new JPanel();
		parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
		parameterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		parameterPanel.setBorder(new EmptyBorder(25, 25, 25, 5));

		parameterPanel
				.add(buildTrackControlPanel("Voice1 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE1_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_1,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_VOICE_1));
		parameterPanel
				.add(buildTrackControlPanel("Voice2 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE2_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_2,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_VOICE_2));
		parameterPanel
				.add(buildTrackControlPanel("Voice3 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE3_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_3,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_VOICE_3));
		parameterPanel
				.add(buildTrackControlPanel("Voice4 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE4_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_4,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_VOICE_4));
		parameterPanel
				.add(buildTrackControlPanel("Chord1 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD1_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_CHORD_1,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_CHORD_1));
		parameterPanel
				.add(buildTrackControlPanel("Chord2 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD2_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_CHORD_2,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_CHORD_2));
		parameterPanel
				.add(buildTrackControlPanel("Pad1 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD1_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_PAD_1,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_PAD_1));
		parameterPanel
				.add(buildTrackControlPanel("Pad2 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD2_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_PAD_2,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_PAD_2));
		parameterPanel
				.add(buildTrackControlPanel("Beat1 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT1_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_1,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BEAT_1));
		parameterPanel
				.add(buildTrackControlPanel("Beat2 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT2_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_2,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BEAT_2));
		parameterPanel
				.add(buildTrackControlPanel("Beat3 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT3_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_3,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BEAT_3));
		parameterPanel
				.add(buildTrackControlPanel("Beat4 ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT4_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_4,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BEAT_4));
		parameterPanel
				.add(buildTrackControlPanel("Base ", InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BASE_SWITCH,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BASE_1,
						InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BASE_1));

		parameterPanel.add(synthTuningPanel());

		Dimension minimumSize = new Dimension(500, 500);
		parameterPanel.setMinimumSize(minimumSize);
		this.add(parameterPanel, BorderLayout.CENTER);
	}

	private Component synthTuningPanel() {
		JPanel tuningPanel = new JPanel();
		tuningPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		tuningPanel.setLayout(new GridLayout(0, 6));
		tuningPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(25, 25, 25, 5), new EtchedBorder()));

		synthesisChordsSwitchCB = new JCheckBox("synthesisChordsSwitchCB");
		synthesisChordsSwitchCB.setText("Synthesis Fill Chords");
		synthesisChordsSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_CHORDS_SWITCH,
						Boolean.toString(newValue));
			}
		});

		synthesisChordsSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_CHORDS_SWITCH));
		tuningPanel.add(synthesisChordsSwitchCB);

		synthesisNotesSwitchCB = new JCheckBox("synthesisNotesSwitchCB");
		synthesisNotesSwitchCB.setText("Synthesis Fill Notes");
		synthesisNotesSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_NOTES_SWITCH,
						Boolean.toString(newValue));
			}
		});

		synthesisNotesSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_NOTES_SWITCH));
		tuningPanel.add(synthesisNotesSwitchCB);

		synthesisLegatoSwitchCB = new JCheckBox("synthesisLegatoSwitchCB");
		synthesisLegatoSwitchCB.setText("Synthesis Fill Legato");
		synthesisLegatoSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_LEGATO_SWITCH,
						Boolean.toString(newValue));
			}
		});

		synthesisLegatoSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_LEGATO_SWITCH));
		tuningPanel.add(synthesisLegatoSwitchCB);

		synthesisChordFirstSwitchCB = new JCheckBox("synthesisChordFirstSwitchCB");
		synthesisChordFirstSwitchCB.setText("Synthesis Chord First");
		synthesisChordFirstSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD_FIRST_SWITCH,
						Boolean.toString(newValue));
			}
		});

		synthesisChordFirstSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD_FIRST_SWITCH));
		tuningPanel.add(synthesisChordFirstSwitchCB);

		synthesisChord1InvertSwitchCB = new JCheckBox("synthesisChord1InvertSwitchCB");
		synthesisChord1InvertSwitchCB.setText("Synthesis Chord1 Invert");
		synthesisChord1InvertSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_INVERT,
						Boolean.toString(newValue));
			}
		});

		synthesisChord1InvertSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_INVERT));
		tuningPanel.add(synthesisChord1InvertSwitchCB);

		synthesisChord2InvertSwitchCB = new JCheckBox("synthesisChord2InvertSwitchCB");
		synthesisChord2InvertSwitchCB.setText("Synthesis Chord2 Invert");
		synthesisChord2InvertSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_INVERT,
						Boolean.toString(newValue));
			}
		});

		synthesisChord2InvertSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_INVERT));
		tuningPanel.add(synthesisChord2InvertSwitchCB);
		

		synthesisChord3InvertSwitchCB = new JCheckBox("synthesisChord3InvertSwitchCB");
		synthesisChord3InvertSwitchCB.setText("Synthesis Chord3 Invert");
		synthesisChord3InvertSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_INVERT,
						Boolean.toString(newValue));
			}
		});

		synthesisChord3InvertSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_INVERT));
		tuningPanel.add(synthesisChord3InvertSwitchCB);

		synthesisChord4InvertSwitchCB = new JCheckBox("synthesisChord4InvertSwitchCB");
		synthesisChord4InvertSwitchCB.setText("Synthesis Chord4 Invert");
		synthesisChord4InvertSwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_INVERT,
						Boolean.toString(newValue));
			}
		});

		synthesisChord4InvertSwitchCB.setSelected(parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_INVERT));
		tuningPanel.add(synthesisChord4InvertSwitchCB);


		JLabel synthesisChordTimingLabel = new JLabel("Synthesis Chord Timing: ");
		synthesisChordTimingInput = new JTextField(4);
		synthesisChordTimingInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChordTimingInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD_TIMING, newValue);
				synthesisChordTimingLabel.setText(String.format("Synthesis Chord Timing (%s):", newValue));
				synthesisChordTimingInput.setText(newValue);
			}
		});
		synthesisChordTimingInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD_TIMING));
		tuningPanel.add(synthesisChordTimingLabel);
		tuningPanel.add(synthesisChordTimingInput);

		JLabel synthesisBeatTimingLabel = new JLabel("Synthesis Beat Timing: ");
		synthesisBeatTimingInput = new JTextField(4);
		synthesisBeatTimingInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeatTimingInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT_TIMING, newValue);
				synthesisBeatTimingLabel.setText(String.format("Synthesis Beat Timing  (%s):", newValue));
				synthesisBeatTimingInput.setText(newValue);
			}
		});
		synthesisBeatTimingInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT_TIMING));
		tuningPanel.add(synthesisBeatTimingLabel);
		tuningPanel.add(synthesisBeatTimingInput);

		tuningPanel.add(new JLabel(""));
		tuningPanel.add(new JLabel(""));

		JLabel synthesisChord1MeasureLabel = new JLabel("Synthesis Chord1 Measure: ");
		synthesisChord1MeasureInput = new JTextField(4);
		synthesisChord1MeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord1MeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_MEASURE, newValue);
				synthesisChord1MeasureLabel.setText(String.format("Synthesis Chord1 Measure (%s):", newValue));
				synthesisChord1MeasureInput.setText(newValue);
			}
		});
		synthesisChord1MeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_MEASURE));
		tuningPanel.add(synthesisChord1MeasureLabel);
		tuningPanel.add(synthesisChord1MeasureInput);

		JLabel synthesisChord1PatternLabel = new JLabel("Synthesis Chord1 Pattern: ");
		synthesisChord1PatternInput = new JTextField(4);
		synthesisChord1PatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord1PatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_PATTERN, newValue);
				synthesisChord1PatternLabel.setText(String.format("Synthesis Chord1 Pattern  (%s):", newValue));
				synthesisChord1PatternInput.setText(newValue);
			}
		});
		synthesisChord1PatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_PATTERN));
		tuningPanel.add(synthesisChord1PatternLabel);
		tuningPanel.add(synthesisChord1PatternInput);

		JLabel synthesisChord1SourceLabel = new JLabel("Synthesis Chord1 Source: ");
		synthesisChord1SourceInput = new JTextField(4);
		synthesisChord1SourceInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord1SourceInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_SOURCE, newValue);
				synthesisChord1SourceLabel.setText(String.format("Synthesis Chord1 Source  (%s):", newValue));
				synthesisChord1SourceInput.setText(newValue);
			}
		});
		synthesisChord1SourceInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_SOURCE));
		tuningPanel.add(synthesisChord1SourceLabel);
		tuningPanel.add(synthesisChord1SourceInput);

		JLabel synthesisChord1OffsetLabel = new JLabel("Synthesis Chord1 Offset: ");
		synthesisChord1OffsetInput = new JTextField(4);
		synthesisChord1OffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord1OffsetInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_OFFSET, newValue);
				synthesisChord1OffsetLabel.setText(String.format("Synthesis Chord1 Offset  (%s):", newValue));
				synthesisChord1OffsetInput.setText(newValue);
			}
		});
		synthesisChord1OffsetInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_OFFSET));
		tuningPanel.add(synthesisChord1OffsetLabel);
		tuningPanel.add(synthesisChord1OffsetInput);

		JLabel synthesisChord1OctaveLabel = new JLabel("Synthesis Chord1 Octave: ");
		synthesisChord1OctaveInput = new JTextField(4);
		synthesisChord1OctaveInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord1OctaveInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_OCTAVE, newValue);
				synthesisChord1OctaveLabel.setText(String.format("Synthesis Chord1 Octave  (%s):", newValue));
				synthesisChord1OctaveInput.setText(newValue);
			}
		});
		synthesisChord1OctaveInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_OCTAVE));
		tuningPanel.add(synthesisChord1OctaveLabel);
		tuningPanel.add(synthesisChord1OctaveInput);

		
		JLabel synthesisChord2MeasureLabel = new JLabel("Synthesis Chord2 Measure: ");
		synthesisChord2MeasureInput = new JTextField(4);
		synthesisChord2MeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord2MeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_MEASURE, newValue);
				synthesisChord2MeasureLabel.setText(String.format("Synthesis Chord2 Measure (%s):", newValue));
				synthesisChord2MeasureInput.setText(newValue);
			}
		});
		synthesisChord2MeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_MEASURE));
		tuningPanel.add(synthesisChord2MeasureLabel);
		tuningPanel.add(synthesisChord2MeasureInput);

		JLabel synthesisChord2PatternLabel = new JLabel("Synthesis Chord2 Pattern: ");
		synthesisChord2PatternInput = new JTextField(4);
		synthesisChord2PatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord2PatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_PATTERN, newValue);
				synthesisChord2PatternLabel.setText(String.format("Synthesis Chord2 Pattern  (%s):", newValue));
				synthesisChord2PatternInput.setText(newValue);
			}
		});
		synthesisChord2PatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_PATTERN));
		tuningPanel.add(synthesisChord2PatternLabel);
		tuningPanel.add(synthesisChord2PatternInput);

		JLabel synthesisChord2SourceLabel = new JLabel("Synthesis Chord2 Source: ");
		synthesisChord2SourceInput = new JTextField(4);
		synthesisChord2SourceInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord2SourceInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_SOURCE, newValue);
				synthesisChord2SourceLabel.setText(String.format("Synthesis Chord2 Source  (%s):", newValue));
				synthesisChord2SourceInput.setText(newValue);
			}
		});
		synthesisChord2SourceInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_SOURCE));
		tuningPanel.add(synthesisChord2SourceLabel);
		tuningPanel.add(synthesisChord2SourceInput);

		JLabel synthesisChord2OffsetLabel = new JLabel("Synthesis Chord2 Offset: ");
		synthesisChord2OffsetInput = new JTextField(4);
		synthesisChord2OffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord2OffsetInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_OFFSET, newValue);
				synthesisChord2OffsetLabel.setText(String.format("Synthesis Chord2 Offset  (%s):", newValue));
				synthesisChord2OffsetInput.setText(newValue);
			}
		});
		synthesisChord2OffsetInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_OFFSET));
		tuningPanel.add(synthesisChord2OffsetLabel);
		tuningPanel.add(synthesisChord2OffsetInput);

		JLabel synthesisChord2OctaveLabel = new JLabel("Synthesis Chord2 Octave: ");
		synthesisChord2OctaveInput = new JTextField(4);
		synthesisChord2OctaveInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord1OctaveInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_OCTAVE, newValue);
				synthesisChord2OctaveLabel.setText(String.format("Synthesis Chord2 Octave  (%s):", newValue));
				synthesisChord2OctaveInput.setText(newValue);
			}
		});
		synthesisChord2OctaveInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD2_OCTAVE));
		tuningPanel.add(synthesisChord2OctaveLabel);
		tuningPanel.add(synthesisChord2OctaveInput);
		
			
		JLabel synthesisChord3MeasureLabel = new JLabel("Synthesis Chord3 Measure: ");
		synthesisChord3MeasureInput = new JTextField(4);
		synthesisChord3MeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord3MeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_MEASURE, newValue);
				synthesisChord3MeasureLabel.setText(String.format("Synthesis Chord3 Measure (%s):", newValue));
				synthesisChord3MeasureInput.setText(newValue);
			}
		});
		synthesisChord3MeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_MEASURE));
		tuningPanel.add(synthesisChord3MeasureLabel);
		tuningPanel.add(synthesisChord3MeasureInput);

		JLabel synthesisChord3PatternLabel = new JLabel("Synthesis Chord3 Pattern: ");
		synthesisChord3PatternInput = new JTextField(4);
		synthesisChord3PatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord3PatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_PATTERN, newValue);
				synthesisChord3PatternLabel.setText(String.format("Synthesis Chord3 Pattern  (%s):", newValue));
				synthesisChord3PatternInput.setText(newValue);
			}
		});
		synthesisChord3PatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_PATTERN));
		tuningPanel.add(synthesisChord3PatternLabel);
		tuningPanel.add(synthesisChord3PatternInput);

		JLabel synthesisChord3SourceLabel = new JLabel("Synthesis Chord3 Source: ");
		synthesisChord3SourceInput = new JTextField(4);
		synthesisChord3SourceInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord3SourceInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_SOURCE, newValue);
				synthesisChord3SourceLabel.setText(String.format("Synthesis Chord3 Source  (%s):", newValue));
				synthesisChord3SourceInput.setText(newValue);
			}
		});
		synthesisChord3SourceInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_SOURCE));
		tuningPanel.add(synthesisChord3SourceLabel);
		tuningPanel.add(synthesisChord3SourceInput);

		JLabel synthesisChord3OffsetLabel = new JLabel("Synthesis Chord3 Offset: ");
		synthesisChord3OffsetInput = new JTextField(4);
		synthesisChord3OffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord3OffsetInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_OFFSET, newValue);
				synthesisChord3OffsetLabel.setText(String.format("Synthesis Chord3 Offset  (%s):", newValue));
				synthesisChord3OffsetInput.setText(newValue);
			}
		});
		synthesisChord3OffsetInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_OFFSET));
		tuningPanel.add(synthesisChord3OffsetLabel);
		tuningPanel.add(synthesisChord3OffsetInput);

		JLabel synthesisChord3OctaveLabel = new JLabel("Synthesis Chord3 Octave: ");
		synthesisChord3OctaveInput = new JTextField(4);
		synthesisChord3OctaveInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord1OctaveInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_OCTAVE, newValue);
				synthesisChord3OctaveLabel.setText(String.format("Synthesis Chord3 Octave  (%s):", newValue));
				synthesisChord3OctaveInput.setText(newValue);
			}
		});
		synthesisChord3OctaveInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD3_OCTAVE));
		tuningPanel.add(synthesisChord3OctaveLabel);
		tuningPanel.add(synthesisChord3OctaveInput);
		
			
		JLabel synthesisChord4MeasureLabel = new JLabel("Synthesis Chord4 Measure: ");
		synthesisChord4MeasureInput = new JTextField(4);
		synthesisChord4MeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord4MeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_MEASURE, newValue);
				synthesisChord4MeasureLabel.setText(String.format("Synthesis Chord4 Measure (%s):", newValue));
				synthesisChord4MeasureInput.setText(newValue);
			}
		});
		synthesisChord4MeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_MEASURE));
		tuningPanel.add(synthesisChord4MeasureLabel);
		tuningPanel.add(synthesisChord4MeasureInput);
	
		JLabel synthesisChord4PatternLabel = new JLabel("Synthesis Chord4 Pattern: ");
		synthesisChord4PatternInput = new JTextField(4);
		synthesisChord4PatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord4PatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_PATTERN, newValue);
				synthesisChord4PatternLabel.setText(String.format("Synthesis Chord4 Pattern  (%s):", newValue));
				synthesisChord4PatternInput.setText(newValue);
			}
		});
		synthesisChord4PatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_PATTERN));
		tuningPanel.add(synthesisChord4PatternLabel);
		tuningPanel.add(synthesisChord4PatternInput);
	
		JLabel synthesisChord4SourceLabel = new JLabel("Synthesis Chord4 Source: ");
		synthesisChord4SourceInput = new JTextField(4);
		synthesisChord4SourceInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord4SourceInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_SOURCE, newValue);
				synthesisChord4SourceLabel.setText(String.format("Synthesis Chord4 Source  (%s):", newValue));
				synthesisChord4SourceInput.setText(newValue);
			}
		});
		synthesisChord4SourceInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_SOURCE));
		tuningPanel.add(synthesisChord4SourceLabel);
		tuningPanel.add(synthesisChord4SourceInput);
	
		JLabel synthesisChord4OffsetLabel = new JLabel("Synthesis Chord4 Offset: ");
		synthesisChord4OffsetInput = new JTextField(4);
		synthesisChord4OffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord4OffsetInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_OFFSET, newValue);
				synthesisChord4OffsetLabel.setText(String.format("Synthesis Chord4 Offset  (%s):", newValue));
				synthesisChord4OffsetInput.setText(newValue);
			}
		});
		synthesisChord4OffsetInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_OFFSET));
		tuningPanel.add(synthesisChord4OffsetLabel);
		tuningPanel.add(synthesisChord4OffsetInput);
	
		JLabel synthesisChord4OctaveLabel = new JLabel("Synthesis Chord4 Octave: ");
		synthesisChord4OctaveInput = new JTextField(4);
		synthesisChord4OctaveInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisChord1OctaveInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_OCTAVE, newValue);
				synthesisChord4OctaveLabel.setText(String.format("Synthesis Chord4 Octave  (%s):", newValue));
				synthesisChord4OctaveInput.setText(newValue);
			}
		});
		synthesisChord4OctaveInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD4_OCTAVE));
		tuningPanel.add(synthesisChord4OctaveLabel);
		tuningPanel.add(synthesisChord4OctaveInput);
		
								
		JLabel synthesisBaseTimingLabel = new JLabel("Synthesis Base Timing: ");
		synthesisBaseTimingInput = new JTextField(4);
		synthesisBaseTimingInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBaseTimingInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_TIMING, newValue);
				synthesisBaseTimingLabel.setText(String.format("Synthesis Base Timing (%s):", newValue));
				synthesisBaseTimingInput.setText(newValue);
			}
		});
		synthesisBaseTimingInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_TIMING));
		tuningPanel.add(synthesisBaseTimingLabel);
		tuningPanel.add(synthesisBaseTimingInput);

		JLabel synthesisBaseMeasureLabel = new JLabel("Synthesis Base Measure: ");
		synthesisBaseMeasureInput = new JTextField(4);
		synthesisBaseMeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBaseMeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_MEASURE, newValue);
				synthesisBaseMeasureLabel.setText(String.format("Synthesis Base Measure (%s):", newValue));
				synthesisBaseMeasureInput.setText(newValue);
			}
		});
		synthesisBaseMeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_MEASURE));
		tuningPanel.add(synthesisBaseMeasureLabel);
		tuningPanel.add(synthesisBaseMeasureInput);

		JLabel synthesisBasePatternLabel = new JLabel("Synthesis Base Pattern: ");
		synthesisBasePatternInput = new JTextField(4);
		synthesisBasePatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBasePatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_PATTERN, newValue);
				synthesisBasePatternLabel.setText(String.format("Synthesis Base Pattern  (%s):", newValue));
				synthesisBasePatternInput.setText(newValue);
			}
		});
		synthesisBasePatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_PATTERN));
		tuningPanel.add(synthesisBasePatternLabel);
		tuningPanel.add(synthesisBasePatternInput);

		JLabel synthesisBeat1PatternLabel = new JLabel("Synthesis Beat1 Pattern: ");
		synthesisBeat1PatternInput = new JTextField(4);
		synthesisBeat1PatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat1PatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_PATTERN, newValue);
				LOG.severe(">>BEAT value: " + newValue);
				synthesisBeat1PatternLabel.setText(String.format("Synthesis Beat1 Pattern  (%s):", newValue));
				synthesisBeat1PatternInput.setText(newValue);
			}
		});
		synthesisBeat1PatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_PATTERN));
		tuningPanel.add(synthesisBeat1PatternLabel);
		tuningPanel.add(synthesisBeat1PatternInput);

		JLabel synthesisBeat1MeasureLabel = new JLabel("Synthesis Beat1 Measure: ");
		synthesisBeat1MeasureInput = new JTextField(4);
		synthesisBeat1MeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat1MeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_MEASURE, newValue);
				synthesisBeat1MeasureLabel.setText(String.format("Synthesis Beat1 Measure (%s):", newValue));
				synthesisBeat1MeasureInput.setText(newValue);
			}
		});
		synthesisBeat1MeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_MEASURE));
		tuningPanel.add(synthesisBeat1MeasureLabel);
		tuningPanel.add(synthesisBeat1MeasureInput);

		JLabel synthesisBeat1OffsetLabel = new JLabel("Synthesis Beat1 Offset: ");
		synthesisBeat1OffsetInput = new JTextField(4);
		synthesisBeat1OffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat1OffsetInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_OFFSET, newValue);
				synthesisBeat1OffsetLabel.setText(String.format("Synthesis Beat1 Offset (%s):", newValue));
				synthesisBeat1OffsetInput.setText(newValue);
			}
		});
		synthesisBeat1OffsetInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_OFFSET));
		tuningPanel.add(synthesisBeat1OffsetLabel);
		tuningPanel.add(synthesisBeat1OffsetInput);

		JLabel synthesisBeat1SourceLabel = new JLabel("Synthesis Beat1 Source: ");
		synthesisBeat1SourceInput = new JTextField(4);
		synthesisBeat1SourceInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat1SourceInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_SOURCE, newValue);
				synthesisBeat1SourceLabel.setText(String.format("Synthesis Beat1 Source (%s):", newValue));
				synthesisBeat1SourceInput.setText(newValue);
			}
		});
		synthesisBeat1SourceInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT1_SOURCE));
		tuningPanel.add(synthesisBeat1SourceLabel);
		tuningPanel.add(synthesisBeat1SourceInput);

		JLabel synthesisBeat2PatternLabel = new JLabel("Synthesis Beat2 Pattern: ");
		synthesisBeat2PatternInput = new JTextField(4);
		synthesisBeat2PatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat2PatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_PATTERN, newValue);
				LOG.severe(">>BEAT value: " + newValue);
				synthesisBeat2PatternLabel.setText(String.format("Synthesis Beat2 Pattern  (%s):", newValue));
				synthesisBeat2PatternInput.setText(newValue);
			}
		});
		synthesisBeat2PatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_PATTERN));
		tuningPanel.add(synthesisBeat2PatternLabel);
		tuningPanel.add(synthesisBeat2PatternInput);

		JLabel synthesisBeat2MeasureLabel = new JLabel("Synthesis Beat2 Measure: ");
		synthesisBeat2MeasureInput = new JTextField(4);
		synthesisBeat2MeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat2MeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_MEASURE, newValue);
				synthesisBeat2MeasureLabel.setText(String.format("Synthesis Beat2 Measure (%s):", newValue));
				synthesisBeat2MeasureInput.setText(newValue);
			}
		});
		synthesisBeat2MeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_MEASURE));
		tuningPanel.add(synthesisBeat2MeasureLabel);
		tuningPanel.add(synthesisBeat2MeasureInput);

		JLabel synthesisBeat2OffsetLabel = new JLabel("Synthesis Beat2 Offset: ");
		synthesisBeat2OffsetInput = new JTextField(4);
		synthesisBeat2OffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat2OffsetInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_OFFSET, newValue);
				synthesisBeat2OffsetLabel.setText(String.format("Synthesis Beat2 Offset (%s):", newValue));
				synthesisBeat2OffsetInput.setText(newValue);
			}
		});
		synthesisBeat2OffsetInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_OFFSET));
		tuningPanel.add(synthesisBeat2OffsetLabel);
		tuningPanel.add(synthesisBeat2OffsetInput);

		JLabel synthesisBeat2SourceLabel = new JLabel("Synthesis Beat2 Source: ");
		synthesisBeat2SourceInput = new JTextField(4);
		synthesisBeat2SourceInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat2SourceInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_SOURCE, newValue);
				synthesisBeat2SourceLabel.setText(String.format("Synthesis Beat2 Source (%s):", newValue));
				synthesisBeat2SourceInput.setText(newValue);
			}
		});
		synthesisBeat2SourceInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT2_SOURCE));
		tuningPanel.add(synthesisBeat2SourceLabel);
		tuningPanel.add(synthesisBeat2SourceInput);
		
		
		

		JLabel synthesisBeat3PatternLabel = new JLabel("Synthesis Beat3 Pattern: ");
		synthesisBeat3PatternInput = new JTextField(4);
		synthesisBeat3PatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat3PatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_PATTERN, newValue);
				LOG.severe(">>BEAT value: " + newValue);
				synthesisBeat3PatternLabel.setText(String.format("Synthesis Beat3 Pattern  (%s):", newValue));
				synthesisBeat3PatternInput.setText(newValue);
			}
		});
		synthesisBeat3PatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_PATTERN));
		tuningPanel.add(synthesisBeat3PatternLabel);
		tuningPanel.add(synthesisBeat3PatternInput);

		JLabel synthesisBeat3MeasureLabel = new JLabel("Synthesis Beat3 Measure: ");
		synthesisBeat3MeasureInput = new JTextField(4);
		synthesisBeat3MeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat3MeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_MEASURE, newValue);
				synthesisBeat3MeasureLabel.setText(String.format("Synthesis Beat3 Measure (%s):", newValue));
				synthesisBeat3MeasureInput.setText(newValue);
			}
		});
		synthesisBeat3MeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_MEASURE));
		tuningPanel.add(synthesisBeat3MeasureLabel);
		tuningPanel.add(synthesisBeat3MeasureInput);

		JLabel synthesisBeat3OffsetLabel = new JLabel("Synthesis Beat3 Offset: ");
		synthesisBeat3OffsetInput = new JTextField(4);
		synthesisBeat3OffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat3OffsetInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_OFFSET, newValue);
				synthesisBeat3OffsetLabel.setText(String.format("Synthesis Beat3 Offset (%s):", newValue));
				synthesisBeat3OffsetInput.setText(newValue);
			}
		});
		synthesisBeat3OffsetInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_OFFSET));
		tuningPanel.add(synthesisBeat3OffsetLabel);
		tuningPanel.add(synthesisBeat3OffsetInput);

		JLabel synthesisBeat3SourceLabel = new JLabel("Synthesis Beat3 Source: ");
		synthesisBeat3SourceInput = new JTextField(4);
		synthesisBeat3SourceInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat3SourceInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_SOURCE, newValue);
				synthesisBeat3SourceLabel.setText(String.format("Synthesis Beat3 Source (%s):", newValue));
				synthesisBeat3SourceInput.setText(newValue);
			}
		});
		synthesisBeat3SourceInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT3_SOURCE));
		tuningPanel.add(synthesisBeat3SourceLabel);
		tuningPanel.add(synthesisBeat3SourceInput);
		
		
		JLabel synthesisBeat4PatternLabel = new JLabel("Synthesis Beat4 Pattern: ");
		synthesisBeat4PatternInput = new JTextField(4);
		synthesisBeat4PatternInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat4PatternInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_PATTERN, newValue);
				LOG.severe(">>BEAT value: " + newValue);
				synthesisBeat4PatternLabel.setText(String.format("Synthesis Beat4 Pattern  (%s):", newValue));
				synthesisBeat4PatternInput.setText(newValue);
			}
		});
		synthesisBeat4PatternInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_PATTERN));
		tuningPanel.add(synthesisBeat4PatternLabel);
		tuningPanel.add(synthesisBeat4PatternInput);

		JLabel synthesisBeat4MeasureLabel = new JLabel("Synthesis Beat4 Measure: ");
		synthesisBeat4MeasureInput = new JTextField(4);
		synthesisBeat4MeasureInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat4MeasureInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_MEASURE, newValue);
				synthesisBeat4MeasureLabel.setText(String.format("Synthesis Beat4 Measure (%s):", newValue));
				synthesisBeat4MeasureInput.setText(newValue);
			}
		});
		synthesisBeat4MeasureInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_MEASURE));
		tuningPanel.add(synthesisBeat4MeasureLabel);
		tuningPanel.add(synthesisBeat4MeasureInput);

		JLabel synthesisBeat4OffsetLabel = new JLabel("Synthesis Beat4 Offset: ");
		synthesisBeat4OffsetInput = new JTextField(4);
		synthesisBeat4OffsetInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat4OffsetInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_OFFSET, newValue);
				synthesisBeat4OffsetLabel.setText(String.format("Synthesis Beat4 Offset (%s):", newValue));
				synthesisBeat4OffsetInput.setText(newValue);
			}
		});
		synthesisBeat4OffsetInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_OFFSET));
		tuningPanel.add(synthesisBeat4OffsetLabel);
		tuningPanel.add(synthesisBeat4OffsetInput);

		JLabel synthesisBeat4SourceLabel = new JLabel("Synthesis Beat4 Source: ");
		synthesisBeat4SourceInput = new JTextField(4);
		synthesisBeat4SourceInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBeat4SourceInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_SOURCE, newValue);
				synthesisBeat4SourceLabel.setText(String.format("Synthesis Beat4 Source (%s):", newValue));
				synthesisBeat4SourceInput.setText(newValue);
			}
		});
		synthesisBeat4SourceInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT4_SOURCE));
		tuningPanel.add(synthesisBeat4SourceLabel);
		tuningPanel.add(synthesisBeat4SourceInput);

		
		JLabel synthesisBaseOctaveLabel = new JLabel("Synthesis Base Octave: ");
		synthesisBaseOctaveInput = new JTextField(4);
		synthesisBaseOctaveInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisBaseOctaveInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_OCTAVE, newValue);
				synthesisBaseOctaveLabel.setText(String.format("Synthesis Base Octave  (%s):", newValue));
				synthesisBaseOctaveInput.setText(newValue);
			}
		});
		synthesisBaseOctaveInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BASE_OCTAVE));
		tuningPanel.add(synthesisBaseOctaveLabel);
		tuningPanel.add(synthesisBaseOctaveInput);

		JLabel synthesisPad1OctaveLabel = new JLabel("Synthesis Pad1 Octave: ");
		synthesisPad1OctaveInput = new JTextField(4);
		synthesisPad1OctaveInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisPad1OctaveInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_PAD1_OCTAVE, newValue);
				synthesisPad1OctaveLabel.setText(String.format("Synthesis Pad1 Octave  (%s):", newValue));
				synthesisPad1OctaveInput.setText(newValue);
			}
		});
		synthesisPad1OctaveInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_PAD1_OCTAVE));
		tuningPanel.add(synthesisPad1OctaveLabel);
		tuningPanel.add(synthesisPad1OctaveInput);

		JLabel synthesisPad2OctaveLabel = new JLabel("Synthesis Pad2 Octave: ");
		synthesisPad2OctaveInput = new JTextField(4);
		synthesisPad2OctaveInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisPad2OctaveInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_PAD2_OCTAVE, newValue);
				synthesisPad2OctaveLabel.setText(String.format("Synthesis Pad2 Octave  (%s):", newValue));
				synthesisPad2OctaveInput.setText(newValue);
			}
		});
		synthesisPad2OctaveInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_PAD2_OCTAVE));
		tuningPanel.add(synthesisPad2OctaveLabel);
		tuningPanel.add(synthesisPad2OctaveInput);

		JLabel synthesisSweepRangeLabel = new JLabel("Synthesis Sweep Range: ");
		synthesisSweepRangeInput = new JTextField(4);
		synthesisSweepRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisSweepRangeInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_SWEEP_RANGE, newValue);
				synthesisSweepRangeLabel.setText(String.format("Synthesis Sweep Range  (%s):", newValue));
				synthesisSweepRangeInput.setText(newValue);
			}
		});
		synthesisSweepRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_SWEEP_RANGE));
		tuningPanel.add(synthesisSweepRangeLabel);
		tuningPanel.add(synthesisSweepRangeInput);

		JLabel synthesisQuantizeBeatLabel = new JLabel("Synthesis Quantize Beat: ");
		synthesisQuantizeBeatInput = new JTextField(4);
		synthesisQuantizeBeatInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisQuantizeBeatInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_BEAT, newValue);
				synthesisQuantizeBeatLabel.setText(String.format("Synthesis Quantize Beat  (%s):", newValue));
				synthesisQuantizeBeatInput.setText(newValue);
			}
		});
		synthesisQuantizeBeatInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_BEAT));
		tuningPanel.add(synthesisQuantizeBeatLabel);
		tuningPanel.add(synthesisQuantizeBeatInput);

		JLabel synthesisQuantizeRangeLabel = new JLabel("Synthesis Quantize Range: ");
		synthesisQuantizeRangeInput = new JTextField(4);
		synthesisQuantizeRangeInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisQuantizeRangeInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE, newValue);
				synthesisQuantizeRangeLabel.setText(String.format("Synthesis Quantize Range  (%s):", newValue));
				synthesisQuantizeRangeInput.setText(newValue);
			}
		});
		synthesisQuantizeRangeInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE));
		tuningPanel.add(synthesisQuantizeRangeLabel);
		tuningPanel.add(synthesisQuantizeRangeInput);

		JLabel synthesisQuantizePercentLabel = new JLabel("Synthesis Quantize Pecent: ");
		synthesisQuantizePercentInput = new JTextField(4);
		synthesisQuantizePercentInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisQuantizePercentInput.getText();
				newValue = parameterManager
						.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT, newValue);
				synthesisQuantizePercentLabel.setText(String.format("Synthesis Quantize Pecent  (%s):", newValue));
				synthesisQuantizePercentInput.setText(newValue);
			}
		});
		synthesisQuantizePercentInput.setText(
				parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT));
		tuningPanel.add(synthesisQuantizePercentLabel);
		tuningPanel.add(synthesisQuantizePercentInput);

		JLabel synthesisMinTimeIncrementLabel = new JLabel("Synthesis Min Time Increment: ");
		synthesisMinTimeIncrementInput = new JTextField(4);
		synthesisMinTimeIncrementInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = synthesisMinTimeIncrementInput.getText();
				newValue = parameterManager.setParameter(
						InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_MIN_TIME_INCREMENT, newValue);
				synthesisMinTimeIncrementLabel.setText(String.format("Synthesis Min Time Increment  (%s):", newValue));
				synthesisMinTimeIncrementInput.setText(newValue);
			}
		});
		synthesisMinTimeIncrementInput.setText(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_MIN_TIME_INCREMENT));
		tuningPanel.add(synthesisMinTimeIncrementLabel);
		tuningPanel.add(synthesisMinTimeIncrementInput);

		return tuningPanel;
	}

	private JPanel buildTrackControlPanel(String name, String playSwitchParam, String trackParam, String volumeParam) {

		JPanel containerPanel = new JPanel(new BorderLayout());

		BorderLayout bl = new BorderLayout();
		bl.setHgap(20);
		JPanel trackControlPanel = new JPanel(bl);

		JPanel leftPanel = new JPanel(new BorderLayout());
		JPanel centerPanel = new JPanel(new BorderLayout());

		JCheckBox midiPlaySwitchCB = new JCheckBox(name + "SwitchCB");
		midiPlaySwitchCB.setText(name);
		midiPlaySwitchCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				boolean newValue = cb.isSelected();
				parameterManager.setParameter(playSwitchParam, Boolean.toString(newValue));
			}
		});

		midiPlaySwitchCB.setSelected(parameterManager.getBooleanParameter(playSwitchParam));
		midiPlaySwitchCB.setPreferredSize(new Dimension(100, 30));

		JTextField trackInput = new JTextField(4);
		trackInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = trackInput.getText();
				newValue = parameterManager.setParameter(trackParam, newValue);
				trackInput.setText(newValue);
			}
		});
		trackInput.setText(parameterManager.getParameter(trackParam));
		trackInput.setPreferredSize(new Dimension(40, 30));
		leftPanel.add(midiPlaySwitchCB, BorderLayout.WEST);
		leftPanel.add(trackInput, BorderLayout.CENTER);

		JSlider volumeSlider = new JSlider(0, 1000);
		final JLabel volumeLabel = new JLabel("Volume :");
		volumeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				volumeLabel.setText(String.format("Volume   (%d):", newValue));
				parameterManager.setParameter(volumeParam, Integer.toString(newValue));
			}
		});
		volumeSlider.setValue(parameterManager.getIntParameter(volumeParam));
		volumeLabel.setPreferredSize(new Dimension(100, 30));
		volumeSlider.setPreferredSize(new Dimension(400, 30));

		centerPanel.add(volumeLabel, BorderLayout.WEST);
		centerPanel.add(volumeSlider, BorderLayout.CENTER);

		trackControlPanel.add(leftPanel, BorderLayout.WEST);
		trackControlPanel.add(centerPanel, BorderLayout.CENTER);

		containerPanel.add(trackControlPanel, BorderLayout.NORTH);
		return containerPanel;

	}

	public void updateParameters() {

	}

}
