package jomu.instrument.world.tonemap;

import java.io.Serializable;

/**
 * This class defines the fields of the data elements contained within the
 * ToneMapMatrix object associated with a ToneMap object.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMapElement implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public double amplitude;
	
	public int noteState; // Status of associated MIDI note derived by processing
	// public NoteListElement noteListElement; // Element used to define MIDI note
	// derived from processing
	private int index;
	private int timeIndex;
	private int pitchIndex;

	public ToneMapElement(double amplitude, int index, int timeIndex, int pitchIndex) {

		this.amplitude = amplitude;
		this.index = index;
		this.timeIndex = timeIndex;
		this.pitchIndex = pitchIndex;
	}

	public ToneMapElement(int index) {
		this(0,index, 0, index);
	}

	public ToneMapElement clone() {
		return new ToneMapElement(this.amplitude, this.index, this.timeIndex, this.pitchIndex);
	}
} // End ToneMapElement