package jomu.instrument.monitor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.enterprise.context.ApplicationScoped;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;

import be.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;
import jomu.instrument.Organ;

@ApplicationScoped
@Component
public class Console implements Organ {

	Visor visor;
	JFrame mainFrame;
	JLabel statusLabel;
	JPanel contentPane;

	public OscilloscopeEventHandler getOscilloscopeHandler() {
		return visor;
	}

	public Visor getVisor() {
		return visor;
	}

	public void updateStatusMessage(String message) {
		statusLabel.setText(message);
	}

	@Override
	public void initialise() {
		EventQueue.invokeLater(() -> {
			buildMainFrame();
		});
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
		System.exit(0);
	}

	public JFrame getMainFrame() {
		return mainFrame;
	}

	public JLabel getStatusLabel() {
		return statusLabel;
	}

	public JPanel getContentPane() {
		return contentPane;
	}

	protected void buildMainFrame() {
		FlatMaterialDesignDarkIJTheme.setup();
		FlatLaf.setUseNativeWindowDecorations(true);
		mainFrame = new JFrame();
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.setTitle("The Instrument");

		buildMenus();

		buildContent();

		final int inset = 10;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height - inset * 2);
		mainFrame.pack();
		mainFrame.setVisible(true);
		mainFrame.setExtendedState(mainFrame.getExtendedState() | Frame.MAXIMIZED_BOTH);
	}

	private void buildContent() {

		contentPane = new JPanel();

		JPanel upperPane = new JPanel();
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
		visor = new Visor(mainFrame);
		upperPane.add(visor, BorderLayout.CENTER);

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
				stop();
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

}
