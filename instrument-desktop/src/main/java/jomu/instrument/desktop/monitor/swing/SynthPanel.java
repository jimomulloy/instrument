package jomu.instrument.desktop.monitor.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
	private final static String[] styles = { "default", "ensemble", "guitar", "piano", "vocal", "birds", "beethoven" };

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

		JPanel parameterPanel = new JPanel();
		parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
		parameterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		parameterPanel.setBorder(new EmptyBorder(25, 25, 25, 5));

		parameterPanel.add(buildTrackControlPanel("1 ", "", ""));
		parameterPanel.add(buildTrackControlPanel("2 ", "", ""));
		parameterPanel.add(buildTrackControlPanel("3 ", "", ""));
		parameterPanel.add(buildTrackControlPanel("4 ", "", ""));

		Dimension minimumSize = new Dimension(500, 500);
		parameterPanel.setMinimumSize(minimumSize);
		this.add(parameterPanel, BorderLayout.CENTER);
	}

	private JPanel buildTrackControlPanel(String name, String track, String property) {

		JPanel containerPanel = new JPanel(new BorderLayout());

		BorderLayout bl = new BorderLayout();
		bl.setHgap(20);
		JPanel trackControlPanel = new JPanel(bl);

		JPanel leftPanel = new JPanel(new BorderLayout());
		JPanel centerPanel = new JPanel(new BorderLayout());

		// trackControlPanel.setLayout(new FlowLayoutackControlPanel,
		// BoxLayout.X_AXIS));

		JLabel trackLabel = new JLabel(name);
		JTextField trackInput = new JTextField(4);
		trackInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newValue = trackInput.getText();
				newValue = parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE,
						newValue);
				trackInput.setText(newValue);
			}
		});
		trackInput.setText(parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE));
		trackLabel.setPreferredSize(new Dimension(60, 30));
		trackInput.setPreferredSize(new Dimension(50, 30));
		leftPanel.add(trackLabel, BorderLayout.WEST);
		leftPanel.add(trackInput, BorderLayout.CENTER);

		JSlider volumeSlider = new JSlider(0, 1000);
		final JLabel volumeLabel = new JLabel("Volume :");
		volumeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newValue = source.getValue();
				volumeLabel.setText(String.format("Volume   (%d):", newValue));
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS,
						Integer.toString(newValue));
			}
		});
		volumeSlider
				.setValue(parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS));
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
