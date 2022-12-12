package jomu.instrument.control;

import java.io.IOException;

import jomu.instrument.Organ;

public class Controller implements Organ {

	private ParameterManager parameterManager = new ParameterManager();

	public ParameterManager getParameterManager() {
		return parameterManager;
	}

	@Override
	public void initialise() {
		try {
			parameterManager.initialise();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}
}
