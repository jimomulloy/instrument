package jomu.instrument.control;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.stereotype.Component;

import jomu.instrument.Organ;

@ApplicationScoped
@Component
public class Controller implements Organ {

	ParameterManager parameterManager = new ParameterManager();

	public ParameterManager getParameterManager() {
		return parameterManager;
	}

	@Override
	public void initialise() {
		parameterManager.initialise();
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}
}
