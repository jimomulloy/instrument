package jomu.instrument.store;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jomu.instrument.control.InstrumentParameterNames;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.types.StorageManager;

//@Configuration
public class DataConfiguration {

	@Value("${one.microstream.storage-directory}")
	String rootPath;

	public DataConfiguration() {
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
		} 
		// Prep Root
		//TODOroot.setStorageManager(storageManager);

		// Init 'database' with some data
		if (initRequired) {
			root.setInitRequired();
			storageManager.setRoot(root);
			storageManager.storeRoot();
		}
		return storageManager;
	}
}