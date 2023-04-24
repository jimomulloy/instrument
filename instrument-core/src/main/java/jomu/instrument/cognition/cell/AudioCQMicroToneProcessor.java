package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.CQMicroToneFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioCQMicroToneProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioCQMicroToneProcessor.class.getName());

	public AudioCQMicroToneProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioCQMicroToneProcessor accept: " + sequence + ", streamId: " + streamId);
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
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		double lowCQThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD);
		double highCQThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD);

		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		CQMicroToneFeatures cqf = aff.getCQMicroToneFeatures();
		cqf.buildToneMapFrame(toneMap);
		LOG.finer(">>CQ TIME: " + toneMap.getTimeFrame().getStartTime());

		LOG.finer(">>CQ MAX/MIN AMP 1: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
				+ toneMap.getTimeFrame().getMinAmplitude());
		if (cqSwitchCompress) {
			toneMap.getTimeFrame().compress(compression, cqSwitchCompressMax);
			LOG.finer(">>CQ MAX/MIN AMP 2: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
					+ toneMap.getTimeFrame().getMinAmplitude());
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

		if (cqSwitchDecibel) {
			toneMap.getTimeFrame().decibel(decibelLevel);
			LOG.finer(">>CQ MAX/MIN AMP 3: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
					+ toneMap.getTimeFrame().getMinAmplitude());
		}

		toneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		LOG.finer(">>CQ MAX/MIN AMP X: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
				+ toneMap.getTimeFrame().getMinAmplitude());

		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
