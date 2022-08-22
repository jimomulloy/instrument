package jomu.instrument.tonemap;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The class contains a list of Notes within NoteListElement objects as derived
 * from the ToneMap Process function
 * 
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteList implements Serializable {

	private ArrayList noteList = new ArrayList();

	private NoteListElement element;

	private int index;

	public void add(NoteListElement element) {

		noteList.add(element);

	}

	public NoteListElement get(int index) {

		this.index = index;
		return (NoteListElement) noteList.get(index);

	}

	public int size() {

		return noteList.size();

	}

} // End NoteList