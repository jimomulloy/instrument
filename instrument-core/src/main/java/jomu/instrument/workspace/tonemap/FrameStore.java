package jomu.instrument.workspace.tonemap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.ObjectStorage;

@ApplicationScoped
public class FrameStore {

	private static final Logger LOG = Logger.getLogger(FrameStore.class.getName());

	@Inject
	ParameterManager parameterManager;

	@Inject
	ObjectStorage objectStorage;

	void write(String key, ToneTimeFrame value) {
		String baseDir = objectStorage.getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FRAME_STORE_DIRECTORY))
				.toString();
		String fileName = key.replaceAll(":", "_") + ".ser";
		String filePath = folder + System.getProperty("file.separator") + fileName;
		File file = new File(filePath);
		// LOG.severe(">>FS WRITE filePath: " + filePath);
		try {
			// LOG.severe(">>FS WRITE 1: " + key);
			FileOutputStream f = new FileOutputStream(file);
			ObjectOutputStream o = new ObjectOutputStream(f);
			// LOG.severe(">>FS WRITE 2: " + key);
			o.writeObject(value);
			o.flush();
			o.close();
			f.close();
			// LOG.severe(">>FS WRITEN: " + key + ", " + value);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Optional<ToneTimeFrame> read(String key) {
		ToneTimeFrame ttf = null;
		String baseDir = objectStorage.getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FRAME_STORE_DIRECTORY))
				.toString();
		String fileName = key.replaceAll(":", "_") + ".ser";
		String filePath = folder + System.getProperty("file.separator") + fileName;
		File file = new File(filePath);
		try {
			if (file.exists()) {
				FileInputStream fi = new FileInputStream(file);
				ObjectInputStream oi = new ObjectInputStream(fi);
				// LOG.severe(">>FS READ: " + key);
				ttf = (ToneTimeFrame) oi.readObject();
				// LOG.severe(">>FS READDEN: " + key + ", " + ttf);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		Optional<ToneTimeFrame> result = Optional.ofNullable(ttf);
		return result;
	}

	public void clear() {
		String baseDir = objectStorage.getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_FRAME_STORE_DIRECTORY))
				.toString();
		File directory = new File(folder);
		for (File file : Objects.requireNonNull(directory.listFiles())) {
			if (!file.isDirectory()) {
				file.delete();
			}
		}

	}
}
