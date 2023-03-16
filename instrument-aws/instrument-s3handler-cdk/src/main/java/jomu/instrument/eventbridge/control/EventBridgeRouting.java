package jomu.instrument.eventbridge.control;

import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.CloudWatchLogGroup;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Function;

import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class EventBridgeRouting extends Construct {

    public EventBridgeRouting(Construct scope, LogGroup logGroup, Function function) {
        super(scope, "EventBridgeRouter");
        var functionTarget = LambdaFunction.Builder.create(function).build();
        var cloudWatchTarget = CloudWatchLogGroup.Builder.create(logGroup).build();
        var rule = Rule.Builder.create(this, "BucketPutRule")
                .ruleName("s3-put-eventbridge")
                .description("airhacks.live demo")
                .targets(List.of(cloudWatchTarget, functionTarget))
                .eventPattern(EventPattern.builder()
                        .source(List.of("aws.s3"))
                        .build())
                .build();
        CfnOutput.Builder.create(this, "RuleARN").value(rule.getRuleArn()).build();
    }

}
