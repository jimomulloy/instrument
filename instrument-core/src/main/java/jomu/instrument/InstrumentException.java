package jomu.instrument;

public class InstrumentException extends RuntimeException {

	public InstrumentException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}

	public InstrumentException(String errorMessage) {
		super(errorMessage);
	}

}