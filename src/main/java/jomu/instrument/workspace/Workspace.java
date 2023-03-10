package jomu.instrument.workspace;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.Instrument;
import jomu.instrument.control.ParameterManager;

@ApplicationScoped
public class Workspace {

	Atlas atlas = new Atlas();
	ParameterManager parameterManager;

	public Atlas getAtlas() {
		return atlas;
	}

	public void initialise() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
	}

	public void setAtlas(Atlas atlas) {
		this.atlas = atlas;
	}

	public void start() {
		// TODO Auto-generated method stub

	}

}
