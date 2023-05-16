package jomu.instrument.desktop.store.microstream;

import java.nio.file.Paths;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.Storage;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

/**
 * Bean to initialize and shutdown Microstream storage manager
 *
 * @author Felix Riess, codecentric AG
 * @since 09 Sep 2022
 */
@ApplicationScoped
public class StorageManagerController {

	@ConfigProperty(name = "one.microstream.storage-directory")
	String msdPath;

	@Inject
	Storage instrumentStorage;

	@Inject
	ParameterManager parameterManager;

	transient boolean initialiseRequired = false;

	/**
	 * Initialize storage manager on quarkus startup.
	 *
	 * @param startupEvent quarkus startup event.
	 */
	public void onStartup(@Observes StartupEvent startupEvent) {
		String baseDir = instrumentStorage.getObjectStorage().getBasePath();
		String rootPath = Paths.get(baseDir, msdPath).toString();
		DataConfiguration.init(rootPath, instrumentStorage);
	}

	public void setInitRequired() {
		this.initialiseRequired = true;
	}

	public boolean isInitRequired() {
		return this.initialiseRequired;
	}

	/**
	 * Shutdown storage manager on quarkus shutdown.
	 *
	 * @param shutdownEvent quarkus shutdown event.
	 */
	public void onShutdown(@Observes ShutdownEvent shutdownEvent) {
		DataConfiguration.getInstance().shutdown();
	}

	public EmbeddedStorageManager getStorageManager() {
		return DataConfiguration.getInstance().getStorageManager();
	}
}