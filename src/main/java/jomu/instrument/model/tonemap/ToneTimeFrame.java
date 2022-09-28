package jomu.instrument.model.tonemap;

public class ToneTimeFrame {
	
	private ToneMapElement[] elements;

	private TimeSet timeSet;

	private PitchSet pitchSet;
	
	public ToneTimeFrame(TimeSet timeSet, PitchSet pitchSet) {
		this.timeSet = timeSet;
		this.pitchSet = pitchSet;
		int pitchRange = pitchSet.getRange();
		elements = new ToneMapElement[pitchRange];	
		for (int i = 0; i < elements.length; i++) {
			elements[i] = new ToneMapElement(i);
		}
	}
	
	public Double getStartTime() {
		return getTimeSet().getStartTime();
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

}
