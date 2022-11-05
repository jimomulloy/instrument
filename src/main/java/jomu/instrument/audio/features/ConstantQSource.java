package jomu.instrument.audio.features;

import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.ConstantQ;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.audio.DispatchJunctionProcessor;

public class ConstantQSource {

	private float binHeight;
	private float[] binStartingPointsInCents;
	private float binWidth;
	private Map<Double, float[]> features = new TreeMap<>();
	private int size;
	private float[] startingPointsInHertz;
	int binsPerOctave = 12;
	ConstantQ constantQ;
	double constantQLag;
	int increment = 1024;
	int maximumFrequencyInCents = 10800;
	int minimumFrequencyInCents = 3600;
	float sampleRate = 44100;
	private AudioDispatcher dispatcher;

	public ConstantQSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
	}

	public float getBinHeight() {
		return binHeight;
	}

	public int getBinsPerOctave() {
		return binsPerOctave;
	}

	public float[] getBinStartingPointsInCents() {
		return binStartingPointsInCents;
	}

	public float getBinWidth() {
		return binWidth;
	}

	public ConstantQ getConstantQ() {
		return constantQ;
	}

	public double getConstantQLag() {
		return constantQLag;
	}

	public TreeMap<Double, float[]> getFeatures() {
		TreeMap<Double, float[]> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, float[]> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public int getIncrement() {
		return increment;
	}

	public int getMaximumFrequencyInCents() {
		return maximumFrequencyInCents;
	}

	public int getMinimumFrequencyInCents() {
		return minimumFrequencyInCents;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public int getSize() {
		return size;
	}

	public float[] getStartingPointsInHertz() {
		return startingPointsInHertz;
	}

	void clear() {
		features.clear();
	}

	void initialise() {
		float minimumFrequencyInHertz = (float) PitchConverter
				.absoluteCentToHertz(minimumFrequencyInCents);
		float maximumFrequencyInHertz = (float) PitchConverter
				.absoluteCentToHertz(maximumFrequencyInCents);

		constantQ = new ConstantQ(sampleRate, minimumFrequencyInHertz,
				maximumFrequencyInHertz, binsPerOctave);

		binWidth = increment / sampleRate;
		binHeight = 1200 / (float) binsPerOctave;

		startingPointsInHertz = constantQ.getFreqencies();
		binStartingPointsInCents = new float[startingPointsInHertz.length];
		for (int i = 0; i < binStartingPointsInCents.length; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter
					.hertzToAbsoluteCent(startingPointsInHertz[i]);
		}

		size = constantQ.getFFTlength();
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(
				sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(
				tarsosDSPFormat, size, size - increment);
		djp.setName("CQ");
		dispatcher.addAudioProcessor(djp);

		constantQLag = size / djp.getFormat().getSampleRate() - binWidth / 2.0;
		System.out.println(">>CQ size: " + size);
		System.out.println(">>CQ lag: " + constantQLag);
		features = new TreeMap<>();

		djp.addAudioProcessor(constantQ);
		djp.addAudioProcessor(new AudioProcessor() {

			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] values = constantQ.getMagnitudes().clone();
				features.put(audioEvent.getTimeStamp()
						- binWidth /* - constantQLag */, values);
				return true;
			}

			@Override
			public void processingFinished() {

			}
		});

		features.clear();
	}

	void removeFeatures(double endTime) {
		features.keySet().removeIf(key -> key <= endTime);
	}

}
