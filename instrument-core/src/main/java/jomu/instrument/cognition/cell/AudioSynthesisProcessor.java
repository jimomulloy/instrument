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
		boolean integrateCQSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_CQ_SWITCH);
		boolean integratePeaksSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_PEAKS_SWITCH);
		boolean integrateSpectralSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_INTEGRATION_SPECTRAL_SWITCH);
		int synthSweepRange = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_SWEEP_RANGE);

		ToneMap synthesisToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		ToneMap postChromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, streamId));
		ToneMap preChromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId));
		ToneMap hpsHarmonicMaskToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.toString() + "_HARMONIC_MASK", streamId));
		ToneMap beatToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId));
		ToneMap onsetToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_ONSET, streamId));
		ToneMap onsetSmoothedToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_ONSET.toString() + "_SMOOTHED", streamId));
		ToneMap hpsPercussionMaskToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_PERCUSSION_MASK", streamId));
		ToneMap percussionToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_PERCUSSION, streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
		ToneMap notatePeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE.toString() + "_PEAKS", streamId));
		ToneMap notateSpectralToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE.toString() + "_SPECTRAL", streamId));

		ToneTimeFrame notateFrame = null;
		ToneTimeFrame notatePeaksFrame = null;
		ToneTimeFrame notateSpectralFrame = null;

		if (integrateCQSwitch) {
			notateFrame = notateToneMap.getTimeFrame(sequence);
		}
		if (integratePeaksSwitch) {
			notatePeaksFrame = notatePeaksToneMap.getTimeFrame(sequence);
		}
		if (integrateSpectralSwitch) {
			notateSpectralFrame = notateSpectralToneMap.getTimeFrame(sequence);
		}

		if (notateFrame == null && notatePeaksFrame == null && notateSpectralFrame == null) {
			throw new InstrumentException("AudioSynthesisProcessor has no options");
		}

		ToneTimeFrame synthesisFrame = null;

		if (integrateCQSwitch) {
			synthesisFrame = notateFrame.clone();
			synthesisToneMap.addTimeFrame(synthesisFrame);
			synthesisFrame.mergeNotes(synthesisToneMap, notateFrame);
		}

		if (integratePeaksSwitch) {
			if (synthesisFrame == null) {
				synthesisFrame = notatePeaksFrame.clone();
				synthesisToneMap.addTimeFrame(synthesisFrame);
			}
			synthesisFrame.integratePeaks(notatePeaksFrame);
		}
		if (integrateSpectralSwitch) {
			if (synthesisFrame == null) {
				synthesisFrame = notateSpectralFrame.clone();
				synthesisToneMap.addTimeFrame(synthesisFrame);
			}
			synthesisFrame.merge(synthesisToneMap, notateSpectralFrame);
		}

		if (synthesisFrame == null) {
			synthesisFrame = cqToneMap.getTimeFrame(sequence).clone();
			synthesisToneMap.addTimeFrame(synthesisFrame);
		}
		synthesisFrame.filter(toneMapMinFrequency, toneMapMaxFrequency);

		if (workspace.getAtlas().hasCalibrationMap(streamId) && calibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
			synthesisFrame.calibrate(synthesisToneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
		}

		ToneTimeFrame preChromaTimeFrame = preChromaToneMap.getTimeFrame(sequence);
		ToneTimeFrame postChromaTimeFrame = postChromaToneMap.getTimeFrame(sequence);
		ToneTimeFrame hpsHarmonicMaskTimeFrame = hpsHarmonicMaskToneMap.getTimeFrame(sequence);
		ToneTimeFrame onsetSmoothedTimeFrame = onsetSmoothedToneMap.getTimeFrame(sequence);

		if (postChromaTimeFrame != null) {
			synthesisFrame.setChord(postChromaTimeFrame);
		}
		if (postChromaTimeFrame != null) {
			synthesisFrame.putChordList(CellTypes.AUDIO_POST_CHROMA.name(), postChromaTimeFrame.getChord());
		}
		if (preChromaTimeFrame != null) {
			synthesisFrame.putChordList(CellTypes.AUDIO_PRE_CHROMA.name(), preChromaTimeFrame.getChord());
		}
		if (hpsHarmonicMaskTimeFrame != null) {
			synthesisFrame.putChordList(CellTypes.AUDIO_HPS.name() + "_HARMONIC_MASK",
					hpsHarmonicMaskTimeFrame.getChord());
		}
		if (onsetSmoothedTimeFrame != null) {
			synthesisFrame.putChordList(CellTypes.AUDIO_ONSET.name() + "_SMOOTHED", onsetSmoothedTimeFrame.getChord());
		}

		ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(sequence);
		ToneTimeFrame onsetTimeFrame = onsetToneMap.getTimeFrame(sequence);
		ToneTimeFrame percussionTimeFrame = percussionToneMap.getTimeFrame(sequence);
		ToneTimeFrame hpsPercussionMaskTimeFrame = hpsPercussionMaskToneMap.getTimeFrame(sequence);

		if (beatTimeFrame != null) {
			synthesisFrame.setBeatAmplitude(beatTimeFrame.getBeatAmplitude());
		}
		if (beatTimeFrame != null) {
			synthesisFrame.putBeat(CellTypes.AUDIO_BEAT.name(), beatTimeFrame.getMaxAmplitude());
		}
		if (onsetTimeFrame != null) {
			synthesisFrame.putBeat(CellTypes.AUDIO_ONSET.name(), onsetTimeFrame.getMaxAmplitude());
		}
		if (percussionTimeFrame != null) {
			synthesisFrame.putBeat(CellTypes.AUDIO_PERCUSSION.name(), percussionTimeFrame.getMaxAmplitude());
		}
		if (hpsPercussionMaskTimeFrame != null) {
			synthesisFrame.putBeat(CellTypes.AUDIO_HPS.name() + "_PERCUSSION_MASK",
					hpsPercussionMaskTimeFrame.getMaxAmplitude());
		}

		CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);
		if (beatTimeFrame != null) {
			double beatTime = cm.getBeatAfterTime(beatTimeFrame.getStartTime(), 110);
			if (beatTime != -1) {
				synthesisFrame.putBeat(CellTypes.AUDIO_BEAT.name() + "_CALIBRATION", synthesisFrame.getMaxAmplitude());
			} else {
				synthesisFrame.putBeat(CellTypes.AUDIO_BEAT.name() + "_CALIBRATION", ToneTimeFrame.AMPLITUDE_FLOOR);
			}
		}

		ToneSynthesiser synthesiser = synthesisToneMap.getToneSynthesiser();

		int tmIndex = sequence - synthSweepRange;

		if (tmIndex > 0) {
			synthesisFrame = synthesisToneMap.getTimeFrame(tmIndex);
			if (synthesisFrame != null) {
				synthesiser.synthesise(synthesisFrame, cm);
				console.getVisor().updateChromaSynthView(synthesisToneMap, synthesisFrame);
				console.getVisor().updateToneMapView(synthesisToneMap, synthesisFrame,
						this.cell.getCellType().toString());
			}
			console.getVisor().updateToneMapView(synthesisToneMap, this.cell.getCellType().toString());
			cell.send(streamId, tmIndex);
		}

		if (isClosing(streamId, sequence)) {
			LOG.finer(">>AudioSynthesisProcessor CLOSE!!: " + sequence);
			if (tmIndex < 0) {
				tmIndex = 0;
			}
			for (int i = tmIndex + 1; i <= sequence; i++) {
				synthesisFrame = synthesisToneMap.getTimeFrame(i);
				if (synthesisFrame != null) {
					synthesiser.synthesise(synthesisFrame, cm);
					console.getVisor().updateChromaSynthView(synthesisToneMap, synthesisFrame);
					console.getVisor().updateToneMapView(synthesisToneMap, synthesisFrame,
							this.cell.getCellType().toString());
				}
				cell.send(streamId, i);
			}
			console.getVisor().updateToneMapView(synthesisToneMap, this.cell.getCellType().toString());
		}
	}
}
