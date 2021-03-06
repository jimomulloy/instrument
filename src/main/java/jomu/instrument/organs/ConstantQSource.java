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
			System.out.println(">>CQ Bin: " + i + ", " + binStartingPointsInCents[i]);
		}

		size = constantQ.getFFTlength();

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, size, size - increment);
		djp.setName("CQ");
		tarsosIO.getDispatcher().addAudioProcessor(djp);

		constantQLag = size / djp.getFormat().getSampleRate() - binWidth / 2.0;

		features = new TreeMap<Double, float[]>();

		djp.addAudioProcessor(constantQ);
		djp.addAudioProcessor(new AudioProcessor() {

			public void processingFinished() {

			}

			public boolean process(AudioEvent audioEvent) {
				System.out.println(">>CQ put audio event: " + audioEvent.getTimeStamp() + ", "
						+ audioEvent.getSamplesProcessed() + ", lag: " + constantQLag);
				float[] values = constantQ.getMagnitudes().clone();
				features.put(audioEvent.getTimeStamp() - binWidth /* - constantQLag */, values);
				return true;
			}
		});

		features.clear();
	}

	public float getBinWidth() {
		return binWidth;
	}

	public float getBinHeight() {
		return binHeight;
	}

	public float[] getStartingPointsInHertz() {
		return startingPointsInHertz;
	}

	public float[] getBinStartingPointsInCents() {
		return binStartingPointsInCents;
	}

	public int getSize() {
		return size;
	}

	void clear() {
		features.clear();
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public int getIncrement() {
		return increment;
	}

	public int getMinimumFrequencyInCents() {
		return minimumFrequencyInCents;
	}

	public int getMaximumFrequencyInCents() {
		return maximumFrequencyInCents;
	}

	public int getBinsPerOctave() {
		return binsPerOctave;
	}

	public TreeMap<Double, float[]> getFeatures() {
		TreeMap<Double, float[]> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, float[]> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	public double getConstantQLag() {
		return constantQLag;
	}

	public ConstantQ getConstantQ() {
		return constantQ;
	}

	void removeFeatures(double endTime) {
		features.keySet().removeIf(key -> key <= endTime);
	}

}
