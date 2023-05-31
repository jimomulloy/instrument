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

import com.google.gson.Gson;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
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
		instrument.reset();
		String s3Key = input.getName();
		String userId = s3Key.substring("private/".length(), s3Key.indexOf("/input/"));
		String propsKey = "private/" + userId + "/input/parameter.properties";
		String stateKey = "private/" + userId + "/state.json";

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
				LOG.log(Level.SEVERE, ">>ProcessingService error overriding parameters", e);
			}
		}

		Map<String, String> metaData = storage.getObjectStorage().getMetaData(input.getName());
		LOG.severe(">>ProcessingService process metaData: " + metaData);
		String style = metaData.containsKey("instrument-style") ? metaData.get("instrument-style") : "default";
		String offset = metaData.containsKey("instrument-offset") ? metaData.get("instrument-offset") : null;
		String range = metaData.containsKey("instrument-range") ? metaData.get("instrument-range") : null;
		if (offset != null) {
			int offsetValue = 0;
			try {
				offsetValue = Integer.parseInt(offset) * 1000;
			} catch (Exception ex) {
				//
			}
			if (offsetValue > 0) {
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET,
						Integer.toString(offsetValue));
			}
		}
		if (range != null) {
			int rangeValue = 0;
			try {
				rangeValue = Integer.parseInt(range) * 1000;
			} catch (Exception ex) {
				//
			}
			if (rangeValue > 0) {
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE,
						Integer.toString(rangeValue));
			}
		}
		LOG.severe(">>ProcessingService process style: " + style + ", " + metaData);
		LOG.severe(">>ProcessingService process offset: "
				+ parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET));
		LOG.severe(">>ProcessingService process range: "
				+ parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE));
		storage.getObjectStorage().clearStore("private/" + userId + "/output");

		if (!s3Key.startsWith("private/" + userId + "/input/recording/")) {
			String[] storeKeys = storage.getObjectStorage().listStore("private/" + userId + "/input/");
			for (String storeKey : storeKeys) {
				LOG.severe(">>ProcessingService input storeKey: " + storeKey);
				if (!storeKey.startsWith("private/" + userId + "/input/recording/") && !storeKey.equals(s3Key)) {
					storage.getObjectStorage().delete(storeKey);
					LOG.severe(">>ProcessingService delete old input files: " + storeKey);
				}
			}
		} else {
			String[] storeKeys = storage.getObjectStorage().listStore("private/" + userId + "/input/recording/");
			for (String storeKey : storeKeys) {
				LOG.severe(">>ProcessingService input storeKey: " + storeKey);
				if (!storeKey.equals(s3Key)) {
					storage.getObjectStorage().delete(storeKey);
					LOG.severe(">>ProcessingService delete old input files: " + storeKey);
				}
			}
		}

		boolean instrumentRun = controller.run(userId, input.getName(), style);
		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();

		if (!instrumentRun) {
			ClassLoader classLoader = getClass().getClassLoader();
			File errorMidi = new File(classLoader.getResource("error.midi").getFile());
			storage.getObjectStorage().write("private/" + userId + "/output/" + errorMidi.getName(), errorMidi);
			String stateContent = storage.getObjectStorage().readString(stateKey);
			if (stateContent != null) {
				Gson gson = new Gson();
				State state = gson.fromJson(stateContent, State.class);
				state.status = "ERROR";
				state.message = instrumentSession.getStatusMessage();
				state.code = instrumentSession.getStatusCode();
				storage.getObjectStorage().writeString(stateKey, gson.toJson(state));
			}
			String result = "error";
			OutputObject out = new OutputObject();
			out.setResult(result);
			return out;
		}

		String midiFilePath = instrumentSession.getOutputMidiFilePath();
		String midiFileFolder = midiFilePath;
		boolean retried = false;
		LOG.severe(">>ProcessingService midiFilePath: " + midiFilePath);
		if (midiFilePath != null && !midiFilePath.isEmpty()) {
			if (midiFilePath.lastIndexOf("/") > -1) {
				midiFileFolder = midiFilePath.substring(0, midiFilePath.lastIndexOf("/"));
			} else if (midiFilePath.lastIndexOf("\\") > -1) {
				midiFileFolder = midiFilePath.substring(0, midiFilePath.lastIndexOf("\\"));
			}
			LOG.severe(">>ProcessingService midiFileFolder: " + midiFileFolder);
			Set<String> fileNames = listFiles(midiFileFolder);
			LOG.severe(">>ProcessingService midiFileFolder instrumentSession.getInputAudioFileName(): "
					+ instrumentSession.getInputAudioFileName() + ", " + fileNames.size() + " ," + fileNames);
			for (String fileName : fileNames) {
				LOG.severe(">>ProcessingService midiFileFolder fileName: " + fileName);
				if (fileName.startsWith(instrumentSession.getInputAudioFileName()) 
						&& (fileName.toLowerCase().endsWith("midi") || fileName.toLowerCase().endsWith("mid"))) {
					File midiFile = new File(midiFileFolder + "/" + fileName);
					LOG.severe(
							">>ProcessingService store: " + midiFileFolder + "/" + fileName + ", " + midiFile.length());
					storage.getObjectStorage().write("private/" + userId + "/output/" + fileName, midiFile);
					if (!retried) {
						retried = true;
						storage.getObjectStorage().write("private/" + userId + "/output/" + fileName, midiFile);
					}
					midiFile.delete();
					LOG.severe(">>ProcessingService deleted: " + midiFileFolder + "/" + fileName);
				}
			}
		}
		String stateContent = storage.getObjectStorage().readString(stateKey);
		if (stateContent != null) {
			Gson gson = new Gson();
			State state = gson.fromJson(stateContent, State.class);
			state.status = "READY";
			state.message = instrumentSession.getStatusMessage();
			state.code = instrumentSession.getStatusCode();
			storage.getObjectStorage().writeString(stateKey, gson.toJson(state));
		}
		String result = "done";
		OutputObject out = new OutputObject();
		out.setResult(result);
		return out;
	}

	private Set<String> listFiles(String dir) {
		return Stream.of(new File(dir).listFiles()).filter(file -> !file.isDirectory()).map(File::getName)
				.collect(Collectors.toSet());
	}

}
