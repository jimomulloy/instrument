package jomu.instrument.organs;

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
	int sampleRate = 44100;
	int increment = 2048;
	int minimumFrequencyInCents = 3600;
	int maximumFrequencyInCents = 10800;
	int binsPerOctave = 12;
	float maxSpectralEnergy = 0;
	float minSpectralEnergy = 100000;
	private TreeMap<Double, float[]> features = new TreeMap<>();
	double constantQLag;
	ConstantQ constantQ;

	public ConstantQSource(TarsosAudioIO tarsosIO) {
		super();
		this.tarsosIO = tarsosIO;
	}

	void initialise() {
		float minimumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(minimumFrequencyInCents);
		float maximumFrequencyInHertz = (float) PitchConverter.absoluteCentToHertz(maximumFrequencyInCents);

		constantQ = new ConstantQ(sampleRate, minimumFrequencyInHertz, maximumFrequencyInHertz, binsPerOctave);

		float binWith = increment / sampleRate;
		float binHeight = 1200 / (float) binsPerOctave;

		float[] startingPointsInHertz = constantQ.getFreqencies();
		float[] binStartingPointsInCents = new float[startingPointsInHertz.length];
		for (int i = 0; i < binStartingPointsInCents.length; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(startingPointsInHertz[i]);
		}

		int size = constantQ.getFFTlength();

		TarsosDSPAudioFormat tarsosDSPFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, true);
		DispatchJunctionProcessor djp = new DispatchJunctionProcessor(tarsosDSPFormat, size, size - increment);
		tarsosIO.getDispatcher().addAudioProcessor(djp);

		constantQLag = size / djp.getFormat().getSampleRate() - binWith / 2.0;// in seconds
		features = new TreeMap<Double, float[]>();

		djp.addAudioProcessor(constantQ);
		djp.addAudioProcessor(new AudioProcessor() {

			public void processingFinished() {
				float minValue = 5 / 1000000.0f;
				for (float[] magnitudes : features.values()) {
					for (int i = 0; i < magnitudes.length; i++) {
						magnitudes[i] = Math.max(minValue, magnitudes[i]);
						magnitudes[i] = (float) Math.log1p(magnitudes[i]);
						maxSpectralEnergy = Math.max(magnitudes[i], maxSpectralEnergy);
						minSpectralEnergy = Math.min(magnitudes[i], minSpectralEnergy);
					}
				}
				minSpectralEnergy = Math.abs(minSpectralEnergy);
			}

			public boolean process(AudioEvent audioEvent) {
				features.put(audioEvent.getTimeStamp() - constantQLag, constantQ.getMagnitudes().clone());
				// System.out.println(">>ConstantQ process");
				return true;
			}
		});

		features.clear();
	}

	void clear() {
		features.clear();
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public int getSampleRate() {
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

	public float getMaxSpectralEnergy() {
		return maxSpectralEnergy;
	}

	public float getMinSpectralEnergy() {
		return minSpectralEnergy;
	}

	public TreeMap<Double, float[]> getFeatures() {
		TreeMap<Double, float[]> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, float[]> entry : features.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue());
		}
		return clonedFeatures;
	}

	public double getConstantQLag() {
		return constantQLag;
	}

	public ConstantQ getConstantQ() {
		return constantQ;
	}

}
