package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

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

		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		ToneMap pitchToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_PITCH, streamId));
		ToneMap sacfToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_SACF, streamId));
		ToneMap spToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_SPECTRAL_PEAKS, streamId));
		ToneMap tpToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_TUNER_PEAKS, streamId));
		ToneMap yinToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_YIN, streamId));
		ToneMap hpsMaskToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_HARMONIC_MASK", streamId));

		ToneMap integrateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap integratePeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_PEAKS", streamId));

		if (integrateSwitchHps) {
			LOG.finer(">>AudioIntegrateProcessor use hpsMaskToneMap");
			integrateToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
			integratePeaksToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
		} else {
			LOG.finer(">>AudioIntegrateProcessor use cqToneMap");
			integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
			integratePeaksToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		}
		integratePeaksToneMap.getTimeFrame().clear();
		LOG.severe(">>IP target: " + integratePeaksToneMap.getTimeFrame().getPitchLow() + ", "
				+ integratePeaksToneMap.getTimeFrame().getPitchHigh());
		LOG.severe(">>IP source Pitch: " + pitchToneMap.getTimeFrame(sequence).getPitchLow() + ", "
				+ pitchToneMap.getTimeFrame(sequence).getPitchHigh());
		integratePeaksToneMap.getTimeFrame().integratePeaks(pitchToneMap.getTimeFrame(sequence));
		LOG.severe(">>IP source sacfToneMap: " + sacfToneMap.getTimeFrame(sequence).getPitchLow() + ", "
				+ sacfToneMap.getTimeFrame(sequence).getPitchHigh());
		integratePeaksToneMap.getTimeFrame().integratePeaks(sacfToneMap.getTimeFrame(sequence));
		LOG.severe(">>IP source sp: " + spToneMap.getTimeFrame(sequence).getPitchLow() + ", "
				+ spToneMap.getTimeFrame(sequence).getPitchHigh());
		integratePeaksToneMap.getTimeFrame().integratePeaks(spToneMap.getTimeFrame(sequence));
		LOG.severe(">>IP source tp: " + tpToneMap.getTimeFrame(sequence).getPitchLow() + ", "
				+ tpToneMap.getTimeFrame(sequence).getPitchHigh());
		integratePeaksToneMap.getTimeFrame().integratePeaks(tpToneMap.getTimeFrame(sequence));
		LOG.severe(">>IP source yin: " + yinToneMap.getTimeFrame(sequence).getPitchLow() + ", "
				+ yinToneMap.getTimeFrame(sequence).getPitchHigh());
		integratePeaksToneMap.getTimeFrame().integratePeaks(yinToneMap.getTimeFrame(sequence));

		integrateToneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		ToneTimeFrame ipttf = integratePeaksToneMap.getTimeFrame();

		if (workspace.getAtlas().hasCalibrationMap(streamId) && cqCalibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			double cmPower = cm.get(ipttf.getStartTime());
			double cmMaxWindowPower = cm.getMaxPower(ipttf.getStartTime() - cqCalibrateRange / 2,
					ipttf.getStartTime() + cqCalibrateRange / 2);
			ipttf.calibrate(cmMaxWindowPower, cmPower, lowThreshold);
		}
		console.getVisor().updateToneMapView(integrateToneMap, this.cell.getCellType().toString());
		console.getVisor().updateToneMapView(integratePeaksToneMap, this.cell.getCellType().toString() + "_PEAKS");
		cell.send(streamId, sequence);
	}
}
