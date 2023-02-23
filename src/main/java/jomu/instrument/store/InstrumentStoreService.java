package jomu.instrument.store;

import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import one.microstream.storage.types.StorageManager;

@ApplicationScoped
@Component
public class InstrumentStoreService {

	InstrumentStorage instrumentStorage;

	public InstrumentStoreService(StorageManager storageManager) {
		instrumentStorage = (InstrumentStorage) storageManager.root();
		System.out.println(">>Construct InstrumentStoreService !!");
	}

	public void initialise() {
		instrumentStorage.initialise();
	}

	// @Store(root = true)
	public void addToneMap(ToneMap toneMap) {
		instrumentStorage.addToneMap(toneMap);
	}

	// @Store(root = true)
	public void setParameters(Properties parameters) {
		System.out.println(">>ISS Set Parmaters!!: "
				+ parameters.get(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR));
		Properties copyParams = new Properties();
		copyParams.putAll(parameters);
		instrumentStorage.setParameters(copyParams);
	}

	// @Store(root = true)
	public void deleteToneMaps() {
		instrumentStorage.removeAllToneMaps();
	}

	public List<ToneMap> findToneMaps() {
		return instrumentStorage.findAllToneMaps();
	}

	public Properties getParameters() {
		Properties copyParams = new Properties();
		copyParams.putAll(instrumentStorage.getParameters());
		return copyParams;
	}

	@PreDestroy
	public void preDestroy() {
		instrumentStorage.shutdown();
	}

}