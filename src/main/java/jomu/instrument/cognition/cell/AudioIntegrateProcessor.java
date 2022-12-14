package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioIntegrateProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;

	private Workspace workspace;

	public AudioIntegrateProcessor(NuCell cell) {
		super();
		this.cell = cell;
		this.workspace = Instrument.getInstance().getWorkspace();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.AUDIO_CQ)) {
				ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
				ToneMap integrateToneMap = workspace.getAtlas()
						.getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
				integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
