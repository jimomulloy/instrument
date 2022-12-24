package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Druid;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaPostProcessor implements Consumer<List<NuMessage>> {

	private static final int C4_NOTE = 36;
	private NuCell cell;
	private Workspace workspace;
	private ParameterManager parameterManager;
	private Druid druid;

	public AudioChromaPostProcessor(NuCell cell) {
		super();
		this.cell = cell;
		this.workspace = Instrument.getInstance().getWorkspace();
		this.druid = Instrument.getInstance().getDruid();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.AUDIO_PRE_CHROMA)) {
				System.out.println(
						">>AudioChromaPostProcessor accept: " + message + ", streamId: " + streamId + ", " + sequence);
				int chromaSmoothFactor = parameterManager
						.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_SMOOTH_FACTOR);
				int chromaDownsampleFactor = parameterManager
						.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_DOWNSAMPLE_FACTOR);

				ToneMap preChromaToneMap = workspace.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId));
				ToneMap postChromaToneMap = workspace.getAtlas()
						.getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
				ToneTimeFrame preTimeFrame = preChromaToneMap.getTimeFrame(sequence);
				ToneTimeFrame postTimeFrame = preTimeFrame.clone();
				postTimeFrame.smoothMedian(preChromaToneMap, chromaSmoothFactor, sequence);
				postChromaToneMap.addTimeFrame(postTimeFrame);

				if (sequence >= chromaDownsampleFactor && (sequence % chromaDownsampleFactor == 0)) {
					System.out.println(">>!!!AudioChromaPostProcessor down: " + sequence);
					postTimeFrame.downSample(postChromaToneMap, chromaDownsampleFactor, sequence);
					druid.getVisor().updateChromaPostView(postChromaToneMap);
					cell.send(streamId, sequence);
				}
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}
}
