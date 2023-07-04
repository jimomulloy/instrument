package jomu.instrument.ws.config;

import io.quarkus.jsonb.JsonbConfigCustomizer;
import jakarta.json.bind.JsonbConfig;

public class JSONConfig implements JsonbConfigCustomizer {
	public void customize(JsonbConfig config) {
		config.withNullValues(false);
	}
}