package jomu.instrument.organs;

import java.util.concurrent.ConcurrentHashMap;

public class Controller {

	private ConcurrentHashMap<String, String> parameters = new ConcurrentHashMap<>();

	public String getParameter(String key) {
		return parameters.get(key);
	}

	public void initialise() {
	}

	public String putParameter(String key, String parameter) {
		return parameters.get(key);
	}

	public void start() {
	}
}
