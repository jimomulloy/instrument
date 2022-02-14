package jomu.instrument.tonemap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class Controls the Sound Media Playback functions and contains a control panel 
 * as an inner class. It interacts with both AidioModel data and MidiModel data through 
 * implementations of the PlayerInterface. 
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

/**
 * Insert the type's description here. Creation date: (27/06/2001 21:17:34)
 * 
 * @author:
 */
public class Player implements ToneMapConstants {

	private int playState = STOPPED;

	private ToneMapFrame toneMapFrame;
	private AudioModel audioModel;
	private MidiModel midiModel;
	private PlayerPanel playerPanel;

	private String errStr;

	private double duration, seconds;

	private double timeStart = INIT_TIME_START;
	private double timeEnd = INIT_TIME_END;
	private int playMode = PLAY_MODE_AUDIO;

	private int seekSetting = 0;
	private int seekToSetting = 100;
	private int panSetting = INIT_PAN_SETTING;

	private int volumeSetting = INIT_VOLUME_SETTING;

	private JButton playB, stopB, pauseB;
	private JRadioButton audioB, audioOB, midiB;

	private JSlider seekSlider;
	private JSlider seekToSlider;

	public class PlayerPanel extends JPanel {

		public PlayTimer playTimer;

		public PlayerPanel() {

			EmptyBorder eb = new EmptyBorder(0, 0, 0, 0);
			setBorder(eb);
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(new PlayerButtons());
			add(new PlayMode());
			add(new SeekControl());
			add(new SeekToControl());
			add(playTimer = new PlayTimer());

		}

		private void play() {

			if (playMode == PLAY_MODE_AUDIO) {
				if (!audioModel.play())
					return;
			} else if (playMode == PLAY_MODE_MIDI) {
				if (!midiModel.play())
					return;
			} else if (playMode == PLAY_MODE_AUDIO_OUT) {
				if (!audioModel.playOut())
					return;
			}
			playTimer.start();
			playB.setEnabled(false);
			stopB.setEnabled(true);
			pauseB.setText("Pause ");
			pauseB.setEnabled(true);

		}

		private void loop() {

			playTimer.stop();

			if (playMode == PLAY_MODE_AUDIO) {
				audioModel.playLoop();
			} else if (playMode == PLAY_MODE_MIDI) {
				midiModel.playLoop();
			} else if (playMode == PLAY_MODE_AUDIO_OUT) {
				audioModel.playLoop();
			}

			playTimer.start();
			playB.setEnabled(false);
			stopB.setEnabled(true);
			pauseB.setText("Pause ");
			pauseB.setEnabled(true);

		}

		private void stop() {

			playTimer.stop();

			if (playMode == PLAY_MODE_AUDIO) {
				audioModel.playStop();
			} else if (playMode == PLAY_MODE_MIDI) {
				midiModel.playStop();
			} else if (playMode == PLAY_MODE_AUDIO_OUT) {
				audioModel.playStop();
			}

			playB.setEnabled(true);
			stopB.setEnabled(false);
			pauseB.setText("Pause ");
			pauseB.setEnabled(false);

		}

		private void pause() {

			if (playMode == PLAY_MODE_AUDIO) {
				audioModel.playPause();
			} else if (playMode == PLAY_MODE_MIDI) {
				midiModel.playPause();
			} else if (playMode == PLAY_MODE_AUDIO_OUT) {
				audioModel.playPause();
			}

			playB.setEnabled(true);
			stopB.setEnabled(true);
			pauseB.setText("Resume");
			pauseB.setEnabled(true);

		}

		private void resume() {

			if (playMode == PLAY_MODE_AUDIO) {
				audioModel.playResume();
			} else if (playMode == PLAY_MODE_MIDI) {
				midiModel.playResume();
			} else if (playMode == PLAY_MODE_AUDIO_OUT) {
				audioModel.playResume();
			}

			playB.setEnabled(true);
			stopB.setEnabled(true);
			pauseB.setText("Pause");
			pauseB.setEnabled(true);

		}

		class SeekControl extends JPanel {

			public SeekControl() {

				JLabel seekLabel = new JLabel("From", JLabel.RIGHT);
				add(seekLabel);
				seekSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
				seekSlider.setEnabled(true);
				seekSlider.addChangeListener(new SeekSliderListener());
				add(seekSlider);

			}

			class SeekSliderListener implements ChangeListener {

				public void stateChanged(ChangeEvent e) {

					JSlider slider = (JSlider) e.getSource();
					int value = slider.getValue();
					seekSetting = value;
					if (seekToSetting < seekSetting) {
						seekToSlider.setValue(seekSetting);
					}
					setSeek(getFromTime());
					slider.repaint();
				}

			}

		}

		class SeekToControl extends JPanel {

			public SeekToControl() {

				seekToSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
				seekToSlider.setEnabled(true);
				seekToSlider.addChangeListener(new seekToSliderListener());
				add(seekToSlider);
				JLabel seekToLabel = new JLabel("To", JLabel.LEFT);
				add(seekToLabel);

			}

			class seekToSliderListener implements ChangeListener {

				public void stateChanged(ChangeEvent e) {

					JSlider slider = (JSlider) e.getSource();
					int value = slider.getValue();
					seekToSetting = value;
					if (seekToSetting < seekSetting) {
						seekSlider.setValue(seekToSetting);
					}

					slider.repaint();
				}

			}

		}

		class PlayerButtons extends JPanel {

			public PlayerButtons() {

				playB = new JButton("Play");
				playB.setEnabled(true);
				playB.addActionListener(new PlayBAction());
				add(playB);

				stopB = new JButton("Stop");
				stopB.setEnabled(false);
				stopB.addActionListener(new StopBAction());
				add(stopB);

				pauseB = new JButton("Pause ");
				pauseB.setEnabled(false);
				pauseB.addActionListener(new PauseBAction());
				add(pauseB);

			}

			class PlayBAction implements ActionListener {

				public void actionPerformed(ActionEvent evt) {

					play();

				}

			}

			class StopBAction implements ActionListener {

				public void actionPerformed(ActionEvent evt) {

					stop();
				}

			}

			class PauseBAction implements ActionListener {

				public void actionPerformed(ActionEvent evt) {

					String s = evt.getActionCommand();
					if (s.startsWith("Pause")) {
						pause();
					} else if (s.startsWith("Resume")) {
						resume();
					}
				}
			}
		}

		class PlayMode extends JPanel {

			PlayMode() {

				audioB = new JRadioButton("Audio");
				audioB.setActionCommand("Audio");
				audioB.setSelected(true);

				audioOB = new JRadioButton("Map");
				audioOB.setActionCommand("Map");
				audioOB.setSelected(false);

				midiB = new JRadioButton("Midi");
				midiB.setActionCommand("Midi");
				midiB.setSelected(false);

				ButtonGroup group = new ButtonGroup();
				group.add(audioB);
				group.add(audioOB);
				group.add(midiB);

				PlayModeListener playModeListener = new PlayModeListener();
				audioB.addActionListener(playModeListener);
				audioOB.addActionListener(playModeListener);
				midiB.addActionListener(playModeListener);

				JPanel radioPanel = new JPanel();
				radioPanel.setLayout(new GridLayout(1, 0));
				radioPanel.add(audioB);
				radioPanel.add(audioOB);
				radioPanel.add(midiB);

				add(radioPanel);

			}

			class PlayModeListener implements ActionListener {

				public void actionPerformed(ActionEvent e) {

					String s = e.getActionCommand();
					if (s.startsWith("Audio")) {
						if (playMode == PLAY_MODE_MIDI || playMode == PLAY_MODE_AUDIO_OUT) {
							stop();
						}
						playMode = PLAY_MODE_AUDIO;
					} else if (s.startsWith("Midi")) {
						if (playMode == PLAY_MODE_AUDIO || playMode == PLAY_MODE_AUDIO_OUT) {
							stop();
						}
						playMode = PLAY_MODE_MIDI;
					} else if (s.startsWith("Map")) {
						if (playMode == PLAY_MODE_AUDIO || playMode == PLAY_MODE_MIDI) {
							stop();
						}
						playMode = PLAY_MODE_AUDIO_OUT;
					}
				}
			}

		}

		class PlayTimer extends JComponent implements Runnable {

			private boolean cbStop = true;
			private BufferedImage bimg;
			private Thread thread;
			private double playTime;
			private int w, h;
			private Font font = new Font("Dialog", Font.BOLD, 12);
			private Color color;
			private NumberFormat nf;

			public PlayTimer() {

				setBackground(Color.black);
				setPreferredSize(new Dimension(35, 20));
				setEnabled(true);
				nf = NumberFormat.getNumberInstance();
				nf.setMaximumFractionDigits(1);
				nf.setMinimumFractionDigits(1);
				nf.setMaximumIntegerDigits(2);
				nf.setMinimumIntegerDigits(1);

			}

			public void start() {

				thread = new Thread(this);
				thread.setPriority(Thread.MIN_PRIORITY);
				thread.setName("Player.PlayTime");
				thread.start();
				playTime = 0.0;
			}

			public synchronized void stop() {
				thread = null;
				notifyAll();
			}

			public void run() {
				playState = getState();
				double playToTime = getToTime();
				while (playState != EOM && playTime < playToTime && thread != null) {

					repaint();
					try {
						Thread.sleep(100);
						playTime = getTime();

					} catch (InterruptedException e) {
						break;
					}
					playState = getState();
					playToTime = getToTime();
				}

				if (thread != null) {
					playTime = 0.0;
					repaint();
					loop();
				}
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
				big.drawString(nf.format(playTime / 1000.0), 10, 15);
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

	public Player(ToneMapFrame toneMapFrame) {

		this.toneMapFrame = toneMapFrame;
		playerPanel = new PlayerPanel();
		audioModel = toneMapFrame.getAudioModel();
		midiModel = toneMapFrame.getMidiModel();
		audioModel.playSetPlayer(this);
		midiModel.playSetPlayer(this);

	}

	public void clear() {
		playerPanel.stop();
	}

	public JPanel getPanel() {
		return playerPanel;
	}

	public double getSeekTime() {
		return (double) seekSetting;
	}

	public double getSeekToTime() {
		return (double) seekToSetting;
	}

	public int getState() {

		if (playMode == PLAY_MODE_AUDIO) {
			return audioModel.playGetState();
		} else if (playMode == PLAY_MODE_MIDI) {
			return midiModel.playGetState();
		} else if (playMode == PLAY_MODE_AUDIO_OUT) {
			return audioModel.playGetState();
		}

		return 0;

	}

	public double getTime() {

		if (playMode == PLAY_MODE_AUDIO) {
			return audioModel.playGetTime();
		} else if (playMode == PLAY_MODE_MIDI) {
			return midiModel.playGetTime();
		} else if (playMode == PLAY_MODE_AUDIO_OUT) {
			return audioModel.playGetTime();
		}
		return 0;

	}

	public double getToTime() {

		if (playMode == PLAY_MODE_AUDIO) {
			return (getSeekToTime() / 100) * (audioModel.playGetLength()) / 1000.0;
		} else if (playMode == PLAY_MODE_MIDI) {
			return (getSeekToTime() / 100) * (midiModel.playGetLength()) / 1000.0;
		} else if (playMode == PLAY_MODE_AUDIO_OUT) {
			return (getSeekToTime() / 100) * (audioModel.playGetLength()) / 1000.0;
		}

		return 0;
	}

	public double getFromTime() {

		if (playMode == PLAY_MODE_AUDIO) {
			return (getSeekTime() / 100) * (audioModel.playGetLength()) / 1000.0;
		} else if (playMode == PLAY_MODE_MIDI) {
			return (getSeekTime() / 100) * (midiModel.playGetLength()) / 1000.0;
		} else if (playMode == PLAY_MODE_AUDIO_OUT) {
			return (getSeekTime() / 100) * (audioModel.playGetLength()) / 1000.0;
		}

		return 0;
	}

	public void setSeek(double seekTime) {

		if (playMode == PLAY_MODE_AUDIO) {
			audioModel.playSetSeek(seekTime);
		} else if (playMode == PLAY_MODE_MIDI) {
			midiModel.playSetSeek(seekTime);
		} else if (playMode == PLAY_MODE_AUDIO_OUT) {
			audioModel.playSetSeek(seekTime);
		}
	}

	public void reset() {

	}
} // End Player