package jomu.instrument.ai;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import jakarta.inject.Inject;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;

public class ParameterSearchModel {

	@Inject
	ParameterManager parameterManager;

	Map<String, ParameterSearchRecord> parameterRecords;

	String inputFile;

	public void initialise() throws FileNotFoundException, IOException {
		Properties parameters = new Properties();
		String paramFile = parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_PARAMETER_FILE);
		InputStream isv = getClass().getClassLoader().getResourceAsStream(paramFile);
		parameters.load(isv);

	}
}
