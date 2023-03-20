package jomu.instrument.desktop.monitor.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.store.ObjectStorage;

@ApplicationScoped
public class FileStorage implements ObjectStorage {

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
	public void write(String name, File file) {
		// TODO Auto-generated method stub
	}

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
	public String getBasePath() {
		return System.getProperty("user.home");
	}
}
