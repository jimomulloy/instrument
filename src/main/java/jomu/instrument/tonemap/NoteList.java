package jomu.instrument.tonemap;

import java.awt.*;
import java.io.*;
import java.util.*;


/**
  * The class contains a list of Notes within NoteListElement objects as derived 
  * from the ToneMap Process function
  *	
  * @version 1.0 01/01/01
  * @author Jim O'Mulloy
  */
public class NoteList implements Serializable {

	public NoteListElement get(int index) {
					
		this.index = index;
		return (NoteListElement)noteList.get(index);
		
	}
	
	public void add(NoteListElement element) {
	
		noteList.add(element);
		
	}
	
	public int size() {
	
		return noteList.size();
	
	}
	
	private ArrayList noteList = new ArrayList();
	private NoteListElement element;
	private int index;
	
} // End NoteList