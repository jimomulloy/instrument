package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.analysis.Whitener;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.PitchDetectorFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.WorldModel;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioPitchProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private WorldModel worldModel;

	public AudioPitchProcessor(NuCell cell) {
		super();
		System.out.println(">>PitchDetectorProcessor create");
		this.cell = cell;
		worldModel = Instrument.getInstance().getWorldModel();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		// System.out.println(">>getAudioCQProcessor");
		// System.out.println(cell.toString());
		System.out.println(">>PitchDetectorProcessor accepting");
		for (NuMessage message : messages) {
			int sequence = message.sequence;
			String streamId = message.streamId;
			System.out.println(">>PitchDetectorProcessor accept: " + message);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
				ToneMap toneMap = worldModel.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_PITCH, streamId));
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
				PitchDetectorFeatures pdf = aff.getPitchDetectorFeatures();
				pdf.buildToneMapFrame(toneMap);
				float[] spectrum = pdf.getSpectrum();
				FFTSpectrum fftSpectrum = new FFTSpectrum(pdf.getPds().getSampleRate(), 1024, spectrum);

				// PolyphonicPitchDetection ppp = new PolyphonicPitchDetection();
				// Klapuri klapuri = new Klapuri(convertFloatsToDoubles(spectrum), ppp);
				Whitener whitener = new Whitener(fftSpectrum);
				whitener.whiten();
				FFTSpectrum whitenedSpectrum = new FFTSpectrum(fftSpectrum.getSampleRate(), fftSpectrum.getWindowSize(),
						whitener.getWhitenedSpectrum());

				// PitchDetect pd = new PitchDetect(fftSpectrum.getWindowSize(),
				// fftSpectrum.getSampleRate());
				// pd.detect(whitenedSpectrum.getSpectrum());

				toneMap.getTimeFrame().loadFFTSpectrum(whitenedSpectrum);
				toneMap.getTimeFrame().square();
				toneMap.getTimeFrame().lowThreshold(0.01, 0.0000001);
				toneMap.getTimeFrame().normaliseThreshold(20, 0.0000001);
				// toneMap.getTimeFrame().deNoise(0.05);

				System.out.println(">PitchDetectorProcessor process tonemap");
				pdf.displayToneMap();
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
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