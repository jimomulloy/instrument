package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaPreProcessor extends ProcessorCommon {

	public AudioChromaPreProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioChromaProcessor accept: " + sequence + ", streamId: " + streamId);
		double normaliseThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD);

		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		ToneMap chromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneTimeFrame cqTimeFrame = cqToneMap.getTimeFrame(sequence);
		ToneTimeFrame chromaTimeFrame = cqTimeFrame.clone().chroma(C4_NOTE, cqTimeFrame.getPitchLow(),
				cqTimeFrame.getPitchHigh());
		chromaTimeFrame.normaliseEuclidian(normaliseThreshold);
		chromaTimeFrame.chromaQuantize();
		chromaToneMap.addTimeFrame(chromaTimeFrame);
		console.getVisor().updateChromaPreView(chromaToneMap);
		cell.send(streamId, sequence);
	}
}
