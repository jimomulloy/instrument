package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioCQOriginProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioCQOriginProcessor.class.getName());

	public AudioCQOriginProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.info(">>AudioCQOriginProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN, streamId));

		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		double lowThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);
		double signalMinimum = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM);
		double normaliseThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD);
		double decibelLevel = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL);
		float compression = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION);
		boolean cqSwitchCompress = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS);
		boolean cqSwitchSquare = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE);
		boolean cqSwitchLowThreshold = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD);
		boolean cqSwitchDecibel = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL);
		boolean cqSwitchNormalise = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE);
		boolean cqSwitchPreHarmonics = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_HARMONICS);
		boolean cqSwitchPostHarmonics = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_HARMONICS);
		boolean cqSwitchPreSharpen = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_SHARPEN);
		boolean cqSwitchPostSharpen = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_SHARPEN);
		boolean cqSwitchSharpenHarmonic = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SHARPEN_HARMONIC);

		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		ConstantQFeatures cqf = aff.getConstantQFeatures();
		cqf.buildToneMapFrame(toneMap);

		if (cqSwitchPreSharpen) {
			toneMap.getTimeFrame().sharpen();
		}

		if (cqSwitchCompress) {
			toneMap.getTimeFrame().compress(compression);
		}
		if (cqSwitchSquare) {
			toneMap.getTimeFrame().square();
		}

		if (cqSwitchLowThreshold) {
			toneMap.getTimeFrame().lowThreshold(lowThreshold, signalMinimum);
		}

		if (cqSwitchDecibel) {
			toneMap.getTimeFrame().decibel(decibelLevel);
		}

		if (cqSwitchPostSharpen) {
			toneMap.getTimeFrame().sharpen();
		}
		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);

	}
}
