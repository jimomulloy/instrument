package jomu.instrument.aws.s3handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

//@QuarkusTest
class ProcessingServiceTest {

	@Inject
	ProcessingService processingService;

	@Test
	void testProcess() {
		try {
			InputObject input = new InputObject();
			input.setName("test");
			String output = processingService.process(input).setRequestId("requestId").getResult();
			assertEquals("test", output, "ProcessingService process output error");
		} catch (Exception e) {
			String result = "error";
			OutputObject out = new OutputObject();
			out.setResult(result);
			fail("ProcessingService process output exception", e);
		}
	}

}
