package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaPreProcessor implements Consumer<List<NuMessage>> {

	private static final int C4_NOTE = 36;
	private NuCell cell;
	private Workspace workspace;
	private ParameterManager parameterManager;
	private Console console;

	public AudioChromaPreProcessor(NuCell cell) {
		super();
		this.cell = cell;
		this.workspace = Instrument.getInstance().getWorkspace();
		this.console = Instrument.getInstance().getConsole();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.AUDIO_CQ)) {
				System.out.println(">>AudioChromaProcessor accept: " + message + ", streamId: " + streamId);
				double normaliseThreshold = parameterManager
						.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD);

				ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
				ToneMap chromaToneMap = workspace.getAtlas()
						.getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
				ToneTimeFrame cqTimeFrame = cqToneMap.getTimeFrame(sequence);
				ToneTimeFrame chromaTimeFrame = cqTimeFrame.clone().chroma(C4_NOTE, cqTimeFrame.getPitchLow(),
						cqTimeFrame.getPitchHigh());
				chromaTimeFrame.normaliseEuclidian(normaliseThreshold);
				chromaTimeFrame.chromaQuantize();
				chromaToneMap.addTimeFrame(chromaTimeFrame);
				console.getVisor().updateChromaPreView(chromaToneMap);
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
