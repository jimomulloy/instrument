package jomu.instrument.monitor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;

import jomu.instrument.Organ;
import jomu.instrument.monitor.core.SwingVisor;

@ApplicationScoped
public class Console implements Organ {

	@Inject
	Visor visor;
	
	//public OscilloscopeEventHandler getOscilloscopeHandler() {
	//	return visor;
	//}

	public Visor getVisor() {
		return visor;
	}

	@Override
	public void initialise() {
	}

	@Override
	public void start() {
		visor.startUp();
	}

	@Override
	public void stop() {
		System.exit(0);
	}
}
