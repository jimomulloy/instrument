package airhacks.lambda.greetings.boundary;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

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
