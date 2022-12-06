package jomu.instrument.cognition;

import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.cognition.cell.Generator;
import jomu.instrument.cognition.cell.NuCell;
import jomu.instrument.cognition.cell.Weaver;
import jomu.instrument.cognition.cell.Cell.CellTypes;

public class Cortex implements Organ, AudioFeatureFrameObserver {

	private NuCell sourceAddCell;
	private NuCell sourceUpdateCell;

	@Override
	public void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame) {
		sourceAddCell.send(audioFeatureFrame.getAudioFeatureProcessor().getStreamId(),
				audioFeatureFrame.getFrameSequence());
	}

	@Override
	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		if (audioFeatureFrame.getConstantQFeatures().isCommitted()) {
			sourceUpdateCell.send(audioFeatureFrame.getAudioFeatureProcessor().getStreamId(),
					audioFeatureFrame.getFrameSequence());
		}
	}

	@Override
	public void initialise() {

		sourceAddCell = Generator.createNuCell(CellTypes.SOURCE);
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		NuCell audioCQCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		NuCell audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		NuCell audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		NuCell audioSinkCell = Generator.createNuCell(CellTypes.AUDIO_SINK);
		Weaver.connect(audioCQCell, audioIntegrateCell);
		Weaver.connect(audioIntegrateCell, audioNotateCell);
		Weaver.connect(audioNotateCell, audioSinkCell);
		NuCell[] pitchCells = new NuCell[1];
		// for (int i = 0; i < 1; i++) {
		// NuCell pitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		// pitchCells[i] = pitchCell;
		// // Weaver.connect(pitchCell, audioSinkCell);
		// Weaver.connect(sourceUpdateCell, pitchCell);
		// }
		Weaver.connect(sourceUpdateCell, audioCQCell);
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
