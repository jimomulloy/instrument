package jomu.instrument.aws.s3handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;

//@QuarkusTest
public class GreetingsResourceIT {

	@Inject
	@RestClient
	GreetingsResource resource;

	@Inject
	@ConfigProperty(name = "base_uri/mp-rest/url")
	String baseURI;

	// @Test
	public void hello() {
		var message = this.resource.content();
		assertNotNull(message);
		System.out.println(message);
	}

}
