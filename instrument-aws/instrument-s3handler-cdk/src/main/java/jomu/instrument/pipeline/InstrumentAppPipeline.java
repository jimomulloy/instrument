package jomu.instrument.pipeline;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class InstrumentAppPipeline {
    public static void main(final String[] args) {
        App app = new App();

        new InstrumentAppStack(app, "InstrumentAppPipelineStack", StackProps.builder()
            .env(Environment.builder()
                .account("942706091699")
                .region("eu-west-2")
                .build())
            .build());

        app.synth();
    }
}