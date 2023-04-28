package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
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
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);

		LOG.finer(">>AudioIntegrateProcessor accept: " + sequence + ", streamId: " + streamId);

		boolean integrateSwitchHps = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_HPS_SWITCH);
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		boolean calibrateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_SWITCH);
		boolean calibrateForwardSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_FORWARD_SWITCH);
		double calibrateRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE);
		double lowThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);

		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		ToneMap pitchToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_PITCH, streamId));
		ToneMap sacfToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_SACF, streamId));
		ToneMap spToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_SPECTRAL_PEAKS, streamId));
		ToneMap tpToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_TUNER_PEAKS, streamId));
		ToneMap yinToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_YIN, streamId));
		ToneMap mfccToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_MFCC, streamId));
		ToneMap hpsMaskToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_HARMONIC_MASK", streamId));

		ToneMap integrateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap integratePeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_PEAKS", streamId));
		ToneMap integrateSpectralToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_SPECTRAL", streamId));

		if (integrateSwitchHps) {
			LOG.finer(">>AudioIntegrateProcessor use hpsMaskToneMap");
			integrateToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
			integratePeaksToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
			integrateSpectralToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
		} else {
			LOG.finer(">>AudioIntegrateProcessor use cqToneMap");
			integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
			integratePeaksToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
			integrateSpectralToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		}

		integrateToneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);
		ToneTimeFrame ttf = integrateToneMap.getTimeFrame();
		ttf.reset();
		LOG.finer(">>INT TTF: " + ttf.getStartTime() + ", " + ttf.getMaxAmplitude() + ", " + ttf.getMinAmplitude()
				+ ", " + ttf.getRmsPower());

		integratePeaksToneMap.getTimeFrame().clear();
		integratePeaksToneMap.getTimeFrame().integratePeaks(pitchToneMap.getTimeFrame(sequence));
		integratePeaksToneMap.getTimeFrame().integratePeaks(sacfToneMap.getTimeFrame(sequence));
		integratePeaksToneMap.getTimeFrame().integratePeaks(tpToneMap.getTimeFrame(sequence));
		integratePeaksToneMap.getTimeFrame().integratePeaks(yinToneMap.getTimeFrame(sequence));
		// integratePeaksToneMap.getTimeFrame().integratePeaks(mfccToneMap.getTimeFrame(sequence));
		// integratePeaksToneMap.getTimeFrame().integratePeaks(spToneMap.getTimeFrame(sequence))

		integratePeaksToneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		ToneTimeFrame ipttf = integratePeaksToneMap.getTimeFrame();

		if (workspace.getAtlas().hasCalibrationMap(streamId) && calibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			ipttf.calibrate(integratePeaksToneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
		}

		integrateSpectralToneMap.getTimeFrame().clear();
		integrateSpectralToneMap.getTimeFrame().merge(pitchToneMap.getTimeFrame(sequence));
		integrateSpectralToneMap.getTimeFrame().merge(sacfToneMap.getTimeFrame(sequence));
		integrateSpectralToneMap.getTimeFrame().merge(spToneMap.getTimeFrame(sequence));
		integrateSpectralToneMap.getTimeFrame().merge(yinToneMap.getTimeFrame(sequence));
		// integrateSpectralToneMap.getTimeFrame().merge(mfccToneMap.getTimeFrame(sequence));

		integrateSpectralToneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		ToneTimeFrame isttf = integrateSpectralToneMap.getTimeFrame();

		if (workspace.getAtlas().hasCalibrationMap(streamId) && calibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			isttf.calibrate(integrateSpectralToneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
		}

		console.getVisor().updateToneMapView(integrateToneMap, this.cell.getCellType().toString());
		console.getVisor().updateToneMapView(integratePeaksToneMap, this.cell.getCellType().toString() + "_PEAKS");
		console.getVisor().updateToneMapView(integrateSpectralToneMap,
				this.cell.getCellType().toString() + "_SPECTRAL");
		cell.send(streamId, sequence);
	}
}
