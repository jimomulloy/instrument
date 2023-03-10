package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioSynthesisProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioSynthesisProcessor.class.getName());

	public AudioSynthesisProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.severe(">>AudioSynthesisProcessor accept: " + sequence + ", streamId: " + streamId);

		ToneMap synthesisToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
		synthesisToneMap.addTimeFrame(notateToneMap.getTimeFrame(sequence).clone());

		console.getVisor().updateToneMapView(synthesisToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
