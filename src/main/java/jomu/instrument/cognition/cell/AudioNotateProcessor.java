package jomu.instrument.cognition.cell;

import java.util.ArrayList;
import java.util.List;

import jomu.instrument.audio.AudioTuner;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.ToneMap;
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
			System.out.println(">>processNotes TTF time: " + ttfv.getStartTime());
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
			elements[elementIndex].amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
			if (elements[elementIndex].noteState > 0) {
				System.out.println(">>PN STATE: " + elementIndex + ", " + elements[elementIndex].getIndex() + ", "
						+ elements[elementIndex].noteState);
				elements[elementIndex].amplitude = 1.0;
			} else if (noteStatusElement.state > 0) {
				System.out.println(">>NS STATE: " + elementIndex + ", " + elements[elementIndex].getIndex() + ", "
						+ noteStatusElement.state);
				elements[elementIndex].amplitude = 0.5;
			} else if (elements[elementIndex].isPeak) {
				// System.out.println(">>PN PEAK: " + elementIndex);
				// elements[elementIndex].amplitude = 0.5;
			}
		}
	}
}
