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
	private static String PARAMETER_CONFIG_VALIDATION_FILE = "parameter-validation.properties";

	Properties parameters = new Properties();
	ParameterValidator parameterValidator = new ParameterValidator();

	public void initialise() {
	}

	public void reset() throws FileNotFoundException, IOException {
		InputStream is = getClass().getClassLoader()
				.getResourceAsStream(PARAMETER_CONFIG_FILE_PREFIX + "." + PARAMETER_CONFIG_FILE_POSTFIX);
		parameters.load(is);
		InputStream isv = getClass().getClassLoader().getResourceAsStream(PARAMETER_CONFIG_VALIDATION_FILE);
		parameterValidator.load(isv);
	}

	public String getParameter(String key) {
		return parameters.getProperty(key);
	}

	public boolean hasParameter(String key) {
		return parameters.getProperty(key) != null;
	}

	public void setParameter(String key, String value) {
		if (parameterValidator.validate(key)) {
			parameters.setProperty(key, value);
		}
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
		String value = getParameter(key);
		if (value != null) {
			try {
				return Integer.parseInt(value.trim());
			} catch (Exception e) {
				return 0;
			}
		} else {
			return 0;
		}
	}

	public boolean getBooleanParameter(String key) {
		String value = getParameter(key);
		if (value != null) {
			try {
				return Boolean.parseBoolean(value.trim());
			} catch (Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	public float getFloatParameter(String key) {
		String value = getParameter(key);
		if (value != null) {
			try {
				return Float.parseFloat(value);
			} catch (Exception e) {
				return 0;
			}
		} else {
			return 0;
		}
	}

	public double getDoubleParameter(String key) {
		String value = getParameter(key);
		if (value != null) {
			try {
				return Double.parseDouble(value.trim());
			} catch (Exception e) {
				return 0;
			}
		} else {
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

	static class ParameterValidator {

		Properties validatorProperties = new Properties();

		public boolean validate(String name) {
			String value = validatorProperties.getProperty(name);
			if (value != null) {
				try {
					return true;
				} catch (Exception e) {
					return true;
				}
			} else {
				return true;
			}
		}

		public void load(InputStream is) {
			try {
				validatorProperties.load(is);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
