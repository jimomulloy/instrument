package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.world.WorldModel;
import jomu.instrument.world.tonemap.ToneMap;

public class AudioIntegrateProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;

	private WorldModel worldModel;

	public AudioIntegrateProcessor(NuCell cell) {
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
		System.out.println(">>AudioIntegrateProcessor accepting");
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>AudioIntegrateProcessor accept: " + message);
			if (message.source.getCellType().equals(CellTypes.AUDIO_CQ)) {
				ToneMap cqToneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ,
								streamId));
				ToneMap integrateToneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(this.cell.getCellType(),
								streamId));
				integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
				System.out.println(">>AudioIntegrateProcessor send");
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
