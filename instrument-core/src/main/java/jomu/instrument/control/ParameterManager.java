package jomu.instrument.control;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ParameterManager {

	private static final Logger LOG = Logger.getLogger(ParameterManager.class.getName());

	private static String PARAMETER_CONFIG_FILE_PREFIX = "instrument";
	private static String PARAMETER_CONFIG_FILE_POSTFIX = "properties";

	Properties parameters = new Properties();

	public void initialise() {
	}

	public void reset() throws FileNotFoundException, IOException {
		InputStream is = getClass().getClassLoader()
				.getResourceAsStream(PARAMETER_CONFIG_FILE_PREFIX + "." + PARAMETER_CONFIG_FILE_POSTFIX);
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

	public void overrideParameters(Properties overides) {
		parameters.putAll(overides);
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

	public void loadStyle(String paramStyle) throws FileNotFoundException, IOException {
		reset();
		Properties styleParameters = new Properties();
		InputStream is = null;
		if (paramStyle != null && !paramStyle.equals("default")) {
			is = getClass().getClassLoader().getResourceAsStream(
					PARAMETER_CONFIG_FILE_PREFIX + "-" + paramStyle + "." + PARAMETER_CONFIG_FILE_POSTFIX);
			try {
				styleParameters.load(is);
			} catch (IOException e) {
				LOG.finer(">>Error loading parameter styles :" + paramStyle);
				return;
			}
			parameters.putAll(styleParameters);
			setParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE, paramStyle);
			LOG.finer(">>Loaded :" + PARAMETER_CONFIG_FILE_PREFIX + "-" + paramStyle + "."
					+ PARAMETER_CONFIG_FILE_POSTFIX);
		} else {
			LOG.finer(">>ReLoaded :" + PARAMETER_CONFIG_FILE_PREFIX + "." + PARAMETER_CONFIG_FILE_POSTFIX);
		}
	}
}
