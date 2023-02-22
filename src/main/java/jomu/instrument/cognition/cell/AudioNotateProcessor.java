package jomu.instrument.cognition.cell;

import java.util.ArrayList;
import java.util.List;

import jomu.instrument.audio.AudioTuner;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioNotateProcessor extends ProcessorCommon {

	public AudioNotateProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioNotateProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap tunerPeaksToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_TUNER_PEAKS, streamId));
		ToneMap notateToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		ToneTimeFrame timeFrame = tunerPeaksToneMap.getTimeFrame(sequence).clone();
		notateToneMap.addTimeFrame(timeFrame);

		AudioTuner tuner = new AudioTuner();

		tuner.noteScan(notateToneMap, sequence);

		List<ToneTimeFrame> timeFrames = new ArrayList<>();
		ToneTimeFrame ttf = timeFrame;
		double fromTime = (ttf.getStartTime() - 3.0) >= 0 ? ttf.getStartTime() - 3.0 : 0;

		System.out.println(">>TTF time: " + ttf.getStartTime());
		while (ttf != null && ttf.getStartTime() >= fromTime) {
			timeFrames.add(ttf);
			ttf = notateToneMap.getPreviousTimeFrame(ttf.getStartTime());
		}

		for (ToneTimeFrame ttfv : timeFrames) {
			processNotes(ttfv);
			console.getVisor().updateToneMapView(notateToneMap, ttfv, this.cell.getCellType().toString());
		}
		cell.send(streamId, sequence);
	}

	private void processNotes(ToneTimeFrame ttfv) {
		ToneMapElement[] elements = ttfv.getElements();
		NoteStatus noteStatus = ttfv.getNoteStatus();
		PitchSet pitchSet = ttfv.getPitchSet();
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			int note = pitchSet.getNote(elements[elementIndex].getPitchIndex());
			NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(note);
			if (noteStatusElement.state == ToneMapConstants.OFF) {
				elements[elementIndex].amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
			}
		}
	}
}
