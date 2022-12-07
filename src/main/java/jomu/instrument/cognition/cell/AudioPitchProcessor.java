package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
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
				toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
				System.out.println(">PitchDetectorProcessor process tonemap");
				// pdf.displayToneMap();
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
