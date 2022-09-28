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

	public AudioFeatureFrameSink getPitchFrameSink() {
		return audioFeatureFrameSink;
	}

	public void initialise() {

		sourceAddCell = Generator.createNuCell(CellTypes.SOURCE);
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		NuCell cqCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		NuCell sinkCell = Generator.createNuCell(CellTypes.SINK);
		Weaver.connect(cqCell, sinkCell);
		NuCell[] pitchCells = new NuCell[1];
		for (int i = 0; i < 1; i++) {
			NuCell pitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
			pitchCells[i] = pitchCell;
			Weaver.connect(pitchCell, sinkCell);
			Weaver.connect(sourceUpdateCell, pitchCell);
		}
		Weaver.connect(sourceUpdateCell, cqCell);
		Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
		hearing.getPitchFrameProcessor().addObserver(this);
		audioFeatureFrameSink = new AudioFeatureFrameSink(sinkCell);
	}

	@Override
	public void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame) {
		sourceAddCell.send(Integer.toString(audioFeatureFrame.getFrameSequence()), audioFeatureFrame);
	}

	@Override
	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		if (audioFeatureFrame.getConstantQFeatures().isCommitted()) {
			sourceUpdateCell.send(Integer.toString(audioFeatureFrame.getFrameSequence()), audioFeatureFrame);
		}
	}

	public void start() {

	}
}
