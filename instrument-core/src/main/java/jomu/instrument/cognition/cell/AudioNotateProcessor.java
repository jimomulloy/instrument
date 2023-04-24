package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.audio.AudioTuner;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioNotateProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioNotateProcessor.class.getName());

	public AudioNotateProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioNotateProcessor accept: " + sequence + ", streamId: " + streamId);

		float compression = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION);
		boolean notateSwitchCompress = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWITCH_COMPRESS);
		boolean notateApplyFormantsSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_APPLY_FORMANTS_SWITCH);
		int noteMaxDuration = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MAX_DURATION);
		int noteMinDuration = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MIN_DURATION);
		int notePeaksMaxDuration = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_PEAKS_MAX_DURATION);
		int notePeaksMinDuration = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_PEAKS_MIN_DURATION);
		int noteSpectralMaxDuration = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SPECTRAL_MAX_DURATION);
		int noteSpectralMinDuration = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SPECTRAL_MIN_DURATION);

		ToneMap integrateToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE, streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneTimeFrame notateTimeFrame = notateToneMap.addTimeFrame(integrateToneMap.getTimeFrame(sequence).clone());

		ToneMap integratePeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE + "_PEAKS", streamId));
		ToneMap integrateSpectralToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE + "_SPECTRAL", streamId));
		ToneMap notatePeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_PEAKS", streamId));
		ToneMap notateSpectralToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_SPECTRAL", streamId));
		ToneTimeFrame notatePeaksTimeFrame = notatePeaksToneMap
				.addTimeFrame(integratePeaksToneMap.getTimeFrame(sequence).clone());
		ToneTimeFrame notateSpectralTimeFrame = notateSpectralToneMap
				.addTimeFrame(integrateSpectralToneMap.getTimeFrame(sequence).clone());

		AudioTuner notateTuner = new AudioTuner();
		AudioTuner peaksTuner = new AudioTuner();
		AudioTuner spTuner = new AudioTuner();

		if (notateApplyFormantsSwitch) {
			notateTuner.applyFormants(notateTimeFrame);
			peaksTuner.applyFormants(notatePeaksTimeFrame);
			spTuner.applyFormants(notateSpectralTimeFrame);
		}

		notateTimeFrame.reset();
		LOG.finer(">>NOTATE TTF: " + notateTimeFrame.getStartTime() + ", " + notateTimeFrame.getMaxAmplitude() + ", "
				+ notateTimeFrame.getMinAmplitude() + ", " + notateTimeFrame.getRmsPower());

		notateTuner.noteScan(notateToneMap, sequence, noteMinDuration, noteMaxDuration);
		console.getVisor().updateToneMapView(notateToneMap, this.cell.getCellType().toString());

		peaksTuner.noteScan(notatePeaksToneMap, sequence, notePeaksMinDuration, notePeaksMaxDuration);
		console.getVisor().updateToneMapView(notatePeaksToneMap, this.cell.getCellType().toString() + "_PEAKS");

		spTuner.noteScan(notateSpectralToneMap, sequence, noteSpectralMinDuration, noteSpectralMaxDuration);
		console.getVisor().updateToneMapView(notateSpectralToneMap, this.cell.getCellType().toString() + "_SPECTRAL");

		int tmIndex = sequence - 12 * (noteMaxDuration / 1000); // TODO !!
		if (tmIndex > 0) {
			notateTimeFrame = notateToneMap.getTimeFrame(tmIndex);
			if (notateTimeFrame != null) {

				if (notateSwitchCompress) {
					clearNotes(notateTimeFrame);
					notateTimeFrame.compress(compression, false);
				}
				console.getVisor().updateToneMapView(notateToneMap, notateTimeFrame,
						this.cell.getCellType().toString());
			}

			notatePeaksTimeFrame = notatePeaksToneMap.getTimeFrame(tmIndex);
			if (notatePeaksTimeFrame != null) {

				if (notateSwitchCompress) {
					clearNotes(notatePeaksTimeFrame);
					notatePeaksTimeFrame.compress(compression, false);
				}
				console.getVisor().updateToneMapView(notatePeaksToneMap, notatePeaksTimeFrame,
						this.cell.getCellType().toString() + "_PEAKS");
			}

			notateSpectralTimeFrame = notateSpectralToneMap.getTimeFrame(tmIndex);
			if (notateSpectralTimeFrame != null) {

				if (notateSwitchCompress) {
					clearNotes(notateSpectralTimeFrame);
					notateSpectralTimeFrame.compress(compression, false);
				}
				console.getVisor().updateToneMapView(notateSpectralToneMap, notateSpectralTimeFrame,
						this.cell.getCellType().toString() + "_SPECTRAL");
			}

			LOG.finer(">>AudioNotateProcessor send: " + tmIndex + ", streamId: " + streamId);
			cell.send(streamId, tmIndex);
		}

		if (isClosing(streamId, sequence)) {
			if (tmIndex < 0) {
				tmIndex = 0;
			}
			LOG.finer(">>AudioNotateProcessor closing: " + sequence + ", streamId: " + streamId);
			for (int i = tmIndex + 1; i <= sequence; i++) {

				notateTimeFrame = notateToneMap.getTimeFrame(i);
				if (notateTimeFrame != null) { // TODO or make fake on here?
					if (notateSwitchCompress) {
						clearNotes(notateTimeFrame);
						notateTimeFrame.compress(compression, false);
					}
					console.getVisor().updateToneMapView(notateToneMap, notateTimeFrame,
							this.cell.getCellType().toString());
				}

				notatePeaksTimeFrame = notatePeaksToneMap.getTimeFrame(i);
				if (notatePeaksTimeFrame != null) { // TODO or make fake on here?
					if (notateSwitchCompress) {
						clearNotes(notatePeaksTimeFrame);
						notatePeaksTimeFrame.compress(compression, false);
					}
					console.getVisor().updateToneMapView(notatePeaksToneMap, notatePeaksTimeFrame,
							this.cell.getCellType().toString() + "_PEAKS");
				}

				notateSpectralTimeFrame = notateSpectralToneMap.getTimeFrame(i);
				if (notateSpectralTimeFrame != null) { // TODO or make fake on here?
					if (notateSwitchCompress) {
						clearNotes(notateSpectralTimeFrame);
						notateSpectralTimeFrame.compress(compression, false);
					}
					console.getVisor().updateToneMapView(notateSpectralToneMap, notateSpectralTimeFrame,
							this.cell.getCellType().toString() + "_SPECTRAL");
				}

				LOG.finer(">>AudioNotateProcessor close send: " + i + ", streamId: " + streamId);
				cell.send(streamId, i);
			}
		}

	}

	private void clearNotes(ToneTimeFrame ttf) {
		ToneMapElement[] elements = ttf.getElements();
		NoteStatus noteStatus = ttf.getNoteStatus();
		PitchSet pitchSet = ttf.getPitchSet();
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			int note = pitchSet.getNote(elements[elementIndex].getPitchIndex());
			NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(note);
			if (noteStatusElement.state == ToneMapConstants.OFF) {
				elements[elementIndex].amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
			}
		}
		ttf.reset();
	}

}
