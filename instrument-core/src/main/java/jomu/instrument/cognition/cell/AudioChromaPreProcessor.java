package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaPreProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioChromaPreProcessor.class.getName());

	public AudioChromaPreProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioChromaProcessor accept: " + sequence + ", streamId: " + streamId);
		double normaliseThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD);
		int chromaRootNote = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_ROOT_NOTE);
		boolean chromaHarmonicsSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HARMONICS_SWITCH);
		boolean chromaCeilingSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CEILING_SWITCH);
		boolean chromaCQOriginSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CQ_ORIGIN_SWITCH);
		boolean chromaHpsSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HPS_SWITCH);
		double chromaChordifyThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_THRESHOLD);

		ToneMap cqToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		if (chromaCQOriginSwitch) {
			cqToneMap = workspace.getAtlas()
					.getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN, streamId));
		}

		ToneMap hpsMaskToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.toString() + "_HARMONIC_MASK", streamId));

		ToneMap chromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));

		ToneTimeFrame sourceTimeFrame = cqToneMap.getTimeFrame(sequence)
				.clone();
		if (chromaHpsSwitch) {
			sourceTimeFrame.mask(hpsMaskToneMap.getTimeFrame(sequence), false);
		}

		chromaToneMap.addTimeFrame(sourceTimeFrame
				.chroma(chromaRootNote, sourceTimeFrame.getPitchLow(), sourceTimeFrame.getPitchHigh(),
						chromaHarmonicsSwitch)
				.normaliseEuclidian(normaliseThreshold, chromaCeilingSwitch)
				.chromaQuantize(chromaChordifyThreshold));

		console.getVisor()
				.updateToneMapView(chromaToneMap, this.cell.getCellType()
						.toString());
		cell.send(streamId, sequence);
	}
}
