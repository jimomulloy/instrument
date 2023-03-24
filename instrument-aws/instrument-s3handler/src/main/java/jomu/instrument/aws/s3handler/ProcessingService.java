package jomu.instrument.aws.s3handler;

import java.io.File;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import jomu.instrument.Instrument;
import jomu.instrument.store.InstrumentSession;

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
		LOG.severe(">>ProcessingService process: " + input);
		String s3Key = input.getName();
		String userId = s3Key.substring("private/".length(), s3Key.indexOf("/input/"));
		instrument.getController().run(userId, input.getName(), "default");
		InstrumentSession instrumentSession = instrument.getWorkspace().getInstrumentSessionManager()
				.getCurrentSession();
		String midiFilePath = instrumentSession.getOutputMidiFilePath();
		String midiFileName = instrumentSession.getOutputMidiFileName();
		File midiFile = new File(midiFilePath);
		instrument.getStorage().getObjectStorage().write("private/" + userId + "/output/" + midiFileName, midiFile);
		String result = "done"; // input.getGreeting() + " " + input.getName();
		OutputObject out = new OutputObject();
		out.setResult(result);
		return out;
	}
}
