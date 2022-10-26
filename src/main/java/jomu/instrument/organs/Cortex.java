package jomu.instrument.organs;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.cell.Generator;
import jomu.instrument.cell.NuCell;
import jomu.instrument.cell.Weaver;

public class Cortex implements AudioFeatureFrameObserver {

	private AudioFeatureFrameSink audioFeatureFrameSink;
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

	public AudioFeatureFrameSink getPitchFrameSink() {
		return audioFeatureFrameSink;
	}

	public void initialise() {

		sourceAddCell = Generator.createNuCell(CellTypes.SOURCE);
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		NuCell audioCQCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		NuCell audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		NuCell audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		NuCell sinkCell = Generator.createNuCell(CellTypes.SINK);
		Weaver.connect(audioCQCell, audioIntegrateCell);
		Weaver.connect(audioIntegrateCell, audioNotateCell);
		Weaver.connect(audioNotateCell, sinkCell);
		NuCell[] pitchCells = new NuCell[1];
		for (int i = 0; i < 1; i++) {
			NuCell pitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
			pitchCells[i] = pitchCell;
			Weaver.connect(pitchCell, sinkCell);
			Weaver.connect(sourceUpdateCell, pitchCell);
		}
		Weaver.connect(sourceUpdateCell, audioCQCell);
		Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
		hearing.getAudioFeatureProcessor().addObserver(this);
		audioFeatureFrameSink = new AudioFeatureFrameSink(sinkCell);
	}

	public void start() {

	}
}
