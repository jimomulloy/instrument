package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneSynthesiser;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioSynthesisProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioSynthesisProcessor.class.getName());

	public AudioSynthesisProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioSynthesisProcessor accept: " + sequence + ", streamId: " + streamId);

		double quantizeRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_RANGE);
		double quantizePercent = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_PERCENT);
		int quantizeBeat = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_QUANTIZE_BEAT);

		ToneMap synthesisToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap chromaToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, streamId));

		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
		ToneMap notatePeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE.toString() + "_PEAKS", streamId));
		ToneMap notateSpectralToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE.toString() + "_SPECTRAL", streamId));
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		boolean synthFillNotes = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_NOTES_SWITCH);
		boolean synthFillChords = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_CHORDS_SWITCH);
		boolean synthFillLegatoSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_FILL_LEGATO_SWITCH);
		boolean synthChordFirstSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD_FIRST_SWITCH);
		boolean calibrateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_SWITCH);
		boolean calibrateForwardSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_FORWARD_SWITCH);
		double calibrateRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE);
		double lowThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);

		ToneTimeFrame notateFrame = notateToneMap.getTimeFrame(sequence);
		ToneTimeFrame notatePeaksFrame = notatePeaksToneMap.getTimeFrame(sequence);
		ToneTimeFrame notateSpectralFrame = notateSpectralToneMap.getTimeFrame(sequence);

		ToneTimeFrame chromaFrame = chromaToneMap.getTimeFrame(sequence);

		ToneTimeFrame synthesisFrame = notateFrame.clone();
		synthesisToneMap.addTimeFrame(synthesisFrame);
		synthesisFrame.mergeNotes(synthesisToneMap, notateFrame);

		synthesisFrame.integratePeaks(notatePeaksFrame);
		synthesisFrame.merge(synthesisToneMap, notateSpectralFrame);

		synthesisFrame.filter(toneMapMinFrequency, toneMapMaxFrequency);

		if (workspace.getAtlas().hasCalibrationMap(streamId) && calibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			synthesisFrame.calibrate(synthesisToneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
		}

		synthesisFrame.setChord(synthesisToneMap, chromaFrame);

		CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);

		ToneSynthesiser synthesiser = synthesisToneMap.getToneSynthesiser();
		synthesiser.synthesise(synthesisFrame, cm, quantizeRange, quantizePercent, quantizeBeat, synthFillChords,
				synthFillNotes, synthChordFirstSwitch, synthFillLegatoSwitch);

		int tmIndex = sequence - 30;
		ToneTimeFrame timeFrame;
		if (tmIndex < 0) {
			tmIndex = 0;
		}
		for (int i = tmIndex + 1; i <= sequence; i++) {
			timeFrame = synthesisToneMap.getTimeFrame(i);
			if (timeFrame != null) {
				console.getVisor().updateChromaSynthView(synthesisToneMap, timeFrame);
				console.getVisor().updateToneMapView(synthesisToneMap, timeFrame, this.cell.getCellType().toString());
			}
		}

		console.getVisor().updateChromaSynthView(synthesisToneMap, synthesisFrame);
		console.getVisor().updateToneMapView(synthesisToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
