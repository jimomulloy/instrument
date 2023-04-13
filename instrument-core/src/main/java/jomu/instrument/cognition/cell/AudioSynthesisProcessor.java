package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneSynthesiser;
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

		double quantizeRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE);
		double quantizePercent = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT);

		ToneMap synthesisToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
		ToneMap chromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, streamId));

		ToneTimeFrame synthesisFrame = notateToneMap.getTimeFrame(sequence).clone();
		synthesisToneMap.addTimeFrame(synthesisFrame);

		ToneTimeFrame notateFrame = notateToneMap.getTimeFrame(sequence);
		ToneTimeFrame chromaFrame = chromaToneMap.getTimeFrame(sequence);

		synthesisFrame.addNotes(synthesisToneMap, notateFrame);
		synthesisFrame.setChord(synthesisToneMap, chromaFrame);

		CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);

		ToneSynthesiser synthesiser = synthesisToneMap.getToneSynthesiser();
		synthesiser.synthesise(synthesisFrame, cm, quantizeRange, quantizePercent);

		console.getVisor().updateChromaSynthView(synthesisToneMap, synthesisFrame);
		console.getVisor().updateToneMapView(synthesisToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
