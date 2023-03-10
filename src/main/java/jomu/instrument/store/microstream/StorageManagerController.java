package jomu.instrument.store.microstream;

import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
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

	/**
	 * Initialize storage manager on quarkus startup.
	 *
	 * @param startupEvent quarkus startup event.
	 */
	public void onStartup(@Observes StartupEvent startupEvent) {
		String userDir = System.getProperty("user.home");
		String rootPath = Paths.get(userDir, msdPath).toString();
		DataConfiguration.init(rootPath);
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