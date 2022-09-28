package jomu.instrument.model.tonemap;

import java.io.Serializable;

/**
 * This class defines the fields of the data elements contained within the
 * ToneMapMatrix object associated with a ToneMap object.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMapElement implements Serializable {

	public double preAmplitude; // audio data Amplitude (loudness) pre-processing

	public double preFTPower; // audio data Power pre-processing
	public double postAmplitude; // audio data Amplitude (ludness) post-processing
	public double postFTPower; // audio data Power post-processing
	public int noteState; // Status of associated MIDI note derived by processing
	//public NoteListElement noteListElement; // Element used to define MIDI note derived from processing
	private int index;
	private int timeIndex;
	private int pitchIndex;

	public ToneMapElement(double amplitude, double FTPower, int index, int timeIndex, int pitchIndex) {

		this.preAmplitude = amplitude;
		this.preFTPower = FTPower;
		this.postAmplitude = amplitude;
		this.postFTPower = FTPower;
		this.index = index;
		this.timeIndex = timeIndex;
		this.pitchIndex = pitchIndex;
	}
	
	public ToneMapElement(int index) {
		this(0, 0, index, 0, index);
	}

	public ToneMapElement clone() {
		return new ToneMapElement(this.preAmplitude, this.preFTPower, this.index, this.timeIndex, this.pitchIndex);
	}
} // End ToneMapElement