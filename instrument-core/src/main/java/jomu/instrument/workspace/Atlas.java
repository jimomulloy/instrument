package jomu.instrument.workspace;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.FrameCache;
import jomu.instrument.workspace.tonemap.ToneMap;

@ApplicationScoped
public class Atlas {

	private static final Logger LOG = Logger.getLogger(Atlas.class.getName());

	private static final int MAX_SAVED_TONEMAPS = 4;

	@Inject
	ParameterManager parameterManager;

	@Inject
	FrameCache frameCache;

	Map<String, ToneMap> toneMaps = new ConcurrentHashMap<>();

	List<ToneMap> savedToneMaps = new CopyOnWriteArrayList<>();

	Map<String, CalibrationMap> calibrationMaps = new ConcurrentHashMap<>();

	public FrameCache getFrameCache() {
		return frameCache;
	}

	public boolean hasToneMap(String key) {
		return toneMaps.containsKey(key);
	}

	public ToneMap getToneMap(String key) {
		if (!toneMaps.containsKey(key)) {
			putToneMap(key, new ToneMap(key, parameterManager, frameCache));
		}
		return toneMaps.get(key);
	}

	public ToneMap getSavedToneMap(int index) {
		if (savedToneMaps.size() > index) {
			return savedToneMaps.get(index);
		}
		return null;
	}

	public void saveToneMap(ToneMap toneMap) {
		if (savedToneMaps.size() >= MAX_SAVED_TONEMAPS) {
			ToneMap tm = savedToneMaps.remove(0);
			tm.clear();
		}
		savedToneMaps.add(toneMap);
	}

	public boolean hasCalibrationMap(String key) {
		return calibrationMaps.containsKey(key);
	}

	public CalibrationMap getCalibrationMap(String key) {
		if (!calibrationMaps.containsKey(key)) {
			putCalibrationMap(key, new CalibrationMap(key));
		}
		return calibrationMaps.get(key);
	}

	private void putCalibrationMap(String key, CalibrationMap calibrationMap) {
		calibrationMaps.put(key, calibrationMap);
	}

	public Map<String, ToneMap> getToneMaps() {
		return toneMaps;
	}

	public void putToneMap(String key, ToneMap toneMap) {
		toneMaps.put(key, toneMap);
	}

	public void setToneMaps(Map<String, ToneMap> toneMaps) {
		this.toneMaps = toneMaps;
	}

	public void removeToneMap(String key) {
		this.toneMaps.remove(key);
	}

	public void removeCalibrationMap(String key) {
		this.calibrationMaps.remove(key);
	}

	public void removeMapsByStreamId(String streamId) {
		for (String key : toneMaps.keySet()) {
			if (streamId.equals(key.substring(key.indexOf(":") + 1))) {
				ToneMap tm = this.toneMaps.remove(key);
				if (!savedToneMaps.contains(tm)) {
					tm.clear();
				}
			}
		}
		for (String key : calibrationMaps.keySet()) {
			if (streamId.equals(key.substring(key.indexOf(":") + 1))) {
				this.calibrationMaps.remove(key);
			}
		}
	}

	public void clear() {
		frameCache.clear();
		toneMaps = new ConcurrentHashMap<>();
		calibrationMaps = new ConcurrentHashMap<>();
	}

	public void commitMapsByStreamId(String streamId, int sequence) {
		for (String key : toneMaps.keySet()) {
			if (streamId.equals(key.substring(key.indexOf(":") + 1))) {
				ToneMap tm = this.toneMaps.get(key);
				tm.commit(sequence);
			}
		}
	}

	public void clearOldMaps(String streamId, Double time) {
		for (String key : toneMaps.keySet()) {
			if (streamId.equals(key.substring(key.indexOf(":") + 1))) {
				ToneMap tm = getToneMap(key);
				tm.clear();
			}
		}
		frameCache.clear();
	}
}
