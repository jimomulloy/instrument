package jomu.instrument.organs;

import java.util.concurrent.ConcurrentHashMap;

public class Controller implements Organ {

	private ConcurrentHashMap<String, String> parameters = new ConcurrentHashMap<>();

	public String getParameter(String key) {
		return parameters.get(key);
	}

	@Override
	public void initialise() {
	}

	public String putParameter(String key, String parameter) {
		return parameters.get(key);
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
