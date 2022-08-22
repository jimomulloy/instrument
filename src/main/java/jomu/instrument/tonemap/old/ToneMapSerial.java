package jomu.instrument.tonemap.old;

import java.io.Serializable;

/**
 * This class encapsulates ToneMap objects for Saving in serialized form in to a
 * file
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMapSerial implements Serializable {

	ToneMapMatrix matrix;
	TimeSet timeSet;
	PitchSet pitchSet;

	public ToneMapConfig config;

	public ToneMapSerial(ToneMapMatrix matrix, TimeSet timeSet, PitchSet pitchSet, ToneMapConfig config) {
		this.matrix = matrix;
		this.timeSet = timeSet;
		this.pitchSet = pitchSet;
		this.config = config;
	}
}