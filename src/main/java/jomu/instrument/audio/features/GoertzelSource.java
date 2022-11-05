package jomu.instrument.audio.features;

import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler;

public class GoertzelSource implements FrequenciesDetectedHandler {

	private TreeMap<Double, GoertzelInfo> features = new TreeMap<>();

	private AudioDispatcher dispatcher;

	private float sampleRate;

	public GoertzelSource(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.sampleRate = dispatcher.getFormat().getSampleRate();
	}

	public TreeMap<Double, GoertzelInfo> getFeatures() {
		TreeMap<Double, GoertzelInfo> clonedFeatures = new TreeMap<>();
		for (java.util.Map.Entry<Double, GoertzelInfo> entry : features
				.entrySet()) {
			clonedFeatures.put(entry.getKey(), entry.getValue().clone());
		}
		return clonedFeatures;
	}

	@Override
	public void handleDetectedFrequencies(double timestamp,
			double[] frequencies, double[] powers, double[] allFrequencies,
			double[] allPowers) {
		// TODO Auto-generated method stub

	}

	void clear() {
		features.clear();
	}

	void initialise() {
		// maxSpectralEnergy = 0;
		// minSpectralEnergy = 100000;
		int blockSize = 8000;
		int overlap = 7500;

		int steps = 50; // 100 steps;
		/*
		 * double stepInCents = (highFrequencyInCents - lowFrequencyInCents) /
		 * (float) steps;
		 *
		 *
		 * binWith = (blockSize - overlap) / sampleRate; binHeight = (float)
		 * stepInCents; double[] frequencies = new double[steps];
		 * binStartingPointsInCents = new float[steps]; for(int i = 0 ; i< steps
		 * ; i++){ double valueInCents = i * stepInCents + lowFrequencyInCents;
		 * frequencies[i] = PitchConverter.absoluteCentToHertz(valueInCents);
		 * binStartingPointsInCents[i]=(float)valueInCents; }
		 *
		 * final TreeMap<Double, double[]> fe = new TreeMap<Double, double[]>();
		 *
		 * FrequenciesDetectedHandler handler= new FrequenciesDetectedHandler(){
		 * int i = 0;
		 *
		 * @Override public void handleDetectedFrequencies(double time, double[]
		 * frequencies, double[] powers, double[] allFrequencies, double[]
		 * allPowers) {
		 *
		 * double timeStamp = (Math.max(0, cs.getMin(Axis.X)/1000.0)) + i *
		 * binWith; i++; fe.put(timeStamp,allPowers.clone()); }};
		 *
		 * final GeneralizedGoertzel goertzel = new
		 * GeneralizedGoertzel(sampleRate, blockSize,frequencies, this);
		 * TarsosDSPAudioFormat tarsosDSPFormat = new
		 * TarsosDSPAudioFormat(44100, 16, 1, true, true);
		 * DispatchJunctionProcessor djp = new
		 * DispatchJunctionProcessor(tarsosDSPFormat, fftsize, overlap);
		 * djp.setName("SP"); tarsosIO.getDispatcher().addAudioProcessor(djp);
		 *
		 * djp.addAudioProcessor(goertzel);
		 *
		 * for (double[] magnitudes : fe.values()) { for (int i = 0; i <
		 * magnitudes.length; i++) { if(magnitudes[i]==0){ magnitudes[i] =
		 * 1.0/(float)1e10; } //to dB magnitudes[i] = 20 *
		 * Math.log(1+Math.abs(magnitudes[i]))/Math.log(10);
		 *
		 * maxSpectralEnergy = Math.max(magnitudes[i],maxSpectralEnergy);
		 * minSpectralEnergy = Math.min(magnitudes[i],minSpectralEnergy); } }
		 * minSpectralEnergy = Math.abs(minSpectralEnergy); this.features = fe;
		 * features.clear();
		 */
	}
}
