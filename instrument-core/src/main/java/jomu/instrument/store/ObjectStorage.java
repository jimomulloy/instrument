package jomu.instrument.store;

import java.io.InputStream;
import java.io.OutputStream;

public interface ObjectStorage {
	
	void write(String name, OutputStream stream);
	
	public InputStream read(String name);
	
}
