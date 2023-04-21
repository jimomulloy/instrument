package jomu.instrument.workspace.tonemap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * This is a class that encapsulates parameters associated with ToneMap Pitch
 * base coordinate range and settings and provides various methods for
 * conversion betwen pitch values as MIDI notes and frequencies
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class PitchSet implements Serializable {

	@Override
	public int hashCode() {
		return Objects.hash(highPitchIndex, lowPitchIndex);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PitchSet other = (PitchSet) obj;
		return highPitchIndex == other.highPitchIndex && lowPitchIndex == other.lowPitchIndex;
	}

	public static final double A440 = 440.00;

	public static final int CENTS_HALFSTEP = 50;
	public static final int CENTS_OCTAVE = 1200;
	public static final int MAX_MIDI_NOTE = 127;
	public static final int MIN_MIDI_NOTE = 12;
	public final static int INIT_PITCH_HIGH = 120;
	public final static int INIT_PITCH_INC = 1;
	public final static int INIT_PITCH_LOW = 36;

	public static char[][] NOTE_SYMBOLS = { { 'C', ' ' }, { 'C', '#' }, { 'D', ' ' }, { 'D', '#' }, { 'E', ' ' },
			{ 'F', ' ' }, { 'F', '#' }, { 'G', ' ' }, { 'G', '#' }, { 'A', ' ' }, { 'A', '#' }, { 'B', ' ' },
			{ '?', ' ' } };
	public static double[] PITCH_FREQ;

	static {

		PITCH_FREQ = new double[MAX_MIDI_NOTE - MIN_MIDI_NOTE + 1];

		for (int i = 0; i < PITCH_FREQ.length; i++) {
			PITCH_FREQ[i] = getMidiFreq(MIN_MIDI_NOTE + i);

		}
	}

	private int currentPitchIndex;

	private double freq;

	private int freqRange;

	private double[] freqSet;

	private int highPitchIndex;

	private int lowPitchIndex;

	private double note;

	public PitchSet() {
		this(INIT_PITCH_LOW, INIT_PITCH_HIGH);
	}

	public PitchSet(int lowNote, int highNote) {

		setLowIndex(lowNote - MIN_MIDI_NOTE);
		setHighIndex(highNote - MIN_MIDI_NOTE);
		setIndex(lowNote - MIN_MIDI_NOTE);
	}

	@Override
	public PitchSet clone() {
		return new PitchSet(this.getLowNote(), this.getHighNote());
	}

	public double getFreq(int index) {
		currentPitchIndex = index;
		if (lowPitchIndex + index >= PITCH_FREQ.length) {
			return -1;
		}
		return PITCH_FREQ[lowPitchIndex + index];
	}

	public double[] getFreqSet() {
		freqRange = getRange();
		freqSet = new double[freqRange];
		for (int i = 0; i < freqRange; i++) {
			freqSet[i] = PITCH_FREQ[lowPitchIndex + i];
		}
		return freqSet;
	}

	public int getHighNote() {
		return (highPitchIndex + MIN_MIDI_NOTE);
	}

	public int getIndex(int note) {
		return note - getLowNote();
	}

	public int getLowNote() {
		return (lowPitchIndex + MIN_MIDI_NOTE);
	}

	public int getNote(int index) {
		return (lowPitchIndex + index + MIN_MIDI_NOTE);
	}

	public int getRange() {
		return (highPitchIndex - lowPitchIndex + 1);
	}

	public int pitchToIndex(int pitchNote) {
		setIndex(pitchNote - (lowPitchIndex + MIN_MIDI_NOTE));
		return currentPitchIndex;
	}

	public void setHighIndex(int index) {
		highPitchIndex = index;
	}

	public void setIndex(int index) {
		currentPitchIndex = index;
	}

	public void setLowIndex(int index) {
		lowPitchIndex = index;
	}

	@Override
	public String toString() {
		return "PitchSet [lowPitchIndex=" + lowPitchIndex + ", highPitchIndex=" + highPitchIndex
				+ ", currentPitchIndex=" + currentPitchIndex + ", freq=" + freq + ", note=" + note + ", freqSet="
				+ Arrays.toString(freqSet) + ", freqRange=" + freqRange + "]";
	}

	public static int freqToMidiNote(double freq) {

		if (PITCH_FREQ[0] >= freq) {
			if (getFreqDiff(0, freq) >= -CENTS_HALFSTEP)
				return MIN_MIDI_NOTE;
			else
				return -1;

		}

		else if (PITCH_FREQ[PITCH_FREQ.length - 1] <= freq) {
			if (getFreqDiff(0, freq) <= CENTS_HALFSTEP)
				return MAX_MIDI_NOTE;
			else
				return -1;
		}

		for (int i = 0; i < (PITCH_FREQ.length - 1); i++) {
			if ((PITCH_FREQ[i] <= freq) && (freq <= PITCH_FREQ[i + 1])) {
				double d1 = Math.abs(PITCH_FREQ[i] - freq);
				double d2 = Math.abs(PITCH_FREQ[i + 1] - freq);
				if (d1 >= d2)
					return ((i + 1) + MIN_MIDI_NOTE);
				else
					return (i + MIN_MIDI_NOTE);
			}
		}
		return -1;
	}

	public static int getFreqDiff(int note, double freq) {
		if (note < MIN_MIDI_NOTE)
			note = MIN_MIDI_NOTE;
		if (note > MAX_MIDI_NOTE)
			note = MAX_MIDI_NOTE;
		double freqNote = PITCH_FREQ[note - MIN_MIDI_NOTE];
		return (int) (-CENTS_OCTAVE * Math.log(freqNote / freq) / Math.log(2.0));
	}

	public static double getMidiFreq(int note) {
		return ((A440 / 32) * (Math.pow(2.0, ((note - 9.0) / 12.0))));
	}

	public static NoteSymbol MidiNoteToSymbol(int note) {
		NoteSymbol noteSymbol = new NoteSymbol();
		noteSymbol.noteChar = NOTE_SYMBOLS[note % 12][0];
		noteSymbol.noteSharp = NOTE_SYMBOLS[note % 12][1];
		noteSymbol.noteOctave = (int) Math.floor(note / 12.0) - 1;
		return noteSymbol;
	}

	public static int noteSymbolToMidi(NoteSymbol noteSymbol) {
		return 0;
	}

	public int getIndex(float frequencyInHertz) {
		int note = PitchSet.freqToMidiNote(frequencyInHertz);
		if (note == -1) {
			return -1;
		} else {
			return getIndex(note);
		}
	}

	public int getOctave(int index) {
		return Math.floorDiv(index, 12) + 3;
	}

} // End PitchSet