package jomu.instrument.organs;

import java.util.concurrent.ConcurrentHashMap;

public class Controller {

	private ConcurrentHashMap<String, String> parameters = new ConcurrentHashMap<>();

	public void initialise() {
	}

	public void start() {
	}

	public String getParameter(String key) {
		return parameters.get(key);
	}

	public String putParameter(String key, String parameter) {
		return parameters.get(key);
	}
}
