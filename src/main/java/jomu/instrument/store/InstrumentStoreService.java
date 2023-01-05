package jomu.instrument.store;

import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jomu.instrument.workspace.tonemap.ToneMap;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

@ApplicationScoped
@Component
public class InstrumentStoreService {

	@Autowired
	InstrumentStorage instrumentStorage;

	@Autowired
	EmbeddedStorageManager embeddedStorageManager;

	// @Store(root = true)
	public void addToneMap(ToneMap toneMap) {
		instrumentStorage.add(toneMap);
		embeddedStorageManager.store(instrumentStorage);
	}

	// @Store(root = true)
	public void setParameters(Properties parameters) {
		Properties copyParams = new Properties();
		copyParams.putAll(parameters);
		instrumentStorage.setParameters(copyParams);
		embeddedStorageManager.storeRoot();
		System.out.println(">>MS stored params!!");
	}

	// @Store(root = true)
	public void deleteToneMaps() {
		instrumentStorage.removeAll();
		embeddedStorageManager.storeRoot();
	}

	public List<ToneMap> findToneMaps() {
		return instrumentStorage.findAll();
	}

	public Properties getParameters() {
		Properties copyParams = new Properties();
		copyParams.putAll(instrumentStorage.getParameters());
		return copyParams;
	}

	@PreDestroy
	public void preDestroy() {
		embeddedStorageManager.shutdown();
		System.out.println(">>MS shut!!");
	}

}