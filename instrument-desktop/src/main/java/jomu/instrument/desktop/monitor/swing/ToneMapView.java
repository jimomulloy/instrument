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
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.JComponent;

import jomu.instrument.Instrument;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.ColorUtil;
import jomu.instrument.workspace.tonemap.NoteListElement;
import jomu.instrument.workspace.tonemap.NoteTracker.NoteTrack;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneMapStatistics;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class ToneMapView extends JComponent implements ComponentListener, ToneMapConstants {

	private static final int MAX_CENTS = 1200 * 10;

	private static final Logger LOG = Logger.getLogger(ToneMapView.class.getName());

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

	private int minCents = 0;

	private int maxCents = MAX_CENTS;

	private ToneMap toneMap;

	private Color[] rainbow;

	private double maxAmplitude = 0;

	private int maxTime = 0;

	private int maxPitchCents = Integer.MIN_VALUE;

	private int minPitchCents = Integer.MAX_VALUE;

	private ParameterManager parameterManager;

	public ToneMapView() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		this.timeAxisStart = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET);
		this.timeAxisEnd = this.timeAxisStart
				+ parameterManager.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);
		this.minCents = parameterManager.getIntParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET);
		this.maxCents = parameterManager.getIntParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE);
		this.addComponentListener(this);
		// rainbow = ColorUtil.generateToneMapColors(256);
		rainbow = ColorUtil.generateRainbow(512);
	}

	public void clear() {
		this.timeAxisStart = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET);
		this.timeAxisEnd = this.timeAxisStart
				+ parameterManager.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);
		maxAmplitude = 0;
		maxTime = 0;
		maxPitchCents = 0;
		minPitchCents = 0;
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

	public double getMaxAmplitude() {
		return maxAmplitude;
	}

	public int getMaxPitchCents() {
		return maxPitchCents + 100 > MAX_CENTS ? MAX_CENTS : maxPitchCents + 100;
	}

	public int getMinPitchCents() {
		return minPitchCents - 100 < 0 ? 0 : minPitchCents - 100;
	}

	public int getMaxTime() {
		return maxTime;
	}

	public ToneMap getToneMap() {
		return toneMap;
	}

	public void updateAxis() {
		this.timeAxisStart = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_OFFSET);
		this.timeAxisEnd = this.timeAxisStart
				+ parameterManager.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);
		this.minCents = parameterManager.getIntParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_OFFSET);
		this.maxCents = parameterManager.getIntParameter(InstrumentParameterNames.MONITOR_VIEW_PITCH_AXIS_RANGE);
		maxAmplitude = 0;
		maxTime = 0;
		maxPitchCents = 0;
		minPitchCents = 0;

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

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
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

	public void renderToneMap(ToneMap toneMap) {
		this.toneMap = toneMap;
		maxAmplitude = 0;
		maxTime = 0;
		maxPitchCents = 0;
		minPitchCents = 0;
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

	@Override
	public void componentShown(ComponentEvent e) {
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

			if (timeEnd > maxTime) {
				maxTime = (int) timeEnd;
			}

			if (timeStart >= timeAxisEnd) {
				return;
			}

			bufferedGraphics.setColor(Color.black);

			double lowViewThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD);
			double highViewThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD);
			boolean showPeaks = parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_PEAKS);
			boolean showColour = parameterManager
					.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_COLOUR);
			boolean showPower = parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_POWER);
			boolean showNotes = parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_NOTES);
			boolean showChords = parameterManager
					.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_CHORDS);
			boolean showBeats = parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BEATS);
			boolean showBase = parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_BASE);
			boolean showLog = parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_LOG);
			boolean showStats = parameterManager.getBooleanParameter(InstrumentParameterNames.MONITOR_VIEW_SHOW_STATS);

			ToneMapElement[] elements = ttf.getElements();

			int spectralCentroid = ttf.getSpectralCentroid();

			int spectralMean = ttf.getSpectralMean();
			int width = (int) Math.ceil((((timeEnd - timeStart + 1) / (timeAxisRange)) * (getWidth() - 1)));
			int height = (int) ((100.0 / (maxCents - minCents)) * getHeight());
			int timeCoordinate = getTimeCoordinate(timeStart - timeAxisStart);

			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {

				ToneMapElement toneMapElement = elements[elementIndex];
				int cents = pitchSet.getNote(elementIndex) * 100;
				int centsCoordinate = getCentsCoordinate(cents);

				Color color = Color.black;
				if (toneMapElement != null) {
					double amplitude = 0.0;
					amplitude = toneMapElement.amplitude;
					if (maxAmplitude < amplitude) {
						maxAmplitude = amplitude;
					}
					if (amplitude > ToneTimeFrame.AMPLITUDE_FLOOR) {
						if (maxPitchCents < cents) {
							maxPitchCents = cents;
						}
						if (minPitchCents > cents) {
							minPitchCents = cents;
						}
					}
					int greyValue = 0;
					if (amplitude > highViewThreshold) {
						greyValue = 255;
						color = Color.WHITE;
					} else if (amplitude <= lowViewThreshold) {
						greyValue = 0;
						color = Color.BLACK;
					} else {
						if (showLog) {
							greyValue = (int) (Math
									.log1p((amplitude - lowViewThreshold) / (highViewThreshold - lowViewThreshold))
									/ Math.log1p(1.0000001) * 255);
						} else {
							greyValue = (int) (((amplitude - lowViewThreshold) / (highViewThreshold - lowViewThreshold))
									* 255);
						}
						greyValue = Math.max(0, greyValue);
						if (showColour) {
							color = rainbow[255 - greyValue];
						} else {
							color = new Color(greyValue, greyValue, greyValue);
						}
					}
					if (showPeaks && toneMapElement.isPeak) {
						color = Color.MAGENTA;
					}

					if (showStats) {
						if (ttf.getMaxAmplitude() > lowViewThreshold) {
							if (spectralCentroid != -1 && elementIndex == spectralCentroid) {
								color = new Color(0x53868b); // cadetblue4
							}
							if (spectralMean != -1 && elementIndex == spectralMean) {
								color = new Color(0xff7f50); // coral
							}
							if ((spectralCentroid != -1 && elementIndex == spectralCentroid)
									&& (spectralMean != -1 && elementIndex == spectralMean)) {
								color = new Color(0x6e4272); // dark purple
							}
						}
					}

					if (showPower) {
						bufferedGraphics.setColor(color);
						bufferedGraphics.fillRect(timeCoordinate, centsCoordinate - height, width, height);
					}

					if (showNotes) {
						if (toneMapElement.noteListElement != null) {
							if (toneMapElement.noteState == START) {
								color = new Color(0xff7f50); // coral
								bufferedGraphics.setColor(color);
								bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 3, 6, (int) 6);
							} else if (toneMapElement.noteState == END) {
								color = new Color(0x53868b); // cadetblue4
								bufferedGraphics.setColor(color);
								bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 3, 6, (int) 6);
							} else {
								color = Color.LIGHT_GRAY;
								bufferedGraphics.setColor(color);
								bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 3, 6, (int) 6);
							}
						}
					}

				}
			}

			if (showPeaks && ttf.getOnsetElement() != null && ttf.getOnsetElement().isPeak) {
				Color color = new Color(0x90422d); // sienna-ish
				bufferedGraphics.setColor(color);
				bufferedGraphics.fillRect(timeCoordinate, height / 2, 6, 6);
			}

			if (showStats) {
				Map<Integer, ToneMapStatistics> bands = toneMap.getStatisticsBands();
				double maxVariance = 0;
				double maxSum = 0;
				double maxMean = 0;
				for (Entry<Integer, ToneMapStatistics> band : bands.entrySet()) {
					ToneMapStatistics bandStatistics = band.getValue();
					if (maxVariance < bandStatistics.variance) {
						maxVariance = bandStatistics.variance;
					}
					if (maxSum < bandStatistics.sum) {
						maxSum = bandStatistics.sum;
					}
					if (maxMean < bandStatistics.mean) {
						maxMean = bandStatistics.mean;
					}
				}

				for (Entry<Integer, ToneMapStatistics> band : bands.entrySet()) {
					int note = band.getKey();
					ToneMapStatistics bandStatistics = band.getValue();
					int centsCoordinate = getCentsCoordinate(note * 100);
					Color color = new Color(0x14ff14); // green-ish

					double variance = bandStatistics.variance;
					int greyValue = 0;
					if (maxVariance == 0) {
						greyValue = 0;
						color = Color.BLACK;
					} else if (variance == maxVariance) {
						greyValue = 255;
						color = Color.WHITE;
					} else {
						if (showLog) {
							greyValue = (int) (Math.log1p((variance) / (maxVariance)) / Math.log1p(1.0000001) * 255);
						} else {
							greyValue = (int) (((variance) / (maxVariance)) * 255);
						}
						greyValue = Math.max(0, greyValue);
						if (showColour) {
							color = rainbow[255 - greyValue];
						} else {
							color = new Color(greyValue, greyValue, greyValue);
						}
					}

					bufferedGraphics.setColor(color);
					bufferedGraphics.fillRect(0, centsCoordinate - height, 20, 6);

					double sum = bandStatistics.sum;
					greyValue = 0;
					if (maxSum == 0) {
						greyValue = 0;
						color = Color.BLACK;
					} else if (sum == maxSum) {
						greyValue = 255;
						color = Color.WHITE;
					} else {
						if (showLog) {
							greyValue = (int) (Math.log1p((sum) / (maxSum)) / Math.log1p(1.0000001) * 255);
						} else {
							greyValue = (int) (((sum) / (maxSum)) * 255);
						}
						greyValue = Math.max(0, greyValue);
						if (showColour) {
							color = rainbow[255 - greyValue];
						} else {
							color = new Color(greyValue, greyValue, greyValue);
						}
					}

					bufferedGraphics.setColor(color);
					bufferedGraphics.fillRect(30, centsCoordinate - height, 20, 6);

					double mean = bandStatistics.mean;
					greyValue = 0;
					if (maxMean == 0) {
						greyValue = 0;
						color = Color.BLACK;
					} else if (mean == maxMean) {
						greyValue = 255;
						color = Color.WHITE;
					} else {
						if (showLog) {
							greyValue = (int) (Math.log1p((mean) / (maxMean)) / Math.log1p(1.0000001) * 255);
						} else {
							greyValue = (int) (((mean) / (maxMean)) * 255);
						}
						greyValue = Math.max(0, greyValue);
						if (showColour) {
							color = rainbow[255 - greyValue];
						} else {
							color = new Color(greyValue, greyValue, greyValue);
						}
					}
					bufferedGraphics.setColor(color);
					bufferedGraphics.fillRect(60, centsCoordinate - height, 20, 6);
				}
			}

			if (showNotes) {
				Color color = Color.BLACK;
				NoteTrack[] tracks = toneMap.getNoteTracker().getTracks();
				for (NoteTrack track : tracks) {
					color = Color.BLACK;
					if (track != null) {
						color = COLORS[track.getNumber() < COLORS.length ? track.getNumber() - 1 : COLORS.length - 1];
					}
					NoteListElement[] nles = track.getNotes(timeStart);
					for (NoteListElement nle : nles) {
						int centsCoordinate = getCentsCoordinate(nle.note * 100);
						bufferedGraphics.setColor(color);
						bufferedGraphics.fillRect(timeCoordinate, centsCoordinate - height, width, height);
						if (nle.startTime == timeStart) {
							color = new Color(0xff7f50); // coral
							bufferedGraphics.setColor(color);
							bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 2, 6, 6);
						} else if (nle.endTime + nle.incrementTime == timeStart) {
							color = new Color(0x53868b); // cadetblue4
							bufferedGraphics.setColor(color);
							int timeEndCoordinate = getTimeCoordinate(timeStart + nle.incrementTime - timeAxisStart);
							bufferedGraphics.fillOval(timeEndCoordinate, centsCoordinate - height - 2, 6, 6);
						}
					}
				}
			}

			if (showBase) {
				Color color = Color.BLACK;
				NoteTrack track = toneMap.getNoteTracker().getBaseTrack();
				color = Color.BLACK;
				if (track != null) {
					color = Color.WHITE;
					NoteListElement[] nles = track.getNotes(timeStart);
					for (NoteListElement nle : nles) {
						int centsCoordinate = getCentsCoordinate(nle.note * 100);
						bufferedGraphics.setColor(color);
						bufferedGraphics.fillRect(timeCoordinate, centsCoordinate - height, width, height);
						if (nle.startTime == timeStart) {
							color = new Color(0xff7f50); // coral
							bufferedGraphics.setColor(color);
							bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 2, 6, 6);
						}
					}
				}
			}

			if (showChords) {
				Color color = Color.BLACK;
				NoteTrack track = toneMap.getNoteTracker().getChordTrack(1);
				if (track != null) {
					color = Color.YELLOW;
					NoteListElement[] nles = track.getNotes(timeStart);
					for (NoteListElement nle : nles) {
						int centsCoordinate = getCentsCoordinate((nle.note) * 100);
						bufferedGraphics.setColor(color);
						bufferedGraphics.fillRect(timeCoordinate, centsCoordinate - height, width, height);
						if (nle.startTime == timeStart) {
							color = new Color(0xff7f50); // coral
							bufferedGraphics.setColor(color);
							bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 2, 6, 6);
						}
					}
				}

				track = toneMap.getNoteTracker().getChordTrack(2);
				color = Color.BLACK;
				if (track != null) {
					color = Color.PINK;
					NoteListElement[] nles = track.getNotes(timeStart);
					for (NoteListElement nle : nles) {
						int centsCoordinate = getCentsCoordinate((nle.note) * 100);
						bufferedGraphics.setColor(color);
						bufferedGraphics.fillRect(timeCoordinate, centsCoordinate - height, width, height);
						if (nle.startTime == timeStart) {
							color = new Color(0xff7f50); // coral
							bufferedGraphics.setColor(color);
							bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 2, 6, 6);
						}
					}
				}
			}

			if (showBeats) {
				NoteTrack track = toneMap.getNoteTracker().getBeatTrack(1);
				Color color = Color.BLACK;
				if (track != null) {
					color = Color.CYAN;
					NoteListElement[] nles = track.getNotes(timeStart);
					for (NoteListElement nle : nles) {
						if (nle.startTime == timeStart) {
							int centsCoordinate = getCentsCoordinate((nle.note) * 100);
							bufferedGraphics.setColor(color);
							bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 2, 6, 6);
						}
					}
				}

				track = toneMap.getNoteTracker().getBeatTrack(2);
				color = Color.BLACK;
				if (track != null) {
					color = Color.GREEN;
					NoteListElement[] nles = track.getNotes(timeStart);
					for (NoteListElement nle : nles) {
						if (nle.startTime == timeStart) {
							int centsCoordinate = getCentsCoordinate((nle.note) * 100);
							bufferedGraphics.setColor(color);
							bufferedGraphics.fillOval(timeCoordinate, centsCoordinate - height - 2, 6, 6);
						}
					}
				}
			}
		}

	}

	private void drawGrid() {
		Color gridColor = new Color(50, 50, 50);
		double timeAxisRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_VIEW_TIME_AXIS_RANGE);

		for (int i = minCents; i < maxCents; i += 100) {
			int centsCoordinate = getCentsCoordinate(i);
			bufferedGraphics.setColor(Color.WHITE);
			bufferedGraphics.drawLine(0, centsCoordinate, 5, centsCoordinate);
			if (i % 1200 == 0) {
				bufferedGraphics.drawString(String.valueOf(i), 10, centsCoordinate);
				bufferedGraphics.setColor(gridColor);
				bufferedGraphics.drawLine(0, centsCoordinate, getWidth() - 1, centsCoordinate);
			}
		}

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
		return getHeight() - 1 - (int) (((double) (cents - minCents) / (double) (maxCents - minCents)) * getHeight());
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
