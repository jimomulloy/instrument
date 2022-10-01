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

	TarsosAudioIO tarsosIO;
	float sampleRate = 44100;
	int increment = 1024;
	int minimumFrequencyInCents = 3600;
	int maximumFrequencyInCents = 10800;
	int binsPerOctave = 12;
	private Map<Double, float[]> features = new TreeMap<>();
	double constantQLag;
	ConstantQ constantQ;
	private float binWidth;
	private float binHeight;
	private float[] startingPointsInHertz;
	private float[] binStartingPointsInCents;
	private int size;

	public ConstantQSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
		this.sampleRate = tarsosIO.getContext().getSampleRate();
	}

	void clear() {
		features.clear();
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

	void initialise() {
		float minimumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(minimumFrequencyInCents);
		float maximumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(maximumFrequencyInCents);

		constantQ = new ConstantQ(sampleRate, minimumFrequencyInHertz, maximumFrequencyInHertz, binsPerOctave);

		binWidth = increment / sampleRate;
		binHeight = 1200 / (float) binsPerOctave;

		startingPointsInHertz = constantQ.getFreqencies();
		binStartingPointsInCents = new float[startingPointsInHertz.length];
		for (int i = 0; i < binStartingPointsInCents.length; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(startingPointsInHertz[i]);
		}

		System.out.println(">>CQ start hertz: " + startingPointsInHertz[0]);
		System.out.println(">>CQ end hertz: " + startingPointsInHertz[startingPointsInHertz.length -1]);
		System.out.println(">>CQ start cents: " + binStartingPointsInCents[0]);
		System.out.println(">>CQ end cents: " + binStartingPointsInCents[binStartingPointsInCents.length -1]);
		System.out.println(">>CQ start midi: " + PitchConverter.hertzToMidiKey((double)startingPointsInHertz[0]));
		System.out.println(">>CQ end midi: " + PitchConverter.hertzToMidiKey((double)startingPointsInHertz[startingPointsInHertz.length -1]));

		size = constantQ.getFFTlength();

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, size, size - increment);
		djp.setName("CQ");
		tarsosIO.getDispatcher().addAudioProcessor(djp);

		constantQLag = size / djp.getFormat().getSampleRate() - binWidth / 2.0;

		features = new TreeMap<Double, float[]>();

		djp.addAudioProcessor(constantQ);
		djp.addAudioProcessor(new AudioProcessor() {

			public boolean process(AudioEvent audioEvent) {
				float[] values = constantQ.getMagnitudes().clone();
				features.put(audioEvent.getTimeStamp() - binWidth /* - constantQLag */, values);
				return true;
			}

			public void processingFinished() {

			}
		});

		features.clear();
	}

	void removeFeatures(double endTime) {
		features.keySet().removeIf(key -> key <= endTime);
	}

}
