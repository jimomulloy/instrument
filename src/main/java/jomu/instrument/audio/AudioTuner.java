
package jomu.instrument.audio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jomu.instrument.Instrument;
import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.NoteListElement;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.OvertoneSet;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioTuner implements ToneMapConstants {

	private static final double MIN_AMPLITUDE = ToneTimeFrame.AMPLITUDE_FLOOR;
	private static final double HARMONIC_VARIANCE = 10;
	private int formantFactor = 0;
	private int formantHighSetting = 100;
	private int formantLowSetting = 0;
	private int formantMiddleSetting = 50;

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

	private double formantHighFreq;
	private double formantLowFreq;
	private double formantMidFreq;

	// private NoteList noteList;
	private double[][] formants;
	private double[] harmonics;
	private OvertoneSet overtoneSet;
	private ParameterManager parameterManager;

	/**
	 * TunerModel constructor. Instantiate TunerPanel
	 */
	public AudioTuner() {
		initParameters();
		initOvertoneSet();
		harmonics = overtoneSet.getHarmonics();
		formants = overtoneSet.getFormants();
		initFormants();
	}

	private void initParameters() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		formantFactor = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_FACTOR);
		formantHighSetting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH);
		formantLowSetting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW);
		formantMiddleSetting = parameterManager.getIntParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE);
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
		formantHighFreq = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_HIGH_FREQUENCY);
		formantLowFreq = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_LOW_FREQUENCY);
		formantMidFreq = parameterManager
				.getDoubleParameter(InstrumentParameterNames.AUDIO_TUNER_FORMANT_MIDDLE_FREQUENCY);

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
			applyFormant(toneMapElement, note);
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

		for (ToneMapElement toneMapElement : ttfElements) {

			index = toneMapElement.getIndex();

			amplitude = toneMapElement.amplitude / maxAmp;

			if (amplitude >= lastAmp && amplitude > normalizePeak) {
				if (troughAmp <= normalizeTrough || (amplitude / troughAmp) > peakFactor) {
					if (amplitude > lastAmp) {
						if (startPeak != 0 && ((lastAmp / amplitude) < peakStepFactor)) {
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
				processPeak(toneTimeFrame, startPeak, endPeak, troughAmp, thresholdElement, maxAmp);
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
				}
			}
			toneTimeFrame.reset();
			System.out
					.println(">>thresholds: " + toneTimeFrame.getHighThreshold() + ", " + toneTimeFrame.getLowThres());
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
//		System.out.println(">>>Note scan seq: " + sequence + ", low: " + (noteLow / 100.0) + ", maxDur: "
//				+ noteMaxDuration + ", minDur: " + noteMinDuration + ", sus: " + noteSustain + ", max: "
//				+ toneTimeFrame.getMaxAmplitude() + ", min: " + toneTimeFrame.getMinAmplitude() + ", highthresh: "
//				+ toneTimeFrame.getHighThres() + ", lowthresh: " + toneTimeFrame.getLowThres());

		for (ToneMapElement toneMapElement : ttfElements) {
			note = pitchSet.getNote(toneMapElement.getPitchIndex());
			previousNoteStatusElement = previousNoteStatus.getNoteStatusElement(note);
			noteStatusElement = noteStatus.getNoteStatusElement(note);
			if (sequence > 1) {
				previousToneMapElement = previousToneTimeFrame.getElement(toneMapElement.getIndex());
			}

			double amplitude = toneMapElement.amplitude;
			// if (toneMapElement.amplitude >= toneTimeFrame.getLowThres()) {
			// amplitude = toneMapElement.amplitude / toneTimeFrame.getHighThreshold();
			// }

			time = timeSet.getStartTime() * 1000.0;
			// Establish range of Matrix entries within a sequence constituting
			// a continuous note within the bounds of the Tuner parameters

			noteStatusElement.state = previousNoteStatusElement.state;
			noteStatusElement.onTime = previousNoteStatusElement.onTime;
			noteStatusElement.offTime = previousNoteStatusElement.offTime;
			noteStatusElement.highFlag = previousNoteStatusElement.highFlag;
			noteStatusElement.note = previousNoteStatusElement.note;
			noteStatusElement.index = previousNoteStatusElement.index;

			switch (previousNoteStatusElement.state) {
			case OFF:
				if (amplitude > noteLow / 100.0) {
					noteStatusElement.state = ON;
					System.out.println(">>>Note scan OFF - ON  seq: " + sequence + ", " + note + ", " + time + ", "
							+ amplitude + ", " + noteHigh + ", " + noteLow);
					noteStatusElement.onTime = time;
					noteStatusElement.offTime = time;
					if (amplitude >= noteHigh / 100.0) {
						noteStatusElement.highFlag = true;
					}
					toneMapElement.noteState = START;
					System.out.println(">>NOTE START 1: " + toneMapElement + ", " + timeSet.getStartTime() + ", " + note
							+ ", " + noteStatusElement.onTime);
				}
				break;

			case ON:
				if (amplitude <= noteLow / 100.0 || (time - noteStatusElement.onTime) > noteMaxDuration) {
					noteStatusElement.state = PENDING;
					System.out.println(">>>Note scan ON - PENDING seq: " + sequence + ", " + note + ", " + time + ", "
							+ amplitude + ", " + noteLow);
					noteStatusElement.offTime = time;
				} else {
					System.out.println(">>>Note scan ON - CONTINUE seq: " + sequence + ", " + note + ", " + time + ", "
							+ amplitude + ", " + noteHigh);
					if (amplitude >= noteHigh / 100.0) {
						noteStatusElement.highFlag = true;
					}
				}
				toneMapElement.noteState = ON;
				System.out.println(">>NOTE ON 1: " + toneMapElement + ", " + timeSet.getStartTime() + ", " + note + ", "
						+ noteStatusElement.onTime);
				break;

			case PENDING:
				if (amplitude > noteLow / 100.0) {
					System.out.println(">>>Note scan PENDING high amp: " + sequence + ", " + note + ", " + time + ", "
							+ amplitude + ", " + noteStatusElement.offTime + ", " + noteSustain);
					if ((time - noteStatusElement.onTime) > noteMaxDuration) {
						noteStatusElement.state = CONTINUING;
						System.out.println(">>>Note scan ON - PENDING NEW NOTE PARTIAL seq: " + sequence + ", " + note
								+ ", " + time + ", " + amplitude + ", " + noteLow);
						// Process partial note here
						processNote(toneMap, noteStatusElement, processedNotes);
						noteStatusElement.state = ON;
						noteStatusElement.onTime = time;
						noteStatusElement.offTime = time;
						if (amplitude >= noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
						toneMapElement.noteState = ON;
						System.out.println(">>NOTE ON 2: " + toneMapElement + ", " + timeSet.getStartTime() + ", "
								+ note + ", " + noteStatusElement.onTime);

					} else if ((time - noteStatusElement.offTime) < (noteSustain)) {
						// back fill set notes ON
						System.out.println(
								">>>Note scan PENDING high - PROCESS BACK FILL ON seq: " + sequence + ", " + note);
						backFillNotes(toneMap, note, toneMapElement.getIndex(), noteStatusElement.onTime, time, ON,
								processedNotes);
						System.out.println(">>>Note scan PENDING - ON seq: " + sequence + ", " + note + ", " + time);
						noteStatusElement.state = ON;
						noteStatusElement.offTime = time;
						if (amplitude >= noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
						toneMapElement.noteState = ON;
						System.out.println(">>NOTE ON 3: " + toneMapElement + ", " + timeSet.getStartTime());
					} else if ((noteStatusElement.offTime - noteStatusElement.onTime) < (noteMinDuration)) {
						// back fill set notes OFF
						System.out.println(">>>Note scan PENDING high - PROCESS BACK FILL OFF seq: " + sequence + ", "
								+ note + ", " + noteStatusElement.onTime + ", " + noteStatusElement.offTime + ", "
								+ noteMinDuration);
						backFillNotes(toneMap, note, toneMapElement.getIndex(), noteStatusElement.onTime, time, OFF,
								processedNotes);
						noteStatusElement.state = ON;
						noteStatusElement.onTime = time;
						noteStatusElement.offTime = time;
						if (amplitude >= noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
						toneMapElement.noteState = START;
						System.out.println(">>NOTE START 2: " + toneMapElement + ", " + timeSet.getStartTime() + ", "
								+ note + ", " + noteStatusElement.onTime);
					} else {
						System.out.println(">>>Note scan PENDING - PROCESS NEW NOTE ON seq: " + sequence + ", " + note);
						previousToneMapElement.noteState = OFF;
						previousNoteStatusElement.state = OFF;
						// Process PREVIOUS note candididate note
						processNote(toneMap, previousNoteStatusElement, processedNotes);
						noteStatusElement.state = ON;
						noteStatusElement.onTime = time;
						noteStatusElement.offTime = time;
						if (amplitude >= noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
						toneMapElement.noteState = START;
						System.out.println(">>NOTE START 3: " + toneMapElement + ", " + timeSet.getStartTime() + ", "
								+ note + ", " + noteStatusElement.onTime);
					}
				} else {
					System.out.println(">>>Note scan PENDING low amp: " + sequence + ", " + note + ", " + time + ", "
							+ amplitude + ", " + noteStatusElement.onTime + ", " + noteStatusElement.offTime + ", "
							+ noteSustain + ", " + noteMaxDuration + ", " + noteLow);
					System.out.println(">>>Note scan PENDING - CONTINUE seq: " + sequence + ", note: " + note
							+ ", sus: " + noteSustain + ", maxDur: " + noteMaxDuration + ", time " + time
							+ ", offTime: " + noteStatusElement.offTime + ", onTime: " + noteStatusElement.onTime);
					if ((time - noteStatusElement.offTime) >= (noteSustain)
							|| (noteStatusElement.offTime - noteStatusElement.onTime) > noteMaxDuration) {
						if ((noteStatusElement.offTime - noteStatusElement.onTime) < (noteMinDuration)) {
							System.out.println(
									">>>Note scan PENDING low - PROCESS BACK FILL OFF seq: " + sequence + ", " + note);
							// back fill set notes OFF
							backFillNotes(toneMap, note, toneMapElement.getIndex(), noteStatusElement.onTime, time, OFF,
									processedNotes);
							noteStatusElement.state = OFF;
							noteStatusElement.onTime = 0.0;
							noteStatusElement.offTime = 0.0;
							noteStatusElement.highFlag = false;
							toneMapElement.noteState = OFF;
						} else {
							previousToneMapElement.noteState = OFF;
							previousNoteStatusElement.state = OFF;
							// Process candidate note
							System.out.println(
									">>>Note scan PENDING low - PROCESS NEW NOTE OFF seq: " + sequence + ", " + note);
							processNote(toneMap, previousNoteStatusElement, processedNotes);
							noteStatusElement.state = OFF;
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

		// IF LAST
//		switch (noteStatusElement.state) {
//		case OFF:
//			break;
//		case ON:
//			noteStatusElement.offTime = time;
//		case PENDING:
//			// Process candidate note
//			processNote(note, toneMap, noteStatusElement);
//			noteStatusElement.state = OFF;
//			noteStatusElement.onTime = 0.0;
//			noteStatusElement.offTime = 0.0;
//			noteStatusElement.highFlag = false;
//			break;
//
//		default:
//			break;
//		}

		// SECOND PASS
		for (int i = 0; i < ttfElements.length; i++) {
			ToneMapElement toneMapElement = ttfElements[i];
			note = pitchSet.getNote(toneMapElement.getPitchIndex());
			noteStatusElement = noteStatus.getNoteStatusElement(note);
			if (noteStatusElement.state == ON) {
				if (i > 0) {
					System.out.println(">>attenuateSemitone A: " + i + ", " + note);
					attenuateSemitone(toneMap, previousToneTimeFrame, ttfElements, i - 1, noteStatusElement, noteStatus,
							note - 1, processedNotes);
				}
				if (i < ttfElements.length - 1) {
					System.out.println(">>attenuateSemitone B: " + i + ", " + note);
					attenuateSemitone(toneMap, previousToneTimeFrame, ttfElements, i + 1, noteStatusElement, noteStatus,
							note + 1, processedNotes);
				}
			}

		}

		for (NoteListElement processedNote : processedNotes) {
			if (!hasPendingHarmonics(toneMap, processedNote)) {
				ToneTimeFrame[] timeFrames = toneMap.getTimeFramesFrom((processedNote.startTime / 1000.0));
				commitNote(timeFrames, processedNote);
			}
		}
		return true;
	}

	private void attenuateSemitone(ToneMap toneMap, ToneTimeFrame previousToneTimeFrame, ToneMapElement[] ttfElements,
			int index, NoteStatusElement parentNoteStatusElement, NoteStatus noteStatus, int note,
			List<NoteListElement> processedNotes) {
		NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(note);
		ToneMapElement toneMapElement = ttfElements[index];
		if (noteStatusElement.state == PENDING && (noteStatusElement.onTime > parentNoteStatusElement.onTime)) {
			System.out.println(">>attenuateSemitone X: " + index);
			if (index > 0 && previousToneTimeFrame != null) {
				System.out.println(">>attenuateSemitone Y: " + index + ", " + note);
				NoteStatus previousNoteStatus = previousToneTimeFrame.getNoteStatus();
				NoteStatusElement previousNoteStatusElement = previousNoteStatus.getNoteStatusElement(note);
				attenuateSemitone(toneMap, toneMap.getPreviousTimeFrame(previousToneTimeFrame.getStartTime()),
						previousToneTimeFrame.getElements(), index - 1, previousNoteStatusElement, noteStatus, note - 1,
						processedNotes);
			}
			if (index < ttfElements.length - 1 && previousToneTimeFrame != null) {
				System.out.println(">>attenuateSemitone Z: " + index + ", " + note);
				NoteStatus previousNoteStatus = previousToneTimeFrame.getNoteStatus();
				NoteStatusElement previousNoteStatusElement = previousNoteStatus.getNoteStatusElement(note);
				attenuateSemitone(toneMap, toneMap.getPreviousTimeFrame(previousToneTimeFrame.getStartTime()),
						previousToneTimeFrame.getElements(), index + 1, previousNoteStatusElement, noteStatus, note + 1,
						processedNotes);
			}
			System.out.println(">>attenuateSemitone BACKFILL: " + index + ", " + note + ", " + toneMapElement.getIndex()
					+ ", " + parentNoteStatusElement.offTime + ", " + noteStatusElement.onTime);

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
				System.out.println(">>BACKFILL ON: " + toneMapElement);
			} else if (state == OFF) {
				toneMapElement.noteState = OFF;
				noteStatusElement.state = OFF;
				noteStatusElement.onTime = 0.0;
				noteStatusElement.offTime = 0.0;
				noteStatusElement.highFlag = false;
				if (toneMapElement.noteListElement != null) {
					processedNotes.remove(toneMapElement.noteListElement);
					toneMapElement.noteListElement = null;
				}
				System.out.println(">>BACKFILL OFF: " + toneMapElement + ", " + tf.getStartTime() + ", " + tooTime);
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
		}

	}

	// Apply formant conversion to ToneMapMatrix element data
	private void applyFormant(ToneMapElement element, int note) {

		if (formantMidFreq < formantLowFreq || formantMidFreq > formantHighFreq)
			return;

		double noteFreq = PitchSet.getMidiFreq(note);

		if (noteFreq < formantLowFreq || noteFreq > formantHighFreq)
			return;

		if (noteFreq <= formantMidFreq) {
			element.amplitude = element.amplitude * (1.0
					- ((formantFactor / 100.0) * ((noteFreq - formantLowFreq) / (formantMidFreq - formantLowFreq))));

		} else {
			element.amplitude = element.amplitude * (1.0
					- ((formantFactor / 100.0) * ((formantHighFreq - noteFreq) / (formantHighFreq - formantMidFreq))));

		}
	}

	// Attenuate audio data power values for given Harmonic overtone
	private void attenuate(ToneMapElement overToneElement, double fundamental, double harmonic) {

		double overToneData = fundamental * harmonic;

		if ((overToneElement.amplitude + MIN_AMPLITUDE) <= overToneData) {
			overToneElement.amplitude = MIN_AMPLITUDE;
		} else {
			if (harmonic == 0.7 && overToneElement.amplitude > 0.01) {
				System.out.println(">>ATTENUATE " + fundamental + ", " + harmonic + ", " + overToneData + ", "
						+ overToneElement.amplitude);
			}
			overToneElement.amplitude -= overToneData;
			if (harmonic == 0.7 && overToneElement.amplitude > 0.01) {
				System.out.println(">>ATTENUATED " + fundamental + ", " + harmonic + ", " + overToneData + ", "
						+ overToneElement.amplitude);
			}
		}
	}

	private void initFormants() {

		formantLowFreq = PitchSet.getMidiFreq(getLowPitch()) + (formantLowSetting / 100.0)
				* (PitchSet.getMidiFreq(getHighPitch()) - PitchSet.getMidiFreq(getLowPitch()));

		formantHighFreq = PitchSet.getMidiFreq(getLowPitch()) + (formantHighSetting / 100.0)
				* (PitchSet.getMidiFreq(getHighPitch()) - PitchSet.getMidiFreq(getLowPitch()));
		formantMidFreq = PitchSet.getMidiFreq(getLowPitch()) + (formantMiddleSetting / 100.0)
				* (PitchSet.getMidiFreq(getHighPitch()) - PitchSet.getMidiFreq(getLowPitch()));

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
		// System.out.println(">>!!PROCESS NOTE DISCARD!!: " + noteStatusElement + ", "
		// + noteMinDuration);
		// return;
		// }

		System.out.println(">>!!PROCESS NOTE: " + noteStatusElement + ", " + noteStatusElement.onTime);
		int numSlots = 0;
		int numLowSlots = 0;
		double amplitude;
		double ampSum = 0;
		double minAmp = 0;
		double maxAmp = 0;
		double avgAmp = 0;
		double percentMin = 0;
		double startTime, endTime;
		int pitchIndex, startTimeIndex, endTimeIndex;

		ToneTimeFrame[] timeFrames = toneMap.getTimeFramesFrom((noteStatusElement.onTime / 1000.0));
		ToneMapElement startElement = null;
		ToneMapElement endElement = null;
		startTime = noteStatusElement.onTime;
		endTime = noteStatusElement.offTime;

		System.out.println(">>!!PROCESS NOTE: " + noteStatusElement + ", startTime: " + startTime + ", endTime: "
				+ endTime + ", flen: " + timeFrames.length);

		// across range of note
		for (ToneTimeFrame toneTimeFrame : timeFrames) {
			if (toneTimeFrame.getStartTime() > noteStatusElement.offTime / 1000.0) {
				break;
			}
			System.out.println(">>!!PROCESS NOTE: " + noteStatusElement.note + ", " + toneTimeFrame.getStartTime());
			ToneMapElement element = toneTimeFrame.getElement(noteStatusElement.index);
			if (startElement == null) {
				startElement = element;
				System.out.println(">>!!PROCESS NOTE START: " + toneTimeFrame.getStartTime() + element.getIndex());

			}
			endElement = element;
			System.out.println(">>!!PROCESS NOTE END: " + toneTimeFrame.getStartTime() + element.getIndex());

			element.noteState = ON;
			System.out.println(">>!!PROCESS NOTE ON: " + toneTimeFrame.getStartTime() + element.getIndex());

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

		System.out.println(">>!!PROCESS NOTE START: " + startElement.getIndex());

		pitchIndex = startElement.getPitchIndex();

		startTimeIndex = startElement.getTimeIndex();

		if (noteStatusElement.state != CONTINUING) {

			endElement.noteState = END;

			System.out.println(">>!!PROCESS NOTE END: " + endElement.getIndex());

			endTimeIndex = endElement.getTimeIndex();

			avgAmp = ampSum / numSlots;
			percentMin = numLowSlots / numSlots;

			// Create noteList element object
			NoteListElement noteListElement = new NoteListElement(noteStatusElement.note, pitchIndex, startTime,
					endTime, startTimeIndex, endTimeIndex, avgAmp, maxAmp, minAmp, percentMin);

			// System.out.println(">>!!PROCESS NOTE: " + note + ", " + noteListElement + ",
			// " + timeFrames.length);

			// Cross-Register NoteList element against ToneMapMatrix elements
			for (ToneTimeFrame toneTimeFrame : timeFrames) {
				if (toneTimeFrame.getStartTime() > noteStatusElement.offTime) {
					break;
				}
				ToneMapElement element = toneTimeFrame.getElement(noteStatusElement.index);
				element.noteListElement = noteListElement;
			}
			processedNotes.add(noteListElement);
			// Add noteList element to noteList object
			// noteList.add(noteListElement);

		}

	}

	private boolean hasPendingHarmonics(ToneMap toneMap, NoteListElement processedNote) {
		System.out.println(">>hasPendingHarmonics: " + processedNote.note);
		boolean result = false;
		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame((processedNote.startTime / 1000.0));
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
		for (ToneMapElement toneMapElement : toneTimeFrame.getElements()) {
			NoteListElement nle = toneMapElement.noteListElement;
			int note = pitchSet.getNote(toneMapElement.getPitchIndex());
			if (note >= processedNote.note) {
				break;
			}
			NoteStatusElement noteStatusElement = noteStatus.getNoteStatusElement(note);
			if (noteStatusElement.onTime <= processedNote.startTime && noteStatusElement.state > OFF) {
				int harmonic = getHarmonic(noteStatusElement.note, processedNote.note);
				if (harmonic > 0) {
					processedNote.noteHarmonics.addNoteHarmonic(noteStatusElement.note, harmonic);
					System.out.println(">>hasPendingHarmonics true: " + noteStatusElement.note + " ," + harmonic + ", "
							+ processedNote.note);
					result = true;
				}
			}
		}
		return result;
	}

	private void commitNote(ToneTimeFrame[] timeFrames, NoteListElement processedNote) {
		System.out.println(">>commitNote: " + processedNote.note);
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
				// System.out.println(">>nle seek: " + nle + ", " + processedNote);
				if (nle != null && nle.startTime >= processedNote.startTime && nle.endTime <= processedNote.endTime
						&& !noteListElements.contains(nle)) {
					noteListElements.add(nle);
					System.out.println(">>nle found: " + nle);
					if (Math.abs(processedNote.note - nle.note) == 1) {
						System.out.println(">>is semitone: " + nle);
						semiTones.add(nle);
					} else if ((processedNote.note - nle.note) == 12) {
						System.out.println(">>is undertone: " + nle);
						underTones.add(nle);
					} else if (isHarmonic(processedNote.note, nle.note)) {
						System.out.println(">>is harmonic: " + nle);
						harmonicTones.add(nle);
					}
				}
			}
		}

		if (noteScanAttenuateHarmonics) {
			System.out.println(">>commitNote attenuateHarmonics: " + processedNote.note);
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
			System.out.println(">>attenuateHarmonics: " + noteFreq + ", " + rootFreq + ", " + nle.note + ", "
					+ processedNote.note);
			int harmonic = (int) (noteFreq / rootFreq);
			System.out.println(">>attenuateHarmonics: " + noteFreq + ", " + rootFreq + ", " + nle.note + ", "
					+ processedNote.note + ", " + harmonic);
			Map<Integer, Integer> noteHarmonics = nle.noteHarmonics.getNoteHarmonics();
			if (!noteHarmonics.containsKey(processedNote.note)) {
				// Should not happen
				System.out.println(">>!!SHOULD NOT HAPPEN: " + nle + ",  " + processedNote);
			} else if (!isMatchingTimbre(timeFrames, processedNote, nle)) {
				System.out.println(">>attenuateHarmonics MatchingTimbre: " + nle.note + ", " + processedNote.note);
				noteHarmonics.remove(processedNote.note);
				if (noteHarmonics.isEmpty()) {
					System.out.println(
							">>attenuateHarmonics commitNote MatchingTimbre: " + nle.note + ", " + processedNote.note);
					commitNote(timeFrames, nle);
				}
			}

			Set<ToneMapElement> elements = new HashSet<ToneMapElement>();
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
				ToneMapElement toneMapElement = toneTimeFrame.getElement(nle.pitchIndex);
				elements.add(toneMapElement);
				System.out.println(">>attenuateHarmonics ADD NOTE: " + toneMapElement + ", "
						+ toneTimeFrame.getStartTime() + ", " + elements.size());
				ToneMapElement rootToneMapElement = toneTimeFrame.getElement(processedNote.pitchIndex);

				double amplitudeFactor = rootToneMapElement.amplitude * harmonics[harmonic - 1];
				if ((toneMapElement.amplitude + MIN_AMPLITUDE) <= amplitudeFactor) {
					toneMapElement.amplitude = MIN_AMPLITUDE;
				} else {
					toneMapElement.amplitude -= amplitudeFactor; // !!TODO Remove all note;
					System.out.println(">>attenuateHarmonics ATTENUATE: " + nle.note + ", " + processedNote.note);
					toneMapElement.amplitude = MIN_AMPLITUDE;
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

			// if (maxAmp <= MIN_AMPLITUDE) {
			for (ToneMapElement toneMapElement : elements) {
				toneMapElement.noteListElement = null;
				toneMapElement.noteState = OFF;
				System.out.println(">>attenuateHarmonics CLEAR NOTE: " + toneMapElement);
			}
			// } else {
			// nle.avgAmp = avgAmp;
			// nle.maxAmp = maxAmp;
			// nle.minAmp = minAmp;
			// nle.percentMin = percentMin;
			// }
		}
	}

	private boolean isMatchingTimbre(ToneTimeFrame[] timeFrames, NoteListElement processedNote,
			NoteListElement rootNote) {
		processedNote.noteTimbre.buildTimbre(timeFrames);
		rootNote.noteTimbre.buildTimbre(timeFrames);
		return processedNote.noteTimbre.matches(rootNote.noteTimbre);
	}

	private void attenuateUndertones(ToneTimeFrame[] timeFrames, Set<NoteListElement> underTones,
			NoteListElement processedNote, ToneMapElement startElement, ToneMapElement endElement) {
		for (NoteListElement nle : underTones) {
			Set<ToneMapElement> elements = new HashSet<ToneMapElement>();
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
				ToneMapElement toneMapElement = toneTimeFrame.getElement(nle.pitchIndex);
				elements.add(toneMapElement);
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
				for (ToneMapElement toneMapElement : elements) {
					toneMapElement.noteListElement = null;
					toneMapElement.noteState = OFF;
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
			Set<ToneMapElement> elements = new HashSet<ToneMapElement>();
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
				ToneMapElement toneMapElement = toneTimeFrame.getElement(nle.pitchIndex);
				elements.add(toneMapElement);
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
				for (ToneMapElement toneMapElement : elements) {
					toneMapElement.noteListElement = null;
					toneMapElement.noteState = OFF;
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
		System.out.println(">>TEST is harmonic: " + root + " ," + note + ", " + rootFreq + ", " + noteFreq);
		for (int n = 2; n < 7; n++) {
			double harmonicFreq = n * rootFreq;
			System.out.println(">>TEST is harmonic N: " + n + " ," + harmonicFreq);
			if (Math.abs(harmonicFreq - noteFreq) < HARMONIC_VARIANCE) {
				return true;
			}
		}
		return false;
	}

	private int getHarmonic(int root, int note) {
		double rootFreq = PitchSet.getMidiFreq(root);
		double noteFreq = PitchSet.getMidiFreq(note);
		for (int n = 2; n < 7; n++) {
			double harmonicFreq = n * rootFreq;
			if (Math.abs(harmonicFreq - noteFreq) < HARMONIC_VARIANCE) {
				return n;
			}
		}
		return 0;
	}

	// Process Harmonic overtones
	private void processOvertones(ToneTimeFrame toneTimeFrame, int pitchIndex) {
		System.out.println(">>processOvertones " + pitchIndex);
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		double f0 = pitchSet.getFreq(pitchIndex);

		double freq;
		int note;
		int n = 2;

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		ToneMapElement f0Element = ttfElements[pitchIndex];
		if (f0Element == null || f0Element.amplitude == -1)
			return;

		for (double harmonic : harmonics) {
			freq = n * f0;
			note = PitchSet.freqToMidiNote(freq);
			if (note == -1 || note > pitchSet.getHighNote())
				break;
			int index = pitchSet.getIndex(note);

			ToneMapElement toneMapElement = ttfElements[index];
			if (toneMapElement == null || toneMapElement.amplitude == -1)
				continue;
			int index1 = toneMapElement.getIndex();

			if (index != index1) {
				System.out.println(">>!!WAHT!!processOvertones attenuate: " + index + ", " + index1);
			}
			if (harmonic == 0.7) {
				System.out.println(">>processOvertones attenuate: " + pitchIndex + ", " + toneMapElement.amplitude
						+ ", " + f0Element.amplitude);
			}
			attenuate(toneMapElement, f0Element.amplitude, harmonic);
			toneMapElement.addHarmonicWieght(n, toneMapElement.amplitude);
			n++;
		}
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

}