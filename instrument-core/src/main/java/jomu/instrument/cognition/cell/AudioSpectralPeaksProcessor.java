package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.SpectralPeaksFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioSpectralPeaksProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioSpectralPeaksProcessor.class.getName());

	public AudioSpectralPeaksProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		double lowThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);
		double signalMinimum = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SIGNAL_MINIMUM);
		float pdLowThreshold = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_LOW_THRESHOLD);
		double normaliseThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_NORMALISE_THRESHOLD);
		double decibelLevel = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_DECIBEL_LEVEL);
		float compression = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_COMPRESSION);
		boolean switchCompress = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_COMPRESS);
		boolean switchSquare = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_SQUARE);
		boolean switchLowThreshold = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_LOW_THRESHOLD);
		boolean switchDecibel = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_DECIBEL);
		boolean switchNormalise = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SP_SWITCH_NORMALISE);
		boolean tpSwitchPeaks = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_PEAKS);
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		boolean cqCalibrateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_SWITCH);
		double cqCalibrateRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_RANGE);

		LOG.finer(">>AudioSpectralPeaksProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);

		SpectralPeaksFeatures spf = aff.getSpectralPeaksFeatures();
		spf.buildToneMapFrame(toneMap, tpSwitchPeaks, pdLowThreshold);

		if (switchCompress) {
			toneMap.getTimeFrame().compress(compression, false);
		}
		if (switchSquare) {
			toneMap.getTimeFrame().square();
		}

		if (switchLowThreshold) {
			toneMap.getTimeFrame().lowThreshold(lowThreshold, signalMinimum);
		}

		if (switchDecibel) {
			toneMap.getTimeFrame().decibel(decibelLevel);
		}

		toneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		ToneTimeFrame ttf = toneMap.getTimeFrame();

		if (workspace.getAtlas().hasCalibrationMap(streamId) && cqCalibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			double cmPower = cm.get(ttf.getStartTime());
			double cmMaxWindowPower = cm.getMaxPower(ttf.getStartTime() - cqCalibrateRange / 2,
					ttf.getStartTime() + cqCalibrateRange / 2);
			ttf.calibrate(cmMaxWindowPower, cmPower, lowThreshold, true);
		}

		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
