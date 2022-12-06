package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.SpectrumFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.WorldModel;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.PitchAnalyser;
import jomu.instrument.workspace.tonemap.PitchDetect2;
import jomu.instrument.workspace.tonemap.ToneMap;

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
					spf.buildToneMapFrame(toneMap);

					FFTSpectrum fftSpectrum = new FFTSpectrum(
							spf.getSps().getSampleRate(), 1024,
							spf.getSpectrum());
					// Whitener whitener = new Whitener(fftSpectrum);
					// float[] whitenedSpectrum = whitener.whiten();
					// fftSpectrum = new
					// FFTSpectrum(spf.getSps().getSampleRate(),
					// 1024, whitenedSpectrum);

					PitchAnalyser pitchAnalyser = new PitchAnalyser(fftSpectrum,
							toneMap.getTimeFrame().getPitches(), 20);

					// Vector<Double> f0s = pitchAnalyser.detectF0s();

					// float[] f0Spectrum = pitchAnalyser.getF0Spectrum();

					// fftSpectrum = new
					// FFTSpectrum(spf.getSps().getSampleRate(),
					// 1024, f0Spectrum);

					PitchDetect2 pd = new PitchDetect2(1024,
							(float) toneMap.getTimeFrame().getTimeSet()
									.getSampleRate(),
							convertDoublesToFloats(toneMap.getTimeFrame()
									.getPitchSet().getFreqSet()));
					// pd.detect(fftSpectrum.getSpectrum());

					// f0Spectrum = pd.getF0Spectrum();

					// fftSpectrum = new
					// FFTSpectrum(spf.getSps().getSampleRate(),
					// 1024, f0Spectrum);

					toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);

					// Whitener whitener = new Whitener(fftSpectrum);
					// whitener.whiten();
					// FFTSpectrum whitenedSpectrum = new FFTSpectrum(
					// fftSpectrum.getSampleRate(),
					// fftSpectrum.getWindowSize(),
					// whitener.getWhitenedSpectrum());
					// PitchAnalyser pitchAnalyser = new
					// PitchAnalyser(fftSpectrum,
					// toneMap.getTimeFrame().getPitches(), 20);
					// toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);

					toneMap.getTimeFrame().deNoise(0.05);
					// spf.displayToneMap();
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
