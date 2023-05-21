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

	final List<ToneMap> toneMapList = new ArrayList<>();
	Properties parameters = new Properties();

	String root = "testing";

	@Inject
	transient StorageManagerController storageManagerController;

	public List<ToneMap> findAllToneMaps() {
		return this.toneMapList;
	}

	private StorageManager getStorageManager() {
		return storageManagerController.getStorageManager();
	}

	public void removeAllToneMaps() {
		this.toneMapList.clear();
		getStorageManager().store(toneMapList);
		getStorageManager().storeRoot();
	}

	public void addToneMap(final ToneMap toneMap) {
		this.toneMapList.add(toneMap);
		getStorageManager().store(toneMapList);
		getStorageManager().storeRoot();
	}

	public void setParameters(final Properties parameters) {
		this.parameters = parameters;
		getStorageManager().store(this.parameters);
		getStorageManager().storeRoot();
		LOG.severe(">>Store params");
	}

	public Properties getParameters() {
		LOG.severe(">>Get params");
		return parameters;
	}

	public void shutdown() {
		getStorageManager().shutdown();
	}

	public void initialise(boolean isInitRequired) {
		if (isInitRequired) {
			Instrument.getInstance().getController().getParameterManager().reset();
			this.setParameters(Instrument.getInstance().getController().getParameterManager().getParameters());
			getStorageManager().setRoot(parameters);
			getStorageManager().storeRoot();
			LOG.severe(">>Initialise Store");
		} else {
			Instrument.getInstance().getController().getParameterManager().setParameters(getParameters());
		}
	}
}
