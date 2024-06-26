package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.BeatListElement;
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
		int chromaRootNote = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_ROOT_NOTE);
		boolean chromaHarmonicsSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_HARMONICS_SWITCH);
		double normaliseThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD);
		boolean chromaCeilingSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CEILING_SWITCH);
		boolean chromaChordifySharpenSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_SHARPEN_SWITCH);
		double chromaChordifyThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CHROMA_CHORDIFY_THRESHOLD);
		int beatTiming = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_BEAT_TIMING);
		int chordTiming = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD_TIMING);
		double windowInterval = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL);
		int onsetSmoothingFactor = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SMOOTHING_FACTOR);
		int onsetPeaksSweep = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_SWEEP);
		double onsetPeaksThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_THRESHOLD);
		double onsetPeaksEdgeFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_PEAKS_EDGE_FACTOR);

		ToneMap synthesisToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneMap cqToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		ToneMap postChromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, streamId));
		ToneMap preChromaToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId));
		ToneMap hpsHarmonicToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.toString() + "_HARMONIC", streamId));
		ToneMap beatToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId));
		ToneMap onsetToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_ONSET, streamId));
		ToneMap onsetSmoothedToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_ONSET.toString() + "_SMOOTHED", streamId));
		ToneMap hpsPercussionToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_PERCUSSION", streamId));
		ToneMap percussionToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_PERCUSSION, streamId));
		ToneMap notateToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
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
			synthesisFrame.merge(synthesisToneMap, notatePeaksFrame);
		}
		if (integrateSpectralSwitch) {
			if (synthesisFrame == null) {
				synthesisFrame = notateSpectralFrame.clone();
				synthesisToneMap.addTimeFrame(synthesisFrame);
			}
			synthesisFrame.integratePeaks(notateSpectralFrame);
			synthesisFrame.merge(synthesisToneMap, notateSpectralFrame);
		}

		if (synthesisFrame == null) {
			synthesisFrame = cqToneMap.getTimeFrame(sequence)
					.clone();
			synthesisToneMap.addTimeFrame(synthesisFrame);
		}
		synthesisFrame.filter(toneMapMinFrequency, toneMapMaxFrequency);

		if (workspace.getAtlas()
				.hasCalibrationMap(streamId) && calibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas()
					.getCalibrationMap(streamId);
			// --synthesisFrame.calibrate(synthesisToneMap, cm, calibrateRange,
			// calibrateForwardSwitch, lowThreshold,
			// false);
		}

		if (sequence > onsetPeaksSweep) {
			ToneTimeFrame previousFrame = synthesisToneMap.getTimeFrame(sequence - 1);
			synthesisFrame.onsetPeaks(previousFrame, onsetPeaksEdgeFactor, onsetPeaksSweep, onsetPeaksThreshold,
					(double) onsetSmoothingFactor / 100.0);
		}

		int chordSourceSequence = sequence;
		int chordTargetSequence = sequence;
		if (chordTiming > 0 && chordSourceSequence - chordTiming > 0) {
			chordSourceSequence -= chordTiming;
		} else if (chordTiming < 0 && chordTargetSequence + chordTiming > 0) {
			chordTargetSequence += chordTiming;
		}

		ToneTimeFrame chordSynthesisFrame = synthesisToneMap.getTimeFrame(chordTargetSequence);

		ToneTimeFrame cqTimeFrame = cqToneMap.getTimeFrame(chordSourceSequence);
		ToneTimeFrame preChromaTimeFrame = preChromaToneMap.getTimeFrame(chordSourceSequence);
		ToneTimeFrame postChromaTimeFrame = postChromaToneMap.getTimeFrame(chordSourceSequence);
		ToneTimeFrame hpsHarmonicTimeFrame = hpsHarmonicToneMap.getTimeFrame(chordSourceSequence);
		ToneTimeFrame onsetSmoothedTimeFrame = onsetSmoothedToneMap.getTimeFrame(chordSourceSequence);

		if (postChromaTimeFrame != null) {
			chordSynthesisFrame.setChord(postChromaTimeFrame);
		}
		if (postChromaTimeFrame != null) {
			chordSynthesisFrame.putChordList(CellTypes.AUDIO_POST_CHROMA.name(), postChromaTimeFrame.getChord());
		}

		if (preChromaTimeFrame != null) {
			ToneTimeFrame chroma = preChromaTimeFrame
					.chroma(chromaRootNote, preChromaTimeFrame.getPitchLow(),
							preChromaTimeFrame.getPitchHigh(),
							chromaHarmonicsSwitch)
					.normaliseEuclidian(normaliseThreshold, chromaCeilingSwitch)
					.chromaQuantize(chromaChordifyThreshold)
					.chromaChordify(chromaChordifyThreshold, chromaChordifySharpenSwitch)
					.chordNoteOctivate(new ToneTimeFrame[] { preChromaTimeFrame });
			chordSynthesisFrame.putChordList(CellTypes.AUDIO_PRE_CHROMA.name(),
					chroma.getChord());
			chroma = preChromaTimeFrame
					.chroma(chromaRootNote, preChromaTimeFrame.getPitchLow(),
							preChromaTimeFrame.getPitchHigh(),
							chromaHarmonicsSwitch)
					.normaliseEuclidian(normaliseThreshold, chromaCeilingSwitch);
			chordSynthesisFrame.putChordList(CellTypes.AUDIO_PRE_CHROMA.name() + "_PADS",
					chroma.getFrameChord(cqTimeFrame));
		}

		if (hpsHarmonicTimeFrame != null) {
			ToneTimeFrame chroma = hpsHarmonicTimeFrame
					.chroma(chromaRootNote, hpsHarmonicTimeFrame.getPitchLow(),
							hpsHarmonicTimeFrame.getPitchHigh(),
							chromaHarmonicsSwitch)
					.normaliseEuclidian(normaliseThreshold, chromaCeilingSwitch)
					.chromaQuantize(chromaChordifyThreshold)
					.chromaChordify(chromaChordifyThreshold, chromaChordifySharpenSwitch)
					.chordNoteOctivate(new ToneTimeFrame[] { hpsHarmonicTimeFrame });
			chordSynthesisFrame.putChordList(CellTypes.AUDIO_HPS.name() + "_HARMONIC",
					chroma.getChord());
			chroma = hpsHarmonicTimeFrame
					.chroma(chromaRootNote, hpsHarmonicTimeFrame.getPitchLow(),
							hpsHarmonicTimeFrame.getPitchHigh(),
							chromaHarmonicsSwitch)
					.normaliseEuclidian(normaliseThreshold, chromaCeilingSwitch);
			chordSynthesisFrame.putChordList(CellTypes.AUDIO_HPS.name() + "_PADS",
					chroma.getFrameChord(hpsHarmonicTimeFrame));
		}
		if (onsetSmoothedTimeFrame != null) {
			ToneTimeFrame chroma = onsetSmoothedTimeFrame
					.chroma(chromaRootNote, onsetSmoothedTimeFrame.getPitchLow(),
							onsetSmoothedTimeFrame.getPitchHigh(),
							chromaHarmonicsSwitch)
					.normaliseEuclidian(normaliseThreshold, chromaCeilingSwitch)
					.chromaQuantize(chromaChordifyThreshold)
					.chromaChordify(chromaChordifyThreshold, chromaChordifySharpenSwitch)
					.chordNoteOctivate(new ToneTimeFrame[] { onsetSmoothedTimeFrame });
			chordSynthesisFrame.putChordList(CellTypes.AUDIO_ONSET.name() + "_SMOOTHED",
					chroma.getChord());
			chroma = onsetSmoothedTimeFrame
					.chroma(chromaRootNote, onsetSmoothedTimeFrame.getPitchLow(),
							onsetSmoothedTimeFrame.getPitchHigh(),
							chromaHarmonicsSwitch)
					.normaliseEuclidian(normaliseThreshold, chromaCeilingSwitch);
			chordSynthesisFrame.putChordList(CellTypes.AUDIO_ONSET.name() + "_PADS",
					chroma.getFrameChord(onsetSmoothedTimeFrame));
		}

		int beatSourceSequence = sequence;
		int beatTargetSequence = sequence;
		if (beatTiming > 0 && beatSourceSequence - beatTiming > 0) {
			beatSourceSequence -= beatTiming;
		} else if (beatTiming < 0 && beatTargetSequence + beatTiming > 0) {
			beatTargetSequence += beatTiming;

		}

		ToneTimeFrame beatSynthesisFrame = synthesisToneMap.getTimeFrame(beatTargetSequence);

		ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(beatSourceSequence);
		ToneTimeFrame onsetTimeFrame = onsetToneMap.getTimeFrame(beatSourceSequence);
		ToneTimeFrame percussionTimeFrame = percussionToneMap.getTimeFrame(beatSourceSequence);
		ToneTimeFrame hpsPercussionTimeFrame = hpsPercussionToneMap.getTimeFrame(beatSourceSequence);

		double beatTime = 0;
		double timeRange = 0;

		CalibrationMap cm = workspace.getAtlas()
				.getCalibrationMap(streamId);
		if (beatTimeFrame != null) {
			beatTime = cm.getBeatAfterTime(beatSynthesisFrame.getStartTime(), windowInterval + 10);
			timeRange = cm.getBeatRange(beatSynthesisFrame.getStartTime());
			if (beatTime != -1) {
				BeatListElement ble = new BeatListElement(1.0, beatSynthesisFrame.getStartTime(), timeRange);
				beatSynthesisFrame.putBeat(CellTypes.AUDIO_BEAT.name() + "_CALIBRATION", ble);
			} else {
				BeatListElement ble = new BeatListElement(ToneTimeFrame.AMPLITUDE_FLOOR,
						beatSynthesisFrame.getStartTime(), timeRange);
				beatSynthesisFrame.putBeat(CellTypes.AUDIO_BEAT.name() + "_CALIBRATION", ble);
			}
		}

		if (beatTimeFrame != null) {
			beatSynthesisFrame.setBeatAmplitude(beatTimeFrame.getBeatAmplitude());
		}
		if (beatSynthesisFrame.getOnsetElement() != null && beatSynthesisFrame.getOnsetElement().isPeak) {
			BeatListElement ble = new BeatListElement(beatSynthesisFrame.getOnsetElement().amplitude,
					beatSynthesisFrame.getStartTime(), timeRange);
			beatSynthesisFrame.putBeat(CellTypes.AUDIO_SYNTHESIS.name() + "_PEAKS", ble);
		} else {
			BeatListElement ble = new BeatListElement(ToneTimeFrame.AMPLITUDE_FLOOR,
					beatSynthesisFrame.getStartTime(), timeRange);
			beatSynthesisFrame.putBeat(CellTypes.AUDIO_SYNTHESIS.name() + "_PEAKS", ble);
		}
		if (beatTimeFrame != null) {
			BeatListElement ble = new BeatListElement(beatTimeFrame.getMaxAmplitude(),
					beatSynthesisFrame.getStartTime(), timeRange);
			beatSynthesisFrame.putBeat(CellTypes.AUDIO_BEAT.name(), ble);
		}
		if (onsetTimeFrame != null) {
			BeatListElement ble = new BeatListElement(onsetTimeFrame.getMaxAmplitude(),
					beatSynthesisFrame.getStartTime(), timeRange);
			beatSynthesisFrame.putBeat(CellTypes.AUDIO_ONSET.name(), ble);
			if (onsetTimeFrame.getOnsetElement() != null && onsetTimeFrame.getOnsetElement().isPeak) {
				ble = new BeatListElement(onsetTimeFrame.getOnsetElement().amplitude,
						beatSynthesisFrame.getStartTime(), timeRange);
				beatSynthesisFrame.putBeat(CellTypes.AUDIO_ONSET.name() + "_PEAKS", ble);
			} else {
				ble = new BeatListElement(ToneTimeFrame.AMPLITUDE_FLOOR, beatSynthesisFrame.getStartTime(),
						timeRange);
				beatSynthesisFrame.putBeat(CellTypes.AUDIO_ONSET.name() + "_PEAKS", ble);
			}
		}
		if (percussionTimeFrame != null) {
			BeatListElement ble = new BeatListElement(percussionTimeFrame.getMaxAmplitude(),
					beatSynthesisFrame.getStartTime(), timeRange);
			beatSynthesisFrame.putBeat(CellTypes.AUDIO_PERCUSSION.name(), ble);
		}
		if (hpsPercussionTimeFrame != null) {
			BeatListElement ble = new BeatListElement(hpsPercussionTimeFrame.getMaxAmplitude(),
					beatSynthesisFrame.getStartTime(), timeRange);
			beatSynthesisFrame.putBeat(CellTypes.AUDIO_HPS.name() + "_PERCUSSION", ble);
		}

		ToneSynthesiser synthesiser = synthesisToneMap.getToneSynthesiser();
		synthesisFrame = synthesisToneMap.getTimeFrame(sequence);
		synthesiser.synthesiseNotes(synthesisFrame, cm);

		int tmIndex = sequence - synthSweepRange;

		if (tmIndex > 0) {
			synthesisFrame = synthesisToneMap.getTimeFrame(tmIndex);
			if (synthesisFrame != null) {
				synthesiser.synthesiseChords(synthesisFrame, cm);
				console.getVisor()
						.updateToneMapView(synthesisToneMap, synthesisFrame, this.cell.getCellType()
								.toString());
			}
			console.getVisor()
					.updateToneMapView(synthesisToneMap, this.cell.getCellType()
							.toString());
			cell.send(streamId, tmIndex);
		}

		if (isClosing(streamId, sequence)) {
			if (tmIndex < 0) {
				tmIndex = 0;
			}
			for (int i = tmIndex + 1; i <= sequence; i++) {
				synthesisFrame = synthesisToneMap.getTimeFrame(i);
				if (synthesisFrame != null) {
					synthesiser.synthesiseChords(synthesisFrame, cm);
					console.getVisor()
							.updateToneMapView(synthesisToneMap, synthesisFrame, this.cell.getCellType()
									.toString());
				}
				cell.send(streamId, i);
			}
			console.getVisor()
					.updateToneMapView(synthesisToneMap, this.cell.getCellType()
							.toString());
		}
	}
}
