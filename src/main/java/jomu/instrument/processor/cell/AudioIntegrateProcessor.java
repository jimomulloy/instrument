package jomu.instrument.processor.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.processor.cell.Cell.CellTypes;
import jomu.instrument.workspace.WorldModel;
import jomu.instrument.workspace.tonemap.ToneMap;

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
		int sequence = 0;
		String streamId = null;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.AUDIO_CQ)) {
				ToneMap cqToneMap = worldModel.getAtlas().getToneMap(
						buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
				ToneMap integrateToneMap = worldModel.getAtlas().getToneMap(
						buildToneMapKey(this.cell.getCellType(), streamId));
				integrateToneMap
						.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
			} else if (message.source.getCellType()
					.equals(CellTypes.AUDIO_SPECTRUM)) {
				// ToneMap cqToneMap =
				// worldModel.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ,
				// streamId));
				// ToneMap integrateToneMap = worldModel.getAtlas()
				// .getToneMap(buildToneMapKey(this.cell.getCellType(),
				// streamId));
				// integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
			}
		}
		if (streamId != null) {
			cell.send(streamId, sequence);
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
