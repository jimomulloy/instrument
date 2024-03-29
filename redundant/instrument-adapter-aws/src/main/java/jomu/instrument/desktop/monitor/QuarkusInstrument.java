package jomu.instrument.desktop.monitor;

import jakarta.inject.Inject;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jomu.instrument.Instrument;

@QuarkusMain
public class QuarkusInstrument implements QuarkusApplication {

	@Inject
	Instrument instrument;

	public static void main(String[] args) {
		Quarkus.run(QuarkusInstrument.class, args);
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