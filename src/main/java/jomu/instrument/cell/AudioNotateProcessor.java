package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.AudioTuner;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.world.WorldModel;
import jomu.instrument.world.tonemap.ToneMap;

public class AudioNotateProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;

	private WorldModel worldModel;

	public AudioNotateProcessor(NuCell cell) {
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
		System.out.println(">>AudioNotateProcessor accepting");
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>AudioNotateProcessor accept: " + message);
			if (message.source.getCellType()
					.equals(CellTypes.AUDIO_INTEGRATE)) {
				ToneMap integrateToneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE,
								streamId));
				ToneMap notateToneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(this.cell.getCellType(),
								streamId));
				notateToneMap.addTimeFrame(integrateToneMap.getTimeFrame(sequence).clone());
				System.out.println(">>>PUT notat frame: " + integrateToneMap.getTimeFrame().getStartTime());
				AudioTuner tuner = new AudioTuner();

				tuner.noteScan(notateToneMap, sequence);

				System.out.println(">>AudioNotateProcessor send");
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
