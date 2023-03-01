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
		LOG.info(">>AudioNotateProcessor accept: " + sequence + ", streamId: " + streamId);

		float compression = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_COMPRESSION);
		boolean notateSwitchCompress = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTATE_SWITCH_COMPRESS);

		ToneMap tunerPeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_TUNER_PEAKS, streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneTimeFrame timeFrame = tunerPeaksToneMap.getTimeFrame(sequence).clone();
		notateToneMap.addTimeFrame(timeFrame);

		AudioTuner tuner = new AudioTuner();

		tuner.noteScan(notateToneMap, sequence);

		ToneTimeFrame ttf = timeFrame;
		double fromTime = (ttf.getStartTime() - 2.0) >= 0 ? ttf.getStartTime() - 2.0 : 0;
		while (ttf != null && ttf.getStartTime() >= fromTime) {
			if (notateSwitchCompress) {
				clearNotes(ttf);
				ttf.compress(compression);
			}
			console.getVisor().updateToneMapView(notateToneMap, ttf, this.cell.getCellType().toString());
			ttf = notateToneMap.getPreviousTimeFrame(ttf.getStartTime());
		}

		cell.send(streamId, sequence);
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
				// elements[elementIndex].amplitude = 1.0;
			}
		}
		ttf.reset();
	}
}
