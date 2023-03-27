package jomu.instrument.aws.s3handler;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		Map<String, String> metaData = instrument.getStorage().getObjectStorage().getMetaData(input.getName());
		LOG.severe(">>ProcessingService process: " + metaData);
		String style = metaData.containsKey("x-amz-meta-instrument-style") ? metaData.get("x-amz-meta-instrument-style")
				: "default";
		LOG.severe(">>ProcessingService process style: " + style + ", " + metaData);
		instrument.getStorage().getObjectStorage().clearStore("private/" + userId + "/output");
		instrument.getController().run(userId, input.getName(), style);
		InstrumentSession instrumentSession = instrument.getWorkspace().getInstrumentSessionManager()
				.getCurrentSession();
		String midiFilePath = instrumentSession.getOutputMidiFilePath();
		String midiFileFolder = midiFilePath;
		LOG.severe(">>ProcessingService midiFilePath: " + midiFilePath);
		if (midiFilePath.lastIndexOf("/") > -1) {
			midiFileFolder = midiFilePath.substring(0, midiFilePath.lastIndexOf("/"));
		} else if (midiFilePath.lastIndexOf("\\") > -1) {
			midiFileFolder = midiFilePath.substring(0, midiFilePath.lastIndexOf("\\"));
		}
		LOG.severe(">>ProcessingService midiFileFolder: " + midiFileFolder);
		Set<String> fileNames = listFiles(midiFileFolder);
		for (String fileName : fileNames) {
			File midiFile = new File(midiFileFolder + "/" + fileName);
			LOG.severe(">>ProcessingService store: " + midiFileFolder + "/" + fileName);
			instrument.getStorage().getObjectStorage().write("private/" + userId + "/output/" + fileName, midiFile);
		}
		String result = "done"; // input.getGreeting() + " " + input.getName();
		OutputObject out = new OutputObject();
		out.setResult(result);
		return out;
	}

	private Set<String> listFiles(String dir) {
		return Stream.of(new File(dir).listFiles()).filter(file -> !file.isDirectory()).map(File::getName)
				.collect(Collectors.toSet());
	}
}
