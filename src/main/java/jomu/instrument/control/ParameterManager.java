package jomu.instrument.control;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ParameterManager {

	private static String PARAMETER_CONFIG_FILE = "app.properties";

	private Properties parameters = new Properties();

	public void initialise() throws FileNotFoundException, IOException {
		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		parameters.load(new FileInputStream(rootPath + PARAMETER_CONFIG_FILE));
	}

	public String getParameter(String key) {
		return parameters.getProperty(key);
	}

	public void setParameter(String key, String value) {
		parameters.setProperty(key, value);
	}

	public int getIntParameter(String key) {
		String value = getParameter(key).trim();
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return 0;
		}
	}

	public float getFloatParameter(String key) {
		String value = getParameter(key);
		try {
			return Float.parseFloat(value);
		} catch (Exception e) {
			return 0;
		}
	}

	public double getDoubleParameter(String key) {
		String value = getParameter(key);
		try {
			return Double.parseDouble(value);
		} catch (Exception e) {
			return 0;
		}
	}
}
