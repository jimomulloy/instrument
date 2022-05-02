package jomu.instrument.organs;

import jomu.instrument.cell.NuCell;

public class AudioFeatureSource implements AudioFeatureObserver {

	NuCell sourceCell;

	public AudioFeatureSource(NuCell sourceCell) {
		this.sourceCell = sourceCell;
	}

	@Override
	public void pitchFrameAdded(PitchFrame pitchFrame) {
		sourceCell.send(Integer.toString(pitchFrame.getFrameSequence()), pitchFrame);
	}

}
