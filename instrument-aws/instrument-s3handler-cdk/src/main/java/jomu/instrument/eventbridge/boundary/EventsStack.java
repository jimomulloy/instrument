package jomu.instrument.eventbridge.boundary;

import jomu.instrument.cloudwatch.control.EventBridgeTargetLogGroup;
import jomu.instrument.eventbridge.control.EventBridgeRouting;
import jomu.instrument.lambda.control.AWSLambda;
import jomu.instrument.lambda.control.QuarkusLambda;
import jomu.instrument.s3.control.S3Bucket;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.s3.NotificationKeyFilter;
import software.amazon.awscdk.services.s3.notifications.LambdaDestination;
import software.constructs.Construct;

public class EventsStack extends Stack {

	public EventsStack(Construct scope, String id, boolean snapStart) {
		super(scope, id);

		var eventBridgeListener = AWSLambda.createFunction(this, "instrument_EventBridgeListener",
				"jomu.instrument.aws.s3handler.EventBridgeListener::handleRequest");

		var s3Listener = new QuarkusLambda(this, "instrument_S3ObjectCreateListener", snapStart);

		var cloudWatchLogGroup = new EventBridgeTargetLogGroup(this);
		var logGroup = cloudWatchLogGroup.getLogGroup();
		var s3Bucket = new S3Bucket(this);
		var bucket = s3Bucket.getBucket();
		bucket.grantReadWrite(s3Listener.getFunction());
		var destination = new LambdaDestination(s3Listener.getFunction());
		NotificationKeyFilter nkfwav = new NotificationKeyFilter.Builder().prefix("private/").suffix("wav").build();
		bucket.addObjectCreatedNotification(destination, new NotificationKeyFilter[] { nkfwav });
		NotificationKeyFilter nkfmp3 = new NotificationKeyFilter.Builder().prefix("private/").suffix("mp3").build();
		bucket.addObjectCreatedNotification(destination, new NotificationKeyFilter[] { nkfmp3 });
		NotificationKeyFilter nkfogg = new NotificationKeyFilter.Builder().prefix("private/").suffix("ogg").build();
		bucket.addObjectCreatedNotification(destination, new NotificationKeyFilter[] { nkfogg });
		NotificationKeyFilter nkfwavc = new NotificationKeyFilter.Builder().prefix("private/").suffix("WAV").build();
		bucket.addObjectCreatedNotification(destination, new NotificationKeyFilter[] { nkfwavc });
		NotificationKeyFilter nkfmp3c = new NotificationKeyFilter.Builder().prefix("private/").suffix("MP3").build();
		bucket.addObjectCreatedNotification(destination, new NotificationKeyFilter[] { nkfmp3c });
		NotificationKeyFilter nkfoggc = new NotificationKeyFilter.Builder().prefix("private/").suffix("OGG").build();
		bucket.addObjectCreatedNotification(destination, new NotificationKeyFilter[] { nkfoggc });
		var eventBridgeRouting = new EventBridgeRouting(this, logGroup, eventBridgeListener);
		CfnOutput.Builder.create(this, "BucketOutput").value(bucket.getBucketArn()).build();
	}
}
