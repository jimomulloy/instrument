package jomu.instrument.store;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public interface ObjectStorage {

	InputStream read(String name);

	void write(String name, File file);

	String getBasePath();

	Map<String, String> getMetaData(String name);

	void clearStore(String name);
}
