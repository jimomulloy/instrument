package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.AudioTuner;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
import jomu.instrument.audio.features.PeakInfo;
import jomu.instrument.audio.features.PeakProcessor;
import jomu.instrument.audio.features.PeakProcessor.SpectralPeak;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.organs.Hearing;
import jomu.instrument.world.WorldModel;
import jomu.instrument.world.tonemap.ToneMap;

public class AudioCQProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private float tmMax = 0;

	private WorldModel worldModel;

	public AudioCQProcessor(NuCell cell) {
		super();
		this.cell = cell;
		worldModel = Instrument.getInstance().getWorldModel();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		// System.out.println(">>getAudioCQProcessor");
		// System.out.println(cell.toString());
		int sequence;
		String streamId;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>ConstantQMessageProcessor accept: " + message
					+ ", streamId: " + streamId);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator()
						.getHearing();
				ToneMap toneMap = worldModel.getAtlas().getToneMap(
						buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
				AudioFeatureProcessor afp = hearing
						.getAudioFeatureProcessor(streamId);
				if (afp != null) {
					AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
					ConstantQFeatures cqf = aff.getConstantQFeatures();
					cqf.buildToneMapFrame(toneMap);
					toneMap.getTimeFrame().compress(10F);
					toneMap.getTimeFrame().square();
					toneMap.getTimeFrame().lowThreshold(0.01, 0.0000001);
					toneMap.getTimeFrame().normaliseThreshold(200, 0.0000001);
					float maxAmplitude = (float) toneMap.getTimeFrame()
							.getMaxAmplitude();
					System.out.println(
							">>MAX AMP: " + maxAmplitude + ", " + tmMax);
					if (tmMax < maxAmplitude) {
						tmMax = maxAmplitude;
					}
					// toneMap.getTimeFrame().deNoise(0.01);
					// toneMap.getTimeFrame()
					// .normalise(tmMax > 0.01F ? tmMax : 0.01F);

					maxAmplitude = (float) toneMap.getTimeFrame()
							.getMaxAmplitude();
					float sampleRate = toneMap.getTimeFrame().getTimeSet()
							.getSampleRate();
					System.out.println(">>MAX AMP AFTER: " + maxAmplitude);

					PeakProcessor peakProcessor = new PeakProcessor(
							toneMap.getTimeFrame());

					PeakInfo peakInfo = new PeakInfo(
							peakProcessor.getMagnitudes(),
							peakProcessor.getFrequencyEstimates());

					int noiseFloorMedianFilterLenth = (int) (sampleRate
							/ 117.0);
					float noiseFloorFactor = 1.2F; // 1.8F; // 2.9F;
					int numberOfSpectralPeaks = 6;
					int minPeakSize = 5;

					List<SpectralPeak> peaks = peakInfo.getPeakList(
							noiseFloorMedianFilterLenth, noiseFloorFactor,
							numberOfSpectralPeaks, minPeakSize);
					AudioTuner tuner = new AudioTuner();
					tuner.normalize(toneMap);
					tuner.processPeaks(toneMap, peaks);

					/*
					 * FFTSpectrum fftSpectrum = toneMap.getTimeFrame()
					 * .extractFFTSpectrum(4096); // PitchDetect pd = new
					 * PitchDetect(4096, (float) //
					 * toneMap.getTimeFrame().getTimeSet().getSampleRate(), //
					 * convertDoublesToFloats(toneMap.getTimeFrame().getPitchSet
					 * (). getFreqSet())); //
					 * pd.whiten(fftSpectrum.getSpectrum()); Whitener whitener =
					 * new Whitener(fftSpectrum); whitener.whiten(); FFTSpectrum
					 * whitenedSpectrum = new FFTSpectrum(
					 * fftSpectrum.getSampleRate(), fftSpectrum.getWindowSize(),
					 * whitener.getWhitenedSpectrum()); PitchAnalyser
					 * pitchAnalyser = new PitchAnalyser(fftSpectrum,
					 * toneMap.getTimeFrame().getPitches(), 20);
					 * toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
					 * toneMap.getTimeFrame().deNoise(0.05);
					 */
					cqf.displayToneMap();
					cell.send(streamId, sequence);
				}
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

	private float[] convertDoublesToFloats(double[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		float[] output = new float[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = (float) input[i];
		}
		return output;
	}

	private double[] convertFloatsToDoubles(float[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}
}
