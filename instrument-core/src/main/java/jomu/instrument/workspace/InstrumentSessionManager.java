package jomu.instrument.workspace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.store.InstrumentSession;

@ApplicationScoped
public class InstrumentSessionManager {

	private static final Logger LOG = Logger.getLogger(Atlas.class.getName());

	Map<String, InstrumentSession> sessions = new ConcurrentHashMap<>();

	InstrumentSession currentSession;

	public InstrumentSession getInstrumentSession(String id) {
		if (!sessions.containsKey(id)) {
			putInstrumentSession(id, new InstrumentSession(id));
		}
		currentSession = sessions.get(id);
		return currentSession;
	}

	public InstrumentSession getCurrentSession() {
		if (currentSession == null) {
			return getInstrumentSession("default");
		}
		return currentSession;
	}

	public Map<String, InstrumentSession> getSessions() {
		return sessions;
	}

	public void putInstrumentSession(String key, InstrumentSession InstrumentSession) {
		sessions.put(key, InstrumentSession);
	}

	public void setSessions(Map<String, InstrumentSession> sessions) {
		this.sessions = sessions;
	}

	public void removeInstrumentSession(String key) {
		this.sessions.remove(key);
	}

	public void removeSessionsByStreamId(String streamId) {
		for (String key : sessions.keySet()) {
			if (streamId.equals(key.substring(key.indexOf(":") + 1))) {
				this.sessions.remove(key);
			}
		}
	}
}
