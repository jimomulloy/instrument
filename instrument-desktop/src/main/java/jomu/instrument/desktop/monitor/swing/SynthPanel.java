package jomu.instrument.desktop.monitor.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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

		Dimension minimumSize = new Dimension(500, 500);
		parameterPanel.setMinimumSize(minimumSize);
		this.add(parameterPanel, BorderLayout.CENTER);
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
