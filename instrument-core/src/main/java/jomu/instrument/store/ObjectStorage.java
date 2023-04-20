package jomu.instrument.store;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public interface ObjectStorage {

	InputStream read(String name);

	void write(String name, File file);

	void writeString(String name, String contents);

	String readString(String name);

	String getBasePath();

	Map<String, String> getMetaData(String name);

	void clearStore(String name);

	void delete(String name);

	String[] listStore(String string);
}
