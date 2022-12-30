package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.OnsetFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioBeatProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private Workspace workspace;
	private Hearing hearing;
	private ParameterManager parameterManager;
	private Console console;

	public AudioBeatProcessor(NuCell cell) {
		super();
		this.cell = cell;
		this.hearing = Instrument.getInstance().getCoordinator().getHearing();
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
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				System.out.println(">>AudioBeatProcessor accept: " + message + ", streamId: " + streamId);
				ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId));
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				if (afp != null) {
					AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
					OnsetFeatures osf = aff.getOnsetFeatures();
					osf.buildToneMapFrame(toneMap);
					console.getVisor().updateBeatsView(toneMap);
					cell.send(streamId, sequence);
				}
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

}
