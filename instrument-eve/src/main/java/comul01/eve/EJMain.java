package comul01.eve;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;


/**
  * This is the main container frame for the ToneMap Application
  * This contains the main frame GUI panels and manages certain
  * generic functionality including Status reporting, Reset and
  * certain Menu functions including Help and Quit.
  *
  * @version 1.0 01/01/01
  * @author Jim O'Mulloy
  */
public class EJMain implements EJConstants , Serializable {

	static boolean JNIStatus = false;

	static final String ABOUTMSG = "JAVED \n \nWritten by Jim O'Mulloy \n";

	static {
		try {
			System.loadLibrary("WavletJNI");
			JNIStatus = true;
		} catch (UnsatisfiedLinkError ex) {
			JNIStatus = false;
		}

		// Register R210 codec for 10-bit RGB video support
		R210Codec.register();
	}

	private int outputName = OUTPUT_NAME_DEFAULT;
	static final public int OUTPUT_NAME_DEFAULT = 0;
	static final public int OUTPUT_NAME_SAVE = 1;
	static final public int OUTPUT_NAME_AUTO = 2;

	public EJMain() {

		setDirectory(new File(System.getProperty("user.dir")));

		mainFrame = new JFrame("JAVED");

		mainFrame.addWindowListener(new WindowAdapter() {
				                       public void windowClosing(WindowEvent e) {
									   quit();
							       }});

		buildMenus();

		buildContent();

		reportStatus(SC_TONEMAP_READY);

		final int inset = 10;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setBounds(inset, inset, screenSize.width - inset*2, screenSize.height - inset*2 );

		mainFrame.pack();

		mainFrame.setVisible(true);



	}
	
	public JFrame getFrame(){
		return mainFrame;
	}

	protected void buildContent() {

		contentPane = new JPanel();
		JPanel upperPane = new JPanel();
		JPanel actionPane = new JPanel();
		JPanel statusPane = new JPanel();
		statusLabel = new JLabel("Status Reset");
		JPanel lowerPane = new JPanel();
		JToolBar toolBar = new JToolBar();
		tabPane = new JTabbedPane(JTabbedPane.BOTTOM);

		contentPane.setLayout(new BorderLayout());

		final int inset1 = 45;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		contentPane.setPreferredSize(new Dimension(screenSize.width - inset1*2, screenSize.height - inset1*2));

		EmptyBorder eb = new EmptyBorder(2,2,2,2);
		BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
		CompoundBorder cb = new CompoundBorder(eb,bb);
		contentPane.setBorder(cb);

		upperPane.setLayout(new BorderLayout());
		lowerPane.setLayout(new BorderLayout());

		actionPane.setLayout(new BorderLayout());
		statusPane.setLayout(new BorderLayout());

		ejView = new EJView(this);
		ejViewPanel = ejView.getPanel();
		JScrollPane viewPane = new JScrollPane(ejViewPanel);
		JScrollPane settingsPane = new JScrollPane(ejSettingsPanel);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, viewPane, settingsPane);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(540);
		Dimension minimumSize = new Dimension(100, 50);
		viewPane.setMinimumSize(minimumSize);
		settingsPane.setMinimumSize(minimumSize);
		splitPane.setPreferredSize(new Dimension(400,200));

		tabPane.addTab("Viewer", viewPane);

		ejSettings = new EJSettings(this);
		ejSettingsPanel = ejSettings.getPanel();

		buildToolBar(toolBar);

		actionPane.add(toolBar, BorderLayout.NORTH);
		//actionPane.add(splitPane, BorderLayout.CENTER);
		actionPane.add(tabPane, BorderLayout.CENTER);

		statusPane.add(statusLabel, BorderLayout.CENTER);
		EmptyBorder eb1 = new EmptyBorder(2,2,2,2);
		BevelBorder bb1 = new BevelBorder(BevelBorder.LOWERED);
		CompoundBorder cb1 = new CompoundBorder(eb1,bb1);
		statusPane.setBorder(cb1);

		upperPane.add(actionPane, BorderLayout.CENTER);
		lowerPane.add(statusPane, BorderLayout.CENTER);

		contentPane.add(upperPane, BorderLayout.CENTER);
		contentPane.add(lowerPane, BorderLayout.SOUTH);
		contentPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		mainFrame.setContentPane(contentPane);

	}

	protected void buildToolBar(JToolBar toolBar) {

		resetB = new JButton("Reset");
		resetB.setEnabled(true);
		resetB.addActionListener(new ResetBAction());

		blankB = new JButton("blank");
		blankB.setEnabled(true);
		blankB.addActionListener(new BlankBAction());

		processB = new JButton("effect");
		processB.setEnabled(true);
		processB.addActionListener(new ProcessBAction());

		mixB = new JButton("mix");
		mixB.setEnabled(true);
		mixB.addActionListener(new MixBAction());

		joinB = new JButton("join");
		joinB.setEnabled(true);
		joinB.addActionListener(new JoinBAction());

		ejControls = new EJControls(this);
		JPanel ejCPanel = ejControls.getPanel();

		toolBar.add(resetB);
		toolBar.add(blankB);
		toolBar.add(processB);
		toolBar.add(mixB);
		toolBar.add(joinB);
		toolBar.add(ejCPanel);



	}

	class ResetBAction implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			reset();
		}
	}


	class BlankBAction implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			if (ejView != null) {
				blankB.setEnabled(false);
				processB.setEnabled(false);
				mixB.setEnabled(false);
				joinB.setEnabled(false);
				ejView.process(EJView.PROCESS_BLANK);
				blankB.setEnabled(true);
				processB.setEnabled(true);
				mixB.setEnabled(true);
				joinB.setEnabled(true);

			}
		}
	}

	class ProcessBAction implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			if (ejView != null) {
				blankB.setEnabled(false);
				processB.setEnabled(false);
				mixB.setEnabled(false);
				joinB.setEnabled(false);
				ejView.process(EJView.PROCESS_EFFECT);
				blankB.setEnabled(true);
				processB.setEnabled(true);
				mixB.setEnabled(true);
				joinB.setEnabled(true);

			}
		}
	}

	class MixBAction implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			if (ejView != null) {
				blankB.setEnabled(false);
				processB.setEnabled(false);
				mixB.setEnabled(false);
				joinB.setEnabled(false);
				ejView.process(EJView.PROCESS_MIX);
				blankB.setEnabled(true);
				processB.setEnabled(true);
				mixB.setEnabled(true);
				joinB.setEnabled(true);

			}
		}
	}
	class JoinBAction implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			if (ejView != null) {
				blankB.setEnabled(false);
				processB.setEnabled(false);
				mixB.setEnabled(false);
				joinB.setEnabled(false);
				ejView.process(EJView.PROCESS_JOIN);
				blankB.setEnabled(true);
				processB.setEnabled(true);
				mixB.setEnabled(true);
				joinB.setEnabled(true);

			}
		}
	}

	public boolean isOutputDefault(){

		return (outputName == OUTPUT_NAME_DEFAULT);

	}


	public boolean isOutputSave(){

		return (outputName == OUTPUT_NAME_SAVE);

	}

	public boolean isOutputAuto(){

		return (outputName == OUTPUT_NAME_AUTO);

	}

	private void reset(){

		//toneMap.clear();

		//audioModel = null;
		System.gc();

		buildContent();

		reportStatus(SC_TONEMAP_READY);

		mainFrame.validate();
		mainFrame.pack();

	}

	protected void buildMenus() {

		menuBar = new JMenuBar();
		menuBar.setOpaque(true);
		JMenu file = buildFileMenu();
		JMenu help = buildHelpMenu();
		JMenu option = buildOptionMenu();

		menuBar.add(file);
		menuBar.add(help);
		menuBar.add(option);

		mainFrame.setJMenuBar(menuBar);
	}

	protected JMenu buildFileMenu() {

		JMenu file = new JMenu("File");
		JMenuItem open = new JMenuItem("Open");
		JMenuItem save = new JMenuItem("Save");
		JMenuItem quit = new JMenuItem("Quit");

		open.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
		   		openEJSet();
       	}});

		save.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
				saveEJSet();
		}});

		quit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
			   	quit();
		}});

		file.add(open);
		file.add(save);
		file.addSeparator();
		file.add(quit);
		return file;
	}

	protected JMenu buildOptionMenu() {

		JMenu name = new JMenu("Option");
		JMenuItem defaultName = new JMenuItem("Default");
		JMenuItem auto = new JMenuItem("Auto");
		JMenuItem save = new JMenuItem("Save");

		auto.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
				outputName = OUTPUT_NAME_AUTO;
       	}});

		save.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
				outputName = OUTPUT_NAME_SAVE;
		}});

		defaultName.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
				outputName = OUTPUT_NAME_DEFAULT;
		}});

		name.add(defaultName);
		name.add(auto);
		name.add(save);
		name.addSeparator();
		return name;
	}


	protected JMenu buildHelpMenu() {

		JMenu help = new JMenu("Help");
	    JMenuItem about = new JMenuItem("About ToneMap...");
		JMenuItem openHelp = new JMenuItem("Open Help Window");

		about.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
		        showAboutBox();
		    }
		});

		openHelp.addActionListener(new ActionListener() {
	                   public void actionPerformed(ActionEvent e) {
					   openHelpWindow();
			       }});

		help.add(about);
		help.add(openHelp);

		return help;
	    }


	public void quit() {
		System.exit(0);
	}

	public void newToneMap() {

	}

	public void setDirectory(File fileDirectory) {
		System.out.println("Settign directory: "+fileDirectory);
		this.fileDirectory = fileDirectory;
	}

	public File getDirectory() {
		return fileDirectory;
	}

	public boolean openEJSet() {
   		try {
		    JFileChooser fc = new JFileChooser(getDirectory());
	        fc.setFileFilter(new javax.swing.filechooser.FileFilter () {
	            public boolean accept(File f) {
	                if (f.isDirectory()) {
	                    return true;
	                }
	       		    String name = f.getName();
	       			if (name.endsWith(".ejs") || name.endsWith(".EJS")) {
	       		        return true;
	       			}
			        	return false;
	           		}
	            	public String getDescription() {
	                    return ".ejs";
	                }
	        });

	        if (fc.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
	            file = fc.getSelectedFile();
				fileName = file.getName();
				setDirectory(file);
	        } else {
		        return false;
	        }
	    } catch (SecurityException ex) {
		    reportStatus(EC_TONEMAP_OPEN_BADFILE);
	        return false;
	    } catch (Exception ex) {
		    reportStatus(EC_TONEMAP_OPEN_BADFILE);
	        return false;
	    }

		if (file.exists()) {
			if (!open(file)) return false;
			return true;
		} else {
			reportStatus(EC_TONEMAP_OPEN_NOFILE);
	  		return false;
		}

	}

	public boolean saveEJSet() {
  		try {
	  		JFileChooser fc = new JFileChooser(getDirectory());
		    fc.setFileFilter(new javax.swing.filechooser.FileFilter () {
	            public boolean accept(File f) {
	                if (f.isDirectory()) {
	                    return true;
	                }
	       		    String name = f.getName();
	       			if (name.endsWith(".ejs")|| name.endsWith(".EJS")) {
	       		        return true;
	       			}
			        	return false;
	           		}
	            	public String getDescription() {
	                    return ".ejs";
	                }
	        });

	        if (fc.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
	            file = fc.getSelectedFile();
				fileName = file.getName();
				setDirectory(file);
	        }
	    } catch (SecurityException ex) {
		    reportStatus(EC_TONEMAP_SAVE);
	        return false;
	    } catch (Exception ex) {
		    reportStatus(EC_TONEMAP_SAVE);
	        return false;
	    }

		if (!save(file)) return false;
		return true;

	}


	/**
	* Open EJS file extracting stream of serialized objects
 	*/
	public boolean open(File file) {

		this.file = file;
		fileName = file.getName();

		if (file.exists()) {
			try {
				FileInputStream fin = new FileInputStream(file);
				ObjectInputStream istrm = new ObjectInputStream(fin);
				System.out.println("open1");
				ejData = (EJData)istrm.readObject();
				ejSettings.setEJData(ejData);
				System.out.println("open2");
				return true;

			} catch (IOException io ){
				io.printStackTrace();
				//toneMapFrame.reportStatus(EC_TONEMAP_OPEN);
				return false;
			} catch (Exception ex ){
				ex.printStackTrace();
				//toneMapFrame.reportStatus(EC_TONEMAP_OPEN);
				return false;
			}
		} else {
			//toneMapFrame.reportStatus(EC_TONEMAP_OPEN_NOFILE);
			return false;
		}
	}

	/**
 	* Save ToneMap objects to a file in serialized form
 	*/
	public boolean save(File file) {

		this.file = file;
		fileName = file.getName();

		try {
			FileOutputStream fout = new FileOutputStream(file);
			ObjectOutputStream ostrm = new ObjectOutputStream(fout);
			ejData = ejSettings.getEJData();
			ostrm.writeObject(ejData);
			ostrm.flush();
			return true;
		} catch (IOException io ){
			//toneMapFrame.reportStatus(EC_TONEMAP_SAVE);
			return false;
		}
	}

	public void openHelpWindow() {
		JOptionPane.showMessageDialog(mainFrame, EJHelp.message);
	}

	public void showAboutBox() {
		JOptionPane.showMessageDialog(mainFrame, ABOUTMSG);
	}

	public EJView getEJView() {
		return ejView;
	}

	public EJSettings getEJSettings() {
		return ejSettings;
	}


	public EJControls getEJControls() {
		return ejControls;
	}


	public boolean getJNIStatus() {
		return JNIStatus;
	}

	public void reportStatus(int status){

		this.status = status;
		StatusInfo SI = EJStatus.getSI(status);
		statusLabel.setText(SI.toString());

	}

	public JTabbedPane getPane() {
		return tabPane;
	}

	public JFrame mainFrame;
	private JPanel contentPane;
	private JMenuBar menuBar;
	private JInternalFrame toolPalette;
	private JCheckBoxMenuItem showToolPaletteMenuItem;
	private File fileDirectory;

	private JTabbedPane tabPane;
	private JFileChooser chooser;
	private EJView ejView;
	private EJSettings ejSettings;
	private JPanel ejViewPanel;
	private JPanel ejSettingsPanel;
	private JLabel statusLabel;
	private EJData ejData;
	private EJControls ejControls;


	private String fileName = "untitled";
	private String errStr;
	private File file;

	private JButton resetB, processB, blankB, mixB, joinB;

	private int status;

	public static void main(String[] args) {

		try {
			//UIManager.setLookAndFeel(
	    	//"com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			//javax.swing.LookAndFeel alloyLnF = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
	  		//javax.swing.UIManager.setLookAndFeel(alloyLnF);
			//javax.swing.LookAndFeel alloyLnF = new com.incors.plaf.alloy.AlloyLookAndFeel();
		  	//javax.swing.UIManager.setLookAndFeel(alloyLnF);

		} catch (Exception e) {
		}
		final EJMain ejm = new EJMain();
	}
}