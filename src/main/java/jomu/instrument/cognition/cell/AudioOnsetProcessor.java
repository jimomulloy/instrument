package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioOnsetProcessor extends ProcessorCommon {

	public AudioOnsetProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioOnsetProcessor accept seq: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		if (afp != null) {
			AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
			// OnsetFeatures osf = aff.getOnsetFeatures();
			// osf.buildToneMapFrame(toneMap);
			// console.getVisor().updateBeatsView(toneMap);
			cell.send(streamId, sequence);
		}
	}
}
