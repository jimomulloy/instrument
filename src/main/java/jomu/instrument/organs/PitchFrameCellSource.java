package jomu.instrument.organs;

import jomu.instrument.cell.NuCell;

public class PitchFrameCellSource implements PitchFrameObserver {

	NuCell sourceCell;

	public PitchFrameCellSource(NuCell sourceCell) {
		this.sourceCell = sourceCell;
	}

	@Override
	public void pitchFrameAdded(PitchFrame pitchFrame) {
		sourceCell.send(Integer.toString(pitchFrame.getFrameSequence()), pitchFrame);
	}

}
