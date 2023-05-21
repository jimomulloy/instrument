package jomu.instrument.st;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jomu.instrument.Instrument;
import jomu.instrument.control.Controller;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@QuarkusTest
public class InstrumentTest {

	private static final Logger LOG = Logger.getLogger(InstrumentTest.class.getName());

	@Inject
	Instrument instrument;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Controller controller;

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

	@BeforeEach
	public void init() {
		Instrument.setInstance(instrument);
		instrument.initialise();
		instrument.start();
	}

	@AfterEach
	public void close() {
		instrument.stop();
	}

	@Test
	@Timeout(value = 20000, unit = TimeUnit.MILLISECONDS)
	public void testInstrumentRun() {
		String fileName = "data/3notescale.wav";
		URL fileResource = getClass().getClassLoader().getResource(fileName);
		String userId = "testUser";
		String style = "default";
		boolean instrumentRun = controller.run(userId, fileResource.getPath(), style);

		if (!instrumentRun) {
			fail("Instrument run failed");
		}

		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();

		assertEquals("3notescale", instrumentSession.getInputAudioFileName(),
				"instrumentSession.getInputAudioFileName invalid");
		assertEquals(InstrumentSession.InstrumentSessionState.STOPPED, instrumentSession.getState(),
				"instrumentSession.getState() invalid");
		assertEquals("0", instrumentSession.getStatusCode(), "instrumentSession.getState() invalid");
		assertEquals("OK", instrumentSession.getStatusMessage(), "instrumentSession.getStatusMessage() invalid");
	}

}
