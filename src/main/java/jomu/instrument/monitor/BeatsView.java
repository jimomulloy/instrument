/*
*      _______                       _____   _____ _____
*     |__   __|                     |  __ \ / ____|  __ \
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
*
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
*
*/

package jomu.instrument.monitor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class BeatsView extends JComponent implements ComponentListener {
	/**
	 *
	 */
	private static final long serialVersionUID = -3729805747119272534L;

	private BufferedImage bufferedImage;
	private Graphics2D bufferedGraphics;

	private int position;

	private PitchSet currentPitchSet;

	private TimeSet currentTimeSet;

	private double timeAxisEnd;

	private double timeAxisStart;

	private int currentWidth = 0;
	private int currentHeight = 0;

	private ToneMap toneMap;

	private Color[] rainbow;

	public BeatsView() {
		this.timeAxisStart = 0;
		this.timeAxisEnd = 20000;
		this.addComponentListener(this);
		rainbow = ColorUtil.generateRainbow(0.9F, 0.9F, 512, false, false, false);
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		bufferedImage = null;
		bufferedGraphics = null;
		position = 0;
		this.currentWidth = getWidth();
		this.currentHeight = getHeight();
		if (toneMap != null) {
			renderToneMap(toneMap);
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {
		bufferedImage = null;
		bufferedGraphics = null;
		position = 0;
		this.currentWidth = getWidth();
		this.currentHeight = getHeight();
		if (toneMap != null) {
			renderToneMap(toneMap);
		}
	}

	@Override
	public void componentShown(ComponentEvent e) {
		bufferedImage = null;
		bufferedGraphics = null;
		position = 0;
		this.currentWidth = getWidth();
		this.currentHeight = getHeight();
		if (toneMap != null) {
			renderToneMap(toneMap);
		}
	}

	public void updateToneMap(ToneMap toneMap) {
		this.toneMap = toneMap;
		renderToneMap(toneMap.getTimeFrame());
		repaint();
	}

	private void renderToneMap(ToneMap toneMap) {
		for (ToneTimeFrame frame : toneMap.getTimeFramesFrom(0.0)) {
			renderToneMap(frame);
		}
	}

	private void renderToneMap(ToneTimeFrame ttf) {
		this.currentWidth = getWidth();
		this.currentHeight = getHeight();
		if (bufferedImage == null) {
			position = 0;
			bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
			bufferedGraphics = bufferedImage.createGraphics();
			this.currentWidth = getWidth();
			this.currentHeight = getHeight();
		}
		if (ttf != null) {
			TimeSet timeSet = ttf.getTimeSet();
			PitchSet pitchSet = ttf.getPitchSet();
			updateAxis(timeSet, pitchSet);
			double timeStart = timeSet.getStartTime() * 1000;
			double timeEnd = timeSet.getEndTime() * 1000;
			if (timeStart > timeAxisEnd) {
				timeAxisStart = timeStart;
				timeAxisEnd = timeStart + 20000.0;
				this.currentWidth = getWidth();
				this.currentHeight = getHeight();
				bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
				bufferedGraphics = bufferedImage.createGraphics();
			} else if (timeStart == 0) {
				timeAxisStart = timeStart;
				timeAxisEnd = timeStart + 20000.0;
				this.currentWidth = getWidth();
				this.currentHeight = getHeight();
				bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
				bufferedGraphics = bufferedImage.createGraphics();
			} else if (timeStart < timeAxisStart) {
				timeAxisStart -= 20000.0;
				timeAxisEnd -= 20000.0;
				this.currentWidth = getWidth();
				this.currentHeight = getHeight();
				bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
				bufferedGraphics = bufferedImage.createGraphics();
			}

			bufferedGraphics.setColor(Color.black);

			ToneMapElement[] elements = ttf.getElements();

			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {

				ToneMapElement toneMapElement = elements[elementIndex];
				Color color = Color.black;
				if (toneMapElement != null) {
					double amplitude = 0.0;
					int width = (int) Math.ceil((((timeEnd - timeStart + 1) / (20000.0)) * (getWidth() - 1)));
					int height = (int) ((double) getHeight() / 2.0);
					amplitude = toneMapElement.amplitude;
					if (amplitude > ttf.getHighThres()) {
						color = Color.white;
					}
					if (amplitude <= ttf.getLowThres()) {
						color = Color.black;
					} else {
						int greyValue = (int) (Math.log1p(amplitude / ttf.getHighThres()) / Math.log1p(1.0000001)
								* 255);
						greyValue = Math.max(0, greyValue);
						color = rainbow[255 - greyValue];
					}

					int timeCoordinate = getTimeCoordinate(timeStart - timeAxisStart);

					bufferedGraphics.setColor(color);
					bufferedGraphics.fillRect(timeCoordinate, (int) (height / 2.0), width, (int) (height / 10.0));
				}
			}
		}
		drawGrid();
	}

	private void drawGrid() {
		Color gridColor = new Color(50, 50, 50);

		for (int i = 0; i <= 20000; i += 1000) {
			bufferedGraphics.setColor(Color.WHITE);
			int timeCoordinate = getTimeCoordinate(i);
			bufferedGraphics.drawLine(timeCoordinate, getHeight(), timeCoordinate, getHeight() - 5);
			bufferedGraphics.drawString(String.valueOf((int) ((timeAxisStart + i) / 1000)), timeCoordinate,
					getHeight() - 10);
			bufferedGraphics.setColor(gridColor);
			bufferedGraphics.drawLine(timeCoordinate, getHeight(), timeCoordinate, 0);
		}
	}

	private int getCentsCoordinate(int cents) {
		return getHeight() - 1 - (int) (((double) (cents) / (double) 12) * getHeight());
	}

	private int getTimeCoordinate(double timeStart) {
		return (int) Math.floor(getWidth() * (timeStart / (timeAxisEnd - timeAxisStart)));
	}

	@Override
	public void paintComponent(final Graphics g) {
		g.drawImage(bufferedImage, 0, 0, null);
	}

	private void updateAxis(TimeSet timeSet, PitchSet pitchSet) {
		this.currentTimeSet = timeSet;
		this.currentPitchSet = pitchSet;
	}

}