package jomu.instrument.cognition.cell;

@FunctionalInterface
public interface ProcessorExceptionHandler<E extends Exception> {
	void handleException(E exception);
}