package jomu.instrument.ws.config;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;

import jakarta.ws.rs.core.Application;

@OpenAPIDefinition(info = @Info(title = "The Instrument Web Service - Powered by Quarkus", version = "0.1", description = "This is an  API for the Instrument Audio processing facility. Upload an Audio file and retrieve the prosessed result as a zip file containing midi tracks", license = @License(name = "Apache 2.0", url = "http://localhost:9080"), contact = @Contact(url = "https://github.com/jimomulloy/instrument/wiki", name = "Jim O'Mulloy", email = "jimomulloy@gmail.com")))

//@LoginConfig(authMethod = "BASIC",realmName = "TCK-MP-JWT")
public class AppConfig extends Application {

}