package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.Instrument;
import jomu.instrument.actuation.Voice;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioSinkProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioSinkProcessor.class.getName());

	public AudioSinkProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioSinkProcessor accept: " + sequence + ", streamId: " + streamId);
		Voice voice = Instrument.getInstance().getCoordinator().getVoice();
		ToneMap synthesisToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, streamId));
		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));

		console.getVisor().updateSpectrumView(cqToneMap.getTimeFrame(),
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_CQ_WINDOW));

		LOG.severe(">>VOICE SEND: " + sequence + ", " + synthesisToneMap.getTimeFrame(sequence).getStartTime());

		if (synthesisToneMap.getTimeFrame(sequence) != null) {
			voice.send(synthesisToneMap.getTimeFrame(sequence), streamId, sequence);
		}
		if (isClosing(streamId, sequence)) {
			LOG.severe(">>AudioSinkProcessor CLOSE!!");
			voice.close(streamId);
			hearing.removeAudioStream(streamId);
		}
	}
}
