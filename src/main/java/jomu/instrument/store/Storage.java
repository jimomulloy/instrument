package jomu.instrument.store;

import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.stereotype.Component;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.Organ;
import jomu.instrument.control.ParameterManager;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

@ApplicationScoped
@Component
public class Storage implements Organ {

	InstrumentStoreService instrumentStoreService;
	private ParameterManager parameterManager;

	@Override
	public void initialise() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		String path = this.parameterManager.getParameter(InstrumentParameterNames.STORAGE_DIRECTORY);
		EmbeddedStorageManager storageManager = EmbeddedStorage.start(Paths.get(path));
		storageManager.setRoot(new InstrumentStorage());
		storageManager.storeRoot();
		this.instrumentStoreService = new InstrumentStoreService((InstrumentStorage) storageManager.root());
	}

	public InstrumentStoreService getInstrumentStoreService() {
		return instrumentStoreService;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
