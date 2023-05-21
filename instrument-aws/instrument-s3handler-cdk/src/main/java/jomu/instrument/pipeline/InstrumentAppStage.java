package jomu.instrument.pipeline;

import jomu.instrument.eventbridge.boundary.EventsStack;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

public class InstrumentAppStage extends Stage {
    public InstrumentAppStage(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InstrumentAppStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);
        Stack eventsStack = new EventsStack(this, "instrument-s3handler", true);
    }

}