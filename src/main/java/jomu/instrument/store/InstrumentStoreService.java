package jomu.instrument.store;

import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import jomu.instrument.store.microstream.InstrumentStorage;
import jomu.instrument.workspace.tonemap.ToneMap;
//import one.microstream.storage.types.StorageManager;

@ApplicationScoped
@Component
public class InstrumentStoreService {

	@Inject
	InstrumentStorage instrumentStorage;

	public void initialise() {
		instrumentStorage.initialise();
	}

	// @Store(root = true)
	public void addToneMap(ToneMap toneMap) {
		instrumentStorage.addToneMap(toneMap);
	}

	// @Store(root = true)
	public void setParameters(Properties parameters) {
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

	// TODO @PreDestroy
	public void preDestroy() {
		instrumentStorage.shutdown();
	}

}