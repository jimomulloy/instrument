package jomu.instrument.control;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jomu.instrument.InstrumentException;

@ApplicationScoped
public class ParameterManager {

	private static final Logger LOG = Logger.getLogger(ParameterManager.class.getName());

	private static String PARAMETER_CONFIG_FILE_PREFIX = "instrument";
	private static String PARAMETER_CONFIG_FILE_POSTFIX = "properties";
	private static String PARAMETER_CONFIG_VALIDATION_FILE = "parameter-validation.properties";

	Properties parameters = new Properties();
	ParameterValidator parameterValidator = new ParameterValidator();

	public void onStartup(@Observes StartupEvent startupEvent) {
		reset();
	}

	public void initialise() {
	}

	public void reset() {
		try {
			LOG.severe("ParameterManager resetting..");
			InputStream is = getClass().getClassLoader()
					.getResourceAsStream(PARAMETER_CONFIG_FILE_PREFIX + "." + PARAMETER_CONFIG_FILE_POSTFIX);
			LOG.severe(">>ParameterManager is: " + PARAMETER_CONFIG_FILE_PREFIX + "." + PARAMETER_CONFIG_FILE_POSTFIX);
			parameters.load(is);
			InputStream isv = getClass().getClassLoader().getResourceAsStream(PARAMETER_CONFIG_VALIDATION_FILE);
			parameterValidator.load(isv);
			validateAll();
			LOG.severe("ParameterManager reset");
		} catch (Exception ex) {
			throw new InstrumentException("ParameterManager reset exception: " + ex.getMessage(), ex);
		}
	}

	private void validateAll() {
		for (Entry<Object, Object> entry : parameters.entrySet()) {
			if (!parameterValidator.validate((String) entry.getKey(), (String) entry.getValue())) {
				throw new InstrumentException("ParameterManager validateAll invalid parameter, key: " + entry.getKey()
						+ ", value: " + entry.getValue());
			}
		}
	}

	public String getParameter(String key) {
		return parameters.getProperty(key);
	}

	public boolean hasParameter(String key) {
		return parameters.getProperty(key) != null;
	}

	public String setParameter(String key, String value) {
		if (parameterValidator.validate(key, value)) {
			parameters.setProperty(key, value);
			return value;
		} else {
			LOG.severe("Invalid parameter: " + key + ", " + value);
			return parameters.getProperty(key);
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
			mergeProperties(styleParameters);
			setParameter(InstrumentParameterNames.CONTROL_PARAMETER_STYLE, paramStyle);
			LOG.finer(">>Loaded :" + PARAMETER_CONFIG_FILE_PREFIX + "-" + paramStyle + "."
					+ PARAMETER_CONFIG_FILE_POSTFIX);
		} else {
			LOG.finer(">>ReLoaded :" + PARAMETER_CONFIG_FILE_PREFIX + "." + PARAMETER_CONFIG_FILE_POSTFIX);
		}
	}

	public void mergeProperties(Properties newParameters) {
		for (Entry<Object, Object> entry : parameters.entrySet()) {
			if (newParameters.containsKey(entry.getKey())) {
				if (!parameterValidator.validate((String) entry.getKey(), (String) entry.getValue())) {
					LOG.severe("ParameterManager mergeProperties invalid parameter, key: " + entry.getKey()
							+ ", value: " + entry.getValue());
				} else {
					parameters.put(entry.getKey(), newParameters.get(entry.getKey()));
				}
			}
		}
	}

	static class ParameterValidator {

		Properties validatorProperties = new Properties();

		public boolean validate(String name, String value) {
			String validateValue = validatorProperties.getProperty(name);
			if (validateValue != null) {
				try {
					if (isRange(validateValue)) {
						return validateRange(validateValue, value);
					} else if (isOptions(validateValue)) {
						return validateOptions(validateValue, value);
					} else {
						return validateSingleton(validateValue, value);
					}
				} catch (Exception e) {
					return true;
				}
			} else {
				return true;
			}
		}

		private boolean isOptions(String validateValue) {
			if (validateValue.contains(",")) {
				return true;
			}
			return false;
		}

		private boolean validateSingleton(String validateValue, String value) {
			if (validateValue.equals(value)) {
				return true;
			}
			return false;
		}

		private boolean validateOptions(String validateValue, String value) {
			String[] options = validateValue.split(",");
			for (String option : options) {
				if (option.equals(value)) {
					return true;
				}
			}
			return false;
		}

		private boolean validateRange(String validateValue, String value) {
			String[] range = validateValue.split(">");
			if (range.length != 2) {
				return true;
			}
			if (!isNumber(range[0]) || !isNumber(range[1])) {
				return true;
			}
			if (!isNumber(value)) {
				return false;
			}
			double low = getNumber(range[0]);
			double high = getNumber(range[1]);
			double nValue = getNumber(value);
			if (nValue >= low && nValue <= high) {
				return true;
			}
			return false;
		}

		private boolean isRange(String validateValue) {
			if (validateValue.contains(">")) {
				return true;
			}
			return false;
		}

		public void load(InputStream is) {
			try {
				validatorProperties.load(is);
			} catch (IOException e) {
				throw new InstrumentException("ParameterManager validatorProperties load: " + e.getMessage(), e);
			}
		}

		private boolean isNumber(String value) {
			if (value != null) {
				try {
					Double.parseDouble(value.trim());
					return true;
				} catch (Exception e) {
					return false;
				}
			} else {
				return false;
			}
		}

		private double getNumber(String value) {
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
	}
}
