package jomu.instrument.adapter.aws;

import java.io.InputStream;
import java.io.OutputStream;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.store.ObjectStorage;

@ApplicationScoped
public class AwsAdapterObjectStorage implements ObjectStorage {

	@Override
	public void write(String name, OutputStream stream) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InputStream read(String name) {
		// TODO Auto-generated method stub
		return null;
	}


}
