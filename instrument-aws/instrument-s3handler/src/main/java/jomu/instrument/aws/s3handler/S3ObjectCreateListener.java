package jomu.instrument.aws.s3handler;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

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
			String srcBucket = record.getS3().getBucket().getName();
			String srcKey = record.getS3().getObject().getUrlDecodedKey();
			InputObject input = new InputObject();
			input.setName(srcKey);
			LOG.severe(">>S3ObjectCreateListener S3 key: " + srcKey);
			return service.process(input).setRequestId(context.getAwsRequestId());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
