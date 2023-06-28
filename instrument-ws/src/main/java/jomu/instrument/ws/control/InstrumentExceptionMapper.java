package jomu.instrument.ws.control;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jomu.instrument.InstrumentException;

@Provider
public class InstrumentExceptionMapper implements ExceptionMapper<InstrumentException> {
	@Metric(name = "InstrumentExceptionMapper", description = "Number of times the InstrumentExceptionMapper is invoked")
	Counter exceptionMapperCounter;

	@Override
	public Response toResponse(InstrumentException ex) {
		exceptionMapperCounter.inc();
		return Response.status(404).entity(ex.getMessage()).build();
	}

}
