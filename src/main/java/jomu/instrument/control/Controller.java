package jomu.instrument.control;

import java.io.File;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.stereotype.Component;

import jomu.instrument.Organ;
import jomu.instrument.utils.FileUtils;

@ApplicationScoped
@Component
public class Controller implements Organ {

	private static final Logger LOG = Logger.getLogger(Controller.class.getName());

	ParameterManager parameterManager = new ParameterManager();

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

	/**
	 * Checks the configured directories and creates them if they are not present.
	 */
	public void configureDirectories() {
		String audioDirectory = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DIRECTORY);
		String userDir = System.getProperty("user.home");

		if (!new File(audioDirectory).isAbsolute()) {
			audioDirectory = FileUtils.combine(userDir, audioDirectory);
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
			LOG.severe(message);
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
			LOG.severe(message);
			System.exit(-1);
		}

	}
}
