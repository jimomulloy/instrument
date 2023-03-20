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
		// var s3Listener = AWSLambda.createFunction(this,
		// "instrument_S3ObjectCreateListener",
		// "jomu.instrument.aws.s3handler.S3ObjectCreateListener::handleRequest");

		var s3Listener = new QuarkusLambda(this, "instrument_S3ObjectCreateListener", snapStart);

		var cloudWatchLogGroup = new EventBridgeTargetLogGroup(this);
		var logGroup = cloudWatchLogGroup.getLogGroup();
		var s3Bucket = new S3Bucket(this);
		var bucket = s3Bucket.getBucket();
		bucket.grantReadWrite(s3Listener.getFunction());
		var destination = new LambdaDestination(s3Listener.getFunction());
		NotificationKeyFilter nkf = new NotificationKeyFilter.Builder().prefix("input/").suffix(".wav").build();
		bucket.addObjectCreatedNotification(destination, new NotificationKeyFilter[] { nkf });
		var eventBridgeRouting = new EventBridgeRouting(this, logGroup, eventBridgeListener);
		CfnOutput.Builder.create(this, "BucketOutput").value(bucket.getBucketArn()).build();
	}
}
