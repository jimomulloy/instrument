package jomu.instrument.desktop.monitor;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jomu.instrument.Instrument;
import jomu.instrument.store.InstrumentSession;

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
		InstrumentSession instrumentSession = instrument.getWorkspace().getInstrumentSessionManager()
				.getInstrumentSession(UUID.randomUUID().toString());
		instrumentSession.setUserId("desktop");
		instrumentSession.setDateTime(Instant.now());
		Quarkus.waitForExit();
		return 0;
	}
}