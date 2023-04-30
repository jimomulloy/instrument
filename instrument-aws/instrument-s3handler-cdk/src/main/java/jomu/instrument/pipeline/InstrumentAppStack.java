package jomu.instrument.pipeline;

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ManualApprovalStep;
import software.amazon.awscdk.pipelines.ShellStep;
import software.amazon.awscdk.pipelines.StageDeployment;

public class InstrumentAppStack extends Stack {
    public InstrumentAppStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InstrumentAppStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        CodePipeline pipeline = CodePipeline.Builder.create(this, "pipeline")
             .pipelineName("InstrumentAppPipeline")
             .synth(ShellStep.Builder.create("Synth")
                .input(CodePipelineSource.gitHub("jimomulloy/instrument", "main"))
                .commands(Arrays.asList("mvn clean install", "cd ./instrument-aws/instrument-s3handler-cdk", "npm install -g aws-cdk", "cdk synth"))
                .primaryOutputDirectory("./instrument-aws/instrument-s3handler-cdk/cdk.out")
                .build())
             .build();
        
        @NotNull
		StageDeployment testingStage = pipeline.addStage(new InstrumentAppStage(this, "test", StageProps.builder()
                .env(Environment.builder()
                    .account("942706091699")
                    .region("eu-west-2")
                    .build())
                .build()));
        
        testingStage.addPost(new ManualApprovalStep("Approval"));
    }
}