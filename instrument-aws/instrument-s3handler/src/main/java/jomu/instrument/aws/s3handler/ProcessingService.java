package jomu.instrument.aws.s3handler;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import jomu.instrument.Instrument;
import jomu.instrument.perception.Hearing;

@ApplicationScoped
public class ProcessingService {

	private static final Logger LOG = Logger.getLogger(ProcessingService.class.getName());

    public static final String CAN_ONLY_GREET_NICKNAMES = "Can only greet nicknames";

	@Inject
	Instrument instrument;

    void onStart(@Observes StartupEvent ev) {      
    	LOG.severe(">>ProcessingService Start up");
        Instrument.setInstance(instrument);
		instrument.initialise();
		instrument.start();
    	LOG.severe(">>ProcessingService Started");
    }

	public OutputObject process(InputObject input) {
    	LOG.severe(">>ProcessingService process");
        //if (input.getName().equals("Stuart")) {
        //   throw new IllegalArgumentException(CAN_ONLY_GREET_NICKNAMES);
        //}
		instrument.test();
        String result = "done"; //input.getGreeting() + " " + input.getName();
        OutputObject out = new OutputObject();
        out.setResult(result);
        return out;
    }
}
