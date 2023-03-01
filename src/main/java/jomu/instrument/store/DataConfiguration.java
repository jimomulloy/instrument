package jomu.instrument.store;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jomu.instrument.control.InstrumentParameterNames;
import one.microstream.storage.embedded.configuration.types.EmbeddedStorageConfiguration;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import one.microstream.storage.types.StorageManager;

@Configuration
public class DataConfiguration {

	private final Environment environment;

	public DataConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean(destroyMethod = "shutdown")
	public StorageManager defineStorageManager() {

		EmbeddedStorageFoundation<?> embeddedStorageFoundation = embeddedStorageFoundation(environment);

		StorageManager storageManager = embeddedStorageFoundation.start();

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

	private EmbeddedStorageFoundation<?> embeddedStorageFoundation(Environment env) {
		String configLocation = env.getProperty("one.microstream.config");

		if (configLocation == null) {
			throw new BeanCreationException(
					"Unable to create StorageManager as the configuration property 'one.microstream.config' could not be resolved");
		}
		return EmbeddedStorageConfiguration.load(configLocation).createEmbeddedStorageFoundation();

	}
}