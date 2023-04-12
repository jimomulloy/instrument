package jomu.instrument.workspace.tonemap;

import java.util.LinkedList;
import java.util.logging.Logger;

public class ChordTracker {

	private static final Logger LOG = Logger.getLogger(ChordTracker.class.getName());

	LinkedList<ChordListElement> chords = new LinkedList<>();

	private ToneMap toneMap;

	public ChordTracker(ToneMap toneMap) {
		this.toneMap = toneMap;
	}

	public void trackChord(ChordListElement chordListElement) {
		chords.add(chordListElement);
	}

}
