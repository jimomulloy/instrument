package jomu.instrument.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jomu.instrument.workspace.tonemap.ToneMap;
import one.microstream.integrations.spring.boot.types.Storage;

@Storage
//@ApplicationScoped
//@Component
public class InstrumentStorage {

	final List<ToneMap> toneMapList = new ArrayList<>();
	Properties parameters = new Properties();

	public List<ToneMap> findAllToneMaps() {
		return this.toneMapList;
	}

	public void removeAllToneMaps() {
		this.toneMapList.clear();
	}

	public void addToneMap(final ToneMap toneMap) {
		this.toneMapList.add(toneMap);
	}

	public void setParameters(final Properties parameters) {
		this.parameters = parameters;
	}

	public Properties getParameters() {
		return parameters;
	}
}
