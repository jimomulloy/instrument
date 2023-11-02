package comul01.eve;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.geom.Line2D;
import javax.swing.event.*;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.font.*;
import java.text.*;

/**
 * This class Provides generic EVE Controls
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

/**
 * Insert the type's description here.
 * Creation date: (27/06/2001 21:17:34)
 * @author:
 */
public class EJControls implements EJConstants {

	private EJMain ejMain;
	private EJCPanel ejCPanel;

	private String errStr;
	private int filmMode = FILM_MODE_AVI;

	private double duration, seconds;

	private JButton playB, stopB, pauseB;
	private JRadioButton aviB, movB;

	private JSlider seekSlider;
	private JSlider seekToSlider;

	public class EJCPanel extends JPanel {

	    public FilmModeControl filmModeControl;
	    public FilmTime filmTime;
		public FilmFrame filmFrame;
	    public FilmProgress filmProgress;
		public FilmP[]  filmPs;



		public EJCPanel() {

			EmptyBorder eb = new EmptyBorder(0, 0, 0, 0);
			setBorder(eb);
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(filmModeControl = new FilmModeControl());
			add(filmTime = new FilmTime());
			add(filmFrame = new FilmFrame());
			add(filmProgress = new FilmProgress());
			filmPs = new FilmP[6];
			for (int i=0; i<6; i++) {

				add(filmPs[i] = new FilmP());
			}


		}

		class FilmModeControl extends JPanel {

			FilmModeControl() {

				aviB = new JRadioButton("AVI");
				aviB.setActionCommand("AVI");
				aviB.setSelected(true);

				movB = new JRadioButton("MOV");
				movB.setActionCommand("MOV");
				movB.setSelected(false);

				ButtonGroup group = new ButtonGroup();
				group.add(aviB);
				group.add(movB);

				FilmModeListener filmModeListener = new FilmModeListener();
				aviB.addActionListener(filmModeListener);
				movB.addActionListener(filmModeListener);

				JPanel radioPanel = new JPanel();
				radioPanel.setLayout(new GridLayout(1, 0));
				radioPanel.add(aviB);
				radioPanel.add(movB);

				add(radioPanel);

			}

			class FilmModeListener implements ActionListener {

				public void actionPerformed(ActionEvent e) {

					String s = e.getActionCommand();
					if (s.startsWith("AVI")) {
						filmMode = FILM_MODE_AVI;
					} else if (s.startsWith("MOV")) {
						filmMode = FILM_MODE_MOV;
					}
				}
			}

		}

		class FilmTime extends JComponent {

			private boolean cbStop = true;
			private BufferedImage bimg;
			private double time;
			private int w, h;
			private Font font = new Font("Dialog", Font.BOLD, 12);
			private Color color;
			private NumberFormat nf;

			public FilmTime() {

				setBackground(Color.black);
				setPreferredSize(new Dimension(40, 20));
				setEnabled(true);
				nf = NumberFormat.getNumberInstance();
				nf.setMaximumFractionDigits(2);
				nf.setMinimumFractionDigits(2);
				nf.setMaximumIntegerDigits(2);
				nf.setMinimumIntegerDigits(1);

			}

			public void setTime(long time) {
				this.time = (double)time;
				repaint();
			}

			public void paint(Graphics g) {
				if (bimg == null) {
					bimg = (BufferedImage) createImage(40, 25);
				}
				int w = bimg.getWidth();
				int h = bimg.getHeight();
				Graphics2D big = bimg.createGraphics();
				big.setBackground(Color.black);
				big.clearRect(0, 0, w, h);
				big.setFont(font);
				big.setColor(color.white);
				big.drawString(nf.format(time/100.0), 10, 15);
				big.setColor(Color.gray);
				big.drawLine(0, 0, 0, h - 1);
				big.drawLine(0, 0, w - 1, 0);
				big.setColor(Color.white);
				big.drawLine(w - 1, 0, w - 1, h - 1);
				big.drawLine(0, h - 1, w - 1, h - 1);
				g.drawImage(bimg, 0, 0, this);
				big.dispose();
			}
		}

		class FilmFrame extends JComponent {

			private boolean cbStop = true;
			private BufferedImage bimg;
			private int w, h;
			private long frame;
			private Font font = new Font("Dialog", Font.BOLD, 12);
			private Color color;
			private NumberFormat nf;

			public FilmFrame() {

				setBackground(Color.black);
				setPreferredSize(new Dimension(40, 20));
				setEnabled(true);
				nf = NumberFormat.getNumberInstance();
				nf.setMaximumFractionDigits(0);
				nf.setMinimumFractionDigits(0);
				nf.setMaximumIntegerDigits(4);
				nf.setMinimumIntegerDigits(1);

			}

			public void setFrame(long frame) {
				this.frame = frame;
				repaint();
			}


			public void paint(Graphics g) {
				if (bimg == null) {
					bimg = (BufferedImage) createImage(40, 25);
				}
				int w = bimg.getWidth();
				int h = bimg.getHeight();
				Graphics2D big = bimg.createGraphics();
				big.setBackground(Color.black);
				big.clearRect(0, 0, w, h);
				big.setFont(font);
				big.setColor(color.white);
				big.drawString(nf.format(frame), 10, 15);
				big.setColor(Color.gray);
				big.drawLine(0, 0, 0, h - 1);
				big.drawLine(0, 0, w - 1, 0);
				big.setColor(Color.white);
				big.drawLine(w - 1, 0, w - 1, h - 1);
				big.drawLine(0, h - 1, w - 1, h - 1);
				g.drawImage(bimg, 0, 0, this);
				big.dispose();
			}
		}

		class FilmP extends JComponent {

			private boolean cbStop = true;
			private BufferedImage bimg;
			private int w, h;
			private long param;
			private Font font = new Font("Dialog", Font.BOLD, 10);
			private Color color;
			private NumberFormat nf;

			public FilmP() {

				setBackground(Color.black);
				setPreferredSize(new Dimension(30, 17));
				setEnabled(true);
				nf = NumberFormat.getNumberInstance();
				nf.setMaximumFractionDigits(0);
				nf.setMinimumFractionDigits(0);
				nf.setMaximumIntegerDigits(3);
				nf.setMinimumIntegerDigits(1);

			}

			public void setParam(long param) {
				this.param = param;
				repaint();
			}


			public void paint(Graphics g) {
				if (bimg == null) {
					bimg = (BufferedImage) createImage(30, 17);
				}
				int w = bimg.getWidth();
				int h = bimg.getHeight();
				Graphics2D big = bimg.createGraphics();
				big.setBackground(Color.black);
				big.clearRect(0, 0, w, h);
				big.setFont(font);
				big.setColor(color.white);
				big.drawString(nf.format(param), 10, 15);
				big.setColor(Color.gray);
				big.drawLine(0, 0, 0, h - 1);
				big.drawLine(0, 0, w - 1, 0);
				big.setColor(Color.white);
				big.drawLine(w - 1, 0, w - 1, h - 1);
				big.drawLine(0, h - 1, w - 1, h - 1);
				g.drawImage(bimg, 0, 0, this);
				big.dispose();
			}
		}

		class FilmProgress extends JComponent {

			private boolean cbStop = true;
			private BufferedImage bimg;
			private int w, h;
			private long progress;
			private Font font = new Font("Dialog", Font.BOLD, 12);
			private Color color;
			private NumberFormat nf;

			public FilmProgress() {

				setBackground(Color.black);
				setPreferredSize(new Dimension(35, 20));
				setEnabled(true);
				nf = NumberFormat.getNumberInstance();
				nf.setMaximumFractionDigits(0);
				nf.setMinimumFractionDigits(0);
				nf.setMaximumIntegerDigits(3);
				nf.setMinimumIntegerDigits(1);

			}

			public void setProgress(long progress) {
				this.progress = progress;
				repaint();
			}


			public void paint(Graphics g) {
				if (bimg == null) {
					bimg = (BufferedImage) createImage(35, 25);
				}
				int w = bimg.getWidth();
				int h = bimg.getHeight();
				Graphics2D big = bimg.createGraphics();
				big.setBackground(Color.black);
				big.clearRect(0, 0, w, h);
				big.setFont(font);
				big.setColor(color.white);
				big.drawString(nf.format(progress*100.0), 10, 15);
				big.setColor(Color.gray);
				big.drawLine(0, 0, 0, h - 1);
				big.drawLine(0, 0, w - 1, 0);
				big.setColor(Color.white);
				big.drawLine(w - 1, 0, w - 1, h - 1);
				big.drawLine(0, h - 1, w - 1, h - 1);
				g.drawImage(bimg, 0, 0, this);
				big.dispose();
			}
		}
	}


	public EJControls(EJMain ejMain) {

		this.ejMain = ejMain;
		ejCPanel = new EJCPanel();

	}
	public JPanel getPanel() {
		return ejCPanel;
	}

	public int getFilmMode() {
		return filmMode;
	}

	public void setFilmTime(long time) {
		ejCPanel.filmTime.setTime(time);
	}

	public void setFilmFrame(long frame) {
		ejCPanel.filmFrame.setFrame(frame);

	}

	public void setFilmP(int i, long param) {
		ejCPanel.filmPs[i].setParam(param);

	}

	public void setFilmProgress(long progress) {
		ejCPanel.filmProgress.setProgress(progress);

	}

	public void reset() {

	}
}