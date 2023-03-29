package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ChordListElement;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaPostProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioChromaPostProcessor.class.getName());

	public AudioChromaPostProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
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

		ToneMap postChromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));

		ToneMap preChromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId));
		ToneTimeFrame preTimeFrame = preChromaToneMap.getTimeFrame(sequence);
		ToneTimeFrame postTimeFrame = preTimeFrame.clone();
		postChromaToneMap.addTimeFrame(postTimeFrame);
		postTimeFrame.smoothMedian(preChromaToneMap, postChromaToneMap, chromaSmoothFactor, sequence,
				chromaChordifySwitch, chromaChordifyThreshold, chromaChordifySharpenSwitch);

		int tmIndex = sequence - 10; // TODO !!
		ToneTimeFrame timeFrame;
		if (tmIndex > 0) {
			timeFrame = postChromaToneMap.getTimeFrame(tmIndex);
			if (timeFrame != null) {
				ChordListElement chord = timeFrame.getChord();
				LOG.finer(">>AudioChromaPostProcessor get chord: " + tmIndex + ", time: " + timeFrame.getStartTime());
				if (chord != null) {
					LOG.finer(">>AudioChromaPostProcessor got chord: " + tmIndex + ", time: " + timeFrame.getStartTime()
							+ " ," + chord);
					postChromaToneMap.trackChord(chord);
				}
				console.getVisor().updateChromaPostView(postChromaToneMap, timeFrame);
			}
			cell.send(streamId, tmIndex);
		}

		if (isClosing(streamId, sequence)) {
			if (tmIndex < 0) {
				tmIndex = 0;
			}
			for (int i = tmIndex + 1; i <= sequence; i++) {
				timeFrame = postChromaToneMap.getTimeFrame(i);
				if (timeFrame != null) { // TODO or make fake on here?
					ChordListElement chord = timeFrame.getChord();
					if (chord != null) {
						postChromaToneMap.trackChord(chord);
					}
					console.getVisor().updateChromaPostView(postChromaToneMap, timeFrame);
				}
				cell.send(streamId, i);
			}
		}
	}
}
