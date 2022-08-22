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

package jomu.instrument.organs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.Oscilloscope;
import be.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;
import be.tarsos.dsp.SpectralPeakProcessor.SpectralPeak;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.AmplitudeAxisLayer;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.HorizontalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.ui.layers.LegendLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.SpectrumLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.VerticalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.InputPanel;
import jomu.instrument.audio.analysis.FeatureFrame;
import jomu.instrument.tonemap.PitchSet;
import jomu.instrument.tonemap.TimeSet;
import jomu.instrument.tonemap.ToneMap;
import jomu.instrument.tonemap.ToneMapElement;
import jomu.instrument.tonemap.ToneMapMatrix;
import jomu.instrument.tonemap.ToneMapMatrix.Iterator;
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks;

public class Visor extends JPanel implements OscilloscopeEventHandler, PitchFrameObserver {

	private static class BandedPitchDetectLayer implements Layer {

		TreeMap<Double, SpectrogramInfo> features;
		private final CoordinateSystem cs;
		private float[] binStartingPointsInCents;
		private float binWidth;
		private float binHeight;

		public BandedPitchDetectLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D graphics) {

			if (features != null) {
				System.out.println(">>PD max amp: " + binWidth + ", " + binHeight);

				Map<Double, SpectrogramInfo> spSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				double maxAmp = 0.00001;
				for (Entry<Double, SpectrogramInfo> column : spSubMap.entrySet()) {

					double timeStart = column.getKey();// in seconds
					SpectrogramInfo spectrogramInfo = column.getValue();// in cents
					float pitch = (float) spectrogramInfo.pitchDetectionResult.getPitch(); // -1?
					float[] amplitudes = spectrogramInfo.amplitudes;
					// draw the pixels
					for (int i = 0; i < amplitudes.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = binStartingPointsInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							// int greyValue = 255 - (int) (Math.log1p(spectralEnergy[i])
							// / Math.log1p(currentMaxSpectralEnergy) * 255);
							if (amplitudes[i] > maxAmp) {
								maxAmp = amplitudes[i];
							}
							int greyValue = 255 - (int) (amplitudes[i] / (maxAmp) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									(int) Math.round(binWidth * 1000), (int) Math.ceil(binHeight));
							// graphics.fillRect((int) Math.round(timeStart * 1000),
							// Math.round(centsStartingPoint),
							// (int) Math.round(100), (int) Math.ceil(100));

						}
					}
					// System.out.println(">>PD max amp: " + maxAmp + ", " + timeStart);

					if (pitch > -1) {
						double cents = PitchConverter.hertzToAbsoluteCent(pitch);
						Color color = Color.red;
						// only draw the visible frequency range
						if (cents >= cs.getMin(Axis.Y) && cents <= cs.getMax(Axis.Y)) {
							// int greyValue = (int) (255F * probability);
							// greyValue = Math.max(0, greyValue);
							// color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), (int) cents, (int) Math.round(40),
									(int) Math.ceil(100));
						}
					}

				}

			}
		}

		@Override
		public String getName() {
			return "Pitch Detect Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					BandedPitchDetectorSource bpds = pitchFrame.getBandedPitchDetectorFeatures().getBpds();
					binStartingPointsInCents = bpds.getBinStartingPointsInCents(2048);
					binWidth = bpds.getBinWidth(2048);
					binHeight = bpds.getBinHeight(2048);
					Map<Integer, TreeMap<Double, SpectrogramInfo>> bfs = pitchFrame.getBandedPitchDetectorFeatures()
							.getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					TreeMap<Double, SpectrogramInfo> fs = bfs.get(2048);
					if (fs != null) {
						for (Entry<Double, SpectrogramInfo> entry : fs.entrySet()) {
							features.put(entry.getKey(), entry.getValue());
						}
					}
				}
			});
		}
	}
	private static class BeadsLayer implements Layer {

		private TreeMap<Double, float[][]> features;
		private final CoordinateSystem cs;

		private float[] binStartingPointsInCents;
		private float binWidth;
		private float binHeight;

		public BeadsLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D graphics) {

			if (features != null) {
				Map<Double, float[][]> spectralInfoSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);

				double currentMaxSpectralEnergy = 0;
				// for (Map.Entry<Double, float[][]> column : spectralInfoSubMap.entrySet()) {
				// float[][] spectralEnergy = column.getValue();
				// for (int i = 0; i < spectralEnergy.length; i++) {
				// currentMaxSpectralEnergy = Math.max(currentMaxSpectralEnergy,
				// spectralEnergy[i]);
				// }
				// }
				for (Map.Entry<Double, float[][]> column : spectralInfoSubMap.entrySet()) {
					double timeStart = column.getKey();// in seconds
					float[][] spectralEnergy = column.getValue();// in cents
					// draw the pixels
					for (int i = 0; i < spectralEnergy.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = (float) PitchConverter.hertzToAbsoluteCent(spectralEnergy[i][0]);
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							int greyValue = 255 - (int) (Math.log1p(spectralEnergy[i][1])
									/ Math.log1p(currentMaxSpectralEnergy) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									(int) Math.round(binWidth * 1000), (int) Math.ceil(binHeight));
						}
					}
				}
			}
		}

		@Override
		public String getName() {
			return "Beads Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (features == null) {
						features = new TreeMap<>();
					}
					List<FeatureFrame> ffs = pitchFrame.getBeadsFeatures();
					for (FeatureFrame ff : ffs) {
						float[][] fs = (float[][]) ff.get(SpectralPeaks.class.getSimpleName());
						features.put(ff.getStartTimeMS(), fs);
					}
				}
			});
		}
	}
	private static class CQLayer implements Layer {

		private TreeMap<Double, float[]> cqFeatures;
		private final CoordinateSystem cs;

		private float[] binStartingPointsInCents;
		private float binWidth;
		private float binHeight;

		public CQLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D graphics) {

			if (cqFeatures != null) {
				Map<Double, float[]> spectralInfoSubMap = cqFeatures.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				float minValue = 5 / 1000000.0F;
				float currentMaxSpectralEnergy = 0;
				for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
					float[] spectralEnergy = column.getValue();
					for (int i = 0; i < spectralEnergy.length; i++) {
						float magnitude = Math.max(minValue, spectralEnergy[i]);
						magnitude = (float) Math.log10(1 + (100.0 * magnitude));
						currentMaxSpectralEnergy = Math.max(currentMaxSpectralEnergy, magnitude);
					}
				}
				for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
					double timeStart = column.getKey();// in seconds
					float[] spectralEnergy = column.getValue();// in cents
					// draw the pixels
					for (int i = 0; i < spectralEnergy.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = binStartingPointsInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							// int greyValue = 255 - (int) (Math.log1p(spectralEnergy[i])
							// / Math.log1p(currentMaxSpectralEnergy) * 255);
							float magnitude = Math.max(minValue, spectralEnergy[i]);
							magnitude = (float) Math.log10(1 + (100.0 * magnitude));
							int greyValue = 255 - (int) (magnitude / (currentMaxSpectralEnergy) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									(int) Math.round(binWidth * 1000), (int) Math.ceil(binHeight));
						}
					}
				}
			}
		}

		@Override
		public String getName() {
			return "CQ Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ConstantQSource cqs = pitchFrame.getConstantQFeatures().getCqs();
					binStartingPointsInCents = cqs.getBinStartingPointsInCents();
					binWidth = cqs.getBinWidth();
					binHeight = cqs.getBinHeight();
					Map<Double, float[]> fs = pitchFrame.getConstantQFeatures().getFeatures();
					if (cqFeatures == null) {
						cqFeatures = new TreeMap<>();
					}
					for (java.util.Map.Entry<Double, float[]> entry : fs.entrySet()) {
						cqFeatures.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}
	private static class OnsetLayer implements Layer {

		private TreeMap<Double, OnsetInfo[]> features;
		private final CoordinateSystem cs;

		public OnsetLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D graphics) {

			if (features != null) {
				Map<Double, OnsetInfo[]> onsetInfoSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);

				for (Map.Entry<Double, OnsetInfo[]> column : onsetInfoSubMap.entrySet()) {
					double timeStart = column.getKey();// in seconds
					OnsetInfo[] onsetInfo = column.getValue();// in cents
					// draw the pixels
					for (int i = 0; i < onsetInfo.length; i++) {
						float centsStartingPoint = (float) (((cs.getMax(Axis.Y) - cs.getMin(Axis.Y)) / 2.0)
								+ cs.getMin(Axis.Y));
						Color color = Color.red;
						graphics.setColor(color);
						graphics.fillRect((int) Math.round(onsetInfo[i].time * 1000), Math.round(centsStartingPoint),
								(int) Math.round(100), (int) Math.ceil(100));
					}
				}
			}
		}

		@Override
		public String getName() {
			return "Onset Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Map<Double, OnsetInfo[]> fs = pitchFrame.getOnsetFeatures().getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					for (Entry<Double, OnsetInfo[]> entry : fs.entrySet()) {
						features.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}
	private static class OscilloscopePanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4969781241442094359L;

		float data[];

		public OscilloscopePanel() {
			setMinimumSize(new Dimension(80, 60));
		}

		public void paint(float[] data, AudioEvent event) {
			this.data = data;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g); // paint background
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.WHITE);
			if (data != null) {
				float width = getWidth();
				float height = getHeight();
				float halfHeight = height / 2;
				for (int i = 0; i < data.length; i += 4) {
					g.drawLine((int) (data[i] * width), (int) (halfHeight - data[i + 1] * height),
							(int) (data[i + 2] * width), (int) (halfHeight - data[i + 3] * height));
				}
			}
		}
	}
	private static class PitchDetectLayer implements Layer {

		TreeMap<Double, SpectrogramInfo> features;
		private final CoordinateSystem cs;
		private float[] binStartingPointsInCents;
		private float binWidth;
		private float binHeight;

		public PitchDetectLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D graphics) {

			if (features != null) {
				System.out.println(">>PD max amp: " + binWidth + ", " + binHeight);

				Map<Double, SpectrogramInfo> spSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				double maxAmp = 0.00001;
				for (Entry<Double, SpectrogramInfo> column : spSubMap.entrySet()) {

					double timeStart = column.getKey();// in seconds
					SpectrogramInfo spectrogramInfo = column.getValue();// in cents
					float pitch = (float) spectrogramInfo.pitchDetectionResult.getPitch(); // -1?
					float[] amplitudes = spectrogramInfo.amplitudes;
					// draw the pixels
					for (int i = 0; i < amplitudes.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = binStartingPointsInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							// int greyValue = 255 - (int) (Math.log1p(spectralEnergy[i])
							// / Math.log1p(currentMaxSpectralEnergy) * 255);
							if (amplitudes[i] > maxAmp) {
								maxAmp = amplitudes[i];
							}
							int greyValue = 255 - (int) (amplitudes[i] / (maxAmp) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									(int) Math.round(binWidth * 1000), (int) Math.ceil(binHeight));

						}
					}
					// System.out.println(">>PD max amp: " + maxAmp + ", " + timeStart);

					if (pitch > -1) {
						double cents = PitchConverter.hertzToAbsoluteCent(pitch);
						Color color = Color.red;
						// only draw the visible frequency range
						if (cents >= cs.getMin(Axis.Y) && cents <= cs.getMax(Axis.Y)) {
							// int greyValue = (int) (255F * probability);
							// greyValue = Math.max(0, greyValue);
							// color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), (int) cents, (int) Math.round(40),
									(int) Math.ceil(100));
						}
					}

				}

			}
		}

		@Override
		public String getName() {
			return "Pitch Detect Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					PitchDetectorSource pds = pitchFrame.getPitchDetectorFeatures().getPds();
					binStartingPointsInCents = pds.getBinStartingPointsInCents();
					binWidth = pds.getBinWidth();
					binHeight = pds.getBinHeight();
					TreeMap<Double, SpectrogramInfo> fs = pitchFrame.getPitchDetectorFeatures().getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					for (Entry<Double, SpectrogramInfo> entry : fs.entrySet()) {
						features.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}
	private static class ScalogramLayer implements Layer {

		private TreeMap<Double, ScalogramFrame> features;
		private final CoordinateSystem cs;

		public ScalogramLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		@Override
		public void draw(Graphics2D graphics) {
			if (features == null) {
				return;
			}
			Map<Double, ScalogramFrame> spectralInfoSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
					cs.getMax(Axis.X) / 1000.0);
			for (Map.Entry<Double, ScalogramFrame> frameEntry : spectralInfoSubMap.entrySet()) {
				double timeStart = frameEntry.getKey();// in seconds
				ScalogramFrame frame = frameEntry.getValue();// in cents

				for (int level = 0; level < frame.dataPerScale.length; level++) {
					for (int block = 0; block < frame.dataPerScale[level].length; block++) {
						Color color = Color.black;
						float centsStartingPoint = frame.startFrequencyPerLevel[level];
						float centsHeight = frame.stopFrequencyPerLevel[level] - centsStartingPoint;
						// only draw the visible frequency range
						if (centsStartingPoint + centsHeight >= cs.getMin(Axis.Y)
								&& centsStartingPoint <= cs.getMax(Axis.Y)) {
							float factor = Math.abs(frame.dataPerScale[level][block] / frame.currentMax);

							double startTimeBlock = timeStart + (block + 1) * frame.durationsOfBlockPerLevel[level];
							double timeDuration = frame.durationsOfBlockPerLevel[level];

							int greyValue = (int) (factor * 0.99 * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							color = Color.black;
							graphics.setColor(color);
							// graphics.fillRect((int) Math.round(startTimeBlock * 1000),
							// Math.round(centsStartingPoint),
							// (int) Math.round(timeDuration * 1000), (int) Math.ceil(centsHeight));
							graphics.fillRect((int) Math.round(startTimeBlock * 1000), Math.round(centsStartingPoint),
									(int) Math.round(100), (int) Math.ceil(100));
							// System.out.println(">>scalo: " + startTimeBlock + ", " + centsStartingPoint +
							// ", " + timeDuration + ", " + centsHeight);
						}
					}
				}
			}
		}

		@Override
		public String getName() {
			return "Scalogram Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ScalogramFeatures scf = pitchFrame.getScalogramFeatures();
					TreeMap<Double, ScalogramFrame> fs = scf.getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					for (java.util.Map.Entry<Double, ScalogramFrame> entry : fs.entrySet()) {
						features.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}
	private class SpectrogramLayer implements Layer {

		TreeMap<Double, SpectrogramInfo> features;
		private final CoordinateSystem cs;
		private float[] binStartingPointsInCents;
		private float[] binHeightInCents;
		private float binWidth;
		private float binHeight;

		public SpectrogramLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D graphics) {

			if (features != null) {
				Map<Double, SpectrogramInfo> spSubMap = features.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				double maxAmp = 100;
				for (Entry<Double, SpectrogramInfo> column : spSubMap.entrySet()) {

					double timeStart = column.getKey();// in seconds
					SpectrogramInfo spectrogramInfo = column.getValue();// in cents
					float pitch = (float) spectrogramInfo.pitchDetectionResult.getPitch(); // -1?
					float[] amplitudes = spectrogramInfo.amplitudes;
					// draw the pixels
					for (int i = 0; i < amplitudes.length; i++) {
						Color color = Color.black;
						float centsStartingPoint = binStartingPointsInCents[i];
						float binHeight = binHeightInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
							// int greyValue = 255 - (int) (Math.log1p(spectralEnergy[i])
							// / Math.log1p(currentMaxSpectralEnergy) * 255);
							if (amplitudes[i] > maxAmp) {
								maxAmp = amplitudes[i];
							}
							int greyValue = 255 - (int) (amplitudes[i] / (maxAmp) * 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
									(int) Math.round(binWidth * 1000), (int) Math.ceil(binHeight));

						}
					}

					if (pitch > -1) {
						double cents = PitchConverter.hertzToAbsoluteCent(pitch);
						Color color = Color.red;
						// only draw the visible frequency range
						if (cents >= cs.getMin(Axis.Y) && cents <= cs.getMax(Axis.Y)) {
							// int greyValue = (int) (255F * probability);
							// greyValue = Math.max(0, greyValue);
							// color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), (int) cents, (int) Math.round(40),
									(int) Math.ceil(100));
						}
					}

				}

			}
		}

		@Override
		public String getName() {
			return "Spectogram Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					SpectrogramSource ss = pitchFrame.getSpectrogramFeatures().getSs();
					binStartingPointsInCents = ss.getBinStartingPointsInCents();
					binHeightInCents = ss.getBinhHeightInCents();
					binWidth = ss.getBinWidth();
					binHeight = ss.getBinHeight();
					TreeMap<Double, SpectrogramInfo> fs = pitchFrame.getSpectrogramFeatures().getFeatures();
					if (features == null) {
						features = new TreeMap<>();
					}
					for (Entry<Double, SpectrogramInfo> entry : fs.entrySet()) {
						features.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}
	private static class SpectrumPeaksLayer implements Layer {

		private TreeMap<Double, SpectralInfo> spFeatures;
		private float[] spectrum;
		private List<Integer> peaksInBins;
		private float multiplier = 10;
		private int sampleRate;
		private int fftSize;
		private final CoordinateSystem cs;
		int noiseFloorMedianFilterLenth = 17;
		float noiseFloorFactor = 1.5F;
		int numberOfSpectralPeaks = 7;
		int minPeakSize = 5;

		public SpectrumPeaksLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D graphics) {

			if (spFeatures != null && !spFeatures.isEmpty()) {
				Map<Double, SpectralInfo> spectralInfoSubMap = spFeatures.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);

				for (Map.Entry<Double, SpectralInfo> column : spectralInfoSubMap.entrySet()) {
					double timeStart = column.getKey();// in seconds
					SpectralInfo spectralInfo = column.getValue();

					List<SpectralPeak> peaks = spectralInfo.getPeakList(noiseFloorMedianFilterLenth, noiseFloorFactor,
							numberOfSpectralPeaks, minPeakSize);

					int markerWidth = Math.round(LayerUtilities.pixelsToUnits(graphics, 7, true));
					int markerheight = Math.round(LayerUtilities.pixelsToUnits(graphics, 7, false));
					// draw the pixels
					for (SpectralPeak peak : peaks) {
						int bin = peak.getBin();
						float hertzValue = (bin * sampleRate) / (float) fftSize;
						int frequencyInCents = (int) Math
								.round(PitchConverter.hertzToAbsoluteCent(hertzValue) - markerWidth / 2.0f);

						Color color = Color.black;
						float magnitude = peak.getMagnitude();
						// only draw the visible frequency range
						if (frequencyInCents >= cs.getMin(Axis.Y) && frequencyInCents <= cs.getMax(Axis.Y)) {
							int greyValue = (int) ((magnitude / 100F) * 255F);
							// int greyValue = 255 - (int) (Math.log1p(magnitude)
							// / Math.log1p(100) * 255);
							// greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000), frequencyInCents, markerWidth,
									markerheight);
						}
					}
				}
			}
		}

		@Override
		public String getName() {
			return "Spectral Peaks Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					SpectralPeaksSource sps = pitchFrame.getSpectralPeaksFeatures().getSps();

					noiseFloorMedianFilterLenth = sps.getNoiseFloorMedianFilterLenth();
					noiseFloorFactor = sps.getNoiseFloorFactor();
					numberOfSpectralPeaks = sps.getNumberOfSpectralPeaks();
					minPeakSize = sps.getMinPeakSize();
					fftSize = sps.getTarsosIO().getContext().getBufferSize();
					sampleRate = (int) sps.getTarsosIO().getContext().getSampleRate();

					TreeMap<Double, SpectralInfo> fs = pitchFrame.getSpectralPeaksFeatures().getFeatures();
					if (spFeatures == null) {
						spFeatures = new TreeMap<>();
					}
					for (java.util.Map.Entry<Double, SpectralInfo> entry : fs.entrySet()) {
						spFeatures.put(entry.getKey(), entry.getValue());
					}
				}
			});
		}
	}
	private static class ToneMapLayer implements Layer {

		private TreeMap<Double, ToneMap> toneMaps;
		private final CoordinateSystem cs;

		public ToneMapLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D g) {

			if (toneMaps != null) {
				Map<Double, ToneMap> toneMapsSubMap = toneMaps.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);
				for (Map.Entry<Double, ToneMap> column : toneMapsSubMap.entrySet()) {
					double timeStart = column.getKey();
					ToneMap toneMap = column.getValue();
					ToneMapMatrix toneMapMatrix = toneMap.getMatrix();
					TimeSet timeSet = toneMap.getTimeSet();
					PitchSet pitchSet = toneMap.getPitchSet();
					timeStart = timeSet.getStartTime();

					if (toneMapMatrix != null) {

						Iterator mapIterator = toneMapMatrix.newIterator();

						double ampT;
						double lowThreshhold = 0.0;
						double highThreshhold = 100.0;
						mapIterator.firstPitch();
						double maxAmplitude = -1;
						do {

							ToneMapElement toneMapElement = mapIterator.getElement();
							if (toneMapElement != null) {
								double amplitude = 100.0 * toneMapElement.preAmplitude / 1.0;
								if (amplitude > maxAmplitude) {
									maxAmplitude = amplitude;
								}
								if (amplitude == -1) {
									g.setColor(new Color(155, 155, 155));
								} else if (amplitude < lowThreshhold) {
									g.setColor(Color.black);
								} else if (amplitude > highThreshhold) {
									g.setColor(Color.red);
								} else {
									ampT = (amplitude - lowThreshhold) / (highThreshhold - lowThreshhold);
									g.setColor(new Color((int) (255 * ampT), 0, (int) (255 * (1 - ampT))));
								}
								int pitchIndex = mapIterator.getPitchIndex();
								double cents = PitchConverter.hertzToAbsoluteCent(pitchSet.getFreq(pitchIndex));

								double width = timeSet.getEndTime() - timeSet.getStartTime();

								g.fillRect((int) Math.floor(timeStart * 1000), (int) Math.floor(cents),
										(int) Math.round(width * 1000), 100);

							}

						} while (mapIterator.nextPitch());
					}
				}
			}
		}

		@Override
		public String getName() {
			return "ToneMap Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ToneMap toneMap = pitchFrame.getConstantQFeatures().getToneMap();
					// ToneMap toneMap = pitchFrame.getPitchDetectorFeatures().getToneMap();
					if (toneMap != null) {
						if (toneMaps == null) {
							toneMaps = new TreeMap<>();
						}
						toneMaps.put(pitchFrame.getStart() / 1000.0, toneMap);
					}
				}
			});
		}
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 3501426880288136245L;
	int counter;
	double threshold;
	AudioDispatcher dispatcher;
	Mixer currentMixer;
	private OscilloscopePanel oscilloscopePanel;
	private LinkedPanel spectrumPanel;
	private SpectrumLayer spectrumLayer;
	private SpectrumLayer noiseFloorLayer;
	// current frequencies and amplitudes of peak list, for sensory dissonance curve
	private List<Double> frequencies;
	private List<Double> amplitudes;
	private int noiseFloorMedianFilterLenth;// 35
	private float noiseFloorFactor;
	private int numberOfSpectralPeaks;
	private int minPeakSize;
	private LinkedPanel constantQPanel;
	private LegendLayer legend;
	private LinkedPanel cqPanel;
	private CQLayer cqLayer;
	private int count = 0;
	private LinkedPanel spectralPeaksPanel;
	private SpectrumPeaksLayer spectralPeaksLayer;
	private LinkedPanel beadsPanel;
	private BeadsLayer beadsLayer;
	private LinkedPanel pitchDetectPanel;
	private PitchDetectLayer pdLayer;

	private SpectrogramLayer sLayer;

	private LinkedPanel spectrogramPanel;

	private LinkedPanel scalogramPanel;

	private ScalogramLayer scalogramLayer;

	private LinkedPanel toneMapPanel;

	private ToneMapLayer toneMapLayer;

	private LinkedPanel onsetPanel;

	private OnsetLayer onsetLayer;

	private LinkedPanel bandedPitchDetectPanel;

	private BandedPitchDetectLayer bpdLayer;

	public Visor() {
		this.setLayout(new BorderLayout());

		JPanel inputPanel = new InputPanel();
		// add(inputPanel);
		inputPanel.addPropertyChangeListener("mixer", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				try {
					setNewMixer((Mixer) arg0.getNewValue());
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		JTabbedPane tabbedPane = new JTabbedPane();
		this.add(inputPanel, BorderLayout.NORTH);
		this.add(tabbedPane, BorderLayout.CENTER);
		cqPanel = createCQPanel();
		tabbedPane.addTab("CQ", cqPanel);
		bandedPitchDetectPanel = createBandedPitchDetectPanel();
		tabbedPane.addTab("Banded Pitch", bandedPitchDetectPanel);
		toneMapPanel = createToneMapPanel();
		tabbedPane.addTab("TM", toneMapPanel);
		pitchDetectPanel = createPitchDetectPanel();
		tabbedPane.addTab("Pitch", pitchDetectPanel);
		spectrogramPanel = createSpectogramPanel();
		tabbedPane.addTab("Spectogram", spectrogramPanel);
		scalogramPanel = createScalogramPanel();
		tabbedPane.addTab("Scalogram", scalogramPanel);
		onsetPanel = createOnsetPanel();
		tabbedPane.addTab("Onset", onsetPanel);
		spectralPeaksPanel = createSpectralPeaksPanel();
		tabbedPane.addTab("SP", spectralPeaksPanel);
		oscilloscopePanel = new OscilloscopePanel();
		tabbedPane.addTab("Oscilloscope", oscilloscopePanel);
		beadsPanel = createBeadsPanel();
		tabbedPane.addTab("Beads", beadsPanel);

		// spectrumPanel = createSpectrumPanel();
		// tabbedPane.addTab("Spectrum", spectrumPanel);
	}

	private LinkedPanel createBandedPitchDetectPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		bandedPitchDetectPanel = new LinkedPanel(cs);
		bpdLayer = new BandedPitchDetectLayer(cs);
		bandedPitchDetectPanel.addLayer(new BackgroundLayer(cs));
		bandedPitchDetectPanel.addLayer(bpdLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		bandedPitchDetectPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		bandedPitchDetectPanel.addLayer(new ZoomMouseListenerLayer());
		bandedPitchDetectPanel.addLayer(new DragMouseListenerLayer(cs));
		bandedPitchDetectPanel.addLayer(new SelectionLayer(cs));
		bandedPitchDetectPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		bandedPitchDetectPanel.addLayer(legend);
		legend.addEntry("Pitch", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				bandedPitchDetectPanel.repaint();
			}
		};
		bandedPitchDetectPanel.getViewPort().addViewPortChangedListener(listener);
		return bandedPitchDetectPanel;
	}

	private LinkedPanel createBeadsPanel() {
		CoordinateSystem beadsCS = getCoordinateSystem(AxisUnit.FREQUENCY);
		beadsCS.setMax(Axis.X, 20000);
		beadsPanel = new LinkedPanel(beadsCS);
		beadsLayer = new BeadsLayer(beadsCS);
		beadsPanel.addLayer(new BackgroundLayer(beadsCS));
		beadsPanel.addLayer(beadsLayer);
		beadsPanel.addLayer(new VerticalFrequencyAxisLayer(beadsCS));
		beadsPanel.addLayer(new ZoomMouseListenerLayer());
		beadsPanel.addLayer(new DragMouseListenerLayer(beadsCS));
		beadsPanel.addLayer(new SelectionLayer(beadsCS));
		beadsPanel.addLayer(new TimeAxisLayer(beadsCS));

		legend = new LegendLayer(beadsCS, 110);
		beadsPanel.addLayer(legend);
		legend.addEntry("Beads", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				beadsPanel.repaint();
			}
		};
		beadsPanel.getViewPort().addViewPortChangedListener(listener);
		return beadsPanel;
	}

	private LinkedPanel createCQPanel() {
		CoordinateSystem constantQCS = getCoordinateSystem(AxisUnit.FREQUENCY);
		constantQCS.setMax(Axis.X, 20000);
		constantQPanel = new LinkedPanel(constantQCS);
		cqLayer = new CQLayer(constantQCS);
		constantQPanel.addLayer(new BackgroundLayer(constantQCS));
		constantQPanel.addLayer(cqLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		constantQPanel.addLayer(new VerticalFrequencyAxisLayer(constantQCS));
		constantQPanel.addLayer(new ZoomMouseListenerLayer());
		constantQPanel.addLayer(new DragMouseListenerLayer(constantQCS));
		constantQPanel.addLayer(new SelectionLayer(constantQCS));
		constantQPanel.addLayer(new TimeAxisLayer(constantQCS));

		legend = new LegendLayer(constantQCS, 110);
		constantQPanel.addLayer(legend);
		legend.addEntry("ConstantQ", Color.BLACK);
		legend.addEntry("Pitch estimations", Color.RED);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				constantQPanel.repaint();
			}
		};
		constantQPanel.getViewPort().addViewPortChangedListener(listener);
		return constantQPanel;
	}

	private LinkedPanel createOnsetPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		LinkedPanel constantQPanel = new LinkedPanel(cs);
		onsetLayer = new OnsetLayer(cs);
		constantQPanel.addLayer(new BackgroundLayer(cs));
		constantQPanel.addLayer(onsetLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		constantQPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		constantQPanel.addLayer(new ZoomMouseListenerLayer());
		constantQPanel.addLayer(new DragMouseListenerLayer(cs));
		constantQPanel.addLayer(new SelectionLayer(cs));
		constantQPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		constantQPanel.addLayer(legend);
		legend.addEntry("Onset", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				constantQPanel.repaint();
			}
		};
		constantQPanel.getViewPort().addViewPortChangedListener(listener);
		return constantQPanel;
	}

	private LinkedPanel createPitchDetectPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		pitchDetectPanel = new LinkedPanel(cs);
		pdLayer = new PitchDetectLayer(cs);
		pitchDetectPanel.addLayer(new BackgroundLayer(cs));
		pitchDetectPanel.addLayer(pdLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		pitchDetectPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		pitchDetectPanel.addLayer(new ZoomMouseListenerLayer());
		pitchDetectPanel.addLayer(new DragMouseListenerLayer(cs));
		pitchDetectPanel.addLayer(new SelectionLayer(cs));
		pitchDetectPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		pitchDetectPanel.addLayer(legend);
		legend.addEntry("Pitch", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				pitchDetectPanel.repaint();
			}
		};
		pitchDetectPanel.getViewPort().addViewPortChangedListener(listener);
		return pitchDetectPanel;
	}

	private LinkedPanel createScalogramPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		scalogramPanel = new LinkedPanel(cs);
		scalogramLayer = new ScalogramLayer(cs);
		scalogramPanel.addLayer(new BackgroundLayer(cs));
		scalogramPanel.addLayer(scalogramLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		scalogramPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		scalogramPanel.addLayer(new ZoomMouseListenerLayer());
		scalogramPanel.addLayer(new DragMouseListenerLayer(cs));
		scalogramPanel.addLayer(new SelectionLayer(cs));
		scalogramPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		scalogramPanel.addLayer(legend);
		legend.addEntry("Scalogram", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				scalogramPanel.repaint();
			}
		};
		scalogramPanel.getViewPort().addViewPortChangedListener(listener);
		return scalogramPanel;
	}

	private LinkedPanel createSpectogramPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		spectrogramPanel = new LinkedPanel(cs);
		sLayer = new SpectrogramLayer(cs);
		spectrogramPanel.addLayer(new BackgroundLayer(cs));
		spectrogramPanel.addLayer(sLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		spectrogramPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		spectrogramPanel.addLayer(new ZoomMouseListenerLayer());
		spectrogramPanel.addLayer(new DragMouseListenerLayer(cs));
		spectrogramPanel.addLayer(new SelectionLayer(cs));
		spectrogramPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		spectrogramPanel.addLayer(legend);
		legend.addEntry("Pitch", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				spectrogramPanel.repaint();
			}
		};
		spectrogramPanel.getViewPort().addViewPortChangedListener(listener);
		return spectrogramPanel;
	}

	private LinkedPanel createSpectralPeaksPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		spectralPeaksPanel = new LinkedPanel(cs);
		spectralPeaksLayer = new SpectrumPeaksLayer(cs);
		spectralPeaksPanel.addLayer(new BackgroundLayer(cs));
		spectralPeaksPanel.addLayer(spectralPeaksLayer);
		spectralPeaksPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		spectralPeaksPanel.addLayer(new ZoomMouseListenerLayer());
		spectralPeaksPanel.addLayer(new DragMouseListenerLayer(cs));
		spectralPeaksPanel.addLayer(new SelectionLayer(cs));
		spectralPeaksPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		spectralPeaksPanel.addLayer(legend);
		legend.addEntry("SpectralPeaks", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				spectralPeaksPanel.repaint();
			}
		};
		spectralPeaksPanel.getViewPort().addViewPortChangedListener(listener);
		return spectralPeaksPanel;
	}

	private LinkedPanel createSpectrumPanel() {
		CoordinateSystem cs = new CoordinateSystem(AxisUnit.FREQUENCY, AxisUnit.AMPLITUDE, 0, 10000, false);
		cs.setMax(Axis.X, 4800);
		cs.setMax(Axis.X, 13200);
		spectrumLayer = new SpectrumLayer(cs, 1024, 44100, Color.red);
		noiseFloorLayer = new SpectrumLayer(cs, 1024, 44100, Color.gray);

		spectrumPanel = new LinkedPanel(cs);
		spectrumPanel.addLayer(new ZoomMouseListenerLayer());
		spectrumPanel.addLayer(new DragMouseListenerLayer(cs));
		spectrumPanel.addLayer(new BackgroundLayer(cs));
		spectrumPanel.addLayer(new AmplitudeAxisLayer(cs));

		spectrumPanel.addLayer(new SelectionLayer(cs));
		spectrumPanel.addLayer(new HorizontalFrequencyAxisLayer(cs));
		spectrumPanel.addLayer(spectrumLayer);
		spectrumPanel.addLayer(noiseFloorLayer);

		spectrumPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			boolean painting = false;

			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				if (!painting) {
					painting = true;
					spectrumPanel.repaint();
					painting = false;
				}
			}
		});
		return spectrumPanel;
	}

	private LinkedPanel createToneMapPanel() {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		cs.setMax(Axis.X, 20000);
		toneMapPanel = new LinkedPanel(cs);
		toneMapLayer = new ToneMapLayer(cs);
		toneMapPanel.addLayer(new BackgroundLayer(cs));
		toneMapPanel.addLayer(toneMapLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,1024,0));
		toneMapPanel.addLayer(new VerticalFrequencyAxisLayer(cs));
		toneMapPanel.addLayer(new ZoomMouseListenerLayer());
		toneMapPanel.addLayer(new DragMouseListenerLayer(cs));
		toneMapPanel.addLayer(new SelectionLayer(cs));
		toneMapPanel.addLayer(new TimeAxisLayer(cs));

		legend = new LegendLayer(cs, 110);
		toneMapPanel.addLayer(legend);
		legend.addEntry("ToneMap", Color.BLACK);
		ViewPortChangedListener listener = new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				toneMapPanel.repaint();
			}
		};
		toneMapPanel.getViewPort().addViewPortChangedListener(listener);
		return toneMapPanel;
	}

	private CoordinateSystem getCoordinateSystem(AxisUnit yUnits) {
		float minValue = -1000;
		float maxValue = 1000;
		if (yUnits == AxisUnit.FREQUENCY) {
			minValue = 400;
			maxValue = 12000;
		}
		return new CoordinateSystem(yUnits, minValue, maxValue);
	}

	@Override
	public void handleEvent(float[] data, AudioEvent event) {
		oscilloscopePanel.paint(data, event);
		oscilloscopePanel.repaint();
	}

	@Override
	public void pitchFrameAdded(PitchFrame pitchFrame) {
		updateView(pitchFrame);
	}

	@Override
	public void pitchFrameChanged(PitchFrame pitchFrame) {
		updateView(pitchFrame);
	}

	public void repaintSpectalInfo(SpectralInfo info) {

		spectrumLayer.clearPeaks();
		spectrumLayer.setSpectrum(info.getMagnitudes());
		noiseFloorLayer.setSpectrum(info.getNoiseFloor(noiseFloorMedianFilterLenth, noiseFloorFactor));

		List<SpectralPeak> peaks = info.getPeakList(noiseFloorMedianFilterLenth, noiseFloorFactor,
				numberOfSpectralPeaks, minPeakSize);

		StringBuilder sb = new StringBuilder("Frequency(Hz);Step(cents);Magnitude\n");
		frequencies.clear();
		amplitudes.clear();
		for (SpectralPeak peak : peaks) {

			String message = String.format("%.2f;%.2f;%.2f\n", peak.getFrequencyInHertz(),
					peak.getRelativeFrequencyInCents(), peak.getMagnitude());
			sb.append(message);
			// float peakFrequencyInCents =(float)
			// PitchConverter.hertzToAbsoluteCent(peak.getFrequencyInHertz());
			spectrumLayer.setPeak(peak.getBin());
			frequencies.add((double) peak.getFrequencyInHertz());
			amplitudes.add((double) peak.getMagnitude());

		}
		// textArea.setText(sb.toString());
		this.spectrumPanel.repaint();
	}

	private void setNewMixer(Mixer mixer) throws LineUnavailableException, UnsupportedAudioFileException {

		if (dispatcher != null) {
			dispatcher.stop();
		}
		currentMixer = mixer;

		float sampleRate = 44100;
		int bufferSize = 1024;
		int overlap = 0;

		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
		final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
		line.start();
		final AudioInputStream stream = new AudioInputStream(line);

		JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
		// create a new dispatcher
		dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);

		// add a processor, handle percussion event.
		// dispatcher.addAudioProcessor(new DelayEffect(400,0.3,sampleRate));
		dispatcher.addAudioProcessor(new Oscilloscope(this));
		// dispatcher.addAudioProcessor(new AudioPlayer(format));

		// run the dispatcher (on a new thread).
		new Thread(dispatcher, "Audio dispatching").start();
	}

	public void updateToneMap(PitchFrame pitchFrame) {
		toneMapLayer.update(pitchFrame);
		this.toneMapPanel.repaint();
	}

	private void updateView(PitchFrame pitchFrame) {
		scalogramLayer.update(pitchFrame);
		toneMapLayer.update(pitchFrame);
		beadsLayer.update(pitchFrame);
		cqLayer.update(pitchFrame);
		onsetLayer.update(pitchFrame);
		spectralPeaksLayer.update(pitchFrame);
		pdLayer.update(pitchFrame);
		bpdLayer.update(pitchFrame);
		sLayer.update(pitchFrame);
		// if (count % 10 == 0) {
		this.scalogramPanel.repaint();
		this.toneMapPanel.repaint();
		this.spectrogramPanel.repaint();
		this.cqPanel.repaint();
		this.onsetPanel.repaint();
		this.spectralPeaksPanel.repaint();
		this.pitchDetectPanel.repaint();
		this.bandedPitchDetectPanel.repaint();
		this.beadsPanel.repaint();
		// }
		count++;
		// SpectralPeaksFeatures specFeatures = pitchFrame.getSpectralPeaksFeatures();
		// repaintSpectalInfo(specFeatures.getSpectralInfo().get(0));
	}
}
