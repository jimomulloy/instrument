package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaPostProcessor extends ProcessorCommon {

	public AudioChromaPostProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(
				">>AudioChromaPostProcessor accept: " + sequence + ", streamId: " + streamId + ", " + sequence);
		int chromaSmoothFactor = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_SMOOTH_FACTOR);
		int chromaDownsampleFactor = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_DOWNSAMPLE_FACTOR);

		ToneMap preChromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId));
		ToneMap postChromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneTimeFrame preTimeFrame = preChromaToneMap.getTimeFrame(sequence);
		ToneTimeFrame postTimeFrame = preTimeFrame.clone();
		postTimeFrame.smoothMedian(preChromaToneMap, chromaSmoothFactor, sequence);
		postChromaToneMap.addTimeFrame(postTimeFrame);

		if (sequence >= chromaDownsampleFactor && (sequence % chromaDownsampleFactor == 0)) {
			System.out.println(">>!!!AudioChromaPostProcessor down: " + sequence);
			postTimeFrame.downSample(postChromaToneMap, chromaDownsampleFactor, sequence);
			console.getVisor().updateChromaPostView(postChromaToneMap);
		}
		cell.send(streamId, sequence);
	}
}
