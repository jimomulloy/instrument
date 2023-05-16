package jomu.instrument.aws.s3handler;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

@Named("test")
public class S3ObjectCreateListener implements RequestHandler<S3Event, OutputObject> {

	private static final Logger LOG = Logger.getLogger(S3ObjectCreateListener.class.getName());

	@Inject
	ProcessingService service;

	@Override
	public OutputObject handleRequest(S3Event event, Context context) {
		LOG.severe(">>S3ObjectCreateListener event: " + event);
		LOG.severe(">>S3ObjectCreateListener handleRequest: " + (service == null));
		try {
			S3EventNotificationRecord record = event.getRecords().get(0);
			String srcKey = record.getS3().getObject().getUrlDecodedKey();
			InputObject input = new InputObject();
			input.setName(srcKey);
			LOG.severe(">>S3ObjectCreateListener S3 key: " + srcKey);
			return service.process(input).setRequestId(context.getAwsRequestId());
		} catch (Exception e) {
			LOG.log(Level.SEVERE, ">>S3ObjectCreateListener error", e);
			String result = "error";
			OutputObject out = new OutputObject();
			out.setResult(result);
			return out;
		}

	}

}
