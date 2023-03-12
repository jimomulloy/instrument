package jomu.instrument.control;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ParameterManager {

	private static String PARAMETER_CONFIG_FILE = "instrument.properties";

	Properties parameters = new Properties();

	public void initialise() {
	}

	public void reset() throws FileNotFoundException, IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(PARAMETER_CONFIG_FILE);
		parameters.load(is);
	}

	public String getParameter(String key) {
		return parameters.getProperty(key);
	}

	public void setParameter(String key, String value) {
		parameters.setProperty(key, value);
	}

	public void setParameters(Properties parameters) {
		this.parameters = parameters;
	}

	public Properties getParameters() {
		return parameters;
	}

	public int getIntParameter(String key) {
		String value = getParameter(key).trim();
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return 0;
		}
	}

	public boolean getBooleanParameter(String key) {
		String value = getParameter(key).trim();
		try {
			return Boolean.parseBoolean(value);
		} catch (Exception e) {
			return false;
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