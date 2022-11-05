package jomu.instrument.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jomu.instrument.world.tonemap.ToneMap;

public class Atlas {

	Map<String, ToneMap> toneMaps = new ConcurrentHashMap<>();

	public ToneMap getToneMap(String key) {
		if (!toneMaps.containsKey(key)) {
			toneMaps.put(key, new ToneMap());
		}
		return toneMaps.get(key);
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
}