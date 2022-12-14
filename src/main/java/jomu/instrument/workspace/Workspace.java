package jomu.instrument.workspace;

import jomu.instrument.Instrument;
import jomu.instrument.control.ParameterManager;

public class Workspace {

	private Atlas atlas = new Atlas();
	private ParameterManager parameterManager;

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
