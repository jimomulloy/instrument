package jomu.instrument.aws.s3handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.json.bind.JsonbBuilder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.vertx.core.json.JsonObject;

public class EventBridgeListener implements RequestStreamHandler {

	static String message = System.getenv("message");

	public EventBridgeListener() {
		System.out.println("initialized with configuration: " + message);
	}

	@Override
	public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
		System.out.println("event received");
		var json = JsonbBuilder.create().fromJson(new InputStreamReader(input), JsonObject.class);
		System.out.println(json);
		System.out.println("---");

	}

}
