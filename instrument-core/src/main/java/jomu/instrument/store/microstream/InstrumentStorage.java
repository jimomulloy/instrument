package jomu.instrument.store.microstream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.Instrument;
import jomu.instrument.workspace.tonemap.ToneMap;
import one.microstream.storage.types.StorageManager;

@ApplicationScoped
//@Storage
public class InstrumentStorage {

	final List<ToneMap> toneMapList = new ArrayList<>();
	Properties parameters = new Properties();

	String root = "testing";

	@Inject
	transient StorageManagerController storageManagerController;

	transient boolean initialiseRequired = false;

	public List<ToneMap> findAllToneMaps() {
		return this.toneMapList;
	}

	private StorageManager getStorageManager() {
		return storageManagerController.getStorageManager();
	}

	public void removeAllToneMaps() {
		this.toneMapList.clear();
		// getStorageManager().store(toneMapList);
		// getStorageManager().storeRoot();
	}

	public void addToneMap(final ToneMap toneMap) {
		this.toneMapList.add(toneMap);
		// getStorageManager().store(toneMapList);
		// getStorageManager().storeRoot();
	}

	public void setParameters(final Properties parameters) {
		this.parameters = parameters;
		// getStorageManager().store(this.parameters);
		// getStorageManager().storeRoot();
	}

	public Properties getParameters() {
		return parameters;
	}

	public void shutdown() {
		// getStorageManager().shutdown();
	}

	public void setInitRequired() {
		this.initialiseRequired = true;
	}

	public boolean isInitRequired() {
		// TODO return this.initialiseRequired;
		return true;
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
			// getStorageManager().setRoot(root);
			// getStorageManager().setRoot(this);
			System.out.println(">>IS init");
			// getStorageManager().storeRoot();
		} else {
			Instrument.getInstance().getController().getParameterManager().setParameters(getParameters());
		}
	}
}
