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

package jomu.instrument.desktop.monitor.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import jakarta.swing.JComponent;

import jomu.instrument.Instrument;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.ColorUtil;
import jomu.instrument.workspace.tonemap.BeatListElement;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class BeatsView extends JComponent implements ComponentListener {

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

	private ParameterManager parameterManager;

	public BeatsView() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.timeAxisStart = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET);
		this.timeAxisEnd = this.timeAxisStart
				+ parameterManager.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);
		this.addComponentListener(this);
		rainbow = ColorUtil.generateRainbow(512);
	}

	public void clear() {
		this.timeAxisStart = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET);
		this.timeAxisEnd = this.timeAxisStart
				+ parameterManager.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);
		this.toneMap = null;
		this.currentWidth = getWidth();
		this.currentHeight = getHeight();
		position = 0;
		bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
		bufferedGraphics = bufferedImage.createGraphics();
		this.currentWidth = getWidth();
		this.currentHeight = getHeight();
		drawGrid();
		repaint();
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

	public void updateAxis() {
		this.timeAxisStart = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET);
		this.timeAxisEnd = this.timeAxisStart
				+ parameterManager.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);
		if (toneMap != null) {
			renderToneMap(toneMap);
		} else {
			this.currentWidth = getWidth();
			this.currentHeight = getHeight();
			if (bufferedImage == null) {
				position = 0;
				bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
				bufferedGraphics = bufferedImage.createGraphics();
				this.currentWidth = getWidth();
				this.currentHeight = getHeight();
			}
			drawGrid();
		}
	}

	public void updateToneMap(ToneMap toneMap) {
		if (this.toneMap != null && this.toneMap.getKey().equals(toneMap.getKey())) {
			this.toneMap = toneMap;
			renderToneMap(toneMap.getTimeFrame());
			repaint();
		} else {
			renderToneMap(toneMap);
		}
	}

	public void renderToneMap(ToneMap toneMap) {
		this.toneMap = toneMap;
		double timeStart = timeAxisStart / 1000;
		double timeEnd = timeAxisEnd / 1000;
		this.currentWidth = getWidth();
		this.currentHeight = getHeight();
		bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
		bufferedGraphics = bufferedImage.createGraphics();
		for (ToneTimeFrame frame : toneMap.getTimeFramesFrom(timeStart)) {
			if (frame.getStartTime() > timeEnd) {
				break;
			}
			renderToneMap(frame);
		}
		repaint();
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
		drawGrid();
		if (ttf != null) {

			double timeAxisRange = parameterManager
					.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);
			TimeSet timeSet = ttf.getTimeSet();
			PitchSet pitchSet = ttf.getPitchSet();
			updateAxis(timeSet, pitchSet);
			double timeStart = timeSet.getStartTime() * 1000;
			double timeEnd = timeSet.getEndTime() * 1000;
			if (timeStart >= timeAxisEnd) {
				return;
			}

			bufferedGraphics.setColor(Color.black);

			double lowViewThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD);
			double highViewThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD);
			boolean showColour = parameterManager
					.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_COLOUR);
			boolean showTracking = parameterManager
					.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_TRACKING);
			boolean showLog = parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_LOG);

			if (showTracking) {

				BeatListElement beat = ttf.getBeat();

				Color color = Color.black;
				if (beat != null) {
					color = Color.WHITE;
					int timeCoordinate = getTimeCoordinate(timeStart - timeAxisStart);
					int width = (int) Math.ceil((((timeEnd - timeStart + 1) / (timeAxisRange)) * (getWidth() - 1)));
					width = 10;
					int height = (int) ((double) getHeight() / 2.0);
					bufferedGraphics.setColor(color);
					bufferedGraphics.fillRect(timeCoordinate, (int) (height / 2.0), width, (int) (height / 10.0));
				}
			} else {
				ToneMapElement[] elements = ttf.getElements();

				for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {

					ToneMapElement toneMapElement = elements[elementIndex];
					Color color = Color.black;
					if (toneMapElement != null) {
						double amplitude = 0.0;
						int width = (int) Math.ceil((((timeEnd - timeStart + 1) / (timeAxisRange)) * (getWidth() - 1)));
						int height = (int) ((double) getHeight() / 2.0);
						amplitude = toneMapElement.amplitude;
						int greyValue = 0;
						if (amplitude > highViewThreshold) {
							greyValue = 255;
							color = Color.WHITE;
						} else if (amplitude <= lowViewThreshold) {
							greyValue = 0;
							color = Color.BLACK;
						} else {
							if (showLog) {
								greyValue = (int) (Math.log1p(amplitude / highViewThreshold) / Math.log1p(1.0000001)
										* 255);
							} else {
								greyValue = (int) ((amplitude / highViewThreshold) * 255);
							}
							greyValue = Math.max(0, greyValue);
							if (showColour) {
								color = rainbow[255 - greyValue];
							} else {
								color = new Color(greyValue, greyValue, greyValue);
							}
						}

						int timeCoordinate = getTimeCoordinate(timeStart - timeAxisStart);

						bufferedGraphics.setColor(color);
						bufferedGraphics.fillRect(timeCoordinate, (int) (height / 2.0), width, (int) (height / 10.0));
					}
				}
			}
		}
	}

	private void drawGrid() {
		Color gridColor = new Color(50, 50, 50);
		double timeAxisRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);

		for (int i = 0; i <= timeAxisRange; i += 1000) {
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
