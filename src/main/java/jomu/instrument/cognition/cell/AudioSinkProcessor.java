package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.Instrument;
import jomu.instrument.actuation.Voice;
import jomu.instrument.cognition.cell.Cell.CellTypes;
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
		LOG.info(">>AudioSinkProcessor accept: " + sequence + ", streamId: " + streamId);
		Voice voice = Instrument.getInstance().getCoordinator().getVoice();
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
		voice.send(notateToneMap.getTimeFrame(sequence), streamId, sequence);
		if (isClosing(streamId, sequence)) {
			LOG.info(">>AudioSinkProcessor CLOSE!!");
			voice.close(streamId);
			hearing.removeAudioStream(streamId);
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT,
			// streamId));
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_CQ,
			// streamId));
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE,
			// streamId));
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE,
			// streamId));
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT,
			// streamId));
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA,
			// streamId));
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA,
			// streamId));
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_PITCH,
			// streamId));
			// workspace.getAtlas().removeToneMap(buildToneMapKey(CellTypes.AUDIO_SPECTRAL_PEAKS,
			// streamId));
		}
	}
}
