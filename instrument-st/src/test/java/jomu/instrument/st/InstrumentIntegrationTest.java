package jomu.instrument.st;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvFileSource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jomu.instrument.Instrument;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.cognition.cell.ProcessorCommon;
import jomu.instrument.control.Controller;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;

@QuarkusTest
public class InstrumentIntegrationTest {

	private static final Logger LOG = Logger.getLogger(InstrumentIntegrationTest.class.getName());

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

	@Timeout(value = 20000, unit = TimeUnit.MILLISECONDS)
	@ParameterizedTest
	@CsvFileSource(resources = "/data/testparams.csv", numLinesToSkip = 1, delimiter = ':')
	public void testInstrumentRun(ArgumentsAccessor argumentsAccessor) {
		String inputFilePath = argumentsAccessor.getString(0);
		String expectedInputFileName = argumentsAccessor.getString(1);
		String expectedState = (String) argumentsAccessor.get(2);
		String expectedStatusCode = (String) argumentsAccessor.get(3);
		String expectedStatusMessage = (String) argumentsAccessor.get(4);
		int expectedNumberOfCQToneMaps = Integer.valueOf((String) argumentsAccessor.get(5));
		int expectedNumberOfSynthToneMaps = Integer.valueOf((String) argumentsAccessor.get(6));

		URL fileResource = getClass().getClassLoader().getResource(inputFilePath);
		String userId = "testUser";
		String style = "default";
		boolean instrumentRun = controller.run(userId, fileResource.getPath(), style);

		if (!instrumentRun) {
			fail("Instrument run failed");
		}

		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();

		assertEquals(expectedInputFileName, instrumentSession.getInputAudioFileName(),
				"instrumentSession.getInputAudioFileName invalid");
		assertEquals(expectedState, instrumentSession.getState().toString(), "instrumentSession.getState() invalid");
		assertEquals(expectedStatusCode, instrumentSession.getStatusCode(), "instrumentSession.getState() invalid");
		assertEquals(expectedStatusMessage, instrumentSession.getStatusMessage(),
				"instrumentSession.getStatusMessage() invalid");

		ToneMap synthToneMap = workspace.getAtlas().getToneMap(
				ProcessorCommon.buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, instrumentSession.getStreamId()));
		assertEquals(expectedNumberOfSynthToneMaps, synthToneMap.getTimeFramesFrom(0).length,
				"Number of Synth ToneMaps is invalid");
		ToneMap cqToneMap = workspace.getAtlas()
				.getToneMap(ProcessorCommon.buildToneMapKey(CellTypes.AUDIO_CQ, instrumentSession.getStreamId()));
		assertEquals(expectedNumberOfCQToneMaps, cqToneMap.getTimeFramesFrom(0).length,
				"Number of CQ ToneMaps is invalid");

	}

}
