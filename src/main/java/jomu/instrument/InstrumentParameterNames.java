package jomu.instrument;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InstrumentParameterNames {
	public static final String PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS = "perception.hearing.minimumFrequencyInCents";
	public static final String PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS = "perception.hearing.maximumFrequencyInCents";
	public static final String PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL = "perception.hearing.auditFeatureInterval";
	public static final String PERCEPTION_HEARING_DEFAULT_WINDOW = "perception.hearing.defaultWindow";
	public static final String PERCEPTION_HEARING_CQ_LOW_THRESHOLD = "perception.hearing.cq.lowThreshold";
	public static final String PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR = "perception.hearing.cq.thresholdFactor";
	public static final String PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM = "perception.hearing.cq.signalMinimum";
	public static final String PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD = "perception.hearing.cq.normaliseThreshold";
	public static final String PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH = "perception.hearing.noiseFloorMedianFilterLenth";
	public static final String PERCEPTION_HEARING_NOISE_FLOOR_FACTOR = "perception.hearing.noiseFloorFactor";
	public static final String PERCEPTION_HEARING_NUMBER_PEAKS = "perception.hearing.numberOfSpectralPeaks";
	public static final String PERCEPTION_HEARING_MINIMUM_PEAK_SIZE = "perception.hearing.minPeakSize";

	private static final Map<String, String> displayNames = prepareMap();

	private static Map<String, String> prepareMap() {
		Map<String, String> hashMap = new HashMap<>();
		hashMap.put(PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS, "PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS");
		hashMap.put(PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS, "PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS");
		hashMap.put(PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL, "PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL");
		hashMap.put(PERCEPTION_HEARING_DEFAULT_WINDOW, "PERCEPTION_HEARING_DEFAULT_WINDOW");
		hashMap.put(PERCEPTION_HEARING_CQ_LOW_THRESHOLD, "PERCEPTION_HEARING_CQ_LOW_THRESHOLD");
		hashMap.put(PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR, "PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR");
		hashMap.put(PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM, "PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM");
		hashMap.put(PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD, "PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD");
		hashMap.put(PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH, "PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH");
		hashMap.put(PERCEPTION_HEARING_NUMBER_PEAKS, "PERCEPTION_HEARING_NUMBER_PEAKS");
		hashMap.put(PERCEPTION_HEARING_MINIMUM_PEAK_SIZE, "PERCEPTION_HEARING_MINIMUM_PEAK_SIZE");
		return hashMap;
	}

	public static Map<String, String> getDisplayNames() {
		return Collections.unmodifiableMap(displayNames);
	}
}
