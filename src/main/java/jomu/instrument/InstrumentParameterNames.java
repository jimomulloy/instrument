package jomu.instrument;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InstrumentParameterNames {
	public static final String PERCEPTION_HEARING_MINIMUM_FREQUENCY_CENTS = "perception.hearing.minimumFrequencyInCents";
	public static final String PERCEPTION_HEARING_MAXIMUM_FREQUENCY_CENTS = "perception.hearing.maximumFrequencyInCents";
	public static final String PERCEPTION_HEARING_AUDIO_FEATURE_INTERVAL = "perception.hearing.auditFeatureInterval";
	public static final String PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE = "perception.hearing.defaultSampleRate";
	public static final String PERCEPTION_HEARING_DEFAULT_WINDOW = "perception.hearing.defaultWindow";
	public static final String PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH = "perception.hearing.noiseFloorMedianFilterLenth";
	public static final String PERCEPTION_HEARING_NOISE_FLOOR_FACTOR = "perception.hearing.noiseFloorFactor";
	public static final String PERCEPTION_HEARING_NUMBER_PEAKS = "perception.hearing.numberOfSpectralPeaks";
	public static final String PERCEPTION_HEARING_MINIMUM_PEAK_SIZE = "perception.hearing.minPeakSize";
	public static final String PERCEPTION_HEARING_AUDIO_LOWPASS = "perception.hearing.audioLowPass";
	public static final String PERCEPTION_HEARING_AUDIO_HIGHPASS = "perception.hearing.audioHighPass";

	public static final String PERCEPTION_HEARING_CQ_LOW_THRESHOLD = "perception.hearing.cq.lowThreshold";
	public static final String PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR = "perception.hearing.cq.thresholdFactor";
	public static final String PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM = "perception.hearing.cq.signalMinimum";
	public static final String PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD = "perception.hearing.cq.normaliseThreshold";
	public static final String PERCEPTION_HEARING_CQ_DECIBEL_LEVEL = "perception.hearing.cq.decibelLevel";
	public static final String PERCEPTION_HEARING_CQ_COMPRESSION = "perception.hearing.cq.compression";
	public static final String PERCEPTION_HEARING_CQ_SWITCH_COMPRESS = "perception.hearing.cq.compressionSwitch";
	public static final String PERCEPTION_HEARING_CQ_SWITCH_SQUARE = "perception.hearing.cq.squareSwitch";
	public static final String PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD = "perception.hearing.cq.lowThresholdSwitch";
	public static final String PERCEPTION_HEARING_CQ_SWITCH_DECIBEL = "perception.hearing.cq.decibelSwitch";
	public static final String PERCEPTION_HEARING_CQ_SWITCH_NORMALISE = "perception.hearing.cq.normaliseSwitch";

	public static final String PERCEPTION_HEARING_TUNER_THRESHOLD_FACTOR = "perception.hearing.tuner.thresholdFactor";
	public static final String PERCEPTION_HEARING_TUNER_THRESHOLD_MINIMUM = "perception.hearing.tuner.signalMinimum";
	public static final String PERCEPTION_HEARING_SWITCH_TUNER = "perception.hearing.tunerSwitch";
	public static final String PERCEPTION_HEARING_SWITCH_PEAKS = "perception.hearing.peaksSwitch";

	public static final String PERCEPTION_HEARING_CHROMA_NORMALISE_THRESHOLD = "perception.hearing.chroma.normaliseThreshold";
	public static final String PERCEPTION_HEARING_CHROMA_SMOOTH_FACTOR = "perception.hearing.chroma.smoothFactor";
	public static final String PERCEPTION_HEARING_CHROMA_DOWNSAMPLE_FACTOR = "perception.hearing.chroma.downSampleFactor";

	public static final String PERCEPTION_HEARING_BEATS_THRESHOLD = "perception.hearing.beats.threshold";
	public static final String PERCEPTION_HEARING_BEATS_SENSITIVITY = "perception.hearing.beats.sensitivity";

	public static final String PERCEPTION_HEARING_ONSET_THRESHOLD = "perception.hearing.onset.threshold";
	public static final String PERCEPTION_HEARING_ONSET_INTERVAL = " perception.hearing.onset.interval";

	public static final String AUDIO_TUNER_FORMANT_FACTOR = "audio.tuner.formantFactor";
	public static final String AUDIO_TUNER_FORMANT_HIGH = "audio.tuner.formantHighSetting";
	public static final String AUDIO_TUNER_FORMANT_LOW = "audio.tuner.formantLowSetting";
	public static final String AUDIO_TUNER_FORMANT_MIDDLE = "audio.tuner.formantMiddleSetting";
	public static final String AUDIO_TUNER_N1_SETTING = "audio.tuner.n1Setting";
	public static final String AUDIO_TUNER_N2_SETTING = "audio.tuner.n2Setting";
	public static final String AUDIO_TUNER_N3_SETTING = "audio.tuner.n3Setting";
	public static final String AUDIO_TUNER_N4_SETTING = "audio.tuner.n4Setting";
	public static final String AUDIO_TUNER_N5_SETTING = "audio.tuner.n5Setting";
	public static final String AUDIO_TUNER_N1_SWITCH = "audio.tuner.n1Switch";
	public static final String AUDIO_TUNER_N2_SWITCH = "audio.tuner.n2Switch";
	public static final String AUDIO_TUNER_N3_SWITCH = "audio.tuner.n3Switch";
	public static final String AUDIO_TUNER_NORMALISE_THRESHOLD = "audio.tuner.normalizeThreshold";
	public static final String AUDIO_TUNER_NORMALISE_TROUGH = "audio.tuner.normalizeTrough";
	public static final String AUDIO_TUNER_NORMALISE_SETTING = "audio.tuner.normalizeSetting";
	public static final String AUDIO_TUNER_NOTE_HIGH = "audio.tuner.noteHigh";
	public static final String AUDIO_TUNER_NOTE_LOW = "audio.tuner.noteLow";
	public static final String AUDIO_TUNER_NOTE_MAX_DURATION = "audio.tuner.noteMaxDuration";
	public static final String AUDIO_TUNER_NOTE_MIN_DURATION = "audio.tuner.noteMinDuration";
	public static final String AUDIO_TUNER_NOTE_SUSTAIN = "audio.tuner.noteSustain";
	public static final String AUDIO_TUNER_PEAK_SWITCH = "audio.tuner.peakSwitch";
	public static final String AUDIO_TUNER_PITCH_HIGH = "audio.tuner.pitchHigh";
	public static final String AUDIO_TUNER_PITCH_LOW = "audio.tuner.pitchLow";
	public static final String AUDIO_TUNER_FORMANT_HIGH_FREQUENCY = "audio.tuner.formantHighFreq";
	public static final String AUDIO_TUNER_FORMANT_LOW_FREQUENCY = "audio.tuner.formantLowFreq";
	public static final String AUDIO_TUNER_FORMANT_MIDDLE_FREQUENCY = "audio.tuner.formantMidFreq";
	public static final String STORAGE_DIRECTORY = "storage.directory";

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
