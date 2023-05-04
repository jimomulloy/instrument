package jomu.instrument.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import jomu.instrument.Instrument;
import jomu.instrument.workspace.tonemap.ToneMap;

@ApplicationScoped
@Alternative
@io.quarkus.arc.Priority(0)
public class FileInstrumentStoreService implements InstrumentStoreService {

	final List<ToneMap> toneMapList = new ArrayList<>();
	Properties parameters = new Properties();

	String root = "testing";

	public void initialise() {
		Instrument.getInstance().getController().getParameterManager().reset();
		this.setParameters(Instrument.getInstance().getController().getParameterManager().getParameters());
	}

	public void addToneMap(ToneMap toneMap) {
		this.toneMapList.add(toneMap);
	}

	public void setParameters(Properties parameters) {
		Properties copyParams = new Properties();
		copyParams.putAll(parameters);
		this.parameters = copyParams;
	}

	public void deleteToneMaps() {
		this.toneMapList.clear();
	}

	public List<ToneMap> findToneMaps() {
		return this.toneMapList;
	}

	public Properties getParameters() {
		Properties copyParams = new Properties();
		copyParams.putAll(parameters);
		return copyParams;
	}
}
