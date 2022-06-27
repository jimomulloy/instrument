package jomu.instrument.organs;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.cell.Generator;
import jomu.instrument.cell.NuCell;
import jomu.instrument.cell.Weaver;

public class Cortex implements PitchFrameObserver {

	private PitchFrameSink pitchFrameSink;
	private NuCell sourceAddCell;
	private NuCell sourceUpdateCell;

	public void initialise() {

		sourceAddCell = Generator.createNuCell(CellTypes.SOURCE);
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		NuCell pitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		NuCell cqCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		NuCell sinkCell = Generator.createNuCell(CellTypes.SINK);
		Weaver.connect(cqCell, sinkCell);
		Weaver.connect(pitchCell, sinkCell);
		Weaver.connect(sourceUpdateCell, cqCell);
		Weaver.connect(sourceUpdateCell, pitchCell);
		Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
		hearing.getPitchFrameProcessor().addObserver(this);
		pitchFrameSink = new PitchFrameSink(sinkCell);
	}

	public void start() {

	}

	@Override
	public void pitchFrameAdded(PitchFrame pitchFrame) {
		sourceAddCell.send(Integer.toString(pitchFrame.getFrameSequence()), pitchFrame);
	}

	@Override
	public void pitchFrameChanged(PitchFrame pitchFrame) {
		if (pitchFrame.getConstantQFeatures().isCommitted()) {
			sourceUpdateCell.send(Integer.toString(pitchFrame.getFrameSequence()), pitchFrame);
		}
	}

	public PitchFrameSink getPitchFrameSink() {
		return pitchFrameSink;
	}
}
