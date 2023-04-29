package jomu.instrument.st;

import java.net.URL;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
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
		LOG.severe(">>ProcessingService Start up");
		Instrument.setInstance(instrument);
		instrument.initialise();
		instrument.start();
		LOG.severe(">>ProcessingService Started");
	}
//
//	@AfterEach
//	public void close() {
//		instrument.stop();
//	}

	@Test
	public void testInstrumentRun() {
		String fileName = "data/3notescale.wav";
		URL fileResource = getClass().getClassLoader().getResource(fileName);
		String userId = "testUser";
		String style = "default";
		String propsKey = "instrument.properties";
//		InputStream stream = storage.getObjectStorage().read(propsKey);
//		if (stream != null) {
//			Properties props = new Properties();
//			try {
//				props.load(stream);
//				controller.getParameterManager().overrideParameters(props);
//			} catch (IOException e) {
//				// LOG.log(Level.SEVERE, ">>ProcessingService error overiding parameters", e);
//			}
//		}

		boolean instrumentRun = controller.run(userId, fileResource.getPath(), style);

		if (!instrumentRun) {
			//
		}

		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();

		LOG.severe(">>Instrument Test session: " + instrumentSession);
		// assertThat(password).containsPattern("[0-9A-F-]+");
	}

}
