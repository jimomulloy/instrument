package jomu.instrument.workspace.tonemap;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.store.Storage;

@ApplicationScoped
public class FrameCache {

	private static final Logger LOG = Logger.getLogger(FrameCache.class.getName());

	public static final Long DEFAULT_CACHE_TIMEOUT = 600000L;

	private static final int MAX_CACHE_SIZE = 100000;

	@Inject
	Storage storage;

	private BlockingQueue<Object> bq;

	ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

	ConcurrentHashMap<String, CacheValue> cacheMap;

	Long cacheTimeout;

	Thread queueConsumerThread;

	public FrameCache() {
		this(DEFAULT_CACHE_TIMEOUT);
	}

	public FrameCache(Long cacheTimeout) {
		this.cacheTimeout = cacheTimeout;
		this.cacheMap = new ConcurrentHashMap<>();
		this.queue = new ConcurrentLinkedQueue<>();
		bq = new LinkedBlockingQueue<>();
		// TODO LOOM Thread.startVirtualThread(new QueueConsumer());
		queueConsumerThread = new Thread(new QueueConsumer(),
				"Thread-FrameCache-Consumer-" + System.currentTimeMillis());
		queueConsumerThread.start();
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
		return this.cacheMap.keySet()
				.parallelStream()
				.filter(this::isExpired)
				.collect(Collectors.toSet());
	}

	protected boolean isExpired(String key) {
		LocalDateTime expirationDateTime = this.cacheMap.get(key)
				.getCreatedAt()
				.plus(this.cacheTimeout, ChronoUnit.MILLIS);
		return LocalDateTime.now()
				.isAfter(expirationDateTime);
	}

	public void clear() {
		this.cacheMap = new ConcurrentHashMap<>();
		this.queue = new ConcurrentLinkedQueue<>();
		this.storage.getFrameStore()
				.clear();
	}

	public Optional<ToneTimeFrame> get(String key) {
		// this.clean();
		Optional<ToneTimeFrame> result = Optional.ofNullable(this.cacheMap.get(key))
				.map(CacheValue::getValue);
		if (result.isEmpty()) {
			LOG.finer(">>FC GET empty: " + this.cacheMap.size() + ", " + this.queue.size() + ", " + key);
			// Optional<ToneTimeFrame> v = this.storeage.getFrameStore().read(key);
			// if (v.isPresent()) {
			// put(key, v.get());
			// return v;
			// } else {
			// LOG.severe(">>FC GET MISSING FILE: " + this.cacheMap.size() + ", " +
			// this.queue.size() + ", " + key);
			// }
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
			Optional<ToneTimeFrame> oldTm = get(fk);
			if (oldTm.isPresent()) {
				bq.add(new FrameCacheMessage(fk, oldTm.get()));
			} else {
				LOG.finer(">>FC PUT Remove NULL: " + this.cacheMap.size() + ", " + this.queue.size() + ", " + fk
						+ ", " + key);
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

	public void backup(String key) {
		Optional<ToneTimeFrame> oldTm = get(key);
		if (oldTm.isPresent()) {
			bq.add(new FrameCacheMessage(key, oldTm.get()));
		} else {
			LOG.finer(">>FC backup NULL: " + this.cacheMap.size() + ", " + this.queue.size() + ", " + key);
		}
	}

	protected interface CacheValue {
		ToneTimeFrame getValue();

		LocalDateTime getCreatedAt();
	}

	public int getSize() {
		return this.cacheMap.size();
	}

	private class FrameCacheMessage {

		public String key;
		public ToneTimeFrame frame;

		public FrameCacheMessage(String key, ToneTimeFrame frame) {
			this.key = key;
			this.frame = frame;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(key);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FrameCacheMessage other = (FrameCacheMessage) obj;
			return Objects.equals(key, other.key);
		}
	}

	private class QueueConsumer implements Runnable {

		@Override
		public void run() {
			try {
				while (true) {
					FrameCacheMessage fcm = (FrameCacheMessage) bq.take();
					// FrameCache.this.storeage.getFrameStore().write(fcm.key, fcm.frame);
					remove(fcm.key);
					FrameCache.this.queue.remove(fcm.key);
				}
			} catch (InterruptedException e) {
				Thread.currentThread()
						.interrupt();
			}
		}
	}

}