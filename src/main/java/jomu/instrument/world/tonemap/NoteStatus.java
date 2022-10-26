package jomu.instrument.world.tonemap;

/**
 * This class encapsulates an array of NoteStatusElements used by the TunerModel
 * execute function for processing the ToneMap and extracting a NoteList object
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteStatus {

	private NoteStatusElement[] noteStatus;

	private NoteStatusElement element;

	private int index;

	private int lowNote;

	private int highNote;

	private int note;

	public NoteStatus(PitchSet pitchSet) {

		noteStatus = new NoteStatusElement[pitchSet.getRange()];

		lowNote = pitchSet.getLowNote();
		highNote = pitchSet.getHighNote();

		for (note = lowNote, index = 0; note <= highNote; note++, index++) {
			noteStatus[note - lowNote] = new NoteStatusElement(note, index);
		}

	}

	public NoteStatusElement get() {

		return noteStatus[index];

	}

	public NoteStatusElement getNote(int note) {
		if (note <= highNote && note >= lowNote) {
			index = note - lowNote;
		}

		return noteStatus[index];

	}

	public NoteStatusElement next() {
		if (index < size()) {
			index++;
			return noteStatus[index];
		} else {
			return null;
		}
	}

	public int size() {

		return noteStatus.length;

	}

} // End NoteStatus