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
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.JComponent;

import jomu.instrument.Instrument;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.ColorUtil;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.BeatListElement;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class BeatsView extends JComponent implements ComponentListener {

	private static final Logger LOG = Logger.getLogger(BeatsView.class.getName());

	public static final Color[] COLORS = { new Color(0xff0000), // red
			new Color(0x0000ff), // blue
			new Color(0x00ff00), // green
			new Color(0xcc02de), // purple
			new Color(0x00aaaa), // cyan-ish
			new Color(0xffa500), // orange
			new Color(0x53868b), // cadetblue4
			new Color(0xff7f50), // coral
			new Color(0x45ab1f), // dark green-ish
			new Color(0x90422d), // sienna-ish
			new Color(0xa0a0a0), // grey-ish
			new Color(0x14ff14), // green-ish
			new Color(0x6e4272), // dark purple
			new Color(0x552209) // brown
	};

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
	private Workspace workspace;

	public BeatsView() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.workspace = Instrument.getInstance().getWorkspace();
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

	public void updateToneMap(ToneMap toneMap, ToneTimeFrame ttf) {
		this.toneMap = toneMap;
		renderToneMap(ttf);
		repaint();
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

			String tmKey = this.toneMap.getKey();
			String streamId = tmKey.substring(tmKey.lastIndexOf(":") + 1);
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);

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

			BeatListElement beat = ttf.getBeat();

			Optional<BeatListElement> obc = ttf.getBeat(CellTypes.AUDIO_BEAT.name() + "_CALIBRATION");
			Optional<BeatListElement> obb = ttf.getBeat(CellTypes.AUDIO_BEAT.name());
			Optional<BeatListElement> obo = ttf.getBeat(CellTypes.AUDIO_ONSET.name());
			Optional<BeatListElement> obp = ttf.getBeat(CellTypes.AUDIO_PERCUSSION.name());
			Optional<BeatListElement> obh = ttf.getBeat(CellTypes.AUDIO_HPS.name() + "_PERCUSSION");

			Color color = Color.black;

			int height = (int) ((double) getHeight() / 2.0);

			int timeCoordinate = getTimeCoordinate(timeStart - timeAxisStart);

			if (cm.getBeatAfterTime(ttf.getStartTime(), 110) != -1) {
				color = Color.RED;
				bufferedGraphics.setColor(color);
				bufferedGraphics.fillOval(timeCoordinate, (int) (height / 4.0), 6, (int) 6);
			}

			if (beat != null) {
				color = Color.BLUE;
				bufferedGraphics.setColor(color);
				bufferedGraphics.fillOval(timeCoordinate, (int) ((4.0 / 5.0) * height), 6, 6);
			}

			if (obc.isPresent() && obc.get().getAmplitude() > 0.001) {
				color = Color.LIGHT_GRAY;
				bufferedGraphics.setColor(color);
				bufferedGraphics.fillOval(timeCoordinate, (int) ((1.0 / 5.0) * height), 6, 6);
			}
			if (obb.isPresent() && obb.get().getAmplitude() > 0.001) {
				color = new Color(0xcc02de); // purple
				bufferedGraphics.setColor(color);
				bufferedGraphics.fillOval(timeCoordinate, (int) ((2.0 / 5.0) * height), 6, 6);
			}
			if (obp.isPresent() && obp.get().getAmplitude() > 0.001) {
				color = Color.CYAN;
				bufferedGraphics.setColor(color);
				bufferedGraphics.fillOval(timeCoordinate, (int) ((4.5 / 5.0) * height), 6, 6);
			}
			if (obo.isPresent() && obo.get().getAmplitude() > 0.001) {
				color = new Color(0xffa500); // orange
				bufferedGraphics.setColor(color);
				bufferedGraphics.fillOval(timeCoordinate, (int) ((4.8 / 5.0) * height), 6, 6);
			}
			if (obh.isPresent() && obh.get().getAmplitude() > 0.001) {
				color = new Color(0x00ff00); // green
				bufferedGraphics.setColor(color);
				bufferedGraphics.fillOval(timeCoordinate, (int) ((3.5 / 5.0) * height), 6, 6);
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
