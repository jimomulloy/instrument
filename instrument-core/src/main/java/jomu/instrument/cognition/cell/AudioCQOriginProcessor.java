package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
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
		LOG.finer(">>AudioCQOriginProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));

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
		double cqSharpenThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SHARPEN_THRESHOLD);
		boolean cqSwitchCompress = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS);
		boolean cqSwitchNormalise = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE);
		boolean cqSwitchNormaliseMax = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE);
		boolean cqSwitchCompressMax = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_MAX);
		boolean cqSwitchCompressLog = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_LOG);
		boolean cqSwitchScale = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SCALE);
		boolean cqSwitchSquare = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE);
		boolean cqSwitchLowThreshold = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD);
		boolean cqSwitchDecibel = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL);
		boolean cqSwitchWhiten = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN);
		boolean cqSwitchWhitenCompensate = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN_COMPENSATE);
		double cqWhitenFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_FACTOR);
		double cqWhitenThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_THRESHOLD);
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
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		double lowCQThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD);
		double highCQThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD);

		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		ConstantQFeatures cqf = aff.getConstantQFeatures();
		cqf.buildToneMapFrame(toneMap);
		ToneMap cqAdaptiveWhitenControlMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_WHITENER", streamId));
		cqAdaptiveWhitenControlMap.addTimeFrame(toneMap.getTimeFrame(sequence).clone());

		if (cqSwitchPreSharpen) {
			toneMap.getTimeFrame().sharpen(cqSharpenThreshold);
		}

		if (cqSwitchCompress) {
			toneMap.getTimeFrame().compress(compression, cqSwitchCompressMax);
		}
		if (cqSwitchSquare) {
			toneMap.getTimeFrame().square();
		}

		if (cqSwitchLowThreshold) {
			toneMap.getTimeFrame().lowThreshold(lowThreshold, signalMinimum);
		}

		if (cqSwitchScale) {
			toneMap.getTimeFrame().scale(lowCQThreshold, highCQThreshold, cqSwitchCompressLog);
		}
		if (cqSwitchWhiten) {
			toneMap.getTimeFrame().adaptiveWhiten(cqAdaptiveWhitenControlMap,
					toneMap.getPreviousTimeFrame(toneMap.getTimeFrame().getStartTime()), cqWhitenFactor,
					cqWhitenThreshold, cqSwitchWhitenCompensate);
		}

		if (cqSwitchDecibel) {
			toneMap.getTimeFrame().decibel(decibelLevel);
		}

		if (cqSwitchPostSharpen) {
			toneMap.getTimeFrame().sharpen(cqSharpenThreshold);
		}

		toneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);

	}
}
