package jomu.instrument.store;

import java.util.List;
import java.util.Properties;
import jomu.instrument.workspace.tonemap.ToneMap;

public interface InstrumentStoreService {

	void initialise();

	void addToneMap(ToneMap toneMap);

	void setParameters(Properties parameters);

	void deleteToneMaps();
	
	List<ToneMap> findToneMaps();
	
	Properties getParameters();

}