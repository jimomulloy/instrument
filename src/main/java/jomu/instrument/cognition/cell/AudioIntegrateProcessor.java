package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioIntegrateProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioIntegrateProcessor.class.getName());

	public AudioIntegrateProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioIntegrateProcessor accept: " + sequence + ", streamId: " + streamId);

		boolean integrateSwitchHps = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_HPS_SWITCH);

		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		ToneMap hpsMaskToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_HARMONIC_MASK", streamId));
		ToneMap integrateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));

		if (integrateSwitchHps) {
			LOG.finer(">>AudioIntegrateProcessor use hpsMaskToneMap");
			integrateToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
		} else {
			LOG.finer(">>AudioIntegrateProcessor use cqToneMap");
			integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		}

		console.getVisor().updateToneMapView(integrateToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
