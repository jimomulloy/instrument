package jomu.instrument.control;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.Organ;
import jomu.instrument.store.Storage;
import jomu.instrument.utils.FileUtils;

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

	public void test() {
		LOG.severe(">>Test INSTRUMENT");
		getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH, "true");
		getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_1, "true");
		getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY, "true");
		getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE, "true");
		CountDownLatch countDownLatch = new CountDownLatch(1);
		setCountDownLatch(countDownLatch);
		coordinator.getHearing().test();
		try {
			countDownLatch.await(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clearCountDownLatch();
		LOG.severe(">>Tested INSTRUMENT");
	}

	public void run(String fileName, String paramStyle) {
		LOG.severe(">>INSTRUMENT Run started");
		getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH, "true");
		getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_1, "true");
		getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY, "true");
		getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE, "true");
		CountDownLatch countDownLatch = new CountDownLatch(1);
		setCountDownLatch(countDownLatch);
		try {
			coordinator.getHearing().startAudioFileStream(fileName);
			countDownLatch.await(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.severe(">>Controller run completed for fileName: " + fileName);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, ">>Controller run error for fileName: " + fileName, e);
		}
		clearCountDownLatch();
		LOG.severe(">>INSTRUMENT Run ended");
	}

	/**
	 * Checks the configured directories and creates them if they are not present.
	 */
	public void configureDirectories() {
		String audioDirectory = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DIRECTORY);
		String baseDir = storage.getObjectStorage().getBasePath();

		if (!new File(audioDirectory).isAbsolute()) {
			audioDirectory = FileUtils.combine(baseDir, audioDirectory);
		}
		LOG.finer("Creating directory: " + audioDirectory);
		if (FileUtils.mkdirs(audioDirectory)) {
			LOG.finer("Created directory: " + audioDirectory);
		}
		// Check if the directory is writable
		if (!new File(audioDirectory).canWrite()) {
			String message = "Required directory " + audioDirectory
					+ " is not writable!\n Please configure another directory for '"
					+ InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DIRECTORY + "'.";
			LOG.finer(message);
			System.exit(-1);
		}

		String audioRecordDirectory = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY);
		audioRecordDirectory = FileUtils.combine(audioDirectory, audioRecordDirectory);
		LOG.finer("Creating directory: " + audioRecordDirectory);
		if (FileUtils.mkdirs(audioRecordDirectory)) {
			LOG.finer("Created directory: " + audioRecordDirectory);
		}
		// Check if the directory is writable
		if (!new File(audioRecordDirectory).canWrite()) {
			String message = "Required directory " + audioRecordDirectory
					+ " is not writable!\n Please configure another directory for '"
					+ InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY + "'.";
			LOG.finer(message);
			System.exit(-1);
		}

	}
}
