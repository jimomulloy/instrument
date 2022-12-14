package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.actuation.Voice;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioSinkProcessor implements Consumer<List<NuMessage>> {

	private Workspace workspace;
	private Hearing hearing;

	public AudioSinkProcessor(NuCell cell) {
		super();
		this.hearing = Instrument.getInstance().getCoordinator().getHearing();
		this.workspace = Instrument.getInstance().getWorkspace();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.AUDIO_NOTATE)) {
				Voice voice = Instrument.getInstance().getCoordinator().getVoice();
				ToneMap notateToneMap = workspace.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
				voice.send(notateToneMap.getTimeFrame(sequence), streamId, sequence);
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				if (afp == null || (afp.isClosed() && afp.isLastSequence(sequence))) {
					voice.close(streamId);
					hearing.removeAudioStream(streamId);
				}
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

}
