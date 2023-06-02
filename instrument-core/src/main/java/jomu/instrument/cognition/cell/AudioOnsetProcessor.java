package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioOnsetProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioOnsetProcessor.class.getName());

	public AudioOnsetProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioOnsetProcessor accept seq: " + sequence + ", streamId: " + streamId);
		int onsetSmoothingFactor = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SMOOTHING_FACTOR);
		int onsetPeaksSweep = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_SWEEP);
		double onsetPeaksThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_THRESHOLD);
		double onsetPeaksEdgeFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_EDGE_FACTOR);
		int onsetEdgeFactor = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_EDGE_FACTOR);
		boolean onsetCQOriginSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_CQ_ORIGIN_SWITCH);
		boolean calibrateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_SWITCH);
		double calibrateRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE);
		boolean calibrateForwardSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_FORWARD_SWITCH);
		double lowThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);

		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		if (onsetCQOriginSwitch) {
			cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN, streamId));
		}
		ToneMap onsetToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap onsetSmoothedToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_SMOOTHED", streamId));
		onsetToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		onsetSmoothedToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone()).reset();

		if (sequence > 1) {
			ToneTimeFrame currentFrame = onsetSmoothedToneMap.getTimeFrame(sequence);
			ToneTimeFrame previousFrame = onsetSmoothedToneMap.getTimeFrame(sequence - 1);
			currentFrame.onsetWhiten(previousFrame, (double) onsetSmoothingFactor / 100.0);
		}

		if (sequence > 1) {
			ToneTimeFrame currentFrame = onsetToneMap.getTimeFrame(sequence);
			ToneTimeFrame previousFrame = cqToneMap.getTimeFrame(sequence - 1);
			currentFrame.onsetEdge(previousFrame, (double) onsetEdgeFactor / 100.0);
		}

		if (sequence > onsetPeaksSweep) {
			ToneTimeFrame currentFrame = onsetToneMap.getTimeFrame(sequence);
			ToneTimeFrame previousFrame = onsetToneMap.getTimeFrame(sequence - 1);
			currentFrame.onsetPeaks(previousFrame, onsetPeaksEdgeFactor, onsetPeaksSweep, onsetPeaksThreshold);
		}

		ToneTimeFrame ottf = onsetToneMap.getTimeFrame();
		ToneTimeFrame osttf = onsetSmoothedToneMap.getTimeFrame();
		if (workspace.getAtlas().hasCalibrationMap(streamId) && calibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			ottf.calibrate(onsetToneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
			osttf.calibrate(onsetSmoothedToneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
		}

		console.getVisor().updateToneMapView(onsetToneMap, this.cell.getCellType().toString());
		console.getVisor().updateToneMapView(onsetSmoothedToneMap, this.cell.getCellType().toString() + "_SMOOTHED");

		cell.send(streamId, sequence);
	}
}
