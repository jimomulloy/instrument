package jomu.instrument;

/**
 * The Interface Organ.
 *
 * @author Jim O'Mulloy
 */
public interface Organ {

	/**
	 * Initialise.
	 *
	 * @throws InstrumentException
	 *             the instrument exception
	 */
	void initialise() throws InstrumentException;

	/**
	 * Process exception.
	 *
	 * @param exception
	 *            the exception
	 * @throws InstrumentException
	 *             the instrument exception
	 */
	void processException(InstrumentException exception) throws InstrumentException;

	/**
	 * Start.
	 *
	 * @throws InstrumentException
	 *             the instrument exception
	 */
	void start() throws InstrumentException;

	/**
	 * Stop.
	 *
	 * @throws InstrumentException
	 *             the instrument exception
	 */
	void stop() throws InstrumentException;

}