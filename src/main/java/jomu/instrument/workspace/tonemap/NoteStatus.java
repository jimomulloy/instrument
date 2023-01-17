package jomu.instrument.workspace.tonemap;

/**
 * This class encapsulates an array of NoteStatusElements used by the TunerModel
 * execute function for processing the ToneMap and extracting a NoteList object
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteStatus {

	private int highNote;

	private int index;

	private int lowNote;

	private int note;

	private NoteStatusElement[] noteStatus;

	private PitchSet pitchSet;

	public NoteStatus(PitchSet pitchSet) {

		this.pitchSet = pitchSet;

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

		if (noteStatus.length > index) {
			return noteStatus[index];
		} else {
			// TODO !!
			System.err.println(">>!! NoteStatus getNote error: " + lowNote + ", " + highNote + ", " + note + ", "
					+ noteStatus.length + ", " + (note - lowNote));
			return noteStatus[index];
		}
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

	@Override
	public NoteStatus clone() {
		NoteStatus copy = new NoteStatus(this.pitchSet);
		for (note = lowNote, index = 0; index < noteStatus.length && note <= highNote; note++, index++) {
			if (noteStatus.length > (note - lowNote)) {
				noteStatus[note - lowNote] = copy.getNote(note);
			} else {
				System.err.println(">>!! NoteStatus clone error: " + index + ", " + lowNote + ", " + highNote + ", "
						+ copy.lowNote + ", " + copy.highNote + ", " + note + ", " + noteStatus.length + ", "
						+ (note - lowNote));
			}
		}
		return copy;
	}

}