package jomu.instrument.adapter.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import jomu.instrument.Instrument;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.workspace.tonemap.ToneMap;

@ApplicationScoped
@Alternative
@io.quarkus.arc.Priority(1)
public class AwsInstrumentStoreService implements InstrumentStoreService {

	private static final Logger LOG = Logger.getLogger(AwsInstrumentStoreService.class.getName());

	final List<ToneMap> toneMapList = new ArrayList<>();
	Properties parameters = new Properties();

	String root = "testing";

	public void initialise() {
		LOG.severe(">>AwsInstrumentStoreService initialise");
		Instrument.getInstance().getController().getParameterManager().reset();
		this.setParameters(Instrument.getInstance().getController().getParameterManager().getParameters());
		LOG.severe(">>AwsInstrumentStoreService initialised");

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
