package jomu.instrument.store;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

@Configuration
public class MicrostreamConfig {

	@Value("${one.microstream.storage-directory}")
	String location;

	@Bean
	public EmbeddedStorageManager storageManager() {

		// EmbeddedStorageManager storageManager = EmbeddedStorage.start(dataRoot(), //
		// root object
		// Paths.get(location) // storage directory
		// );
		EmbeddedStorageManager storageManager = EmbeddedStorage.start(Paths.get(location));
		if (storageManager.root() == null) {
			InstrumentStorage rootInstance = new InstrumentStorage();
			storageManager.setRoot(rootInstance);
			storageManager.storeRoot();
		} else {
			InstrumentStorage rootInstance = (InstrumentStorage) storageManager.root();
			// Use existing root loaded from storage
		}
		return storageManager;
	}
}