package jomu.instrument.ws.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jomu.instrument.Instrument;
import jomu.instrument.control.Controller;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
public class InstrumentRunner {

	private static final Logger LOG = Logger.getLogger(InstrumentRunner.class.getName());

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
		Instrument.setInstance(instrument);
		instrument.initialise();
		instrument.start();
	}

	@ConsumeEvent("upload")
	@Blocking
	public void consume(String uploadId) {
		instrument.reset();
		File paramsFile = null;
		File audioFile = null;
		java.nio.file.Path audioInputPath = Paths
				.get(System.getProperty("user.home") + File.separator + ".instrumentuploads" + File.separator + "input"
						+ File.separator + uploadId + File.separator + "audio" + File.separator);
		List<String> audioFiles = new ArrayList<>();
		try {
			audioFiles = Files.list(audioInputPath).filter(Files::isRegularFile).map(p -> p.getFileName().toString())
					.collect(Collectors.toList());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (audioFiles.size() > 0) {
			audioFile = new File(audioInputPath + File.separator + audioFiles.get(0));
		}

		java.nio.file.Path paramsInputPath = Paths
				.get(System.getProperty("user.home") + File.separator + ".instrumentuploads" + File.separator + "input"
						+ File.separator + uploadId + File.separator + "params" + File.separator);
		List<String> paramsFiles = new ArrayList<>();
		try {
			paramsFiles = Files.list(paramsInputPath).filter(Files::isRegularFile).map(p -> p.getFileName().toString())
					.collect(Collectors.toList());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (paramsFiles.size() > 0) {
			paramsFile = new File(paramsInputPath + File.separator + paramsFiles.get(0));
			System.out.println(">>PARAMS FILE: " + paramsFile.getAbsolutePath());
		}
		boolean instrumentRun = false;
		if (audioFile != null && audioFile.exists()) {
			instrumentRun = controller.run(uploadId, audioFile.getAbsolutePath(), "default");
		}

		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();

		java.nio.file.Path resultOutputPath = Paths.get(System.getProperty("user.home") + File.separator
				+ ".instrumentuploads" + File.separator + "output" + File.separator + uploadId + File.separator);

		if (!instrumentRun) {
			LOG.severe(">>InstrumentRunner error");
			ClassLoader classLoader = getClass().getClassLoader();
			File errorMidi = new File(classLoader.getResource("error.midi").getFile());
			try {
				Files.createDirectories(resultOutputPath);
				String errorFileName = resultOutputPath + File.separator + "error.midi";
				java.nio.file.Path copied = Paths.get(errorFileName);
				java.nio.file.Path originalPath = errorMidi.toPath();
				Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		LOG.severe(">>InstrumentRunner OK");
		String midiFilePath = instrumentSession.getOutputMidiFilePath();
		String midiFileFolder = midiFilePath;
		if (midiFilePath != null && !midiFilePath.isEmpty()) {
			LOG.severe(">>InstrumentRunner OK2");
			try {
				Files.createDirectories(resultOutputPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (midiFilePath.lastIndexOf("/") > -1) {
				midiFileFolder = midiFilePath.substring(0, midiFilePath.lastIndexOf("/"));
			} else if (midiFilePath.lastIndexOf("\\") > -1) {
				midiFileFolder = midiFilePath.substring(0, midiFilePath.lastIndexOf("\\"));
			}
			Set<String> fileNames = listFiles(midiFileFolder);
			LOG.severe(">>InstrumentRunner midiFileFolder instrumentSession.getInputAudioFileName(): "
					+ instrumentSession.getInputAudioFileName() + ", " + fileNames.size() + " ," + fileNames);
			for (String fileName : fileNames) {
				LOG.severe(">>InstrumentRunner midiFileFolder fileName: " + fileName);
				if (fileName.startsWith(instrumentSession.getInputAudioFileName())
						&& (fileName.toLowerCase().endsWith("midi") || fileName.toLowerCase().endsWith("mid"))) {
					File midiFile = new File(midiFileFolder + File.separator + fileName);
					java.nio.file.Path copied = Paths.get(resultOutputPath + File.separator + fileName);
					java.nio.file.Path originalPath = midiFile.toPath();
					try {
						Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					LOG.severe(">>InstrumentRunner store: " + midiFileFolder + File.separator + fileName + ", "
							+ midiFile.length());
					midiFile.delete();
					LOG.severe(">>InstrumentRunner deleted: " + midiFileFolder + "/" + fileName);
				}
			}
			LOG.severe(">>InstrumentRunner OK3");

		}
		LOG.severe(">>InstrumentRunner OK4");

		return;
	}

	private Set<String> listFiles(String dir) {
		return Stream.of(new File(dir).listFiles()).filter(file -> !file.isDirectory()).map(File::getName)
				.collect(Collectors.toSet());
	}
}
