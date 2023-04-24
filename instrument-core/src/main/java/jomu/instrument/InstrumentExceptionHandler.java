package jomu.instrument;

@FunctionalInterface
public interface InstrumentExceptionHandler {
	void handleException(InstrumentException exception);
}