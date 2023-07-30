package jomu.instrument.desktop.store.microstream;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.Instrument;
import jomu.instrument.workspace.tonemap.ToneMap;
import one.microstream.storage.types.StorageManager;

@ApplicationScoped
public class InstrumentStorage {

	private static final Logger LOG = Logger.getLogger(InstrumentStorage.class.getName());

	class InstrumentStorageRoot {
		public List<ToneMap> toneMapList = new ArrayList<>();
		public Properties parameters = new Properties();
	}

	InstrumentStorageRoot root = new InstrumentStorageRoot();

	@Inject
	transient StorageManagerController storageManagerController;

	public List<ToneMap> findAllToneMaps() {
		return this.root.toneMapList;
	}

	private StorageManager getStorageManager() {
		return storageManagerController.getStorageManager();
	}

	public void removeAllToneMaps() {
		this.root.toneMapList.clear();
		getStorageManager().store(root.toneMapList);
		getStorageManager().storeRoot();
	}

	public void addToneMap(final ToneMap toneMap) {
		this.root.toneMapList.add(toneMap);
		getStorageManager().store(root.toneMapList);
		getStorageManager().storeRoot();
	}

	public void setParameters(final Properties parameters) {
		this.root.parameters = parameters;
		getStorageManager().store(this.root.parameters);
		getStorageManager().storeRoot();
		LOG.severe(">>Store params");
	}

	public Properties getParameters() {
		LOG.severe(">>Get params");
		return root.parameters;
	}

	public void shutdown() {
		getStorageManager().shutdown();
	}

	public void initialise(boolean isInitRequired) {
		if (isInitRequired) {
			Instrument.getInstance().getController().getParameterManager().reset();
			this.setParameters(Instrument.getInstance().getController().getParameterManager().getParameters());
			getStorageManager().setRoot(this.root.parameters);
			getStorageManager().storeRoot();
			LOG.severe(">>Initialise Store");
		} else {
			Instrument.getInstance().getController().getParameterManager().setParameters(getParameters());
		}
	}
}
