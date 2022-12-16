package jomu.instrument.store;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.workspace.tonemap.ToneMap;

@ApplicationScoped
public class InstrumentStoreService {

	@Inject
	private InstrumentStorage instrumentStorage;

	public InstrumentStoreService(InstrumentStorage instrumentStorage) {
		this.instrumentStorage = instrumentStorage;
	}

	public void addToneMap(ToneMap toneMap) {
		instrumentStorage.add(toneMap);
	}

	public void deleteToneMaps() {
		instrumentStorage.removeAll();
	}

	public List<ToneMap> findToneMaps() {
		return instrumentStorage.findAll();
	}
}