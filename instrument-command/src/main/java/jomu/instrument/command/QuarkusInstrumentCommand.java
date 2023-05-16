package jomu.instrument.command;

import jakarta.inject.Inject;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jomu.instrument.Instrument;

@QuarkusMain
public class QuarkusInstrumentCommand implements QuarkusApplication {

	@Inject
	Instrument instrument;

	public static void main(String[] args) {
		Quarkus.run(QuarkusInstrumentCommand.class, args);
	}

	@Override
	public int run(String... args) throws Exception {
		Instrument.setInstance(instrument);
		instrument.initialise();
		instrument.start();
		Quarkus.waitForExit();
		return 0;
	}
}