package jomu.instrument.cognition.cell;

import java.util.ArrayList;
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
		boolean chromaChordifySwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SWITCH);

		ToneMap preChromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId));
		ToneMap postChromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneTimeFrame preTimeFrame = preChromaToneMap.getTimeFrame(sequence);
		ToneTimeFrame postTimeFrame = preTimeFrame.clone();
		postChromaToneMap.addTimeFrame(postTimeFrame);
		postTimeFrame.smoothMedian(preChromaToneMap, postChromaToneMap, chromaSmoothFactor, sequence,
				chromaChordifySwitch);

		List<ToneTimeFrame> timeFrames = new ArrayList<>();
		double fromTime = (postTimeFrame.getStartTime() - 2.0) >= 0 ? postTimeFrame.getStartTime() - 2.0 : 0;
		while (postTimeFrame != null && postTimeFrame.getStartTime() >= fromTime) {
			timeFrames.add(postTimeFrame);
			postTimeFrame = postChromaToneMap.getPreviousTimeFrame(postTimeFrame.getStartTime());
		}

		for (ToneTimeFrame ttfv : timeFrames) {
			console.getVisor().updateChromaPostView(postChromaToneMap, ttfv);
		}
		console.getVisor().updateChromaPostView(postChromaToneMap);
		cell.send(streamId, sequence);
	}
}
