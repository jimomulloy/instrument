package jomu.instrument.audio.features;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;

public class BandedPitchDetectorSource {

	private PitchDetectorSource pitchDetectorSourceBand1;

	private PitchDetectorSource pitchDetectorSourceBand2;

	private PitchDetectorSource pitchDetectorSourceBand3;

	private PitchDetectorSource pitchDetectorSourceBand4;

	private PitchDetectorSource pitchDetectorSourceBand5;

	private Map<Integer, PitchDetectorSource> sourceMap = new HashMap<>();

	private AudioDispatcher dispatcher;

	private int sampleRate;

	public BandedPitchDetectorSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = (int) dispatcher.getFormat().getSampleRate();
		pitchDetectorSourceBand1 = new PitchDetectorSource(dispatcher, 256);
		sourceMap.put(256, pitchDetectorSourceBand1);
		pitchDetectorSourceBand2 = new PitchDetectorSource(dispatcher, 512);
		sourceMap.put(512, pitchDetectorSourceBand2);
		pitchDetectorSourceBand3 = new PitchDetectorSource(dispatcher, 1024);
		sourceMap.put(1024, pitchDetectorSourceBand3);
		pitchDetectorSourceBand4 = new PitchDetectorSource(dispatcher, 2048);
		sourceMap.put(2048, pitchDetectorSourceBand4);
		pitchDetectorSourceBand5 = new PitchDetectorSource(dispatcher, 4096);
		sourceMap.put(4096, pitchDetectorSourceBand5);
	}

	public float getBinHeight(int bufferSize) {
		return sourceMap.get(bufferSize).getBinHeight();
	}

	public float[] getBinStartingPointsInCents(int bufferSize) {
		return sourceMap.get(bufferSize).getBinStartingPointsInCents();
	}

	public float getBinWidth(int bufferSize) {
		return sourceMap.get(bufferSize).getBinWidth();
	}

	public Map<Integer, TreeMap<Double, SpectrogramInfo>> getFeatures() {
		Map<Integer, TreeMap<Double, SpectrogramInfo>> clonedFeatures = new HashMap<>();
		clonedFeatures.put(pitchDetectorSourceBand1.getBufferSize(), pitchDetectorSourceBand1.getFeatures());
		clonedFeatures.put(pitchDetectorSourceBand2.getBufferSize(), pitchDetectorSourceBand2.getFeatures());
		clonedFeatures.put(pitchDetectorSourceBand3.getBufferSize(), pitchDetectorSourceBand3.getFeatures());
		clonedFeatures.put(pitchDetectorSourceBand4.getBufferSize(), pitchDetectorSourceBand4.getFeatures());
		clonedFeatures.put(pitchDetectorSourceBand5.getBufferSize(), pitchDetectorSourceBand5.getFeatures());
		return clonedFeatures;
	}

	void clear() {
		pitchDetectorSourceBand1.clear();
		pitchDetectorSourceBand2.clear();
		pitchDetectorSourceBand3.clear();
		pitchDetectorSourceBand4.clear();
		pitchDetectorSourceBand5.clear();
	}

	void initialise() {
		pitchDetectorSourceBand1.initialise();
		pitchDetectorSourceBand2.initialise();
		pitchDetectorSourceBand3.initialise();
		pitchDetectorSourceBand4.initialise();
		pitchDetectorSourceBand5.initialise();
	}

}
