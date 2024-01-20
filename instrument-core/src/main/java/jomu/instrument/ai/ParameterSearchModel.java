package jomu.instrument.ai;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.ai.ParameterSearchScore.Note;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
public class ParameterSearchModel {

	private static final Logger LOG = Logger.getLogger(ParameterSearchModel.class.getName());

	@Inject
	ParameterManager parameterManager;

	@Inject
	Workspace workspace;

	@Inject
	Storage storage;

	Map<String, ParameterSearchDimension> dimensionMap;

	Map<Integer, ParameterSearchRecord> recordings;

	String sourceAudioFile;

	String targetMidiFile;

	int searchCount;

	int frameCount;

	int highScore;

	int searchThreshold;

	boolean searchCombinations;

	Map<Integer, Properties> propertyFrameMap;

	/**
	 * @return the highScore
	 */
	public int getHighScore() {
		return highScore;
	}

	public void initialise() throws FileNotFoundException, IOException {
		recordings = new HashMap<>();
		searchCount = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_SEARCH_COUNT);
		searchThreshold = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_SEARCH_THRESHOLD);
		searchCombinations = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_SEARCH_COMBINATIONS);
		String sourceFileResource = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_PARAMETER_DIMENSIONS_SOURCE);
		URL sourceFileUrl = getClass().getResource(sourceFileResource);
		sourceAudioFile = new File(sourceFileUrl.getFile()).getAbsolutePath();
		String targetFileResource = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_PARAMETER_DIMENSIONS_TARGET);
		URL targetFileUrl = getClass().getResource(targetFileResource);
		targetMidiFile = new File(targetFileUrl.getFile()).getAbsolutePath();
		loadDimensions(parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_PARAMETER_DIMENSIONS_FILE));
		reset();
	}

	private void loadDimensions(String dimensionsFile) throws IOException {
		Properties dimensions = new Properties();
		InputStream isv = getClass().getClassLoader().getResourceAsStream(dimensionsFile);
		dimensions.load(isv);
		dimensionMap = new HashMap<>();
		List<ParameterSearchDimension> dimensionList = new ArrayList<>();
		for (Entry<Object, Object> entry : dimensions.entrySet()) {
			ParameterSearchDimension psDimension = new ParameterSearchDimension();
			psDimension.name = entry.getKey().toString();
			psDimension.value = entry.getValue().toString();
			if (entry.getValue().toString().equalsIgnoreCase("b")) {
				psDimension.setBoolean(true);
			}
			dimensionMap.put(psDimension.name, psDimension);
			dimensionList.add(psDimension);
		}

		if (searchCombinations) {
			propertyFrameMap = new HashMap<>();
			Properties properties = new Properties();
			processDimensions(0, properties, dimensionList);
			searchCount = propertyFrameMap.size();
			System.out.println(">>Size of property frame map: " + propertyFrameMap.size());
		}
	}

	public void reset() throws FileNotFoundException, IOException {
		ParameterSearchRecord parameterSearchRecord = new ParameterSearchRecord();
		if (searchCombinations) {
			Properties propertyFrame = propertyFrameMap.get(frameCount);
			for (Entry<Object, Object> prop : propertyFrame.entrySet()) {
				parameterSearchRecord.parameterMap.put(prop.getKey().toString(), prop.getValue().toString());
			}
		} else {
			for (Entry<String, ParameterSearchDimension> entry : dimensionMap.entrySet()) {
				ParameterSearchDimension psDimension = entry.getValue();
				if (psDimension.isBoolean()) {
					Random r = new Random();
					boolean b = r.nextBoolean();
					parameterSearchRecord.parameterMap.put(psDimension.name, Boolean.toString(b));
				}
			}
		}
		recordings.put(frameCount, parameterSearchRecord);
		updateParameters();
		searchCount--;
		System.out.println(">>PSM update search count: " + searchCount);
	}

	public void score() throws Exception {
		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();

		String sourceMidiFile = instrumentSession.getOutputMidiFilePath();
		ParameterSearchScore parameterSearchScore = new ParameterSearchScore();
		int score = 0;

		LOG.severe(">>PSM score extract source: " + sourceMidiFile);
		Map<Integer, List<Note>> noteMap = parameterSearchScore.extractMidiNotes(sourceMidiFile, 1000.0 / 20.0);
		int noteCount = countNotes(noteMap);
		if (noteCount < 10) {
			LOG.severe(">>PSM score extract source size: " + noteCount);
			boolean[][] source = parameterSearchScore.buildMidiNoteArray(noteMap, 60, 140, 100);

			LOG.severe(">>PSM score extract target: " + targetMidiFile);
			noteMap = parameterSearchScore.extractMidiNotes(targetMidiFile, 1);
			noteCount = countNotes(noteMap);
			LOG.severe(">>PSM score extract target size: " + noteCount);
			boolean[][] target = parameterSearchScore.buildMidiNoteArray(noteMap, 60, 140, 100);

			score = parameterSearchScore.scoreMidiNoteArray(source, target);
			LOG.severe(">>PSM score: " + score + ", frame: " + frameCount + ", source: " + sourceMidiFile + ", target: "
					+ targetMidiFile);
		}

		ParameterSearchRecord parameterSearchRecord = recordings.get(frameCount);
		parameterSearchRecord.score = score;
		frameCount++;
		System.out.println(">>PSM !!! score: " + score + ", frame: " + frameCount);
		if (score > highScore && score > searchThreshold) {
			exportParameters();
			highScore = score;
			System.out.println(">>PSM !!! high score: " + score + ", frame: " + frameCount);
		}
	}

	private int countNotes(Map<Integer, List<Note>> noteMap) {
		int count = 0;
		for (List<Note> nl : noteMap.values()) {
			count += nl.size();
		}
		return count;
	}

	public void exportParameters() {
		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PROJECT_DIRECTORY))
				.toString();
		String exportFileName = folder + System.getProperty("file.separator") + "instrument-user.properties";
		try (FileOutputStream fs = new FileOutputStream(exportFileName)) {
			SortedStoreProperties ssp = new SortedStoreProperties();
			if (parameterManager.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_EXPORT_DELTA_SWITCH)) {
				try (InputStream is = getClass().getClassLoader().getResourceAsStream("instrument.properties");
						InputStream isc = getClass().getClassLoader()
								.getResourceAsStream("instrument-client.properties");) {
					Properties props = new Properties();
					props.load(is);
					Properties clientParameters = new Properties();
					clientParameters.load(isc);
					props.putAll(clientParameters);
					ssp.putAll(parameterManager.getDeltaParameters(props));
				} catch (IOException ex) {
					LOG.log(Level.SEVERE, "Export Parameters exception", ex);
				}
			} else {
				ssp.putAll(parameterManager.getParameters());
			}
			ssp.store(fs, null);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Export Parameters exception", ex);
		}
	}

	public void updateParameters() throws FileNotFoundException, IOException {
		parameterManager.reset();
		String styleFile = parameterManager
				.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_PARAMETER_STYLE_FILE);
		if (styleFile != null && !styleFile.isBlank()) {
			parameterManager.loadStyle(styleFile, false);
		} else {
			parameterManager.reset();
		}
		Properties searchParameters = new Properties();
		ParameterSearchRecord parameterSearchRecord = recordings.get(frameCount);
		System.out.print(">>Params: frame: " + frameCount);
		for (Entry<String, String> entry : parameterSearchRecord.parameterMap.entrySet()) {
			searchParameters.put(entry.getKey(), entry.getValue());
			System.out.print(" [" + entry.getKey() + " - " + entry.getValue() + "] ,");
		}
		System.out.println("");
		parameterManager.mergeProperties(searchParameters);
		parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_SEARCH_COUNT,
				Integer.toString(searchCount));
		parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY, "true");
		parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE, "true");
		parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE1_SWITCH, "true");
		parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE2_SWITCH, "true");
		parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE3_SWITCH, "true");
		parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE4_SWITCH, "true");
		parameterManager.setParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH, "true");
	}

	/**
	 * @return the parameterManager
	 */
	public ParameterManager getParameterManager() {
		return parameterManager;
	}

	/**
	 * @return the dimensionMap
	 */
	public Map<String, ParameterSearchDimension> getDimensionMap() {
		return dimensionMap;
	}

	/**
	 * @return the recordings
	 */
	public Map<Integer, ParameterSearchRecord> getRecordings() {
		return recordings;
	}

	/**
	 * @return the sourceAudioFile
	 */
	public String getSourceAudioFile() {
		return sourceAudioFile;
	}

	/**
	 * @return the targetMidiFile
	 */
	public String getTargetMidiFile() {
		return targetMidiFile;
	}

	/**
	 * @return the searchCount
	 */
	public int getSearchCount() {
		return searchCount;
	}

	private void processDimensions(int offset, Properties properties, List<ParameterSearchDimension> dimensionList) {
		ParameterSearchDimension dimension = dimensionList.get(offset);
		String[] values = dimension.getValue().split(",");
		for (String value : values) {
			Properties clonedProps = new Properties();
			clonedProps.putAll(properties);
			clonedProps.put(dimension.getName(), value);
			if (offset + 1 < dimensionList.size()) {
				processDimensions(offset + 1, clonedProps, dimensionList);
			} else {
				propertyFrameMap.put(propertyFrameMap.size(), clonedProps);
			}
		}
	}

	class SortedStoreProperties extends Properties {

		@Override
		public void store(OutputStream out, String comments) throws IOException {
			Properties sortedProps = new Properties() {
				@Override
				public Set<Map.Entry<Object, Object>> entrySet() {
					/*
					 * Using comparator to avoid the following exception on jdk >=9:
					 * java.lang.ClassCastException:
					 * java.base/java.util.concurrent.ConcurrentHashMap$MapEntry cannot be cast to
					 * java.base/java.lang.Comparable
					 */
					Set<Map.Entry<Object, Object>> sortedSet = new TreeSet<Map.Entry<Object, Object>>(
							new Comparator<Map.Entry<Object, Object>>() {
								@Override
								public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {
									return o1.getKey().toString().compareTo(o2.getKey().toString());
								}
							});
					sortedSet.addAll(super.entrySet());
					return sortedSet;
				}

				@Override
				public Set<Object> keySet() {
					return new TreeSet<Object>(super.keySet());
				}

				@Override
				public synchronized Enumeration<Object> keys() {
					return Collections.enumeration(new TreeSet<Object>(super.keySet()));
				}

			};
			sortedProps.putAll(this);
			sortedProps.store(out, comments);
		}
	}
}
