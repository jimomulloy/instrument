package jomu.instrument.desktop.store;

import java.util.List;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jomu.instrument.desktop.store.microstream.InstrumentStorage;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.workspace.tonemap.ToneMap;

@ApplicationScoped
@Alternative
@jakarta.annotation.Priority(1)
public class DesktopInstrumentStoreService implements InstrumentStoreService {

	@Inject
	InstrumentStorage instrumentStorage;

	public void initialise() {
		instrumentStorage.initialise(true);
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