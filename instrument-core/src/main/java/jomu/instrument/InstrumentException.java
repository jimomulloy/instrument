package jomu.instrument;

/**
 * The Class InstrumentException.
 *
 * @author Jim O'Mulloy
 */
public class InstrumentException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -2011024007874461217L;

	/**
	 * Instantiates a new instrument exception.
	 *
	 * @param errorMessage
	 *            the error message
	 */
	public InstrumentException(final String errorMessage) {
		super(errorMessage);
	}

	/**
	 * Instantiates a new instrument exception.
	 *
	 * @param errorMessage
	 *            the error message
	 * @param err
	 *            the err
	 */
	public InstrumentException(final String errorMessage, final Throwable err) {
		super(errorMessage, err);
	}

}