package jomu.instrument.workspace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ToneMap;

@ApplicationScoped
public class Atlas {

	@Inject
	ParameterManager parameterManager;

	private static final Logger LOG = Logger.getLogger(Atlas.class.getName());

	Map<String, ToneMap> toneMaps = new ConcurrentHashMap<>();

	Map<String, CalibrationMap> calibrationMaps = new ConcurrentHashMap<>();

	public boolean hasToneMap(String key) {
		return toneMaps.containsKey(key);
	}

	public ToneMap getToneMap(String key) {
		if (!toneMaps.containsKey(key)) {
			putToneMap(key, new ToneMap(key, parameterManager));
		}
		return toneMaps.get(key);
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
				this.toneMaps.remove(key);
			}
		}
		for (String key : calibrationMaps.keySet()) {
			if (streamId.equals(key.substring(key.indexOf(":") + 1))) {
				this.calibrationMaps.remove(key);
			}
		}
	}

	public void clearOldMaps(String streamId, Double time) {
		for (String key : toneMaps.keySet()) {
			if (streamId.equals(key.substring(key.indexOf(":") + 1))) {
				ToneMap tm = getToneMap(key);
				tm.clearOldFrames(time);
			}
		}
	}
}
