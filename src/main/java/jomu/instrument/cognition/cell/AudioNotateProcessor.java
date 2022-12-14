package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.AudioTuner;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioNotateProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;

	private Workspace workspace;

	public AudioNotateProcessor(NuCell cell) {
		super();
		this.cell = cell;
		this.workspace = Instrument.getInstance().getWorkspace();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.AUDIO_INTEGRATE)) {
				ToneMap integrateToneMap = workspace.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE, streamId));
				ToneMap notateToneMap = workspace.getAtlas()
						.getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
				notateToneMap.addTimeFrame(integrateToneMap.getTimeFrame(sequence).clone());
				AudioTuner tuner = new AudioTuner();

				tuner.noteScan(notateToneMap, sequence);

				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
