package jomu.instrument;

/**
 * The Interface InstrumentExceptionHandler.
 *
 * @author Jim O'Mulloy
 */
@FunctionalInterface
public interface InstrumentExceptionHandler {

	/**
	 * Handle exception.
	 *
	 * @param exception
	 *            the exception
	 */
	void handleException(InstrumentException exception);
}