package jomu.instrument.organs;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.cell.Generator;
import jomu.instrument.cell.NuCell;
import jomu.instrument.cell.Weaver;

public class Cortex {

	private PitchFrameCellSource pitchFrameCellSource;
	private PitchFrameSink pitchFrameSink;

	public void initialise() {

		NuCell sourceCell = Generator.createNuCell(CellTypes.SOURCE);
		NuCell pitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		NuCell cqCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		NuCell sinkCell = Generator.createNuCell(CellTypes.SINK);
		Weaver.connect(cqCell, sinkCell);
		// Weaver.connect(pitchCell, cqCell);
		Weaver.connect(sourceCell, cqCell);
		Weaver.connect(sourceCell, pitchCell);
		Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
		pitchFrameCellSource = new PitchFrameCellSource(sourceCell);
		hearing.getPitchFrameProcessor().addObserver(pitchFrameCellSource);
		pitchFrameSink = new PitchFrameSink(sinkCell);
	}

	public void start() {

	}

	public PitchFrameCellSource getAudioFeatureSource() {
		return pitchFrameCellSource;
	}

	public PitchFrameSink getPitchFrameSink() {
		return pitchFrameSink;
	}
}
