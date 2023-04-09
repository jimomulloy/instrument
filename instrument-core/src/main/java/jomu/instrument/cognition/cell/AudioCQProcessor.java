package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.AudioTuner;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioCQProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioCQProcessor.class.getName());

	public AudioCQProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioCQProcessor accept: " + sequence + ", streamId: " + streamId);
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
		boolean cqSwitchCompressUeMax = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE_MAX);
		boolean cqSwitchSquare = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE);
		boolean cqSwitchLowThreshold = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD);
		boolean cqSwitchDecibel = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL);
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
		boolean cqWhiten = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN);
		double cqSharpenThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SHARPEN_THRESHOLD);
		double cqEnvelopeWhitenThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_THRESHOLD);
		double cqEnvelopeWhitenAttackFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_ATTACK_FACTOR);
		double cqEnvelopeWhitenDecayFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_DECAY_FACTOR);
		boolean cqEnvelopeWhitenPreSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_PRE_SWITCH);
		boolean cqEnvelopeWhitenPostSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_ENVELOPE_WHITEN_POST_SWITCH);
		boolean cqCalibrateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_SWITCH);
		double cqCalibrateRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_RANGE);
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		double lowCQThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD);
		double highCQThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD);
		double lowCQPitchThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD);
		double highCQPitchThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD);

		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		ConstantQFeatures cqf = aff.getConstantQFeatures();
		cqf.buildToneMapFrame(toneMap);

		ToneMap cqAdaptiveWhitenControlMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_WHITENER", streamId));
		cqAdaptiveWhitenControlMap.addTimeFrame(toneMap.getTimeFrame(sequence).clone());

		AudioTuner tuner = new AudioTuner();

		ToneTimeFrame ttf = toneMap.getTimeFrame(sequence);
		ToneTimeFrame previousTimeFrame = toneMap.getPreviousTimeFrame(ttf.getStartTime());

		LOG.finer(">>CQ TIME: " + ttf.getStartTime() + ", " + ttf.getMaxAmplitude() + ", " + ttf.getMinAmplitude()
				+ ", " + ttf.getRmsPower());

		if (cqEnvelopeWhitenPreSwitch && previousTimeFrame != null) {
			ttf.envelopeWhiten(previousTimeFrame, cqEnvelopeWhitenThreshold, cqEnvelopeWhitenDecayFactor,
					cqEnvelopeWhitenAttackFactor);
		}

		if (cqSwitchSharpenHarmonic) {
			if (cqSwitchPreHarmonics) {
				tuner.processOvertones(toneMap.getTimeFrame());
			}
			if (cqSwitchPreSharpen) {
				ttf.sharpen(cqSharpenThreshold);
			}
		} else {
			if (cqSwitchPreSharpen) {
				ttf.sharpen(cqSharpenThreshold);
			}
			if (cqSwitchPreHarmonics) {
				tuner.processOvertones(toneMap.getTimeFrame());
			}
		}

		if (cqSwitchCompress) {
			ttf.compress(compression, cqSwitchCompressMax);
		}
		if (cqSwitchSquare) {
			ttf.square();
		}

		if (cqSwitchLowThreshold) {
			ttf.lowThreshold(lowThreshold, signalMinimum);
		}

		if (cqSwitchScale) {
			ttf.scale(lowCQThreshold, highCQThreshold, cqSwitchCompressLog);
		}

		if (cqSwitchWhiten) {
			ttf.adaptiveWhiten(cqAdaptiveWhitenControlMap, toneMap.getPreviousTimeFrame(ttf.getStartTime()),
					cqWhitenFactor, cqWhitenThreshold, cqSwitchWhitenCompensate);
		}

		if (cqSwitchDecibel) {
			ttf.decibel(decibelLevel);
		}

		if (cqSwitchSharpenHarmonic) {
			if (cqSwitchPostHarmonics) {
				tuner.processOvertones(toneMap.getTimeFrame());
			}
			if (cqSwitchPostSharpen) {
				ttf.sharpen(cqSharpenThreshold);
			}
		} else {
			if (cqSwitchPostSharpen) {
				ttf.sharpen(cqSharpenThreshold);
			}
			if (cqSwitchPostHarmonics) {
				tuner.processOvertones(toneMap.getTimeFrame());
			}
		}

		if (cqEnvelopeWhitenPostSwitch && previousTimeFrame != null) {
			ttf.envelopeWhiten(previousTimeFrame, cqEnvelopeWhitenThreshold, cqEnvelopeWhitenDecayFactor,
					cqEnvelopeWhitenAttackFactor);
		}

		if (previousTimeFrame != null) {
			ttf.updateSpectralFlux(previousTimeFrame);
		}

		ttf.filter(toneMapMinFrequency, toneMapMaxFrequency);

		if (workspace.getAtlas().hasCalibrationMap(streamId) && cqCalibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			double cmPower = cm.get(ttf.getStartTime());
			double cmMaxWindowPower = cm.getMaxPower(ttf.getStartTime() - cqCalibrateRange / 2,
					ttf.getStartTime() + cqCalibrateRange / 2);
			ttf.calibrate(cmMaxWindowPower, cmPower, lowThreshold);
		}

		LOG.finer(">>CQ POST WHITEN: " + ttf.getStartTime() + ", " + ttf.getRmsPower() + ", " + ttf.getSpectralFlux()
				+ ", " + ttf.getSpectralCentroid());

		LOG.finer(">>CQ MAX/MIN AMP X: " + ttf.getMaxAmplitude() + ", " + ttf.getMinAmplitude());

		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());

		cell.send(streamId, sequence);

	}
}
