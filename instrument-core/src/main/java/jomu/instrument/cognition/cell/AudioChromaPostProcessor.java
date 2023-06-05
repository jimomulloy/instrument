package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaPostProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioChromaPostProcessor.class.getName());

	public AudioChromaPostProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioChromaPostProcessor accept: " + sequence + ", streamId: " + streamId);
		int chromaSmoothFactor = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_SMOOTH_FACTOR);
		boolean chromaChordifySwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SWITCH);
		boolean chromaChordifySharpenSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SHARPEN_SWITCH);
		double chromaChordifyThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_THRESHOLD);
		boolean chromaCQOriginSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CQ_ORIGIN_SWITCH);

		ToneMap originToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		if (chromaCQOriginSwitch) {
			originToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN, streamId));
		}

		ToneMap postChromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));

		ToneMap preChromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId));
		ToneTimeFrame preTimeFrame = preChromaToneMap.getTimeFrame(sequence);
		ToneTimeFrame postTimeFrame = preTimeFrame.clone();
		postChromaToneMap.addTimeFrame(postTimeFrame);
		postTimeFrame.chromaCens(preChromaToneMap, postChromaToneMap, originToneMap, chromaSmoothFactor, sequence,
				chromaChordifySwitch, chromaChordifyThreshold, chromaChordifySharpenSwitch);

		int tmIndex = sequence - 10;
		ToneTimeFrame timeFrame;
		if (tmIndex > 0) {
			timeFrame = postChromaToneMap.getTimeFrame(tmIndex);
			if (timeFrame != null) {
				console.getVisor().updateToneMapView(postChromaToneMap, timeFrame, this.cell.getCellType().toString());
			}
			cell.send(streamId, tmIndex);
		}

		if (isClosing(streamId, sequence)) {
			if (tmIndex < 0) {
				tmIndex = 0;
			}
			for (int i = tmIndex + 1; i <= sequence; i++) {
				timeFrame = postChromaToneMap.getTimeFrame(i);
				if (timeFrame != null) {
					console.getVisor().updateToneMapView(postChromaToneMap, timeFrame,
							this.cell.getCellType().toString());
				}
				cell.send(streamId, i);
			}
		}
	}
}
