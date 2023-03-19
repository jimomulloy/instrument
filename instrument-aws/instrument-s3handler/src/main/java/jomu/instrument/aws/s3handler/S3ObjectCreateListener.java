package jomu.instrument.aws.s3handler;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

@Named("test")
public class S3ObjectCreateListener implements RequestHandler<S3Event, OutputObject> {

	private static final Logger LOG = Logger.getLogger(S3ObjectCreateListener.class.getName());

	@Inject
	ProcessingService service;

	@Override
	public OutputObject handleRequest(S3Event event, Context context) {
		var records = event.getRecords();
		for (var record : records) {
			System.out.println(String.format("%s-%s-%s", record.getEventName(), record.getEventSource(),
					record.getS3().getBucket().getArn()));
		}
		System.out.println(">>S3ObjectCreateListener handleRequest LOG1: " + (service == null));
		context.getLogger().log(">>S3ObjectCreateListener handleRequest LOG2: " + (service == null));
		LOG.severe(">>S3ObjectCreateListener handleRequest LOG3: " + (service == null));
		InputObject input = new InputObject();
		return service.process(input).setRequestId(context.getAwsRequestId());
	}

}
