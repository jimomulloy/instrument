package jomu.instrument.cloudwatch.control;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public class EventBridgeTargetLogGroup extends Construct {

	LogGroup logGroup;

	public EventBridgeTargetLogGroup(Construct scope) {
		super(scope, "LogGroup");
		this.logGroup = LogGroup.Builder.create(this, "LogGroup").logGroupName("/jomu/instrument/s3-put-eventbridge")
				.retention(RetentionDays.ONE_DAY).build();
		CfnOutput.Builder.create(this, "LogGroupARNOutput").value(this.logGroup.getLogGroupArn()).build();
	}

	public LogGroup getLogGroup() {
		return this.logGroup;
	}

}
