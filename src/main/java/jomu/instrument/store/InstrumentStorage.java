package jomu.instrument.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jomu.instrument.Instrument;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import one.microstream.storage.types.StorageManager;

public class InstrumentStorage {

	final List<ToneMap> toneMapList = new ArrayList<>();
	Properties parameters = new Properties();

	private transient StorageManager storageManager;
	private transient boolean initialiseRequired = false;

	public void setStorageManager(StorageManager storageManager) {
		this.storageManager = storageManager;
	}

	public List<ToneMap> findAllToneMaps() {
		return this.toneMapList;
	}

	public void removeAllToneMaps() {
		this.toneMapList.clear();
		storageManager.store(toneMapList);
		storageManager.storeRoot();
	}

	public void addToneMap(final ToneMap toneMap) {
		this.toneMapList.add(toneMap);
		storageManager.store(toneMapList);
		storageManager.storeRoot();
	}

	public void setParameters(final Properties parameters) {
		this.parameters = parameters;
		storageManager.store(this.parameters);
		storageManager.storeRoot();
	}

	public Properties getParameters() {
		return parameters;
	}

	public void shutdown() {
		storageManager.shutdown();
	}

	public void setInitRequired() {
		this.initialiseRequired = true;
	}

	public boolean isInitRequired() {
		return this.initialiseRequired;
	}

	public void initialise() {
		if (isInitRequired()) {
			try {
				Instrument.getInstance().getController().getParameterManager().reset();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.setParameters(Instrument.getInstance().getController().getParameterManager().getParameters());
			storageManager.setRoot(this);
			storageManager.storeRoot();
		} else {
			Instrument.getInstance().getController().getParameterManager().setParameters(getParameters());
		}
	}
}
