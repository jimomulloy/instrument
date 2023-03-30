package jomu.instrument.aws.s3handler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import jomu.instrument.Instrument;
import jomu.instrument.control.Controller;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
public class ProcessingService {

	private static final Logger LOG = Logger.getLogger(ProcessingService.class.getName());

	public static final String CAN_ONLY_GREET_NICKNAMES = "Can only greet nicknames";

	@Inject
	Instrument instrument;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Controller controller;

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

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
		String propsKey = s3Key.substring(0, s3Key.lastIndexOf("/") + 1) + "parameter.properties";
		String stateKey = s3Key.substring(0, s3Key.lastIndexOf("/") + 1) + "state.txt";

		LOG.severe(">>ProcessingService propsKey: " + propsKey);
		LOG.severe(">>ProcessingService stateKey: " + stateKey);
		InputStream stream = storage.getObjectStorage().read(propsKey);
		if (stream != null) {
			Properties props = new Properties();
			try {
				props.load(stream);
				controller.getParameterManager().overrideParameters(props);
				LOG.severe(">>ProcessingService overrideParameters");
			} catch (IOException e) {
				LOG.log(Level.SEVERE, ">>ProcessingService error overiding parameters", e);
			}
		}

		Map<String, String> metaData = storage.getObjectStorage().getMetaData(input.getName());
		LOG.severe(">>ProcessingService process metaData: " + metaData);
		String style = metaData.containsKey("instrument-style") ? metaData.get("instrument-style") : "default";
		String offset = metaData.containsKey("instrument-offset") ? metaData.get("instrument-offset") : null;
		String range = metaData.containsKey("instrument-range") ? metaData.get("instrument-offset") : null;
		if (offset != null) {
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET, offset);
		}
		if (range != null) {
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE, range);
		}
		LOG.severe(">>ProcessingService process style: " + style + ", " + metaData);
		storage.getObjectStorage().clearStore("private/" + userId + "/output");
		storage.getObjectStorage().writeString(stateKey, "processing");

		controller.run(userId, input.getName(), style);

		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();
		String midiFilePath = instrumentSession.getOutputMidiFilePath();
		String midiFileFolder = midiFilePath;
		LOG.severe(">>ProcessingService midiFilePath: " + midiFilePath);
		if (midiFilePath != null && !midiFilePath.isEmpty()) {
			if (midiFilePath.lastIndexOf("/") > -1) {
				midiFileFolder = midiFilePath.substring(0, midiFilePath.lastIndexOf("/"));
			} else if (midiFilePath.lastIndexOf("\\") > -1) {
				midiFileFolder = midiFilePath.substring(0, midiFilePath.lastIndexOf("\\"));
			}
			LOG.severe(">>ProcessingService midiFileFolder: " + midiFileFolder);
			Set<String> fileNames = listFiles(midiFileFolder);
			for (String fileName : fileNames) {
				if (fileName.split("_")[0].equals(instrumentSession.getInputAudioFileName())) {
					File midiFile = new File(midiFileFolder + "/" + fileName);
					LOG.severe(">>ProcessingService store: " + midiFileFolder + "/" + fileName);
					storage.getObjectStorage().write("private/" + userId + "/output/" + fileName, midiFile);
					midiFile.delete();
				}
			}
		}
		String result = "done";
		OutputObject out = new OutputObject();
		out.setResult(result);
		storage.getObjectStorage().writeString(stateKey, "processed");
		return out;
	}

	private Set<String> listFiles(String dir) {
		return Stream.of(new File(dir).listFiles()).filter(file -> !file.isDirectory()).map(File::getName)
				.collect(Collectors.toSet());
	}

}
