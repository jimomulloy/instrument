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

public class Visor extends JPanel implements OscilloscopeEventHandler, AudioFeatureObserver {

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
		spectralPeaksPanel = createSpectralPeaksPanel();
		tabbedPane.addTab("SP", spectralPeaksPanel);
		oscilloscopePanel = new OscilloscopePanel();
		tabbedPane.addTab("Oscilloscope", oscilloscopePanel);
		beadsPanel = createBeadsPanel();
		tabbedPane.addTab("Beads", beadsPanel);

		// spectrumPanel = createSpectrumPanel();
		// tabbedPane.addTab("Spectrum", spectrumPanel);
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

	private LinkedPanel createCQPanel() {
		CoordinateSystem constantQCS = getCoordinateSystem(AxisUnit.FREQUENCY);
		constantQCS.setMax(Axis.X, 20000);
		constantQPanel = new LinkedPanel(constantQCS);
		cqLayer = new CQLayer(constantQCS);
		constantQPanel.addLayer(new BackgroundLayer(constantQCS));
		constantQPanel.addLayer(cqLayer);
		// constantQ.addLayer(new PitchContourLayer(constantQCS,
		// player.getLoadedFile(),Color.red,2048,0));
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

	private LinkedPanel createBeadsPanel() {
		CoordinateSystem beadsCS = getCoordinateSystem(AxisUnit.FREQUENCY);
		beadsCS.setMax(Axis.X, 20000);
		beadsPanel = new LinkedPanel(beadsCS);
		beadsLayer = new BeadsLayer(beadsCS);
		beadsPanel.addLayer(new BackgroundLayer(beadsCS));
		beadsPanel.addLayer(cqLayer);
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

	private static class OscilloscopePanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4969781241442094359L;

		float data[];

		public OscilloscopePanel() {
			setMinimumSize(new Dimension(80, 60));
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

		public void paint(float[] data, AudioEvent event) {
			this.data = data;
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

				double currentMaxSpectralEnergy = 0;
				for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
					float[] spectralEnergy = column.getValue();
					for (int i = 0; i < spectralEnergy.length; i++) {
						currentMaxSpectralEnergy = Math.max(currentMaxSpectralEnergy, spectralEnergy[i]);
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
							int greyValue = 255 - (int) (Math.log1p(spectralEnergy[i])
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
			return "CQ Layer";
		}

		public void update(PitchFrame pitchFrame) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ConstantQSource cqs = pitchFrame.getConstantQFeatures().getCqs();
					binStartingPointsInCents = cqs.getBinStartingPointsInCents();
					binWidth = cqs.getBinWidth();
					binHeight = cqs.getBinHeight();
					TreeMap<Double, float[]> fs = pitchFrame.getConstantQFeatures().getFeatures();
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

	private static class BeadsLayer implements Layer {

		private TreeMap<Double, float[]> cqFeatures;
		private final CoordinateSystem cs;

		private float[] binStartingPointsInCents;
		private float binWidth;
		private float binHeight;

		public BeadsLayer(CoordinateSystem cs) {
			this.cs = cs;
		}

		public void draw(Graphics2D graphics) {

			if (cqFeatures != null) {
				Map<Double, float[]> spectralInfoSubMap = cqFeatures.subMap(cs.getMin(Axis.X) / 1000.0,
						cs.getMax(Axis.X) / 1000.0);

				double currentMaxSpectralEnergy = 0;
				for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
					float[] spectralEnergy = column.getValue();
					for (int i = 0; i < spectralEnergy.length; i++) {
						currentMaxSpectralEnergy = Math.max(currentMaxSpectralEnergy, spectralEnergy[i]);
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
							int greyValue = 255 - (int) (Math.log1p(spectralEnergy[i])
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
					List<FeatureFrame> beadsFeatures = pitchFrame.getBeadsFeatures();
					for (FeatureFrame beadsFeatureFrame : beadsFeatures) {
						// System.out.println(">> BEADS FRAME: " + beadsFeatureFrame);
					}
				}
			});
		}
	}

	private void setNewMixer(Mixer mixer) throws LineUnavailableException, UnsupportedAudioFileException {

		if (dispatcher != null) {
			dispatcher.stop();
		}
		currentMixer = mixer;

		float sampleRate = 44100;
		int bufferSize = 2048;
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

	@Override
	public void handleEvent(float[] data, AudioEvent event) {
		oscilloscopePanel.paint(data, event);
		oscilloscopePanel.repaint();
	}

	@Override
	public void pitchFrameAdded(PitchFrame pitchFrame) {
		beadsLayer.update(pitchFrame);
		cqLayer.update(pitchFrame);
		spectralPeaksLayer.update(pitchFrame);
		if (count % 10 == 0) {
			this.cqPanel.repaint();
			this.spectralPeaksPanel.repaint();
			// this.beadsPanel.repaint();
		}
		count++;
		// SpectralPeaksFeatures specFeatures = pitchFrame.getSpectralPeaksFeatures();
		// repaintSpectalInfo(specFeatures.getSpectralInfo().get(0));
	}
}
