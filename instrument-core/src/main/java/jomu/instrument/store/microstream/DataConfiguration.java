package jomu.instrument.store.microstream;

import one.microstream.afs.nio.types.NioFileSystem;
import one.microstream.reflect.ClassLoaderProvider;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;
import one.microstream.storage.types.Storage;
import one.microstream.storage.types.StorageChannelCountProvider;
import one.microstream.storage.types.StorageConfiguration;
import one.microstream.storage.types.StorageManager;

//@Configuration
public class DataConfiguration {

	// @Value("${one.microstream.storage-directory}")
	// String rootPath;

	private static volatile DataConfiguration INSTANCE;

	private volatile EmbeddedStorageManager storageManager;

	public DataConfiguration(String rootPath) {
		defineStorageManager(rootPath);
	}

	public static void init(String rootPath) {
		if (INSTANCE == null) {
			synchronized (DataConfiguration.class) {
				if (INSTANCE == null) {
					INSTANCE = new DataConfiguration(rootPath);
				}
			}
		}
	}

	public static DataConfiguration getInstance() {
		if (INSTANCE == null) {
			// LOG.error("Storage Manager is not yet initialized");
			// throw new Throwable("Storage Manager is not yet initialized");
		}
		return INSTANCE;
	}

	/**
	 * Gracefully shutdown the storage manager if not yet done.
	 */
	public synchronized void shutdown() {
		if (this.storageManager != null) {
			this.storageManager.shutdown();
			this.storageManager = null;
		}
	}

	public EmbeddedStorageManager getStorageManager() {
		return this.storageManager;
	}

	public StorageManager defineStorageManager(String rootPath) {

		System.out.println(">>MS root: " + rootPath);
		NioFileSystem fileSystem = NioFileSystem.New();
		// StorageManager storageManager = EmbeddedStorage.start(Paths.get(userDir,
		// rootPath));

		final EmbeddedStorageFoundation<?> foundation = EmbeddedStorageFoundation.New()
				.setConfiguration(StorageConfiguration.Builder()
						.setStorageFileProvider(Storage.FileProviderBuilder(fileSystem)
								.setDirectory(fileSystem.ensureDirectoryPath(rootPath)).createFileProvider())
						.setChannelCountProvider(StorageChannelCountProvider.New(Math.max(1, // minimum one channel, if
																								// only 1 core is
																								// available
								Integer.highestOneBit(Runtime.getRuntime().availableProcessors() - 1))))
						.createConfiguration());
		// handle changing class definitions at runtime ("hot code replacement" by
		// quarkus by running app in development mode)
		foundation.onConnectionFoundation(connectionFoundation -> connectionFoundation
				.setClassLoaderProvider(ClassLoaderProvider.New(Thread.currentThread().getContextClassLoader())));
		this.storageManager = foundation.createEmbeddedStorageManager().start();

		// Check Root available within StorageManager
		Object root = storageManager.root();
		boolean initRequired = false;
		if (root == null) {
			root = new InstrumentStorage();
			root = "Testing";
			initRequired = true;
		}
		// Prep Root
		// TODOroot.setStorageManager(storageManager);

		// Init 'database' with some data
		if (initRequired) {
			// root.setInitRequired();
			storageManager.setRoot(root);
			storageManager.storeRoot();
		}
		return storageManager;
	}
}