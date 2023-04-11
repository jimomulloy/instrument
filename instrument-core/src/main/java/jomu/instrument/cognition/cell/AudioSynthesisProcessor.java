package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.TonePredictor;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioSynthesisProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioSynthesisProcessor.class.getName());

	public AudioSynthesisProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioSynthesisProcessor accept: " + sequence + ", streamId: " + streamId);

		boolean synthesisSwitchChords = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORDS_SWITCH);
		double quantizeRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE);
		double quantizePercent = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT);

		ToneMap synthesisToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
		ToneMap chromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, streamId));
		ToneMap beatToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId));

		ToneTimeFrame synthesisFrame = notateToneMap.getTimeFrame(sequence).clone();
		synthesisToneMap.addTimeFrame(synthesisFrame);
		synthesisToneMap.getTonePredictor().load(notateToneMap.getTonePredictor());
		synthesisToneMap.getTonePredictor().load(chromaToneMap.getTonePredictor());
		synthesisToneMap.getTonePredictor().load(beatToneMap.getTonePredictor());

		ToneTimeFrame notateFrame = notateToneMap.getTimeFrame(sequence);
		ToneTimeFrame chromaFrame = chromaToneMap.getTimeFrame(sequence);
		ToneTimeFrame beatFrame = beatToneMap.getTimeFrame(sequence);

		CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);

		if (synthesisSwitchChords) {
			TonePredictor chordPredictor = chromaToneMap.getTonePredictor();
			chordPredictor.predictChord(chromaFrame, cm, quantizeRange, quantizePercent);
			TonePredictor notePredictor = notateToneMap.getTonePredictor();
			notePredictor.predictChord(notateFrame, cm, quantizeRange, quantizePercent);
			TonePredictor beatPredictor = beatToneMap.getTonePredictor();
			beatPredictor.predictChord(beatFrame, cm, quantizeRange, quantizePercent);
		}
		console.getVisor().updateChromaPostView(chromaToneMap, chromaFrame);
		console.getVisor().updateBeatsView(beatToneMap, beatFrame);
		console.getVisor().updateToneMapView(notateToneMap, notateFrame, CellTypes.AUDIO_NOTATE.toString());
		console.getVisor().updateToneMapView(synthesisToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
