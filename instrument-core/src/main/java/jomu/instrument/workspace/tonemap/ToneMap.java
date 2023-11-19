package jomu.instrument.workspace.tonemap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;

/**
 * This class is the main centre of control of program flow for the ToneMap.
 * This manages the data held in internal structures that define the "map"
 * including classes ToneMapMatrix, TimeSet and PitchSet. Functions include
 * Loading of the Map from the Audio data, Processing of the Map through the
 * Tuner functions to produce MIDI sequences and Saving and Opening of the data
 * objects in Serialised form
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMap {

	@Override
	public String toString() {
		return "ToneMap [key=" + key + "]";
	}

	private static final Logger LOG = Logger.getLogger(ToneMap.class.getName());

	CopyOnWriteArrayList<Integer> frameIndex = new CopyOnWriteArrayList<>();

	String key;

	NoteTracker noteTracker;

	ToneSynthesiser toneSynthesiser;

	ToneMapStatistics statistics = new ToneMapStatistics();

	double amplitudeThreshold = 0;

	Map<Integer, ToneMapStatistics> statisticsBands = new HashMap<>();

	ParameterManager parameterManager;

	FrameCache frameCache;

	public ToneMap(String key, ParameterManager parameterManager, FrameCache frameCache) {
		this.key = key;
		this.parameterManager = parameterManager;
		this.frameCache = frameCache;
		this.frameIndex = new CopyOnWriteArrayList<>();
		this.noteTracker = new NoteTracker(this);
		this.toneSynthesiser = new ToneSynthesiser(this);
	}

	public NoteTracker getNoteTracker() {
		return noteTracker;
	}

	public ToneSynthesiser getToneSynthesiser() {
		return toneSynthesiser;
	}

	public ParameterManager getParameterManager() {
		return parameterManager;
	}

	public ToneMapStatistics getStatistics() {
		return statistics;
	}

	public Map<Integer, ToneMapStatistics> getStatisticsBands() {
		return statisticsBands;
	}

	public ToneTimeFrame addTimeFrame(ToneTimeFrame toneTimeFrame) {
		toneTimeFrame.setToneMap(this);
		int indexTime = buildTimeIndex(toneTimeFrame.getStartTime());
		frameCache.put(getFrameKey(indexTime), toneTimeFrame);
		frameIndex.addIfAbsent(indexTime);
		return toneTimeFrame;
	}

	private int buildTimeIndex(double time) {
		return (int) Math.floor(time * 1000);
	}

	private String getFrameKey(int indexTime) {
		return getKey() + ":" + indexTime;
	}

	public final static String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

	public final static String buildToneMapKey(String tmType, String streamId) {
		return tmType + ":" + streamId;
	}

	public String getKey() {
		return key;
	}

	public String getStreamId() {
		return key.substring(key.indexOf(":") + 1);
	}

	/**
	 * Clear current ToneMap objects after Reset
	 */
	public void clear() {
		clearOldFrames(Double.MAX_VALUE);
		frameIndex = new CopyOnWriteArrayList<>();
		statistics = new ToneMapStatistics();
		noteTracker = new NoteTracker(this);
		toneSynthesiser = new ToneSynthesiser(this);
	}

	public void deleteTimeFrame() {
		if (frameIndex.size() > 0) {
			int fk = frameIndex.get(0);
			frameCache.remove(getFrameKey(fk));
			frameIndex.remove(fk);
		}
	}

	public void deleteTimeFrame(double time) {
		int indexTime = buildTimeIndex(time);
		if (frameIndex.contains(indexTime)) {
			int fk = frameIndex.get(frameIndex.indexOf(indexTime));
			frameCache.remove(getFrameKey(fk));
			// frameIndex.remove(fk);
		}
	}

	public ToneTimeFrame getNextTimeFrame(double time) {
		int indexTime = buildTimeIndex(time);
		if (frameIndex.contains(indexTime)) {
			int index = frameIndex.indexOf(indexTime);
			if (index < frameIndex.size() - 1) {
				int fk = frameIndex.get(index + 1);
				Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
				if (result.isPresent()) {
					return result.get();
				}
			}
		}
		return null;
	}

	public ToneTimeFrame getPreviousTimeFrame() {
		if (frameIndex.size() > 1) {
			int fk = frameIndex.get(frameIndex.size() - 2);
			Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
			if (result.isPresent()) {
				return result.get();
			}
		}
		return null;
	}

	public ToneTimeFrame getPreviousTimeFrame(double time) {
		int indexTime = buildTimeIndex(time);
		if (frameIndex.contains(indexTime)) {
			int index = frameIndex.indexOf(indexTime);
			if (index > 0) {
				int fk = frameIndex.get(index - 1);
				Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
				if (result.isPresent()) {
					return result.get();
				}
			}
		}
		return null;
	}

	public ToneTimeFrame getTimeFrame() {
		return getLastTimeFrame();
	}

	public ToneTimeFrame getFirstTimeFrame() {
		if (!frameIndex.isEmpty()) {
			int fk = frameIndex.get(0);
			Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
			if (result.isPresent()) {
				return result.get();
			}
		}
		return null;
	}

	public ToneTimeFrame getLastTimeFrame() {
		if (!frameIndex.isEmpty()) {
			int fk = frameIndex.get(frameIndex.size() - 1);
			Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
			if (result.isPresent()) {
				return result.get();
			}
		}
		return null;
	}

	public ToneTimeFrame getTimeFrame(double time) {
		int indexTime = buildTimeIndex(time);
		if (frameIndex.contains(indexTime)) {
			int index = frameIndex.indexOf(indexTime);
			int fk = frameIndex.get(index);
			Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
			if (result.isPresent()) {
				return result.get();
			}
		}
		return null;
	}

	public ToneTimeFrame getTimeFrame(int sequence) {
		if (frameIndex.size() >= sequence) {
			int fk = frameIndex.get(sequence - 1);
			Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
			if (result.isPresent()) {
				return result.get();
			}
		}
		return null;
	}

	public ToneTimeFrame[] getTimeFramesFrom(double time) {
		int indexTime = buildTimeIndex(time);
		List<ToneTimeFrame> tailMap = new ArrayList<>();
		for (int fk : frameIndex) {
			if (fk >= indexTime) {
				Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
				if (result.isPresent()) {
					tailMap.add(result.get());
				}
			}
		}
		return tailMap.toArray(new ToneTimeFrame[tailMap.size()]);
	}

	public ToneTimeFrame[] getTimeFramesTo(double time) {
		int indexTime = buildTimeIndex(time);
		List<ToneTimeFrame> tMap = new ArrayList<>();
		for (int fk : frameIndex) {
			if (fk <= indexTime) {
				Optional<ToneTimeFrame> result = frameCache.get(getFrameKey(fk));
				if (result.isPresent()) {
					tMap.add(result.get());
				}
			}
		}
		return tMap.toArray(new ToneTimeFrame[tMap.size()]);
	}

	public int getNumberOfTimeFrames() {
		return frameIndex.size();
	}

	public void initialise() {
		frameIndex = new CopyOnWriteArrayList<>();
	}

	public void clearOldFrames(double frameStartTime) {
		ToneTimeFrame[] ttfs = getTimeFramesTo(frameStartTime);
		for (ToneTimeFrame ttf : ttfs) {
			deleteTimeFrame(ttf.getStartTime());
		}
	}

	public void updateStatistics(ToneTimeFrame frame) {

		LOG.finer(">>TM Update stats: " + frame.getStartTime());

		ToneTimeFrame prevFrame = getPreviousTimeFrame(frame.getStartTime());
		ToneTimeFrame[] prevFrames = new ToneTimeFrame[0];
		if (prevFrame != null) {
			prevFrames = getTimeFramesTo(prevFrame.getStartTime());
		}
		double frameMax = frame.getStatistics().max;
		double frameMin = frame.getStatistics().min;
		double frameSum = frame.getStatistics().sum;
		boolean frameIsSilent = frame.isSilent();

		statistics.sum += frameSum;
		if (statistics.max < frameMax) {
			statistics.max = frameMax;
		}
		if (statistics.min > frameMin) {
			statistics.min = frameMin;
		}

		if (prevFrames.length > 0) {
			statistics.mean = ((statistics.mean * prevFrames.length) + frameSum) / (prevFrames.length + 1);
		} else {
			statistics.mean = frameSum;
		}

		if (!frameIsSilent) {
			LOG.finer(">>TM Update stats NON SILENT FRAME: " + frame.getStartTime() + ", " + frameSum + ", "
					+ statistics.mean);
			double sum = 0;
			double mean = 0;
			int count = 0;
			for (ToneTimeFrame pf : prevFrames) {
				if (!pf.isSilent) {
					sum += pf.getStatistics().sum;
					count++;
				}
			}
			sum += frame.getStatistics().sum;
			count++;
			mean = sum / count;

			sum = 0;
			for (ToneTimeFrame pf : prevFrames) {
				if (!pf.isSilent) {
					sum += (pf.getStatistics().sum - mean) * (pf.getStatistics().sum - mean);
				}
			}
			sum += (frame.getStatistics().sum - mean) * (frame.getStatistics().sum - mean);

			statistics.variance = sum / count;

			LOG.finer(">>TM Update stats NON SILENT FRAME variance: " + statistics.variance);
		}

		ToneMapStatistics statisticsBand = null;
		for (Entry<Integer, ToneMapStatistics> entry : frame.getStatisticsBands()
				.entrySet()) {
			int bandIndex = entry.getKey();
			ToneMapStatistics frameStatisticsBand = entry.getValue();
			if (!statisticsBands.containsKey(bandIndex)) {
				statisticsBand = new ToneMapStatistics();
				statisticsBands.put(bandIndex, statisticsBand);
				LOG.finer(">>TM Update stats PUT stats band: " + frame.getStartTime());
			} else {
				statisticsBand = statisticsBands.get(bandIndex);
			}
			if (statisticsBand.max < frameStatisticsBand.max) {
				statisticsBand.max = frameStatisticsBand.max;
			}
			if (statisticsBand.min > frameStatisticsBand.min) {
				statisticsBand.min = frameStatisticsBand.min;
			}
			statisticsBand.mean = ((statisticsBand.mean * prevFrames.length) + frameStatisticsBand.sum)
					/ (prevFrames.length + 1);

			statisticsBand.sum += frameStatisticsBand.sum;

			if (!frameIsSilent) {
				double sum = 0;
				double mean = 0;
				int count = 0;
				ToneMapStatistics pfStatisticsBand = null;
				for (ToneTimeFrame pf : prevFrames) {
					if (!pf.isSilent) {
						pfStatisticsBand = pf.getStatisticsBands()
								.get(bandIndex);
						sum += pfStatisticsBand.sum;
						count++;
					}
				}
				sum += frameStatisticsBand.sum;
				count++;
				mean = sum / count;

				sum = 0;
				for (ToneTimeFrame pf : prevFrames) {
					if (!pf.isSilent) {
						pfStatisticsBand = pf.getStatisticsBands()
								.get(bandIndex);
						sum += (pfStatisticsBand.sum - mean) * (pfStatisticsBand.sum - mean);
					}
				}
				sum += (frameStatisticsBand.sum - mean) * (frameStatisticsBand.sum - mean);

				statisticsBand.variance = sum / count;
				LOG.finer(
						">>TM Update BAND stats NON SILENT FRAME variance: " + statisticsBand.variance + ", "
								+ bandIndex);
			}
		}

		LOG.finer(">>TM Updated stats: " + frame.getStartTime());

	}

	public double getAmplitudeThreshold() {
		return amplitudeThreshold;
	}

	public void setAmplitudeThreshold(double amplitudeThreshold) {
		this.amplitudeThreshold = amplitudeThreshold;
	}

	public void commit(int sequence) {
		if (frameIndex.size() >= sequence) {
			frameCache.backup(getFrameKey(frameIndex.get(sequence - 1)));
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ToneMap other = (ToneMap) obj;
		return Objects.equals(key, other.key);
	}
}