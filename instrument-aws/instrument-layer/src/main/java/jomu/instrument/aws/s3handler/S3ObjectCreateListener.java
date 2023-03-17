package jomu.instrument.aws.s3handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

public class S3ObjectCreateListener implements RequestHandler<S3Event, OutputObject> {

	@Override
	public OutputObject handleRequest(S3Event event, Context context) {
		var records = event.getRecords();
		for (var record : records) {
			System.out.println(String.format("%s-%s-%s", record.getEventName(), record.getEventSource(),
					record.getS3().getBucket().getArn()));
		}
		return null;
	}

}
