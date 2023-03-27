package jomu.instrument.command.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.ObjectStorage;

@ApplicationScoped
public class FileStorage implements ObjectStorage {

	@Inject
	ParameterManager parameterManager;

//	@Override
//	public OutputStream createOutputStream(String name) {
//		FileOutputStream stream = null; // Place to store the stream reference
//		try {
//			stream = new FileOutputStream(name);
//		} catch (FileNotFoundException e) {
//			File file = new File(name);
//			try {
//				file.createNewFile();
//				stream = new FileOutputStream(name);
//			} catch (IOException ex) {
//				// TODO Auto-generated catch block
//				ex.printStackTrace();
//			}
//		}
//		return stream;
//	}

	@Override
	public InputStream read(String name) {
		InputStream stream;
		try {
			stream = new FileInputStream(name);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return stream;
	}

	@Override
	public void write(String name, File file) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getBasePath() {
		return System.getProperty("user.home");
	}

	@Override
	public Map<String, String> getMetaData(String name) {
		return new HashMap<>();
	}

	@Override
	public void clearStore(String name) {
	}

}
