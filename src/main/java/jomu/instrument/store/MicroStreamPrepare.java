package jomu.instrument.store;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.stereotype.Component;

import jomu.instrument.Instrument;
import one.microstream.integrations.spring.boot.types.config.StorageManagerInitializer;
import one.microstream.storage.types.StorageManager;

@ApplicationScoped
@Component
public class MicroStreamPrepare implements StorageManagerInitializer {

	@Override
	public void initialize(StorageManager storageManager) {

		InstrumentStorage root = (InstrumentStorage) storageManager.root();
		if (root.getParameters().isEmpty()) {
			try {
				Instrument.getInstance().getController().getParameterManager().reset();
				root.setParameters(Instrument.getInstance().getController().getParameterManager().getParameters());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Instrument.getInstance().getController().getParameterManager().setParameters(root.getParameters());
		}
	}
}
