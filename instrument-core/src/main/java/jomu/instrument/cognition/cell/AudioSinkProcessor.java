package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.Instrument;
import jomu.instrument.actuation.Voice;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

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

		boolean pausePlay = parameterManager
				.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PAUSE_PLAY_SWITCH);

		Voice voice = Instrument.getInstance().getCoordinator().getVoice();
		ToneMap synthesisToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, streamId));
		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));

		console.getVisor().updateSpectrumView(cqToneMap.getTimeFrame(),
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW));

		if (synthesisToneMap.getTimeFrame(sequence) != null) {
			LOG.finer(">>AudioSinkProcessor VOICE SEND time: " + synthesisToneMap.getTimeFrame(sequence).getStartTime()
					+ ", sequence: " + sequence + ", streamId: " + streamId);
			voice.send(synthesisToneMap.getTimeFrame(sequence), streamId, sequence, pausePlay);
		}
		if (workspace.getInstrumentSessionManager().getCurrentSession().isJob()) {
			int tmIndex = sequence - 20;
			if (tmIndex > 0) {
				ToneTimeFrame tf = cqToneMap.getTimeFrame(tmIndex);
				if (tf != null) {
					LOG.finer(">>AudioSinkProcessor isJob clear old maps: " + sequence + ", " + tf.getStartTime());
					// !!TODO !!workspace.getAtlas().clearOldMaps(streamId, tf.getStartTime());
				}
			}
		}
		if (isClosing(streamId, sequence)) {
			LOG.severe(">>AudioSinkProcessor CLOSE!!");
			voice.close(streamId);
			hearing.removeAudioStream(streamId);
		}
	}
}
