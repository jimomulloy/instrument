package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.PercussionFeatures;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioPercussionProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioPercussionProcessor.class.getName());

	public AudioPercussionProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.info(">>AudioPercussionProcessor accept seq: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		PercussionFeatures features = aff.getPercussionFeatures();
		features.buildToneMapFrame(toneMap);
		console.getVisor().updatePercussionView(toneMap);
		cell.send(streamId, sequence);
	}
}
