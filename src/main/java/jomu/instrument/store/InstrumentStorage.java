package jomu.instrument.store;

import java.util.ArrayList;
import java.util.List;

import jomu.instrument.workspace.tonemap.ToneMap;
import one.microstream.integrations.spring.boot.types.Storage;

@Storage
public class InstrumentStorage {
	private final List<ToneMap> toneMapList = new ArrayList<>();

	public List<ToneMap> findAll() {
		return this.toneMapList;
	}

	public void removeAll() {
		this.toneMapList.clear();
	}

	public void add(final ToneMap toneMap) {
		this.toneMapList.add(toneMap);
	}
}
