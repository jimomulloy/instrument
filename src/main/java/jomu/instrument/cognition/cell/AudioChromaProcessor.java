package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaProcessor implements Consumer<List<NuMessage>> {

	private static final int C4_NOTE = 36;
	private NuCell cell;
	private Workspace workspace;

	public AudioChromaProcessor(NuCell cell) {
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
			System.out.println(">>AudioChromaProcessor accept: " + message + ", streamId: " + streamId);
			if (message.source.getCellType().equals(CellTypes.AUDIO_CQ)) {
				ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
				ToneMap chromaToneMap = workspace.getAtlas()
						.getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
				ToneTimeFrame cqTimeFrame = cqToneMap.getTimeFrame(sequence);
				chromaToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone().chroma(C4_NOTE,
						cqTimeFrame.getPitchLow(), cqTimeFrame.getPitchHigh()));
				// visor.updateToneMap(chromaToneMap);
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
