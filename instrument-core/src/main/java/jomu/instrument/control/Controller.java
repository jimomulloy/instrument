package jomu.instrument.control;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.InstrumentSession.InstrumentSessionMode;
import jomu.instrument.store.Storage;
import jomu.instrument.utils.FileUtils;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
public class Controller implements Organ {

	private static final int TIMEOUT = 120;

	private static final Logger LOG = Logger.getLogger(Controller.class.getName());

	private CountDownLatch countDownLatch = null;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Coordinator coordinator;

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

	public ParameterManager getParameterManager() {
		return parameterManager;
	}

	@Override
	public void initialise() {
		parameterManager.initialise();
	}

	@Override
	public void start() {
		configureDirectories();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}

	public CountDownLatch getCountDownLatch() {
		return countDownLatch;
	}

	public boolean isCountDownLatch() {
		return countDownLatch != null;
	}

	public void clearCountDownLatch() {
		countDownLatch = null;
	}

	public void setCountDownLatch(CountDownLatch countDownLatch) {
		this.countDownLatch = countDownLatch;
	}

	public boolean run(String userId, String fileName, String paramStyle) {
		LOG.severe(">>INSTRUMENT Run started userId: " + userId + ", fileName: " + fileName + ", styel: " + paramStyle);
		CountDownLatch countDownLatch = new CountDownLatch(1);
		setCountDownLatch(countDownLatch);
		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager()
				.getInstrumentSession(UUID.randomUUID().toString());
		try {
			instrumentSession.setUserId(userId);
			instrumentSession.setDateTime(Instant.now());
			instrumentSession.setParamStyle(paramStyle);
			instrumentSession.setMode(InstrumentSessionMode.JOB);
			if (paramStyle != null && !paramStyle.equals("default")) {
				parameterManager.loadStyle(paramStyle, false);
			}
			getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH, "true");
			getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY, "true");
			getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE, "true");
			coordinator.getHearing().startAudioFileStream(fileName);
			LOG.severe(">>INSTRUMENT Run userId: " + userId + ", fileName: " + fileName + ", style: " + paramStyle);
			countDownLatch.await(TIMEOUT, TimeUnit.SECONDS);
			if (!InstrumentSession.InstrumentSessionState.FAILED.equals(instrumentSession.getState())) {
				instrumentSession.setState(InstrumentSession.InstrumentSessionState.STOPPED);
				instrumentSession.setStatusCode("0");
				instrumentSession.setStatusMessage("OK");
			} else {
				instrumentSession.setStatusMessage("Controller run failure");
				instrumentSession.setStatusCode("9001");
				clearCountDownLatch();
				LOG.severe(">>INSTRUMENT Run FAILED");
				return false;
			}
		} catch (InterruptedException e) {
			LOG.severe(">>Controller run completed for fileName: " + fileName);
			instrumentSession.setState(InstrumentSession.InstrumentSessionState.FAILED);
			instrumentSession.setStatusCode("9001");
			instrumentSession.setStatusMessage("Controller run exception: " + e.getMessage());
			clearCountDownLatch();
			LOG.severe(">>INSTRUMENT Run FAILED");
			return false;
		} catch (Exception e) {
			LOG.log(Level.SEVERE, ">>Controller run error for fileName: " + fileName, e);
			instrumentSession.setState(InstrumentSession.InstrumentSessionState.FAILED);
			instrumentSession.setStatusCode("9002");
			instrumentSession.setStatusMessage("Controller run exception: " + e.getMessage());
			clearCountDownLatch();
			LOG.severe(">>INSTRUMENT Run FAILED");
			return false;
		}
		clearCountDownLatch();
		LOG.severe(">>INSTRUMENT Run ended");
		return true;
	}

	public String dumpStatusInfo() {
		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PROJECT_DIRECTORY))
				.toString();
		String dumpFileName = folder + System.getProperty("file.separator") + "instrument-status-dump.txt";

		try (BufferedWriter bwr = new BufferedWriter(new FileWriter(new File(dumpFileName)))) {
			StringBuilder buff = new StringBuilder();
			Throwable t = workspace.getInstrumentSessionManager().getCurrentSession().getException();
			buff.append("Intrument Status Dump on: " + java.time.LocalDateTime.now() + "\n");
			buff.append("=======================================================\n");
			buff.append("\n");
			buff.append("Last Stack Trace:\n");
			buff.append("=================\n");
			if (t != null) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				t.printStackTrace(new PrintStream(out));
				buff.append(new String(out.toByteArray()));
			} else {
				buff.append("None");
			}
			buff.append("\n");
			buff.append("\n");
			buff.append("Parameters:\n");
			buff.append("===========\n");
			Properties params = parameterManager.getParameters();
			for (Object key : params.keySet()) {
				buff.append(key + ": " + params.getProperty(key.toString()));
				buff.append("\n");
			}
			bwr.write(buff.toString());
			bwr.flush();
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Dump Status exception", ex);
			return "Status Dump error: " + ex.getMessage();
		}

		return "Status Dump to file: " + dumpFileName;

	}

	/**
	 * Checks the configured directories and creates them if they are not present.
	 */
	public void configureDirectories() {

		String baseDir = storage.getObjectStorage().getBasePath();

		String audioRecordDirectory = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY);
		LOG.severe("Creating directory: " + audioRecordDirectory);
		audioRecordDirectory = FileUtils.combine(baseDir, audioRecordDirectory);
		if (FileUtils.mkdirs(audioRecordDirectory)) {
			LOG.severe("Created directory: " + audioRecordDirectory);
		}
		// Check if the directory is writable
		if (!new File(audioRecordDirectory).canWrite()) {
			String message = "Required directory " + audioRecordDirectory
					+ " is not writable!\n Please configure another directory for '"
					+ InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY + "'.";
			LOG.severe(message);
			throw new InstrumentException(message);
		}

		String audioProjectDirectory = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PROJECT_DIRECTORY);
		LOG.severe("Creating directory: " + audioProjectDirectory);
		audioProjectDirectory = FileUtils.combine(baseDir, audioProjectDirectory);
		if (FileUtils.mkdirs(audioProjectDirectory)) {
			LOG.severe("Created directory: " + audioProjectDirectory);
		}
		// Check if the directory is writable
		if (!new File(audioProjectDirectory).canWrite()) {
			String message = "Required directory " + audioProjectDirectory
					+ " is not writable!\n Please configure another directory for '"
					+ InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PROJECT_DIRECTORY + "'.";
			LOG.severe(message);
			throw new InstrumentException(message);
		}

		String audioFrameStoreDirectory = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FRAME_STORE_DIRECTORY);
		LOG.severe("Creating directory: " + audioFrameStoreDirectory);
		audioFrameStoreDirectory = FileUtils.combine(baseDir, audioFrameStoreDirectory);
		if (FileUtils.mkdirs(audioFrameStoreDirectory)) {
			LOG.severe("Created directory: " + audioFrameStoreDirectory);
		}
		// Check if the directory is writable
		if (!new File(audioFrameStoreDirectory).canWrite()) {
			String message = "Required directory " + audioFrameStoreDirectory
					+ " is not writable!\n Please configure another directory for '"
					+ InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FRAME_STORE_DIRECTORY + "'.";
			LOG.severe(message);
			throw new InstrumentException(message);
		}
	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		// TODO Auto-generated method stub

	}

}
