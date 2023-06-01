package jomu.instrument.workspace.tonemap;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * This class encapsulates an array of NoteStatusElements used by the TunerModel
 * execute function for processing the ToneMap and extracting a NoteList object
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteStatus implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = Logger.getLogger(NoteStatus.class.getName());

	private int highNote;

	private int index;

	private int lowNote;

	private NoteStatusElement[] noteStatusElements;

	private PitchSet pitchSet;

	public NoteStatus(PitchSet pitchSet) {

		this.pitchSet = pitchSet;

		noteStatusElements = new NoteStatusElement[pitchSet.getRange()];

		lowNote = pitchSet.getLowNote();
		highNote = pitchSet.getHighNote();

		for (int note = lowNote, index = 0; note <= highNote; note++, index++) {
			noteStatusElements[note - lowNote] = new NoteStatusElement(note, index);
		}

	}

	public NoteStatusElement get() {

		return noteStatusElements[index];

	}

	public NoteStatusElement getNoteStatusElement(int note) {
		if (note <= highNote && note >= lowNote) {
			index = note - lowNote;
		} else {
			return noteStatusElements[0];
		}

		if (noteStatusElements.length > index) {
			return noteStatusElements[index];
		} else {
			return noteStatusElements[0];
		}
	}

	public NoteStatusElement next() {
		if (index < size()) {
			index++;
			return noteStatusElements[index];
		} else {
			return null;
		}
	}

	public int size() {

		return noteStatusElements.length;

	}

	@Override
	public NoteStatus clone() {
		NoteStatus copy = new NoteStatus(this.pitchSet);
		for (int note = lowNote, index = 0; index < noteStatusElements.length && note <= highNote; note++, index++) {
			// if (copy.noteStatus.length > (note - lowNote)) {
			// noteStatus[note - lowNote] = copy.getNoteStatusElement(note);
			copy.noteStatusElements[index] = getNoteStatusElement(note).clone();
			// } else {
			// System.err.println(">> NoteStatus clone error: " + index + ", " + lowNote +
			// ", " + highNote + ", "
			// + copy.lowNote + ", " + copy.highNote + ", " + noteStatus.length + ", " +
			// (note - lowNote));
			// }
		}
		return copy;
	}

}