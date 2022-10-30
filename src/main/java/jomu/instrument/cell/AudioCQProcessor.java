package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.AudioTuner;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.organs.Hearing;
import jomu.instrument.world.WorldModel;
import jomu.instrument.world.tonemap.ToneMap;

public class AudioCQProcessor implements Consumer<List<NuMessage>> {

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
		System.out.println(">>ConstantQMessageProcessor accepting");
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out
					.println(">>ConstantQMessageProcessor accept: " + message + ", streamId: "+ streamId);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator()
						.getHearing();
				ToneMap toneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ,
								streamId));
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
				ConstantQFeatures cqf = aff.getConstantQFeatures();
				cqf.buildToneMapFrame(toneMap);
				toneMap.getTimeFrame().compress(1000F);
				float maxAmplitude = (float) toneMap.getTimeFrame()
						.getMaxAmplitude();
				System.out.println(">>MAX AMP: " + maxAmplitude + ", " + tmMax);
				if (tmMax < maxAmplitude) {
					tmMax = maxAmplitude;
				}
				toneMap.getTimeFrame().normalise(tmMax);
				toneMap.getTimeFrame().deNoise(0.2);

				maxAmplitude = (float) toneMap.getTimeFrame().getMaxAmplitude();
				System.out.println(">>MAX AMP AFTER: " + maxAmplitude);

				AudioTuner tuner = new AudioTuner();

				tuner.normalize(toneMap);

				/*
				 * FFTSpectrum fftSpectrum = toneMap.getTimeFrame()
				 * .extractFFTSpectrum(4096); // PitchDetect pd = new
				 * PitchDetect(4096, (float) //
				 * toneMap.getTimeFrame().getTimeSet().getSampleRate(), //
				 * convertDoublesToFloats(toneMap.getTimeFrame().getPitchSet().
				 * getFreqSet())); // pd.whiten(fftSpectrum.getSpectrum());
				 * Whitener whitener = new Whitener(fftSpectrum);
				 * whitener.whiten(); FFTSpectrum whitenedSpectrum = new
				 * FFTSpectrum( fftSpectrum.getSampleRate(),
				 * fftSpectrum.getWindowSize(), whitener.getWhitenedSpectrum());
				 * PitchAnalyser pitchAnalyser = new PitchAnalyser(fftSpectrum,
				 * toneMap.getTimeFrame().getPitches(), 20);
				 * toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
				 * toneMap.getTimeFrame().deNoise(0.05);
				 */
				System.out
						.println(">>ConstantQMessageProcessor process tonemap");
				cqf.displayToneMap();
				System.out.println(">>ConstantQMessageProcessor send");
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
