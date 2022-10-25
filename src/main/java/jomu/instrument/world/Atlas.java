package jomu.instrument.world;

import java.util.HashMap;
import java.util.Map;

import jomu.instrument.world.tonemap.ToneMap;

public class Atlas {

	Map<String, ToneMap> toneMaps = new HashMap<>();

	public ToneMap getToneMap(String source) {
		return toneMaps.get(source);
	}

	public Map<String, ToneMap> getToneMaps() {
		return toneMaps;
	}

	public void putToneMap(String source, ToneMap toneMap) {
		toneMaps.put(source, toneMap);
	}

	public void setToneMaps(Map<String, ToneMap> toneMaps) {
		this.toneMaps = toneMaps;
	}
}
