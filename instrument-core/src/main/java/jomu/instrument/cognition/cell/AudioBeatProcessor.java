package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.BeatFeatures;
import jomu.instrument.workspace.tonemap.BeatListElement;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioBeatProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioBeatProcessor.class.getName());

	public AudioBeatProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioBeatProcessor accept seq: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		BeatFeatures features = aff.getBeatFeatures();
		features.buildToneMapFrame(toneMap);
		BeatListElement beat = toneMap.getTimeFrame().getBeat();
		console.getVisor().updateBeatsView(toneMap);
		cell.send(streamId, sequence);
	}
}
