package jomu.instrument.processor.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.SpectrumFeatures;
import jomu.instrument.processor.cell.Cell.CellTypes;
import jomu.instrument.sensor.Hearing;
import jomu.instrument.workspace.WorldModel;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.PitchAnalyser;
import jomu.instrument.workspace.tonemap.PitchDetect;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.Whitener;

public class AudioSpectrumProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private float tmMax = 0;

	private WorldModel worldModel;

	public AudioSpectrumProcessor(NuCell cell) {
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
			System.out.println(">>AudioSpectrumProcessor accept: " + message
					+ ", streamId: " + streamId);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator()
						.getHearing();
				ToneMap toneMap = worldModel.getAtlas().getToneMap(
						buildToneMapKey(CellTypes.AUDIO_SPECTRUM, streamId));
				AudioFeatureProcessor afp = hearing
						.getAudioFeatureProcessor(streamId);
				if (afp != null) {
					AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
					SpectrumFeatures spf = aff.getSpectrumFeatures();
					FFTSpectrum fftSpectrum = new FFTSpectrum(
							spf.getSps().getSampleRate(), 4096,
							spf.getSpectrum());
					PitchDetect pd = new PitchDetect(4096,
							(float) toneMap.getTimeFrame().getTimeSet()
									.getSampleRate(),
							convertDoublesToFloats(toneMap.getTimeFrame()
									.getPitchSet().getFreqSet()));
					pd.detect(fftSpectrum.getSpectrum());
					Whitener whitener = new Whitener(fftSpectrum);
					whitener.whiten();
					FFTSpectrum whitenedSpectrum = new FFTSpectrum(
							fftSpectrum.getSampleRate(),
							fftSpectrum.getWindowSize(),
							whitener.getWhitenedSpectrum());
					PitchAnalyser pitchAnalyser = new PitchAnalyser(fftSpectrum,
							toneMap.getTimeFrame().getPitches(), 20);
					toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
					toneMap.getTimeFrame().deNoise(0.05);

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
