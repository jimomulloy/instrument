package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.SACFFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioSACFProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioSACFProcessor.class.getName());

	public AudioSACFProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);

		LOG.finer(">>AudioSACFProcessor accept: " + sequence + ", streamId: " + streamId);
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		boolean cqCalibrateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_SWITCH);
		double cqCalibrateRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_CALIBRATE_RANGE);
		double lowThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);

		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);

		SACFFeatures features = aff.getSACFFeatures();
		features.buildToneMapFrame(toneMap);

		toneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		ToneTimeFrame ttf = toneMap.getTimeFrame();

		if (workspace.getAtlas().hasCalibrationMap(streamId) && cqCalibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			double cmPower = cm.get(ttf.getStartTime());
			double cmMaxWindowPower = cm.getMaxPower(ttf.getStartTime() - cqCalibrateRange / 2,
					ttf.getStartTime() + cqCalibrateRange / 2);
			ttf.calibrate(cmMaxWindowPower, cmPower, lowThreshold);
			LOG.severe(">>AudioSACFProcessor calib: " + ttf.getStartTime() + ", cmMaxWindowPower: " + cmMaxWindowPower
					+ ", cmPower: " + cmPower + ", lowThreshold: " + lowThreshold);
		}

		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
