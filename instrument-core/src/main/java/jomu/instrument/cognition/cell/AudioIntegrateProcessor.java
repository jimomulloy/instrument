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

		boolean integrateHpsSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_HPS_SWITCH);
		boolean integrateCQSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_CQ_SWITCH);
		boolean integratePeaksSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PEAKS_SWITCH);
		boolean integrateSpectralSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SPECTRAL_SWITCH);
		boolean integratePitchSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PITCH_SWITCH);
		boolean integrateSACFSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SACF_SWITCH);
		boolean integrateYINSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_YIN_SWITCH);
		boolean integrateSPSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SP_SWITCH);
		boolean integrateTPSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_TP_SWITCH);
		boolean integrateMFCCSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_MFCC_SWITCH);
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
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.toString() + "_HARMONIC_MASK", streamId));

		ToneMap integrateToneMap = null;
		ToneMap integratePeaksToneMap = null;
		ToneMap integrateSpectralToneMap = null;

		if (integrateCQSwitch) {
			integrateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		}
		if (integratePeaksSwitch) {
			integratePeaksToneMap = workspace.getAtlas()
					.getToneMap(buildToneMapKey(this.cell.getCellType().toString() + "_PEAKS", streamId));
		}
		if (integrateSpectralSwitch) {
			integrateSpectralToneMap = workspace.getAtlas()
					.getToneMap(buildToneMapKey(this.cell.getCellType().toString() + "_SPECTRAL", streamId));
		}

		if (integrateToneMap == null && integratePeaksToneMap == null && integrateSpectralToneMap == null) {
			throw new InstrumentException("AudioIntegrateProcessor has no options");
		}

		if (integrateHpsSwitch) {
			if (integrateCQSwitch) {
				integrateToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
			}
			if (integratePeaksSwitch) {
				integratePeaksToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
			}
			if (integrateSpectralSwitch) {
				integrateSpectralToneMap.addTimeFrame(hpsMaskToneMap.getTimeFrame(sequence).clone());
			}
		} else {
			if (integrateCQSwitch) {
				integrateToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
			}
			if (integratePeaksSwitch) {
				integratePeaksToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
			}
			if (integrateSpectralSwitch) {
				integrateSpectralToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
			}
		}

		if (integrateCQSwitch) {
			integrateToneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);
			ToneTimeFrame ttf = integrateToneMap.getTimeFrame();
			ttf.reset();
			console.getVisor().updateToneMapView(integrateToneMap, this.cell.getCellType().toString());
		}

		if (integratePeaksSwitch) {
			integratePeaksToneMap.getTimeFrame().clear();
			if (integratePitchSwitch) {
				integratePeaksToneMap.getTimeFrame().integratePeaks(pitchToneMap.getTimeFrame(sequence));
			}
			if (integrateSACFSwitch) {
				integratePeaksToneMap.getTimeFrame().integratePeaks(sacfToneMap.getTimeFrame(sequence));
			}
			if (integrateTPSwitch) {
				integratePeaksToneMap.getTimeFrame().integratePeaks(tpToneMap.getTimeFrame(sequence));
			}
			if (integrateYINSwitch) {
				integratePeaksToneMap.getTimeFrame().integratePeaks(yinToneMap.getTimeFrame(sequence));
			}
			if (integrateMFCCSwitch) {
				integratePeaksToneMap.getTimeFrame().integratePeaks(mfccToneMap.getTimeFrame(sequence));
			}
			if (integrateSPSwitch) {
				integratePeaksToneMap.getTimeFrame().integratePeaks(spToneMap.getTimeFrame(sequence));
			}

			integratePeaksToneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

			ToneTimeFrame ipttf = integratePeaksToneMap.getTimeFrame();

			if (workspace.getAtlas().hasCalibrationMap(streamId) && calibrateSwitch) {
				CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
				ipttf.calibrate(integratePeaksToneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
			}
			console.getVisor().updateToneMapView(integratePeaksToneMap, this.cell.getCellType().toString() + "_PEAKS");
		}

		if (integrateSpectralSwitch) {
			integrateSpectralToneMap.getTimeFrame().clear();
			if (integratePitchSwitch) {
				integrateSpectralToneMap.getTimeFrame().merge(pitchToneMap.getTimeFrame(sequence));
			}
			if (integrateSACFSwitch) {
				integrateSpectralToneMap.getTimeFrame().merge(sacfToneMap.getTimeFrame(sequence));
			}
			if (integrateSPSwitch) {
				integrateSpectralToneMap.getTimeFrame().merge(spToneMap.getTimeFrame(sequence));
			}
			if (integrateYINSwitch) {
				integrateSpectralToneMap.getTimeFrame().merge(yinToneMap.getTimeFrame(sequence));
			}

			integrateSpectralToneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

			ToneTimeFrame isttf = integrateSpectralToneMap.getTimeFrame();

			if (workspace.getAtlas().hasCalibrationMap(streamId) && calibrateSwitch) {
				CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
				isttf.calibrate(integrateSpectralToneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold,
						false);
			}
			console.getVisor().updateToneMapView(integrateSpectralToneMap,
					this.cell.getCellType().toString() + "_SPECTRAL");
		}

		cell.send(streamId, sequence);
	}
}
