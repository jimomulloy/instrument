package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
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
		LOG.severe(">>AudioSynthesisProcessor accept: " + sequence + ", streamId: " + streamId);

		boolean synthesisSwitchChords = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORDS_SWITCH);

		ToneMap synthesisToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
		ToneMap chromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, streamId));
		ToneTimeFrame synthesisFrame = notateToneMap.getTimeFrame(sequence).clone();
		synthesisToneMap.addTimeFrame(synthesisFrame);
		ToneTimeFrame chromaFrame = chromaToneMap.getTimeFrame(sequence);

		if (synthesisSwitchChords) {
			TonePredictor chordPredictor = chromaToneMap.getTonePredictor();
			chordPredictor.predictChord(chromaFrame);
		}
		console.getVisor().updateChromaPostView(chromaToneMap, chromaFrame);
		console.getVisor().updateToneMapView(synthesisToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
