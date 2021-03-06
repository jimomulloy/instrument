package jomu.instrument.tonemap;

import java.io.Serializable;

/**
 * This class defines the fields of the elements contained in the NoteList
 * object which represent Notes derived from the ToneMap Processing function
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteListElement implements Serializable {

	public NoteListElement(int note, int pitchIndex, double startTime, double endTime, int startTimeIndex,
			int endTimeIndex, double avgFTPower, double maxFTPower, double minFTPower, double avgAmp, double maxAmp,
			double minAmp, double percentMin) {
		this.note = note;
		this.pitchIndex = pitchIndex;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startTimeIndex = startTimeIndex;
		this.endTimeIndex = endTimeIndex;
		this.avgFTPower = avgFTPower;
		this.maxFTPower = maxFTPower;
		this.minFTPower = minFTPower;
		this.avgAmp = avgAmp;
		this.maxAmp = maxAmp;
		this.minAmp = minAmp;
		this.percentMin = percentMin;
	}

	public int note; // Midi note pitch
	public int pitchIndex; // Pitch index in ToneMapMatrix
	public double startTime; // Note Start Time
	public double endTime; // Note End Time
	public int startTimeIndex; // Start Time Index in ToneMapMatrix
	public int endTimeIndex; // End Time Index in ToneMapMatrix
	public double avgFTPower; // Average audio data Power
	public double maxFTPower; // Maximum audio data Power
	public double minFTPower; // Minimum audio data Power
	public double avgAmp; // Average amplitude (loudness)
	public double maxAmp; // Maximum amplitude
	public double minAmp; // Minumum amplitude
	public double percentMin; // Percentage # entries below minimum
	public boolean underTone; // Flag note as undertone
	public boolean overTone; // Flag note as overtone

} // End NoteListElement