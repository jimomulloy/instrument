package jomu.instrument.organs;

import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.ConstantQ;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.audio.DispatchJunctionProcessor;
import jomu.instrument.audio.TarsosAudioIO;

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
	TarsosAudioIO tarsosIO;

	public ConstantQSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
		this.sampleRate = tarsosIO.getContext().getSampleRate();
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

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
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

		// System.out.println(">>CQ start hertz: " + startingPointsInHertz[0]);
		// System.out.println(">>CQ end hertz: " +
		// startingPointsInHertz[startingPointsInHertz.length -1]);
		// System.out.println(">>CQ start cents: " +
		// binStartingPointsInCents[0]);
		// System.out.println(">>CQ end cents: " +
		// binStartingPointsInCents[binStartingPointsInCents.length -1]);
		// System.out.println(">>CQ start midi: " +
		// PitchConverter.hertzToMidiKey((double)startingPointsInHertz[0]));
		// System.out.println(">>CQ end midi: " +
		// PitchConverter.hertzToMidiKey((double)startingPointsInHertz[startingPointsInHertz.length
		// -1]));

		size = constantQ.getFFTlength();
		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(
				sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(
				tarsosDSPFormat, size, size - increment);
		djp.setName("CQ");
		tarsosIO.getDispatcher().addAudioProcessor(djp);

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
