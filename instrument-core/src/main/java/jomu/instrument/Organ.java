package jomu.instrument;

public interface Organ {

	void initialise() throws InstrumentException;

	void start() throws InstrumentException;

	void stop() throws InstrumentException;

	void processException(InstrumentException exception) throws InstrumentException;

}