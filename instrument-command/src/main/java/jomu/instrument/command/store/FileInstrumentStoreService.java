package jomu.instrument.command.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentException;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.workspace.tonemap.ToneMap;

@ApplicationScoped
public class FileInstrumentStoreService implements InstrumentStoreService {

	final List<ToneMap> toneMapList = new ArrayList<>();
	Properties parameters = new Properties();

	String root = "testing";

	public void initialise() {
		try {
			Instrument.getInstance().getController().getParameterManager().reset();
		} catch (IOException e) {
			throw new InstrumentException("InstrumentStorage initialise exception: " + e.getMessage(), e);
		}
		this.setParameters(Instrument.getInstance().getController().getParameterManager().getParameters());
		System.out.println(">>IS init");
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
