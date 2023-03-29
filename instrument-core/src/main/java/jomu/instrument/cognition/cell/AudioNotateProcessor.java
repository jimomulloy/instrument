package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

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
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.severe(">>AudioNotateProcessor accept: " + sequence + ", streamId: " + streamId);

		float compression = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION);
		boolean notateSwitchCompress = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWITCH_COMPRESS);
		int noteMaxDuration = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MAX_DURATION);

		ToneMap tunerPeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_TUNER_PEAKS, streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneTimeFrame timeFrame = tunerPeaksToneMap.getTimeFrame(sequence).clone();
		notateToneMap.addTimeFrame(timeFrame);

		AudioTuner tuner = new AudioTuner();

		tuner.noteScan(notateToneMap, sequence);
		console.getVisor().updateToneMapView(notateToneMap, this.cell.getCellType().toString());

		int tmIndex = sequence - 10 * (noteMaxDuration / 1000); // TODO !!
		if (tmIndex > 0) {
			timeFrame = notateToneMap.getTimeFrame(tmIndex);
			if (timeFrame != null) {

				if (notateSwitchCompress) {
					clearNotes(timeFrame);
					timeFrame.compress(compression, false);
				}
				console.getVisor().updateToneMapView(notateToneMap, timeFrame, this.cell.getCellType().toString());
			}
			LOG.severe(">>AudioNotateProcessor send: " + tmIndex + ", streamId: " + streamId);
			cell.send(streamId, tmIndex);
		}

		if (isClosing(streamId, sequence)) {
			if (tmIndex < 0) {
				tmIndex = 0;
			}
			LOG.severe(">>AudioNotateProcessor closing: " + sequence + ", streamId: " + streamId);
			for (int i = tmIndex + 1; i <= sequence; i++) {
				timeFrame = notateToneMap.getTimeFrame(i);
				if (timeFrame != null) { // TODO or make fake on here?
					if (notateSwitchCompress) {
						clearNotes(timeFrame);
						timeFrame.compress(compression, false);
					}
					console.getVisor().updateToneMapView(notateToneMap, timeFrame, this.cell.getCellType().toString());
				}
				LOG.severe(">>AudioNotateProcessor close send: " + i + ", streamId: " + streamId);
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
