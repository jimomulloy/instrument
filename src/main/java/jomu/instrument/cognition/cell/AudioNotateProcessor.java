package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.audio.AudioTuner;
import jomu.instrument.cognition.cell.Cell.CellTypes;
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

		processNotes(notateToneMap.getTimeFrame().getElements());

		console.getVisor().updateToneMapView(notateToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}

	private void processNotes(ToneMapElement[] elements) {
		System.out.println(">>IN PN:");
		for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
			elements[elementIndex].amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
			if (elements[elementIndex].noteState > 0) {
				System.out.println(">>PN: " + elementIndex);
				elements[elementIndex].amplitude = 1.0;
			}
		}
	}
}
