package jomu.instrument.workspace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.workspace.tonemap.ToneMap;

@ApplicationScoped
public class Atlas {

	private static final Logger LOG = Logger.getLogger(Atlas.class.getName());

	Map<String, ToneMap> toneMaps = new ConcurrentHashMap<>();

	public ToneMap getToneMap(String key) {
		if (!toneMaps.containsKey(key)) {
			putToneMap(key, new ToneMap(key));
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

	public void removeToneMap(String key) {
		this.toneMaps.remove(key);
	}

	public void removeToneMapsByStreamId(String streamId) {
		for (String key : toneMaps.keySet()) {
			if (streamId.equals(key.substring(key.indexOf(":") + 1))) {
				this.toneMaps.remove(key);
			}
		}
	}
}