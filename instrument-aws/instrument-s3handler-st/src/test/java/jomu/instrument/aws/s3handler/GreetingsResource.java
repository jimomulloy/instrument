package jomu.instrument.aws.s3handler;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "base_uri")
public interface GreetingsResource {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("hello")
	String content();

}
