package jomu.instrument.workspace.tonemap;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.store.Storage;

@ApplicationScoped
public class FrameCache {

	private static final Logger LOG = Logger.getLogger(FrameCache.class.getName());

	public static final Long DEFAULT_CACHE_TIMEOUT = 600000L;

	private static final int MAX_CACHE_SIZE = 100000;

	@Inject
	Storage storeage;

	ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

	ConcurrentHashMap<String, CacheValue> cacheMap;

	Long cacheTimeout;

	public FrameCache() {
		this(DEFAULT_CACHE_TIMEOUT);
	}

	public FrameCache(Long cacheTimeout) {
		this.cacheTimeout = cacheTimeout;
		this.clear();
	}

	public void clean() {
		for (String key : this.getExpiredKeys()) {
			this.remove(key);
		}
	}

	public boolean containsKey(String key) {
		return this.cacheMap.containsKey(key);
	}

	protected Set<String> getExpiredKeys() {
		return this.cacheMap.keySet().parallelStream().filter(this::isExpired).collect(Collectors.toSet());
	}

	protected boolean isExpired(String key) {
		LocalDateTime expirationDateTime = this.cacheMap.get(key).getCreatedAt().plus(this.cacheTimeout,
				ChronoUnit.MILLIS);
		return LocalDateTime.now().isAfter(expirationDateTime);
	}

	public void clear() {
		this.cacheMap = new ConcurrentHashMap<>();
		this.queue = new ConcurrentLinkedQueue<>();
	}

	public Optional<ToneTimeFrame> get(String key) {
		// this.clean();
		Optional<ToneTimeFrame> result = Optional.ofNullable(this.cacheMap.get(key)).map(CacheValue::getValue);
		if (result.isEmpty()) {
			// LOG.severe(">>FC GET: " + this.cacheMap.size() + ", " + this.queue.size() +
			// ", " + key);
			return this.storeage.getFrameStore().read(key);
		}
		return result;
	}

	public void put(String key, ToneTimeFrame value) {
		// LOG.severe(">>FC PUT: " + this.cacheMap.size() + ", " + this.queue.size() +
		// ", " + key);
		this.cacheMap.put(key, this.createCacheValue(value));
		this.queue.add(key);
		if (this.cacheMap.size() > MAX_CACHE_SIZE) {
			String fk = this.queue.remove();
			CacheValue oldCV = remove(fk);
			if (oldCV != null) {
				this.storeage.getFrameStore().write(fk, oldCV.getValue());
			} else {
				LOG.severe(">>FC PUT Remove NULL: " + this.cacheMap.size() + ", " + this.queue.size() + ", " + fk + ", "
						+ key);
			}
		}
	}

	protected CacheValue createCacheValue(ToneTimeFrame value) {
		LocalDateTime now = LocalDateTime.now();
		return new CacheValue() {
			@Override
			public ToneTimeFrame getValue() {
				return value;
			}

			@Override
			public LocalDateTime getCreatedAt() {
				return now;
			}
		};
	}

	public CacheValue remove(String key) {
		return this.cacheMap.remove(key);
	}

	protected interface CacheValue {
		ToneTimeFrame getValue();

		LocalDateTime getCreatedAt();
	}

	public int getSize() {
		return this.cacheMap.size();
	}

}