package jomu.instrument.store;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jomu.instrument.control.InstrumentParameterNames;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.types.StorageManager;

@Configuration
public class DataConfiguration {

	private final Environment environment;

	@Value("${one.microstream.storage-directory}")
	String rootPath;

	public DataConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean(destroyMethod = "shutdown")
	public StorageManager defineStorageManager() {

		String userDir = System.getProperty("user.home");

		StorageManager storageManager = EmbeddedStorage.start(Paths.get(userDir, rootPath) // storage directory
		);

		// Check Root available within StorageManager
		InstrumentStorage root = (InstrumentStorage) storageManager.root();
		boolean initRequired = false;
		if (root == null) {
			root = new InstrumentStorage();
			initRequired = true;
			System.out.println(">>DataConfiguration init required");
		} else {
			System.out.println(">>DataConfiguration init existing "
					+ root.getParameters().get(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR));
		}
		// Prep Root
		root.setStorageManager(storageManager);

		// Init 'database' with some data
		if (initRequired) {
			root.setInitRequired();
			storageManager.setRoot(root);
			storageManager.storeRoot();
		}
		return storageManager;
	}
}