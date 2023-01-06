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
		instrumentStorage.addToneMap(toneMap);
		embeddedStorageManager.store(instrumentStorage.findAllToneMaps());
	}

	// @Store(root = true)
	public void setParameters(Properties parameters) {
		Properties copyParams = new Properties();
		copyParams.putAll(parameters);
		instrumentStorage.setParameters(copyParams);
		embeddedStorageManager.store(instrumentStorage.getParameters());
		System.out.println(">>MS stored params!!");
	}

	// @Store(root = true)
	public void deleteToneMaps() {
		instrumentStorage.removeAllToneMaps();
		embeddedStorageManager.store(instrumentStorage.findAllToneMaps());
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
		embeddedStorageManager.shutdown();
		System.out.println(">>MS shut!!");
	}

}