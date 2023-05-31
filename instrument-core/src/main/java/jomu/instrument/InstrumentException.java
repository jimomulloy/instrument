package jomu.instrument;

/**
 * The Class InstrumentException.
 * 
 * @author Jim O'Mulloy
 */
public class InstrumentException extends RuntimeException {

	/**
	 * Instantiates a new instrument exception.
	 *
	 * @param errorMessage the error message
	 * @param err          the err
	 */
	public InstrumentException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}

	/**
	 * Instantiates a new instrument exception.
	 *
	 * @param errorMessage the error message
	 */
	public InstrumentException(String errorMessage) {
		super(errorMessage);
	}

}