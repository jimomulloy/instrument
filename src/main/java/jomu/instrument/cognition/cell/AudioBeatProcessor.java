package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.BeatFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.BeatListElement;
import jomu.instrument.workspace.tonemap.ChordListElement;
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
		LOG.info(">>AudioBeatProcessor accept seq: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		BeatFeatures features = aff.getBeatFeatures();
		features.buildToneMapFrame(toneMap);
		BeatListElement beat = toneMap.getTimeFrame().getBeat();
		if (beat != null) {
			toneMap.trackBeat(beat);
		}
		console.getVisor().updateBeatsView(toneMap);
		cell.send(streamId, sequence);
	}
}
