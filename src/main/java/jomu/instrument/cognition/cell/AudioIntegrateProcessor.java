package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioIntegrateProcessor extends ProcessorCommon {

	public AudioIntegrateProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioIntegrateProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		ToneMap integrateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		console.getVisor().updateToneMapView(integrateToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
