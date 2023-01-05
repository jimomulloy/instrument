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
	InstrumentStorage dataRoot() {
		InstrumentStorage dataRoot = new InstrumentStorage();
		return dataRoot;
	}

	@Bean
	public EmbeddedStorageManager storageManager() {

		EmbeddedStorageManager storageManager = EmbeddedStorage.start(dataRoot(), // root object
				Paths.get(location) // storage directory
		);
		System.out.println(">>MS init!!");
		return storageManager;
	}
}