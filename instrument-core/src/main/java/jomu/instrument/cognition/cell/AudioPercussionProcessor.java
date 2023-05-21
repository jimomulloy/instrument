package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.PercussionFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioPercussionProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioPercussionProcessor.class.getName());

	public AudioPercussionProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioPercussionProcessor accept seq: " + sequence + ", streamId: " + streamId);
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		boolean calibrateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_SWITCH);
		double calibrateRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE);
		boolean calibrateForwardSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_FORWARD_SWITCH);
		double lowThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);

		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		PercussionFeatures features = aff.getPercussionFeatures();
		features.buildToneMapFrame(toneMap);

		toneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);
		ToneTimeFrame ttf = toneMap.getTimeFrame();

		if (ttf.getMaxAmplitude() > ToneTimeFrame.AMPLITUDE_FLOOR) {
			LOG.severe(">>AudioPercProcessor 1 max: " + sequence + ", : " + ttf.getMaxAmplitude());
		}
		if (workspace.getAtlas().hasCalibrationMap(streamId) && calibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			ttf.calibrate(toneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
		}

		if (ttf.getMaxAmplitude() > ToneTimeFrame.AMPLITUDE_FLOOR) {
			LOG.severe(">>AudioPercProcessor 2 max: " + sequence + ", : " + ttf.getMaxAmplitude());
		}

		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
