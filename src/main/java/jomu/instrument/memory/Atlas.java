package jomu.instrument.memory;

import java.util.HashMap;
import java.util.Map;

import jomu.instrument.tonemap.ToneMap;

public class Atlas {

	Map<String, ToneMap> toneMaps = new HashMap<>();

	public Map<String, ToneMap> getToneMaps() {
		return toneMaps;
	}

	public void setToneMaps(Map<String, ToneMap> toneMaps) {
		this.toneMaps = toneMaps;
	}

	public void putToneMap(String source, ToneMap toneMap) {
		toneMaps.put(source, toneMap);
	}

	public ToneMap getToneMap(String source) {
		return toneMaps.get(source);
	}
}
