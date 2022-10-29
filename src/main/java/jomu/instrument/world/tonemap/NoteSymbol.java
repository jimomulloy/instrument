package jomu.instrument.world.tonemap;

/**
 * This is a class that encapsulates a set of Note Musical Symbols
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class NoteSymbol {

	public char noteChar;
	public int noteOctave;
	public char noteSharp;

	@Override
	public String toString() {
		String s = null;
		if (noteSharp == ' ')
			s = String.valueOf(noteChar) + String.valueOf(noteOctave);
		else
			s = String.valueOf(noteChar) + String.valueOf(noteSharp)
					+ String.valueOf(noteOctave);
		return s;
	}

} // End NoteSymbol