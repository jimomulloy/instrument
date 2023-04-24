package jomu.instrument.cognition.cell;

import jomu.instrument.InstrumentException;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends InstrumentException> {
	void accept(T t) throws E;
}