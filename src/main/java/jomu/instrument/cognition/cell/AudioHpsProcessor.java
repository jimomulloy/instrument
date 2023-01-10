package jomu.instrument.cognition.cell;

import java.util.List;

public class AudioHpsProcessor extends ProcessorCommon {

	public AudioHpsProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioHpsProcessor accept: " + sequence + ", streamId: " + streamId);
		// ToneMap cqToneMap =
		// workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ,
		// streamId));
		// ToneMap integrateToneMap =
		// workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(),
		// streamId));
		// integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		cell.send(streamId, sequence);
	}
}
