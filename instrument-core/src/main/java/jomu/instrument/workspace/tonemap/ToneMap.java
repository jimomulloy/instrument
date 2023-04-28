package jomu.instrument.workspace.tonemap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;

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

	private static final Logger LOG = Logger.getLogger(ToneMap.class.getName());

	public boolean audioSwitch = false;

	public boolean midiSwitch = false;
	private ConcurrentSkipListMap<Double, ToneTimeFrame> toneMapStore = new ConcurrentSkipListMap<>();

	private String key;

	private NoteTracker noteTracker;

	private ToneSynthesiser toneSynthesiser;

	private ToneMapStatistics statistics = new ToneMapStatistics();

	private double amplitudeThreshold = 0;

	private Map<Integer, ToneMapStatistics> statisticsBands = new HashMap<>();

	public ToneMap(String key) {
		this.key = key;
		toneMapStore = new ConcurrentSkipListMap<>();
		noteTracker = new NoteTracker(this);
		toneSynthesiser = new ToneSynthesiser(this);
	}

	public NoteTracker getNoteTracker() {
		return noteTracker;
	}

	public ToneSynthesiser getToneSynthesiser() {
		return toneSynthesiser;
	}

	public ToneMapStatistics getStatistics() {
		return statistics;
	}

	public Map<Integer, ToneMapStatistics> getStatisticsBands() {
		return statisticsBands;
	}

	public ToneTimeFrame addTimeFrame(ToneTimeFrame toneTimeFrame) {
		toneMapStore.put(toneTimeFrame.getStartTime(), toneTimeFrame);
		return toneTimeFrame;
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

	/**
	 * Clear current ToneMap objects after Reset
	 */
	public void clear() {
		toneMapStore = new ConcurrentSkipListMap<>();
	}

	public void deleteTimeFrame() {
		toneMapStore.remove(toneMapStore.firstKey());
	}

	public void deleteTimeFrame(Double time) {
		toneMapStore.remove(time);
	}

	public ToneTimeFrame getNextTimeFrame(Double key) {
		Entry<Double, ToneTimeFrame> nextEntry = toneMapStore.higherEntry(key);
		if (nextEntry != null) {
			return nextEntry.getValue();
		} else {
			return null;
		}
	}

	public ToneTimeFrame getPreviousTimeFrame() {
		Entry<Double, ToneTimeFrame> previousEntry = toneMapStore.lowerEntry(toneMapStore.lastKey());
		if (previousEntry != null) {
			return previousEntry.getValue();
		} else {
			return null;
		}
	}

	public ToneTimeFrame getPreviousTimeFrame(Double key) {
		Entry<Double, ToneTimeFrame> previousEntry = toneMapStore.lowerEntry(key);
		if (previousEntry != null) {
			return previousEntry.getValue();
		} else {
			return null;
		}
	}

	public ToneTimeFrame getTimeFrame() {
		if (!toneMapStore.isEmpty()) {
			return toneMapStore.lastEntry().getValue();
		}
		return null;
	}

	public ToneTimeFrame getTimeFrame(Double key) {
		return toneMapStore.get(key);
	}

	public ToneTimeFrame getTimeFrame(int sequence) {
		NavigableSet<Double> keySet = toneMapStore.keySet();
		int counter = sequence;
		Iterator<Double> iterator = keySet.iterator();
		Double key = null;
		while (counter > 0 && iterator.hasNext()) {
			key = iterator.next();
			counter--;
		}
		if (key == null) {
			return null;
		}
		return toneMapStore.get(key);
	}

	public ToneTimeFrame[] getTimeFramesFrom(Double key) {
		Collection<ToneTimeFrame> tailMap = toneMapStore.tailMap(key).values();
		return tailMap.toArray(new ToneTimeFrame[tailMap.size()]);
	}

	public ToneTimeFrame[] getTimeFramesTo(Double key) {
		Collection<ToneTimeFrame> headMap = toneMapStore.headMap(key).values();
		return headMap.toArray(new ToneTimeFrame[headMap.size()]);
	}

	public void initialise() {
		toneMapStore = new ConcurrentSkipListMap<>();
	}

	public void clearOldFrames(Double time) {
		ToneTimeFrame[] ttfs = getTimeFramesTo(time);
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
		for (Entry<Integer, ToneMapStatistics> entry : frame.getStatisticsBands().entrySet()) {
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
						pfStatisticsBand = pf.getStatisticsBands().get(bandIndex);
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
						pfStatisticsBand = pf.getStatisticsBands().get(bandIndex);
						sum += (pfStatisticsBand.sum - mean) * (pfStatisticsBand.sum - mean);
					}
				}
				sum += (frameStatisticsBand.sum - mean) * (frameStatisticsBand.sum - mean);

				statisticsBand.variance = sum / count;
				LOG.finer(">>TM Update BAND stats NON SILENT FRAME variance: " + statisticsBand.variance + ", "
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
}