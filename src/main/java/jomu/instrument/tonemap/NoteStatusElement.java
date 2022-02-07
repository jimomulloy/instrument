package jomu.instrument.tonemap;

import java.io.*;

/**
  * This class defines the fields of the data elements contained within the 
  * NoteSequence object which register state of Note data used for creating MIDI Messages 
  * as written to a MIDI Sequence 
  *
  * @version 1.0 01/01/01
  * @author Jim O'Mulloy
  */
public class NoteStatusElement {

	public NoteStatusElement(int note ) {
	
		this.note = note;
	}
		
	public int note;			//Midi note pitch
	public int state;			//Note state code 
	public boolean highFlag;	//Note High state flag  
	public double onTime;		//Note ON time
	public double offTime;		//Note OFF time
	public int onIndex;			//Note ON index into ToneMapMatrix 
	public int offIndex;		//Note OFF index into ToneMapMatrix
	
} // End NoteStatusElement