package comul01.eve;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.font.*;
import java.text.*;

/**
 * This class is a GUI component that extends the basic JSlider class
 * and is used as the generic component for input of data values on the
 * ToneMap Sub System Control panels.
 * The slider restricts input to minimum and maximum limits 
 * and shows the current value along with a titled border.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

class EJSlider extends JSlider implements ChangeListener {

		
	public EJSlider(int orient, int min, int max, 
						int value, String name, 
						ChangeListener listener) {
		super(orient, min, max, value);
		TitledBorder tb = new TitledBorder(new EtchedBorder());
		tb.setTitle(name + "=" + value);
		setBorder(tb);
		setPreferredSize(new Dimension(200,70));
		setMaximumSize(new Dimension(200,70));
		setMajorTickSpacing(max-min);
		setPaintLabels(true);
		this.name = name;
		this.value = value;
		this.listener = listener;
		addChangeListener(this);
							
	}

	public void showValue(int value) {
		
		TitledBorder tb = (TitledBorder)getBorder();
		String s = tb.getTitle();
		tb.setTitle(s.substring(0, s.indexOf('=')+1) + s.valueOf(value));
	}

	public void stateChanged(ChangeEvent e) {
   	
	 	value = getValue();
		TitledBorder tb = (TitledBorder)getBorder();
		String s = tb.getTitle();
		tb.setTitle(s.substring(0, s.indexOf('=')+1) + s.valueOf(value));
		listener.stateChanged(e);				
		repaint();
		 
	}

	public String getName() {
   	
		return name;
	
	}
	

	private ChangeListener listener;
	private String name;
	private int value;

}