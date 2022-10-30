package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.PitchDetectorFeatures;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.organs.Hearing;
import jomu.instrument.world.WorldModel;
import jomu.instrument.world.tonemap.ToneMap;

public class PitchDetectorProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private WorldModel worldModel;

	public PitchDetectorProcessor(NuCell cell) {
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
			// TODO ONLY Process one message?
			System.out.println(">>PitchDetectorProcessor accept: " + message);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator()
						.getHearing();
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
				PitchDetectorFeatures pdf = aff.getPitchDetectorFeatures();
				pdf.buildToneMap();
				ToneMap toneMap = pdf.getToneMap();
				System.out.println(">PitchDetectorProcessor process tonemap");
				// worldModel.getAtlas().putToneMap(this.cell.getCellType(),
				// toneMap);
				// if (toneMap != null && toneMap.getTunerModel().tune()) {
				// cqf.displayToneMap();
				// System.out.println(">>ConstantQMessageProcessor send");
				// cell.send(sequence, output);
				// }

			}
		}
	}

}
