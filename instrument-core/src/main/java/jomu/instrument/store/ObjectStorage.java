package jomu.instrument.store;

import java.io.File;
import java.io.InputStream;

public interface ObjectStorage {

	public InputStream read(String name);

	void write(String name, File file);

	String getBasePath();
}
