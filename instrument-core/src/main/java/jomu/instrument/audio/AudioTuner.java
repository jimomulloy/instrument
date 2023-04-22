
package jomu.instrument.audio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jomu.instrument.Instrument;
import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.NoteListElement;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.NoteTimbre;
import jomu.instrument.workspace.tonemap.OvertoneSet;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioTuner implements ToneMapConstants {

	private static final Logger LOG = Logger.getLogger(AudioTuner.class.getName());

	private static final double MIN_AMPLITUDE = ToneTimeFrame.AMPLITUDE_FLOOR;
	private static final double HARMONIC_VARIANCE = 10;

	private int n1Setting = 10;
	private boolean n1Switch;
	private int n2Setting = 100;
	private boolean n2Switch;
	private int n3Setting = 100;
	private boolean n3Switch;
	private int n4Setting = 100;
	private boolean n4Switch;
	private boolean n5Switch;
	private boolean n6Switch;
	private boolean n7Switch;
	private int n5Setting = 100;
	private int n6Setting = 80;
	private int normalizeSetting = 100;
	private double normalizeThreshold = 0.01;
	private double normalizeTrough = 0.05;
	private double normalizePeak = 0.1;

	private int harmonic1Setting = 70;
	private int harmonic2Setting = 50;
	private int harmonic3Setting = 40;
	private int harmonic4Setting = 30;
	private int harmonic5Setting = 20;
	private int harmonic6Setting = 10;
	private double harmonicDriftFactor = 0.1;
	private boolean harmonicsOperator = false;
	private boolean harmonicsWeighting = false;
	private boolean harmonicsGuitar = false;
	private boolean harmonicAttenuateSwitch = true;
	private boolean harmonicAccumulateSwitch = true;

	private boolean noteScanAttenuateHarmonics;
	private boolean noteScanAttenuateUndertones;
	private boolean noteScanAttenuateSemitones;

	private int noteHigh = INIT_NOTE_HIGH;
	private int noteLow = INIT_NOTE_LOW;
	private int noteMaxDuration = INIT_NOTE_MAX_DURATION;
	private int noteMinDuration = INIT_NOTE_MIN_DURATION;

	private int noteSustain = INIT_NOTE_SUSTAIN;
	private boolean peakSwitch;
	private int pitchHigh = INIT_PITCH_HIGH;
	private int pitchLow = INIT_PITCH_LOW;

	private FormantSettings formantSettings = new FormantSettings();
	private FormantSettings formantSettingsBottom = new FormantSettings();
	private FormantSettings formantSettingsTop = new FormantSettings();

	// private NoteList noteList;
	private double[] harmonics;
	private OvertoneSet overtoneSet;
	private ParameterManager parameterManager;

	private int harmonicLowLimit;
	private int harmonicHighLimit;

	private double noteTimbreFrequencyRange;

	private double noteTimbreFrequencyRatio;

	private double noteTimbreMedianRange;

	private double noteTimbreMedianRatio;

	private boolean noteTimbreCQSwitch;

	private boolean noteTimbreNotateSwitch;

	private static int thresholdHysteresisBaseNote = 12;
	private static double[][] thresholdHysteresis = new double[][] { { 0.5, 0.3 }, { 0.6, 0.3 }, { 0.8, 0.4 },
			{ 1.0, 0.5 }, { 0.8, 0.3 }, { 0.6, 0.2 }, { 0.5, 0.1 }, { 0.3, 0.05 }, { 0.2, 0.05 }, { 0.1, 0.01 },
			{ 0.1, 0.01 }, { 0.05, 0.01 }, { 0.05, 0.01 } };

	/**
	 * TunerModel constructor. Instantiate TunerPanel
	 */
	public AudioTuner() {
		initParameters();
		initOvertoneSet();
		harmonics = overtoneSet.getHarmonics();
	}

	private void initParameters() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		harmonicLowLimit = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_LOW_NOTE);
		harmonicHighLimit = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HARMONIC_HIGH_NOTE);
		n1Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SETTING);
		n2Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SETTING);
		n3Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N3_SETTING);
		n4Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N4_SETTING);
		n5Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N5_SETTING);
		n6Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_N6_SETTING);
		n1Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SWITCH);
		n2Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SWITCH);
		n3Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N3_SWITCH);
		n4Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N4_SWITCH);
		n5Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N5_SWITCH);
		n6Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N6_SWITCH);
		n7Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N7_SWITCH);

		noteScanAttenuateHarmonics = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_HARMONICS);
		noteScanAttenuateUndertones = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_UNDERTONES);
		noteScanAttenuateSemitones = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SCAN_ATTENUATE_SEMITONES);

		harmonic1Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC1_SETTING);
		harmonic2Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC2_SETTING);
		harmonic3Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC3_SETTING);
		harmonic4Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC4_SETTING);
		harmonic5Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC5_SETTING);
		harmonic6Setting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC6_SETTING);
		harmonicDriftFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_DRIFT_FACTOR);
		harmonicsOperator = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_OPERATOR_SWITCH);
		harmonicsWeighting = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_WEIGHTING_SWITCH);
		harmonicsGuitar = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_GUITAR_SWITCH);
		harmonicAccumulateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_ACCUMULATE_SWITCH);
		harmonicAttenuateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_HARMONIC_ATTENUATE_SWITCH);

		normalizeThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_THRESHOLD);
		normalizeTrough = parameterManager.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_TROUGH);
		normalizePeak = parameterManager.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_PEAK);
		normalizeSetting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NORMALISE_SETTING);
		noteHigh = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_HIGH);
		noteLow = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_LOW);
		noteMaxDuration = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MAX_DURATION);
		noteMinDuration = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_MIN_DURATION);
		noteSustain = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_SUSTAIN);
		peakSwitch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_PEAK_SWITCH);
		pitchHigh = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_HIGH);
		pitchLow = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_PITCH_LOW);

		formantSettings.formantHigh = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH);
		formantSettings.formantLow = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW);
		formantSettings.formantMid = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE);
		formantSettings.formantRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_RANGE);
		formantSettings.formantFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_FACTOR);

		formantSettingsTop.formantHigh = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_HIGH);
		formantSettingsTop.formantLow = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_LOW);
		formantSettingsTop.formantMid = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_MIDDLE);
		formantSettingsTop.formantRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_RANGE);
		formantSettingsTop.formantFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_TOP_FORMANT_FACTOR);

		formantSettingsBottom.formantHigh = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_HIGH);
		formantSettingsBottom.formantLow = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_LOW);
		formantSettingsBottom.formantMid = parameterManager
				.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_MIDDLE);
		formantSettingsBottom.formantRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_RANGE);
		formantSettingsBottom.formantFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_BOTTOM_FORMANT_FACTOR);

		noteTimbreFrequencyRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RANGE);
		noteTimbreFrequencyRatio = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_FREQUENCY_RATIO);
		noteTimbreMedianRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RANGE);
		noteTimbreMedianRatio = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_MEDIAN_RATIO);
		noteTimbreCQSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_CQ_SWITCH);
		noteTimbreNotateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_NOTE_TIMBRE_NOTATE_SWITCH);

	}

	// Apply formant conversion to ToneMapMatrix element data
	public void applyFormants(ToneTimeFrame toneTimeFrame) {

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		for (ToneMapElement toneMapElement : ttfElements) {
			int note = pitchSet.getNote(toneMapElement.getPitchIndex());
			if (toneMapElement == null || toneMapElement.amplitude == -1)
				continue;
			applyFormant(formantSettings, toneMapElement, note);
			applyFormant(formantSettingsBottom, toneMapElement, note);
			applyFormant(formantSettingsTop, toneMapElement, note);
		}
		return;
	}

	public int getHighPitch() {
		return pitchHigh;
	}

	public int getLowPitch() {
		return pitchLow;
	}

	/**
	 * Normalise peak amplitudes
	 */
	public boolean processPeaks(ToneMap toneMap) {

		double amplitude, maxAmp = 0;
		int startPeak, endPeak;
		int index = 0;
		ToneMapElement thresholdElement = null;

		double troughAmp, lastAmp, lastPeakAmp;

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();

		maxAmp = MIN_AMPLITUDE;

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();
		for (ToneMapElement toneMapElement : ttfElements) {
			amplitude = toneMapElement.amplitude;

			if (amplitude > maxAmp) {
				maxAmp = amplitude;
				thresholdElement = toneMapElement;
			}
		}

		if (maxAmp <= normalizeThreshold) {
			return true;
		}

		if (n5Switch) {
			processOvertones(toneTimeFrame, false);
		}

		troughAmp = MIN_AMPLITUDE;
		startPeak = 0;
		endPeak = 0;
		lastAmp = 0;
		if (!n1Switch) {
			thresholdElement = null;
		}
		lastPeakAmp = 0;
		double peakFactor = (double) n2Setting / 100.0;
		double peakStepFactor = (double) n6Setting / 100.0;

		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		LOG.finer(">>Process peaks: " + normalizePeak + ", " + toneTimeFrame);
		for (ToneMapElement toneMapElement : ttfElements) {

			index = toneMapElement.getIndex();

			amplitude = toneMapElement.amplitude / maxAmp;

			if (amplitude >= lastAmp && amplitude > normalizePeak) {
				if (troughAmp <= normalizeTrough || (amplitude / troughAmp) > peakFactor) {
					if (amplitude > lastAmp) {
						if (startPeak != 0 && ((lastAmp / amplitude) < peakStepFactor)) {
							LOG.finer(">>Process peaks 1: " + amplitude + ", " + lastAmp + ", " + startPeak + ", "
									+ endPeak);
							processPeak(toneTimeFrame, startPeak, endPeak, troughAmp, thresholdElement, maxAmp);
						}
						startPeak = index;
						endPeak = index;
						lastPeakAmp = amplitude;
					} else if (startPeak != 0) {
						endPeak = index;
					}
				} else {
					lastPeakAmp = 0;
					startPeak = 0;
					endPeak = 0;
				}
			}

			if (amplitude < lastAmp) {
				if (startPeak != 0) {
					LOG.finer(">>Process peaks 2: " + amplitude + ", " + lastAmp + ", " + startPeak + ", " + endPeak);
					processPeak(toneTimeFrame, startPeak, endPeak, troughAmp, thresholdElement, maxAmp);
					if ((amplitude / lastAmp) < peakStepFactor) {
						startPeak = index;
						endPeak = index;
						lastPeakAmp = amplitude;
					} else {
						lastPeakAmp = 0;
						startPeak = 0;
						endPeak = 0;
					}
				}
			}
			if (amplitude < troughAmp) {
				troughAmp = amplitude;
			}
			lastAmp = amplitude;

		}

		if (startPeak != 0) {
			if (lastAmp <= normalizeTrough || (lastPeakAmp / lastAmp) > peakFactor) {
				LOG.finer(">>Process peaks 3: " + lastPeakAmp + ", " + lastAmp + ", " + startPeak + ", " + endPeak);
				// TODO Produces invalid peak at end ?? processPeak(toneTimeFrame, startPeak,
				// endPeak, troughAmp, thresholdElement, maxAmp);
			}
		}

		if (n6Switch) {
			processOvertones(toneTimeFrame, true);
		}

		Stream<ToneMapElement> ttfStream = Arrays.stream(ttfElements);

		List<ToneMapElement> topPeaks = ttfStream.filter(t1 -> t1.isPeak).sorted(new Comparator<ToneMapElement>() {
			public int compare(ToneMapElement t1, ToneMapElement t2) {
				return Double.valueOf(t2.amplitude).compareTo(Double.valueOf(t1.amplitude));
			}
		}).limit(n1Setting).map(t1 -> t1).collect(Collectors.toList());

		if (n3Switch) {
			toneTimeFrame.reset();
			for (ToneMapElement toneMapElement : ttfElements) {
				if (!topPeaks.contains(toneMapElement)) {
					toneMapElement.amplitude = MIN_AMPLITUDE;
					toneMapElement.isPeak = false;
				} else {
					toneMapElement.isPeak = true;
					LOG.finer(">>PEAK found: " + toneMapElement);
				}
			}
			toneTimeFrame.reset();
			LOG.finer(">>PEAK thresholds: " + toneTimeFrame.getHighThreshold() + ", " + toneTimeFrame.getLowThres());
		}

		return true;
	}

	/**
	 * Scan through ToneMapMatrix extracting MIDI note data into NoteList object
	 * Apply filtering and conversion processing on basis of Tuner Parameters
	 *
	 */
	public boolean noteScan(ToneMap toneMap, int sequence) {
		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();
		ToneTimeFrame previousToneTimeFrame = null;
		NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
		NoteStatus previousNoteStatus = noteStatus;
		if (sequence > 1) {
			previousToneTimeFrame = toneMap.getTimeFrame(sequence - 1);
			previousNoteStatus = previousToneTimeFrame.getNoteStatus();
		}

		NoteStatusElement noteStatusElement = null;
		NoteStatusElement previousNoteStatusElement = null;

		List<NoteListElement> processedNotes = new ArrayList<>();

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		// Iterate through ToneTimeFrame processing data elements to derive
		// NoteList
		// elements
		// Scan through each Pitch coordinate from Low to High limit.
		ToneMapElement[] ttfElements = toneTimeFrame.getElements();
		int note = 0;
		double time = 0;
		ToneMapElement previousToneMapElement = null;

		for (ToneMapElement toneMapElement : ttfElements) {
			note = pitchSet.getNote(toneMapElement.getPitchIndex());

			int thresholdHysteresisIndex = (note - thresholdHysteresisBaseNote) / 12;

			if (thresholdHysteresisIndex >= thresholdHysteresis.length) {
				LOG.finer(">>NOTE SCAN ERROR: " + note + ", " + toneTimeFrame.getStartTime());
				continue;
			}
			double noteOnThresholdWithHysteresis = noteLow * thresholdHysteresis[thresholdHysteresisIndex][0];
			double noteOffThresholdhWithHysteresis = noteLow * thresholdHysteresis[thresholdHysteresisIndex][1];
			double noteHighThresholdhWithHysteresis = noteHigh * thresholdHysteresis[thresholdHysteresisIndex][0];

			previousNoteStatusElement = previousNoteStatus.getNoteStatusElement(note);
			noteStatusElement = noteStatus.getNoteStatusElement(note);
			time = timeSet.getStartTime() * 1000.0;
			double timeIncrement = 100;
			if (sequence > 1) {
				previousToneMapElement = previousToneTimeFrame.getElement(toneMapElement.getIndex());
				timeIncrement = time - previousToneTimeFrame.getStartTime() * 1000.0;
			}

			double amplitude = toneMapElement.amplitude;

			noteStatusElement.state = previousNoteStatusElement.state;
			noteStatusElement.onTime = previousNoteStatusElement.onTime;
			noteStatusElement.offTime = previousNoteStatusElement.offTime;
			noteStatusElement.highFlag = previousNoteStatusElement.highFlag;
			noteStatusElement.note = previousNoteStatusElement.note;
			noteStatusElement.index = previousNoteStatusElement.index;
			noteStatusElement.isContinuation = previousNoteStatusElement.isContinuation;

			switch (previousNoteStatusElement.state) {
			case OFF:
				if (amplitude > noteOnThresholdWithHysteresis / 100.0) {
					noteStatusElement.state = ON;
					LOG.finer(">>>Note scan OFF - ON  seq: " + sequence + ", " + note + ", " + time + ", " + amplitude
							+ ", " + noteHighThresholdhWithHysteresis + ", " + noteOnThresholdWithHysteresis);
					noteStatusElement.onTime = time;
					noteStatusElement.offTime = time;
					if (amplitude >= noteHighThresholdhWithHysteresis / 100.0) {
						noteStatusElement.highFlag = true;
					}
					toneMapElement.noteState = START;
					LOG.finer(">>NOTE START 1: " + toneMapElement + ", " + timeSet.getStartTime() + ", " + note + ", "
							+ noteStatusElement.onTime);
				}
				break;

			case ON:
				if (amplitude <= noteOffThresholdhWithHysteresis / 100.0
						|| (time - noteStatusElement.onTime) > noteMaxDuration) {
					noteStatusElement.state = PENDING;
					LOG.finer(">>>Note scan ON - PENDING seq: " + sequence + ", " + note + ", " + time + ", "
							+ amplitude + ", " + noteOffThresholdhWithHysteresis);
					noteStatusElement.offTime = time;
				} else {
					LOG.finer(">>>Note scan ON - CONTINUE seq: " + sequence + ", " + note + ", " + time + ", "
							+ amplitude + ", " + noteOffThresholdhWithHysteresis);
					if (amplitude >= noteHighThresholdhWithHysteresis / 100.0) {
						noteStatusElement.highFlag = true;
					}
				}
				toneMapElement.noteState = ON;
				LOG.finer(">>NOTE ON 1: " + toneMapElement + ", " + timeSet.getStartTime() + ", " + note + ", "
						+ noteStatusElement.onTime);
				break;

			case PENDING:
				if (amplitude > noteOffThresholdhWithHysteresis / 100.0) {
					LOG.finer(">>>Note scan PENDING high amp: " + sequence + ", " + note + ", " + time + ", "
							+ amplitude + ", " + noteStatusElement.offTime + ", " + noteSustain + ", noteMaxDuration: "
							+ noteMaxDuration);
					if ((time - noteStatusElement.onTime) > noteMaxDuration) {
						noteStatusElement.state = CONTINUING;
						if (amplitude >= noteHighThresholdhWithHysteresis / 100.0) {
							noteStatusElement.highFlag = true;
						}
						LOG.finer(">>PROCESS NEW NOTE X - Note scan ON - PENDING NEW NOTE PARTIAL CONTINUING seq: "
								+ sequence + ", " + note + ", " + time + ", " + amplitude + ", "
								+ noteOffThresholdhWithHysteresis);
						// Process partial note here
						noteStatusElement.offTime = time;
						processNote(toneMap, noteStatusElement, processedNotes);
						noteStatusElement.state = ON;
						noteStatusElement.isContinuation = true;
						noteStatusElement.onTime = time;
						noteStatusElement.offTime = time;
						if (amplitude >= noteHighThresholdhWithHysteresis / 100.0) {
							noteStatusElement.highFlag = true;
						}
						toneMapElement.noteState = ON;
						LOG.finer(">>NOTE ON CONTINUING 2: " + toneMapElement + ", " + timeSet.getStartTime() + ", "
								+ note + ", " + noteStatusElement.onTime);

					} else if ((time - noteStatusElement.offTime) < (noteSustain)
							&& !noteStatusElement.isContinuation) {
						// back fill set notes ON
						LOG.finer(">>>Note scan PENDING high - PROCESS BACK FILL ON seq: " + sequence + ", " + note);
						backFillNotes(toneMap, note, toneMapElement.getIndex(), noteStatusElement.onTime, time, ON,
								processedNotes);
						LOG.finer(">>>Note scan PENDING - ON seq: " + sequence + ", " + note + ", " + time);
						noteStatusElement.state = ON;
						noteStatusElement.offTime = time;
						noteStatusElement.isContinuation = false;
						if (amplitude >= noteHighThresholdhWithHysteresis / 100.0) {
							noteStatusElement.highFlag = true;
						}
						toneMapElement.noteState = ON;
						LOG.finer(">>NOTE ON 3: " + toneMapElement + ", " + timeSet.getStartTime());
					} else if (shortNote(noteStatusElement, timeIncrement) && !noteStatusElement.isContinuation) {
						// back fill set notes OFF
						LOG.finer(">>>Note scan PENDING high - PROCESS BACK FILL OFF seq: " + sequence + ", " + note
								+ ", " + noteStatusElement.onTime + ", " + noteStatusElement.offTime + ", "
								+ noteMinDuration);
						backFillNotes(toneMap, note, toneMapElement.getIndex(), noteStatusElement.onTime, time, OFF,
								processedNotes);
						noteStatusElement.state = ON;
						noteStatusElement.isContinuation = false;
						noteStatusElement.onTime = time;
						noteStatusElement.offTime = time;
						if (amplitude >= noteHighThresholdhWithHysteresis / 100.0) {
							noteStatusElement.highFlag = true;
						}
						toneMapElement.noteState = START;
						LOG.finer(">>NOTE START 2: " + toneMapElement + ", " + timeSet.getStartTime() + ", " + note
								+ ", " + noteStatusElement.onTime);
					} else {
						LOG.finer(">>PROCESS NEW NOTE Y - Note scan PENDING - PROCESS NEW NOTE ON seq: " + sequence
								+ ", " + note);
						previousToneMapElement.noteState = OFF;
						previousNoteStatusElement.state = OFF;
						previousNoteStatusElement.isContinuation = false;
						// Process PREVIOUS note candididate note
						processNote(toneMap, previousNoteStatusElement, processedNotes);
						noteStatusElement.state = ON;
						noteStatusElement.isContinuation = false;
						noteStatusElement.onTime = time;
						noteStatusElement.offTime = time;
						if (amplitude >= noteHighThresholdhWithHysteresis / 100.0) {
							noteStatusElement.highFlag = true;
						}
						toneMapElement.noteState = START;
						LOG.finer(">>NOTE START 3: " + toneMapElement + ", " + timeSet.getStartTime() + ", " + note
								+ ", " + noteStatusElement.onTime);
					}
				} else {
					LOG.finer(">>>Note scan PENDING low amp: " + sequence + ", " + note + ", " + time + ", " + amplitude
							+ ", " + noteStatusElement.onTime + ", " + noteStatusElement.offTime + ", " + noteSustain
							+ ", " + noteMaxDuration + ", " + noteOffThresholdhWithHysteresis);
					LOG.finer(">>>Note scan PENDING - CONTINUE seq: " + sequence + ", note: " + note + ", sus: "
							+ noteSustain + ", maxDur: " + noteMaxDuration + ", time " + time + ", offTime: "
							+ noteStatusElement.offTime + ", onTime: " + noteStatusElement.onTime);
					if ((time - noteStatusElement.offTime) >= (noteSustain)
							|| (noteStatusElement.offTime - noteStatusElement.onTime) > noteMaxDuration) {
						if (shortNote(noteStatusElement, timeIncrement) && !noteStatusElement.isContinuation) {
							LOG.finer(
									">>>Note scan PENDING low - PROCESS BACK FILL OFF seq: " + sequence + ", " + note);
							// back fill set notes OFF
							backFillNotes(toneMap, note, toneMapElement.getIndex(), noteStatusElement.onTime, time, OFF,
									processedNotes);
							noteStatusElement.state = OFF;
							noteStatusElement.isContinuation = false;
							noteStatusElement.onTime = 0.0;
							noteStatusElement.offTime = 0.0;
							noteStatusElement.highFlag = false;
							toneMapElement.noteState = OFF;
						} else {
							previousToneMapElement.noteState = OFF;
							previousNoteStatusElement.state = OFF;
							// Process candidate note
							LOG.finer(">>>PROCESS NEW NOTE Z - Note scan PENDING low - PROCESS NEW NOTE OFF seq: "
									+ sequence + ", " + note);
							processNote(toneMap, previousNoteStatusElement, processedNotes);
							noteStatusElement.state = OFF;
							noteStatusElement.isContinuation = false;
							noteStatusElement.onTime = 0.0;
							noteStatusElement.offTime = 0.0;
							noteStatusElement.highFlag = false;
							toneMapElement.noteState = OFF;
						}

					}
				}

				break;

			default:
				break;
			}

		}

		if (n7Switch) {

			for (int i = 0; i < ttfElements.length; i++) {
				ToneMapElement toneMapElement = ttfElements[i];
				note = pitchSet.getNote(toneMapElement.getPitchIndex());
				noteStatusElement = noteStatus.getNoteStatusElement(note);
				if (noteStatusElement.state == ON) {
					if (i > 0) {
						LOG.finer(">>attenuateSemitone A: " + i + ", " + note);
						attenuateSemitone(toneMap, previousToneTimeFrame, ttfElements, i - 1, noteStatusElement,
								noteStatus, note - 1, processedNotes);
					}
					if (i < ttfElements.length - 1) {
						LOG.finer(">>attenuateSemitone B: " + i + ", " + note);
						attenuateSemitone(toneMap, previousToneTimeFrame, ttfElements, i + 1, noteStatusElement,
								noteStatus, note + 1, processedNotes);
					}
				}

			}

			LOG.finer(">>NOTESCAN POST PROCESS");
			for (NoteListElement processedNote : processedNotes) {
				LOG.severe(">>NOTESCAN POST PROCESS hasPendingHarmonics?: " + processedNote);
				if (!hasPendingHarmonics(toneMap, processedNote)) {
					LOG.severe(">>NOTESCAN POST PROCESS hasPendingHarmonics commitNote: " + processedNote);
					ToneTimeFrame[] timeFrames = toneMap.getTimeFramesFrom((processedNote.startTime / 1000.0));
					commitNote(timeFrames, processedNote);
				}
			}
		}
		return true;
	}

	private boolean shortNote(NoteStatusElement noteStatusElement, double timeIncrement) {
		if ((noteStatusElement.offTime - noteStatusElement.onTime) <= (noteMinDuration)) {
			return true;
		}
		return false;
	}

	private void attenuateSemitone(ToneMap toneMap, ToneTimeFrame previousToneTimeFrame, ToneMapElement[] ttfElements,
			int index, NoteStatusElement parentNoteStatusElement, NoteStatus noteStatus, int note,
			List<NoteListElement> processedNotes) {
		NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(note);
		ToneMapElement toneMapElement = ttfElements[index];
		if (noteStatusElement.state == PENDING && (noteStatusElement.onTime > parentNoteStatusElement.onTime)) {
			LOG.finer(">>attenuateSemitone X: " + index);
			if (index > 0 && previousToneTimeFrame != null) {
				LOG.finer(">>attenuateSemitone Y: " + index + ", " + note);
				NoteStatus previousNoteStatus = previousToneTimeFrame.getNoteStatus();
				NoteStatusElement previousNoteStatusElement = previousNoteStatus.getNoteStatusElement(note);
				attenuateSemitone(toneMap, toneMap.getPreviousTimeFrame(previousToneTimeFrame.getStartTime()),
						previousToneTimeFrame.getElements(), index - 1, previousNoteStatusElement, noteStatus, note - 1,
						processedNotes);
			}
			if (index < ttfElements.length - 1 && previousToneTimeFrame != null) {
				LOG.finer(">>attenuateSemitone Z: " + index + ", " + note);
				NoteStatus previousNoteStatus = previousToneTimeFrame.getNoteStatus();
				NoteStatusElement previousNoteStatusElement = previousNoteStatus.getNoteStatusElement(note);
				attenuateSemitone(toneMap, toneMap.getPreviousTimeFrame(previousToneTimeFrame.getStartTime()),
						previousToneTimeFrame.getElements(), index + 1, previousNoteStatusElement, noteStatus, note + 1,
						processedNotes);
			}
			LOG.finer(">>attenuateSemitone BACKFILL: " + index + ", " + note + ", " + toneMapElement.getIndex() + ", "
					+ parentNoteStatusElement.offTime + ", " + noteStatusElement.onTime);

			backFillNotes(toneMap, note, toneMapElement.getIndex(), noteStatusElement.onTime,
					parentNoteStatusElement.offTime, OFF, processedNotes);
		}

	}

	private void backFillNotes(ToneMap toneMap, int note, int index, double onTime, double tooTime, int state,
			List<NoteListElement> processedNotes) {

		ToneTimeFrame tf = toneMap.getTimeFrame(onTime / 1000.0);
		while (tf != null && tf.getStartTime() <= tooTime / 1000.0) {
			NoteStatus noteStatus = tf.getNoteStatus();
			NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(note);
			ToneMapElement toneMapElement = tf.getElement(index);
			if (state == ON && (noteStatusElement.state == PENDING || noteStatusElement.state == OFF)) {
				toneMapElement.noteState = ON;
				noteStatusElement.state = ON;
				noteStatusElement.onTime = onTime;
				noteStatusElement.offTime = tooTime;
				toneMapElement.noteState = ON;
				LOG.finer(">>BACKFILL ON: " + toneMapElement);
			} else if (state == OFF) {
				toneMapElement.noteState = OFF;
				noteStatusElement.state = OFF;
				noteStatusElement.isContinuation = false;
				noteStatusElement.onTime = 0.0;
				noteStatusElement.offTime = 0.0;
				noteStatusElement.highFlag = false;
				if (toneMapElement.noteListElement != null) {
					processedNotes.remove(toneMapElement.noteListElement);
					toneMapElement.noteListElement = null;
				}
				LOG.finer(">>BACKFILL OFF: " + toneMapElement + ", " + tf.getStartTime() + ", " + tooTime);
			}
			tf = toneMap.getNextTimeFrame(tf.getStartTime());
		}
	}

	public void processPeaks(ToneMap toneMap, List<SpectralPeak> peaks) {

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();
		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		peaks.iterator();

		for (ToneMapElement toneMapElement : ttfElements) {
			toneMapElement.amplitude = MIN_AMPLITUDE;
		}
		for (SpectralPeak peak : peaks) {
			ttfElements[peak.getBin()].amplitude = toneTimeFrame.getMaxAmplitude();
			ttfElements[peak.getBin()].isPeak = true;
		}

	}

	public void flagPeaks(ToneMap toneMap, List<SpectralPeak> peaks) {

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();
		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		peaks.iterator();
		for (SpectralPeak peak : peaks) {
			ttfElements[peak.getBin()].isPeak = true;
		}

	}

	private void applyFormant(FormantSettings settings, ToneMapElement element, int note) {

		if (settings.formantLow > settings.formantMid || settings.formantLow >= settings.formantHigh
				|| settings.formantMid > settings.formantHigh)
			return;

		double formantMidStart = (settings.formantMid - settings.formantRange / 2) < settings.formantLow
				? settings.formantLow
				: settings.formantMid - settings.formantRange / 2;
		double formantMidEnd = (settings.formantMid + settings.formantRange / 2) > settings.formantHigh
				? settings.formantHigh
				: settings.formantMid + settings.formantRange / 2;

		if (note < settings.formantLow || note > settings.formantHigh)
			return;

		if (note <= formantMidStart && formantMidStart > settings.formantLow) {
			double factor = (1.0 - ((settings.formantFactor / 100.0)
					* ((note - settings.formantLow) / (formantMidStart - settings.formantLow))));
			element.amplitude *= factor;

		} else if (note >= formantMidEnd && formantMidEnd < settings.formantHigh) {
			double factor = (1.0 - ((settings.formantFactor / 100.0)
					* ((settings.formantHigh - note) / (settings.formantHigh - formantMidEnd))));
			element.amplitude *= factor;

		} else {
			double factor = (1.0 - ((settings.formantFactor / 100.0)));
			element.amplitude *= factor;
		}

	}

	// Attenuate audio data power values for given Harmonic overtone
	private double attenuate(ToneMapElement overToneElement, double fundamental, double harmonic) {
		double overToneData = fundamental * harmonic;
		double difference = 0;

		if ((overToneElement.amplitude + MIN_AMPLITUDE) <= overToneData) {
			difference = overToneElement.amplitude - MIN_AMPLITUDE;
			if (harmonicAttenuateSwitch) {
				overToneElement.amplitude = MIN_AMPLITUDE;
			}
		} else {
			if (harmonicAttenuateSwitch) {
				overToneElement.amplitude -= overToneData;
			}
			difference = overToneData;
		}
		return difference;
	}

	private void initOvertoneSet() {
		overtoneSet = new OvertoneSet();
		if (!harmonicsGuitar) {
			double[] initHarmonics = { harmonic1Setting / 100.0, harmonic2Setting / 100.0, harmonic3Setting / 100.0,
					harmonic4Setting / 100.0, harmonic5Setting / 100.0, harmonic6Setting / 100.0 };
			overtoneSet.setHarmonics(initHarmonics);
		}
	}

	private double normalThreshold(ToneTimeFrame toneTimeFrame, ToneMapElement toneMapElement,
			ToneMapElement thresholdElement) {

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		double thresholdAmp;
		double thresholdFreq, normalFreq;
		int thresholdNote, normalNote;

		if (thresholdElement == null || toneMapElement.getIndex() < thresholdElement.getIndex()) {
			thresholdAmp = 1.0;
			thresholdFreq = pitchSet.getFreq(pitchSet.pitchToIndex(getLowPitch()));
			thresholdNote = pitchSet.getNote(pitchSet.pitchToIndex(getLowPitch()));

		} else {

			thresholdAmp = thresholdElement.amplitude;
			thresholdFreq = pitchSet.getFreq(thresholdElement.getPitchIndex());
			thresholdNote = pitchSet.getNote(thresholdElement.getPitchIndex());
		}

		normalFreq = pitchSet.getFreq(toneMapElement.getPitchIndex());
		normalNote = pitchSet.getNote(toneMapElement.getPitchIndex());
		double threshold = 0;
		if (n2Switch) {
			threshold = (noteHigh / 100.0) * (thresholdAmp) / (n5Setting / 10.0
					+ (((double) n4Setting / (double) normalizeSetting) * (normalNote - thresholdNote)));
		} else {
			threshold = (noteHigh / 100.0) * (thresholdAmp) / (n5Setting / 10.0
					+ (((double) n4Setting / (double) normalizeSetting) * (normalFreq - thresholdFreq)));
		}

		return threshold;
	}

	// Process individual Note across sequence of ToneMapMatrix elements
	private void processNote(ToneMap toneMap, NoteStatusElement noteStatusElement,
			List<NoteListElement> processedNotes) {

		// if (!noteStatusElement.highFlag || ((noteStatusElement.offTime -
		// noteStatusElement.onTime) < noteMinDuration)) {
		// LOG.finer(">>PROCESS NOTE DISCARD!!: " + noteStatusElement + ", "
		// + noteMinDuration);
		// return;
		// }

		LOG.finer(">>PROCESS NOTE: " + noteStatusElement + ", " + noteStatusElement.onTime);

		int numSlots = 0;
		int numLowSlots = 0;
		double amplitude;
		double ampSum = 0;
		double minAmp = 0;
		double maxAmp = 0;
		double avgAmp = 0;
		double percentMin = 0;
		double startTime, endTime;
		boolean isContinuation = false;
		int pitchIndex, startTimeIndex, endTimeIndex;

		ToneTimeFrame[] timeFrames = toneMap.getTimeFramesFrom((noteStatusElement.onTime / 1000.0));
		ToneMapElement startElement = null;
		ToneMapElement endElement = null;
		startTime = noteStatusElement.onTime;
		endTime = noteStatusElement.offTime;
		isContinuation = noteStatusElement.isContinuation;

		// LOG.finer(">>PROCESS NOTE: " + noteStatusElement + ", startTime: " +
		// startTime + ", endTime: " + endTime
		// + ", flen: " + timeFrames.length);

		// across range of note
		for (ToneTimeFrame toneTimeFrame : timeFrames) {
			if (toneTimeFrame.getStartTime() > noteStatusElement.offTime / 1000.0) {
				break;
			}
			// LOG.finer(">>PROCESS NOTE: " + noteStatusElement.note + ", " +
			// toneTimeFrame.getStartTime());
			ToneMapElement element = toneTimeFrame.getElement(noteStatusElement.index);
			if (startElement == null) {
				startElement = element;
				// LOG.finer(">>PROCESS NOTE START: " + toneTimeFrame.getStartTime() +
				// element.getIndex());

			}
			endElement = element;
			// LOG.finer(">>PROCESS NOTE END: " + toneTimeFrame.getStartTime() +
			// element.getIndex());

			element.noteState = ON;
			LOG.finer(">>PROCESS NOTE ON: " + toneTimeFrame.getStartTime() + element.getIndex());

			numSlots++;

			amplitude = element.amplitude;
			ampSum = ampSum + amplitude;
			if (maxAmp < amplitude) {
				maxAmp = amplitude;
				// if (peakSwitch) {
				// startTime = toneTimeFrame.getStartTime();
				// startElement = element;
				// }
			}
			if ((minAmp == 0) || (minAmp > amplitude))
				minAmp = amplitude;

			if (amplitude < noteLow / 100.0)
				numLowSlots++;
			// if (peakSwitch && (amplitude >= noteHigh / 100.0)) {
			// endTime = toneTimeFrame.getStartTime();
			// endElement = element;
			// break;
			// }
		}

		if (startTime >= endTime)
			return;

		startElement.noteState = START;

		// LOG.finer(">>PROCESS NOTE START: " + startTime + ", " +
		// startElement.getIndex());

		pitchIndex = startElement.getPitchIndex();

		startTimeIndex = startElement.getTimeIndex();

		// if (noteStatusElement.state != CONTINUING) {

		endElement.noteState = END;

		// LOG.finer(">>PROCESS NOTE END: " + endElement.getIndex());

		endTimeIndex = endElement.getTimeIndex();

		avgAmp = ampSum / numSlots;
		percentMin = numLowSlots / numSlots;

		// Create noteList element object
		NoteListElement noteListElement = new NoteListElement(noteStatusElement.note, pitchIndex, startTime, endTime,
				startTimeIndex, endTimeIndex, avgAmp, maxAmp, minAmp, percentMin, isContinuation);

		// LOG.finer(">>PROCESS NOTE: " + note + ", " + noteListElement + ",
		// " + timeFrames.length);

		// Cross-Register NoteList element against ToneMapMatrix elements
		for (ToneTimeFrame toneTimeFrame : timeFrames) {
			if (toneTimeFrame.getStartTime() >= noteStatusElement.offTime / 1000.0) {
				break;
			}
			ToneMapElement element = toneTimeFrame.getElement(noteStatusElement.index);
			element.noteListElement = noteListElement;
			LOG.finer(">>PROCESS NOTE ADDED NOTE TO: " + toneTimeFrame.getStartTime() + ", " + noteStatusElement);
		}
		processedNotes.add(noteListElement);
		LOG.finer(">>PROCESS NOTE ADDED NOTE DONE " + noteStatusElement.onTime + ", " + noteStatusElement.note
				+ ", max in note: " + maxAmp + ", tm max: " + toneMap.getStatistics().max + ", " + noteStatusElement
				+ ", " + noteStatusElement.offTime);

	}

	private boolean hasPendingHarmonics(ToneMap toneMap, NoteListElement processedNote) {
		boolean result = false;
		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame((processedNote.startTime / 1000.0));
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
		for (ToneMapElement toneMapElement : toneTimeFrame.getElements()) {
			int rootNote = pitchSet.getNote(toneMapElement.getPitchIndex());
			if (rootNote >= processedNote.note) {
				break;
			}
			NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(rootNote);
			if (noteStatusElement.onTime <= processedNote.startTime && noteStatusElement.state > OFF) {
				int harmonic = getHarmonic(noteStatusElement.note, processedNote.note);
				if (harmonic > 0) {
					ToneTimeFrame[] timeFrames = toneMap.getTimeFramesFrom((processedNote.startTime / 1000.0));
					if (!noteTimbreNotateSwitch || !isMatchingTimbre(timeFrames, processedNote, rootNote)) {
						processedNote.noteHarmonics.addNoteHarmonic(noteStatusElement.note, harmonic);
						result = true;
					}
				}
			}
		}
		return result;
	}

	private void commitNote(ToneTimeFrame[] timeFrames, NoteListElement processedNote) {
		LOG.severe(">>commitNote: " + processedNote.note + ", " + processedNote.pitchIndex);
		ToneMapElement startElement = null;
		ToneMapElement endElement = null;

		Set<NoteListElement> semiTones = new HashSet<NoteListElement>();
		Set<NoteListElement> underTones = new HashSet<NoteListElement>();
		Set<NoteListElement> harmonicTones = new HashSet<NoteListElement>();
		Set<NoteListElement> noteListElements = new HashSet<NoteListElement>();
		for (ToneTimeFrame toneTimeFrame : timeFrames) {
			if (toneTimeFrame.getStartTime() > processedNote.endTime / 1000.0) {
				break;
			}
			for (ToneMapElement toneMapElement : toneTimeFrame.getElements()) {
				NoteListElement nle = toneMapElement.noteListElement;
				if (nle != null && nle.startTime >= processedNote.startTime && nle.endTime <= processedNote.endTime
						&& !noteListElements.contains(nle)) {
					LOG.severe(">>nle seek: " + nle + ", " + processedNote);
					noteListElements.add(nle);
					if (Math.abs(processedNote.note - nle.note) == 1) {
						semiTones.add(nle);
					} else if ((processedNote.note - nle.note) == 12) {
						underTones.add(nle);
					} else if (processedNote.note >= harmonicLowLimit && nle.note <= harmonicHighLimit) {
						if (isHarmonic(processedNote.note, nle.note)) {
							LOG.severe(">>commitNote isMatchingTimbre: " + processedNote.note + " ," + nle);
							if (!noteTimbreNotateSwitch || isMatchingTimbre(timeFrames, processedNote, nle)) {
								LOG.severe(">>commitNote isHarmonic: " + processedNote.note);
								harmonicTones.add(nle);
							}
						}
					}
				}
			}
		}

		if (noteScanAttenuateHarmonics) {
			attenuateHarmonics(timeFrames, harmonicTones, processedNote, startElement, endElement);
		}
		// if (noteScanAttenuateUndertones) {
		// attenuateUndertones(timeFrames, underTones, processedNote, startElement,
		// endElement);
		// }
		if (noteScanAttenuateSemitones) {
			attenuateSemitones(timeFrames, semiTones, processedNote, startElement, endElement);
		}
	}

	private void attenuateHarmonics(ToneTimeFrame[] timeFrames, Set<NoteListElement> harmonicTones,
			NoteListElement processedNote, ToneMapElement startElement, ToneMapElement endElement) {
		for (NoteListElement nle : harmonicTones) {

			double rootFreq = PitchSet.getMidiFreq(processedNote.note);
			double noteFreq = PitchSet.getMidiFreq(nle.note);
			LOG.finer(">>attenuateHarmonics: " + noteFreq + ", " + rootFreq + ", " + nle.note + ", "
					+ processedNote.note);
			int harmonic = (int) (noteFreq / rootFreq);
			LOG.severe(">>attenuateHarmonics: " + noteFreq + ", " + rootFreq + ", " + nle.note + ", "
					+ processedNote.note + ", " + harmonic);

			Set<ToneMapElement> tmElements = new HashSet<ToneMapElement>();
			Set<NoteStatusElement> nsElements = new HashSet<NoteStatusElement>();
			int numSlots = 0;
			int numLowSlots = 0;
			double amplitude;
			double ampSum = 0;
			double minAmp = 0;
			double maxAmp = 0;
			double avgAmp = 0;
			double percentMin = 0;
			boolean hasWeight = false;
			for (ToneTimeFrame toneTimeFrame : timeFrames) {
				if (toneTimeFrame.getStartTime() > nle.endTime / 1000.0) {
					break;
				}
				if (toneTimeFrame.getStartTime() < nle.startTime / 1000.0) {
					continue;
				}

				NoteStatus noteStatus = toneTimeFrame.getNoteStatus();

				ToneMapElement toneMapElement = toneTimeFrame.getElement(nle.pitchIndex);
				tmElements.add(toneMapElement);

				NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(nle.note);
				nsElements.add(noteStatusElement);

				LOG.finer(">>attenuateHarmonics ADD NOTE: " + toneMapElement + ", " + toneTimeFrame.getStartTime()
						+ ", " + tmElements.size());

				ToneMapElement rootToneMapElement = toneTimeFrame.getElement(processedNote.pitchIndex);

				double amplitudeFactor = rootToneMapElement.amplitude * harmonics[harmonic - 1];
				if ((toneMapElement.amplitude + MIN_AMPLITUDE) <= amplitudeFactor) {
					toneMapElement.amplitude = MIN_AMPLITUDE;
				} else {
					toneMapElement.amplitude -= amplitudeFactor; // !!TODO Remove all note;
					LOG.severe(">>attenuateHarmonics ATTENUATE: " + nle.note + ", " + processedNote.note);
					if (!harmonicsWeighting) {
						toneMapElement.amplitude = MIN_AMPLITUDE;
					} else {
						hasWeight = true;
					}
				}

				numSlots++;
				amplitude = toneMapElement.amplitude;
				ampSum = ampSum + amplitude;
				if (maxAmp < amplitude) {
					maxAmp = amplitude;
				}
				if ((minAmp == 0) || (minAmp > amplitude))
					minAmp = amplitude;

				if (amplitude < noteLow / 100.0) {
					numLowSlots++;
				}
			}

			avgAmp = ampSum / numSlots;
			percentMin = numLowSlots / numSlots;

			if (hasWeight) {
				Map<Integer, Integer> noteHarmonics = nle.noteHarmonics.getNoteHarmonics();
				if (!noteHarmonics.containsKey(processedNote.note)) {
					// Should not happen
					LOG.severe(">>SHOULD NOT HAPPEN: " + nle + ",  " + processedNote);
					return;
				} else {
					LOG.finer(">>attenuateHarmonics NON - MatchingTimbre: " + nle.note + ", " + processedNote.note);
					noteHarmonics.remove(processedNote.note);
					if (noteHarmonics.isEmpty()) {
						LOG.severe(">>attenuateHarmonics RECURSIVE!! commitNote MatchingTimbre: " + nle.note + ", "
								+ processedNote.note);
						commitNote(timeFrames, nle);
					}
				}
			} else {

				LOG.severe(">>attenuateHarmonics nle: " + nle);
				for (ToneMapElement toneMapElement : tmElements) {
					toneMapElement.noteListElement = null;
					toneMapElement.noteState = OFF;
					LOG.severe(">>attenuateHarmonics toneMapElement: " + toneMapElement);
				}
				for (NoteStatusElement noteStatusElement : nsElements) {
					noteStatusElement.state = OFF;
					noteStatusElement.onTime = 0.0;
					noteStatusElement.offTime = 0.0;
					noteStatusElement.highFlag = false;
					LOG.severe(">>attenuateHarmonics noteStatusElement: " + noteStatusElement);
				}
			}
			// } else {
			// nle.avgAmp = avgAmp;
			// nle.maxAmp = maxAmp;
			// nle.minAmp = minAmp;
			// nle.percentMin = percentMin;
			// }
		}
	}

	private void attenuateUndertones(ToneTimeFrame[] timeFrames, Set<NoteListElement> underTones,
			NoteListElement processedNote, ToneMapElement startElement, ToneMapElement endElement) {
		for (NoteListElement nle : underTones) {
			Set<ToneMapElement> tmElements = new HashSet<ToneMapElement>();
			Set<NoteStatusElement> nsElements = new HashSet<NoteStatusElement>();
			int numSlots = 0;
			int numLowSlots = 0;
			double amplitude;
			double ampSum = 0;
			double minAmp = 0;
			double maxAmp = 0;
			double avgAmp = 0;
			double percentMin = 0;
			for (ToneTimeFrame toneTimeFrame : timeFrames) {
				if (toneTimeFrame.getStartTime() > nle.endTime / 1000.0) {
					break;
				}
				if (toneTimeFrame.getStartTime() < nle.startTime / 1000.0) {
					continue;
				}

				NoteStatus noteStatus = toneTimeFrame.getNoteStatus();

				ToneMapElement toneMapElement = toneTimeFrame.getElement(nle.pitchIndex);
				tmElements.add(toneMapElement);

				NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(nle.note);
				nsElements.add(noteStatusElement);

				ToneMapElement rootToneMapElement = toneTimeFrame.getElement(processedNote.pitchIndex);
				if ((toneMapElement.amplitude + MIN_AMPLITUDE) <= rootToneMapElement.amplitude) {
					toneMapElement.amplitude = MIN_AMPLITUDE;
				} else {
					toneMapElement.amplitude -= rootToneMapElement.amplitude;
				}

				numSlots++;
				amplitude = toneMapElement.amplitude;
				ampSum = ampSum + amplitude;
				if (maxAmp < amplitude) {
					maxAmp = amplitude;
				}
				if ((minAmp == 0) || (minAmp > amplitude))
					minAmp = amplitude;

				if (amplitude < noteLow / 100.0) {
					numLowSlots++;
				}
			}

			avgAmp = ampSum / numSlots;
			percentMin = numLowSlots / numSlots;

			if (maxAmp <= MIN_AMPLITUDE) {
				for (ToneMapElement toneMapElement : tmElements) {
					toneMapElement.noteListElement = null;
					toneMapElement.noteState = OFF;
				}
				for (NoteStatusElement noteStatusElement : nsElements) {
					noteStatusElement.state = OFF;
					noteStatusElement.isContinuation = false;
					noteStatusElement.onTime = 0.0;
					noteStatusElement.offTime = 0.0;
					noteStatusElement.highFlag = false;
				}
			} else {
				nle.avgAmp = avgAmp;
				nle.maxAmp = maxAmp;
				nle.minAmp = minAmp;
				nle.percentMin = percentMin;
			}
		}
	}

	private void attenuateSemitones(ToneTimeFrame[] timeFrames, Set<NoteListElement> semiTones,
			NoteListElement processedNote, ToneMapElement startElement, ToneMapElement endElement) {
		for (NoteListElement nle : semiTones) {
			Set<ToneMapElement> tmElements = new HashSet<ToneMapElement>();
			Set<NoteStatusElement> nsElements = new HashSet<NoteStatusElement>();
			int numSlots = 0;
			int numLowSlots = 0;
			double amplitude;
			double ampSum = 0;
			double minAmp = 0;
			double maxAmp = 0;
			double avgAmp = 0;
			double percentMin = 0;

			for (ToneTimeFrame toneTimeFrame : timeFrames) {
				if (toneTimeFrame.getStartTime() > nle.endTime / 1000.0) {
					break;
				}
				if (toneTimeFrame.getStartTime() < nle.startTime / 1000.0) {
					continue;
				}

				NoteStatus noteStatus = toneTimeFrame.getNoteStatus();

				ToneMapElement toneMapElement = toneTimeFrame.getElement(nle.pitchIndex);
				tmElements.add(toneMapElement);

				NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(nle.note);
				nsElements.add(noteStatusElement);

				ToneMapElement rootToneMapElement = toneTimeFrame.getElement(processedNote.pitchIndex);
				if ((toneMapElement.amplitude + MIN_AMPLITUDE) <= rootToneMapElement.amplitude) {
					toneMapElement.amplitude = MIN_AMPLITUDE;
				} else {
					toneMapElement.amplitude -= rootToneMapElement.amplitude;
				}

				numSlots++;
				amplitude = toneMapElement.amplitude;
				ampSum = ampSum + amplitude;
				if (maxAmp < amplitude) {
					maxAmp = amplitude;
				}
				if ((minAmp == 0) || (minAmp > amplitude))
					minAmp = amplitude;

				if (amplitude < noteLow / 100.0) {
					numLowSlots++;
				}
			}

			avgAmp = ampSum / numSlots;
			percentMin = numLowSlots / numSlots;

			if (maxAmp <= MIN_AMPLITUDE) {
				for (ToneMapElement toneMapElement : tmElements) {
					toneMapElement.noteListElement = null;
					toneMapElement.noteState = OFF;
				}
				for (NoteStatusElement noteStatusElement : nsElements) {
					noteStatusElement.state = OFF;
					noteStatusElement.isContinuation = false;
					noteStatusElement.onTime = 0.0;
					noteStatusElement.offTime = 0.0;
					noteStatusElement.highFlag = false;
				}
			} else {
				nle.avgAmp = avgAmp;
				nle.maxAmp = maxAmp;
				nle.minAmp = minAmp;
				nle.percentMin = percentMin;
			}
		}
	}

	private boolean isHarmonic(int root, int note) {
		double rootFreq = PitchSet.getMidiFreq(root);
		double noteFreq = PitchSet.getMidiFreq(note);
		for (int n = 2; n < 9; n++) {
			double harmonicFreq = n * rootFreq;
			if (Math.abs(harmonicFreq - noteFreq) < HARMONIC_VARIANCE) {
				return true;
			}
		}
		return false;
	}

	private int getHarmonic(int root, int note) {
		double rootFreq = PitchSet.getMidiFreq(root);
		double noteFreq = PitchSet.getMidiFreq(note);
		for (int n = 2; n < 9; n++) {
			double harmonicFreq = n * rootFreq;
			if (Math.abs(harmonicFreq - noteFreq) < HARMONIC_VARIANCE) {
				return n;
			}
		}
		return 0;
	}

	// Process Harmonic overtones
	private void processOvertones(ToneTimeFrame toneTimeFrame, int pitchIndex) {
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		ToneTimeFrame[] timeFrames = { toneTimeFrame };

		double f0 = pitchSet.getFreq(pitchIndex);
		int note = PitchSet.freqToMidiNote(f0);

		if (note < harmonicLowLimit) {
			return;
		}

		double freq;
		int n = 2;

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		ToneMapElement f0Element = ttfElements[pitchIndex];
		if (f0Element == null || f0Element.amplitude == -1)
			return;

		double difference = 0;

		for (double harmonic : harmonics) {
			freq = n * f0;
			note = PitchSet.freqToMidiNote(freq);

			if (note == -1 || note > harmonicHighLimit || note > pitchSet.getHighNote())
				break;
			int index = pitchSet.getIndex(note);

			ToneMapElement toneMapElement = ttfElements[index];
			if (toneMapElement == null || toneMapElement.amplitude == -1)
				continue;
			if (!noteTimbreCQSwitch || isMatchingTimbre(toneMapElement, f0Element)) {
				difference += attenuate(toneMapElement, f0Element.amplitude, harmonic);
				toneMapElement.addHarmonicWieght(n, toneMapElement.amplitude);
			}
			n++;
		}

		if (harmonicAccumulateSwitch) {
			f0Element.amplitude += difference;
		}
	}

	private boolean isMatchingTimbre(ToneMapElement harmonicElement, ToneMapElement f0Element) {
		NoteTimbre hnt = new NoteTimbre(noteTimbreFrequencyRange, noteTimbreFrequencyRatio, noteTimbreMedianRange,
				noteTimbreMedianRatio);
		NoteTimbre rnt = new NoteTimbre(noteTimbreFrequencyRange, noteTimbreFrequencyRatio, noteTimbreMedianRange,
				noteTimbreMedianRatio);
		hnt.buildTimbre(harmonicElement);
		rnt.buildTimbre(f0Element);
		if (f0Element.getPitchIndex() == 24) {
			boolean result = hnt.matches2(rnt);
			LOG.severe(">>!!AT isMatchingTimbre A: " + result + ", " + harmonicElement + ", " + f0Element);
			LOG.severe(">>!!AT isMatchingTimbre A TIMBRES: " + hnt + ", " + rnt);
			return result;
		} else {
			boolean result = hnt.matches(rnt);
			return result;
		}
	}

	private boolean isMatchingTimbre(ToneTimeFrame[] timeFrames, NoteListElement rootNote,
			NoteListElement processedNote) {
		NoteTimbre pnt = new NoteTimbre(noteTimbreFrequencyRange, noteTimbreFrequencyRatio, noteTimbreMedianRange,
				noteTimbreMedianRatio);
		NoteTimbre rnt = new NoteTimbre(noteTimbreFrequencyRange, noteTimbreFrequencyRatio, noteTimbreMedianRange,
				noteTimbreMedianRatio);
		processedNote.noteTimbre = pnt;
		rootNote.noteTimbre = rnt;
		processedNote.noteTimbre.buildTimbre(timeFrames, processedNote);
		rootNote.noteTimbre.buildTimbre(timeFrames, rootNote);
		return processedNote.noteTimbre.matches(rootNote.noteTimbre);
	}

	private boolean isMatchingTimbre(ToneTimeFrame[] timeFrames, NoteListElement processedNote, int rootNote) {
		NoteTimbre pnt = new NoteTimbre(noteTimbreFrequencyRange, noteTimbreFrequencyRatio, noteTimbreMedianRange,
				noteTimbreMedianRatio);
		NoteTimbre rnt = new NoteTimbre(noteTimbreFrequencyRange, noteTimbreFrequencyRatio, noteTimbreMedianRange,
				noteTimbreMedianRatio);
		pnt.buildTimbre(timeFrames, processedNote);
		rnt.buildTimbre(timeFrames, rootNote);
		return pnt.matches(rnt);
	}

	private void processOvertones(ToneTimeFrame toneTimeFrame, boolean peaks) {
		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		for (ToneMapElement toneMapElement : ttfElements) {
			if (toneMapElement.amplitude > MIN_AMPLITUDE) {
				if (!peaks || toneMapElement.isPeak) {
					processOvertones(toneTimeFrame, toneMapElement.getIndex());
				}
			}
		}
	}

	public void processOvertones(ToneTimeFrame toneTimeFrame) {
		processOvertones(toneTimeFrame, false);
	}

	private void processPeak(ToneTimeFrame toneTimeFrame, int startPeak, int endPeak, double trough,
			ToneMapElement thresholdElement, double maxAmp) {

		double ampThres = n3Setting / 100.0;
		int index;

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		for (ToneMapElement toneMapElement : ttfElements) {
			index = toneMapElement.getIndex();
			if (index < startPeak) {
				continue;
			} else if (index > endPeak) {
				break;
			} else {
				toneMapElement.isPeak = true;
//				double amplitude = toneMapElement.amplitude / maxAmp;
//
//				double nt = normalThreshold(toneTimeFrame, toneMapElement, thresholdElement);
//				if (amplitude > nt && amplitude > ampThres) {
//					toneMapElement.amplitude = maxAmp;
//				}
				if (n4Switch) {
					processOvertones(toneTimeFrame, index);
				}
			}
		}
	}

	public static void harmonicPeakSubtract(float[][] bins, float freqRes, int harmonic_size) {
		int len = bins.length;
		int len_frame = bins[0].length;
		for (int i = 0; i < len; i++) {
			int H = harmonic_size;
			for (int j = 1; j < len_frame; j++) { // discard 0Hz
				if (j * H >= len_frame)
					H = len_frame / j - 1;
				float sum = 0;
				for (int h = 0, hf = j; h < H; h++, hf += j) {
					sum += bins[i][hf] - Math.max(a(bins[i], j, H), Math.max(b(bins[i], j), g(bins[i], j)));
				}
				bins[i][j] = sum;
			}
		}
	}

	/*** Penalize even harmonic ***/
	public static float a(final float[] bin, int freq_idx, int harmonic_size) {
		float sum = 0;
		float idx = (0.5f) * freq_idx;
		for (int h = 0; h < harmonic_size; h++, idx += freq_idx) {
			sum += bin[Math.round(idx)];
		}
		return sum;
	}

	/*** Penalize third harmonic ***/
	final static float[] h3rd = { 1 / 3f, 2 / 3f, 4 / 3f, 5 / 3f };

	public static float b(final float[] bin, int freq_idx) {
		float min = bin[Math.round(h3rd[0] * freq_idx)];
		for (int h = 1; h < h3rd.length; h++) {
			int i = Math.round(h3rd[h] * freq_idx);
			if (i >= bin.length)
				break;
			min = Math.min(min, bin[i]);
		}
		return min;
	}

	/*** Penalize fifth harmonic ***/
	final static float[] h5th = { 1 / 5f, 2 / 5f, 3 / 5f, 4 / 5f };

	public static float g(final float[] bin, int freq_idx) {
		float min = bin[Math.round(h5th[0] * freq_idx)];
		for (int h = 1; h < h5th.length; h++) {
			min = Math.min(min, bin[Math.round(h5th[h] * freq_idx)]);
		}
		return min;
	}

	class FormantSettings {

		public int formantHigh;
		public int formantLow;
		public int formantMid;
		public double formantRange;
		public double formantFactor;

	}

}