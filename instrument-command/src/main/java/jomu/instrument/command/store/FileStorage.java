package jomu.instrument.command.store;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.store.ObjectStorage;

@ApplicationScoped
public class FileStorage implements ObjectStorage {

	@Override
	public void write(String name, OutputStream stream) {
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

	
}
