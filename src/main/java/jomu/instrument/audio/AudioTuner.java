
package jomu.instrument.audio;

import java.util.List;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.NoteList;
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

/**
 * This class defines the Tuner Sub System Data Model processing functions for
 * the ToneMap including execution of ToneMapMatrix data filtering and
 * conversion, generation of NoteList objects of MIDI notes and Control Settings
 * management through the TunerPanel class.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class AudioTuner implements ToneMapConstants {

	private int formantFactor = 0;
	private int formantHighSetting = 100;
	private int formantLowSetting = 0;
	private int formantMiddleSetting = 50;

	private int n1Setting = 10;
	private boolean n1Switch;
	private int n2Setting = 100;
	private boolean n2Switch;
	private int n3Setting = 100;
	private int n4Setting = 100;
	private int n5Setting = 100;

	private int normalizeSetting = 100;

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

	private NoteList noteList;
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
		n1Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N1_SWITCH);
		n2Switch = parameterManager.getBooleanParameter(InstrumentParameterNames.AUDIO_TUNER_N2_SWITCH);
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

	/**
	 * Clear current TunerModel objects after Reset
	 */
	public void clear() {
		noteList = null;
	}

	public int getHighPitch() {
		return pitchHigh;
	}

	public int getLowPitch() {
		return pitchLow;
	}

	public NoteList getNoteList() {
		return noteList;
	}

	/**
	 * Normalise peak amplitudes
	 */
	public boolean normalize(ToneMap toneMap) {

		double amplitude, maxAmp = 0;
		int startPeak, endPeak, lastStartPeak, lastEndPeak;
		int index = 0;
		ToneMapElement thresholdElement = null;
		int note;
		double troughAmp, peakAmp, lastAmp, lastPeakAmp;

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();

		maxAmp = 0.00001;

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();
		for (ToneMapElement toneMapElement : ttfElements) {
			note = toneTimeFrame.getPitchSet().getNote(toneMapElement.getPitchIndex());
			amplitude = toneMapElement.amplitude;

			if (amplitude > maxAmp) {
				maxAmp = amplitude;
				thresholdElement = toneMapElement;
			}
			if (toneMapElement.amplitude < 0) {
				System.out.println("!!!AMM < 0 !!: " + toneMapElement.amplitude);
			}

		}

		troughAmp = 1.0;
		// System.out.println("!!!trough: " + troughAmp);
		startPeak = 0;
		endPeak = 0;
		lastAmp = 0;
		if (!n1Switch) {
			thresholdElement = null;
		}
		lastStartPeak = 0;
		lastEndPeak = 0;
		lastPeakAmp = 0;
		int peakcount = n1Setting;
		double peakFactor = n2Setting;

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		for (ToneMapElement toneMapElement : ttfElements) {

			index = toneMapElement.getIndex();
			note = pitchSet.getNote(toneMapElement.getPitchIndex());

			amplitude = toneMapElement.amplitude / maxAmp;
			if (amplitude > lastAmp)
				startPeak = index;
			if (amplitude >= lastAmp)
				endPeak = index;
			if (amplitude < lastAmp) {
				if (lastStartPeak != 0) {
					if (troughAmp == 0 || (lastAmp / troughAmp) > peakFactor) {
						if ((troughAmp == 0 || (lastPeakAmp / troughAmp) > peakFactor) && peakcount > 0) {
							peakcount = peakcount - 1;
							processPeak(toneTimeFrame, lastStartPeak, lastEndPeak, troughAmp, thresholdElement, maxAmp);
						}
						lastPeakAmp = lastAmp;
						lastStartPeak = startPeak;
						lastEndPeak = endPeak;
						startPeak = 0;
						endPeak = 0;
						troughAmp = 1.0; // maxAmp;
					}
				} else {
					if (startPeak != 0) {
						if (troughAmp == 0 || (lastAmp / troughAmp) > peakFactor) {
							lastPeakAmp = lastAmp;
							lastStartPeak = startPeak;
							lastEndPeak = endPeak;
							startPeak = 0;
							endPeak = 0;
							troughAmp = 1.0; // maxAmp;
						}
					}
				}

			}
			if (amplitude < troughAmp) {
				troughAmp = amplitude;
				// System.out.println("!!!trough2: " + troughAmp);
			}
			lastAmp = amplitude;

		}

		if (lastStartPeak != 0 && peakcount > 0) {
			if (troughAmp == 0 || (lastPeakAmp / troughAmp) > peakFactor) {
				processPeak(toneTimeFrame, lastStartPeak, lastEndPeak, troughAmp, thresholdElement, maxAmp);
			}
		}

		return true;
	}

	/**
	 * Scan through ToneMapMatrix extracting MIDI note data into NoteList object
	 * Apply filtering and conversion processing on basis of Tuner Parameters
	 *
	 * @param previousToneMap
	 */
	public boolean noteScan(ToneMap toneMap, int sequence) {
		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();
		NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
		NoteStatus previousNoteStatus = noteStatus;
		if (sequence > 1) {
			previousNoteStatus = toneMap.getTimeFrame(sequence - 1).getNoteStatus();
		}
		// timeRange = toneTimeFrame.getTimeSet().getRange();
		// pitchRange = toneTimeFrame.getPitchSet().getRange();

		// Initialise noteList object
		noteList = new NoteList();
		// Initialise noteStatus object

		NoteStatusElement noteStatusElement = null;
		NoteStatusElement previousNoteStatusElement = null;

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		// Iterate through ToneTimeFrame processing data elements to derive
		// NoteList
		// elements
		// Scan through each Pitch coordinate from Low to High limit.
		ToneMapElement[] ttfElements = toneTimeFrame.getElements();
		for (ToneMapElement toneMapElement : ttfElements) {
			int note = pitchSet.getNote(toneMapElement.getPitchIndex());
			previousNoteStatusElement = previousNoteStatus.getNote(note);
			noteStatusElement = noteStatus.getNote(note);
			if (toneMapElement == null || toneMapElement.amplitude == -1)
				continue;

			double amplitude = toneMapElement.amplitude;

			double time = timeSet.getStartTime();
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
				if (amplitude >= noteLow / 100.0) {
					noteStatusElement.state = ON;
					noteStatusElement.onTime = time;
					noteStatusElement.offTime = 0.0;
					if (amplitude >= noteHigh / 100.0) {
						noteStatusElement.highFlag = true;
					}
				}
				break;

			case ON:
				if (amplitude < noteLow / 100.0 || (time - noteStatusElement.onTime) > noteMaxDuration) {
					noteStatusElement.state = PENDING;
					noteStatusElement.offTime = time;
				} else {
					if (amplitude >= noteHigh / 100.0) {
						noteStatusElement.highFlag = true;
					}
				}
				break;

			case PENDING:
				if (amplitude >= noteLow / 100.0) {
					if ((time - noteStatusElement.offTime) < (noteSustain)
							&& (noteStatusElement.offTime - noteStatusElement.onTime) <= noteMaxDuration) {
						noteStatusElement.state = ON;
						noteStatusElement.offTime = 0.0;
						if (amplitude >= noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
					} else {
						// Process candididate note
						processNote(note, toneMap, noteStatusElement);
						noteStatusElement.state = ON;
						noteStatusElement.onTime = time;
						noteStatusElement.offTime = 0.0;
						if (amplitude >= noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
					}
				} else {
					if ((time - noteStatusElement.offTime) >= (noteSustain)
							|| (noteStatusElement.offTime - noteStatusElement.onTime) > noteMaxDuration) {
						// Process candidate note
						processNote(note, toneMap, noteStatusElement);
						noteStatusElement.state = OFF;
						noteStatusElement.onTime = 0.0;
						noteStatusElement.offTime = 0.0;
						noteStatusElement.highFlag = false;
					}
				}

				break;

			default:
				break;
			}

			/*
			 * IF LAST switch (noteStatusElement.state) { case OFF: break;
			 *
			 * case ON: noteStatusElement.offTime = time; noteStatusElement.offIndex =
			 * index; // ??
			 *
			 * case PENDING:
			 *
			 * // Process candidate note processNote(); noteStatusElement.state = OFF;
			 * noteStatusElement.onTime = 0.0; noteStatusElement.onIndex = 0;
			 * noteStatusElement.offTime = 0.0; noteStatusElement.offIndex = 0;
			 * noteStatusElement.highFlag = false; break;
			 *
			 * default: break; }
			 */
		}

		return true;
	}

	public void processPeaks(ToneMap toneMap, List<SpectralPeak> peaks) {

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();
		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		peaks.iterator();

		for (ToneMapElement toneMapElement : ttfElements) {
			toneMapElement.amplitude = 0.00001;
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

		if (overToneElement.amplitude <= overToneData) {
			overToneElement.amplitude = 0;
		} else {
			if (harmonic == 0.7 && overToneElement.amplitude > 0.01) {
				System.out.println(">>ATTENUATE " + fundamental + ", " + harmonic + ", " + overToneData + ", "
						+ overToneElement.amplitude);
			}
			// System.out.println(">>ATTENUATE " + fundamental + ", " + harmonic
			// + ", " + overToneData + ", " + overToneElement.amplitude);
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

		// double[] initHarmonics = {harmonic1Setting / 100.0,
		// harmonic2Setting / 100.0, harmonic3Setting / 100.0,
		// harmonic4Setting / 100.0, harmonic4Setting / 100.0,
		// harmonic4Setting / 100.0};

		// overtoneSet.setHarmonics(initHarmonics);

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
	private void processNote(int note, ToneMap toneMap, NoteStatusElement noteStatusElement) {
		if (!noteStatusElement.highFlag || ((noteStatusElement.offTime - noteStatusElement.onTime) < noteMinDuration)) {
			// Discard note < min duration
			return;
		}

		int index;

		int numSlots = 0;
		int numLowSlots = 0;
		double amplitude;
		double ampSum = 0;
		double FTPower;
		double FTPowerSum = 0;
		double minAmp = 0;
		double maxAmp = 0;
		double avgAmp = 0;
		double minFTPower = 0;
		double maxFTPower = 0;
		double avgFTPower = 0;
		double percentMin = 0;
		double startTime, endTime;
		int pitchIndex, startTimeIndex, endTimeIndex;

		ToneTimeFrame[] timeFrames = toneMap.getTimeFramesFrom(noteStatusElement.onTime);
		ToneMapElement startElement = null;
		ToneMapElement endElement = null;
		startTime = noteStatusElement.onTime;
		endTime = noteStatusElement.offTime;

		// across range of note
		for (ToneTimeFrame toneTimeFrame : timeFrames) {

			TimeSet timeSet = toneTimeFrame.getTimeSet();
			PitchSet pitchSet = toneTimeFrame.getPitchSet();

			ToneMapElement element = toneTimeFrame.getElement(noteStatusElement.index);
			if (startElement == null) {
				startElement = element;
			}
			endElement = element;

			element.noteState = ON;

			numSlots++;

			amplitude = element.amplitude;
			ampSum = ampSum + amplitude;
			if (maxAmp < amplitude) {
				maxAmp = amplitude;
				if (peakSwitch) {
					startTime = toneTimeFrame.getStartTime();
					startElement = element;
				}
			}
			if ((minAmp == 0) || (minAmp > amplitude))
				minAmp = amplitude;

			if (amplitude < noteLow / 100.0)
				numLowSlots++;
			if (peakSwitch && (amplitude >= noteHigh / 100.0)) {
				endTime = toneTimeFrame.getStartTime();
				endElement = element;
				break;
			}
		}

		if (startTime >= endTime)
			return;

		startElement.noteState = START;

		pitchIndex = startElement.getPitchIndex();

		ToneTimeFrame toneTimeFrame = timeFrames[timeFrames.length - 1];

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		startTime = timeSet.getTime(startElement.getTimeIndex());
		startTimeIndex = startElement.getTimeIndex();

		endElement.noteState = END;
		endTime = timeSet.getTime(endElement.getTimeIndex());
		endTimeIndex = endElement.getTimeIndex();

		avgAmp = ampSum / numSlots;
		percentMin = numLowSlots / numSlots;

		// Create noteList element object
		NoteListElement noteListElement = new NoteListElement(note, pitchIndex, startTime, endTime, startTimeIndex,
				endTimeIndex, avgAmp, maxAmp, minAmp, percentMin);

		// Cross-Register NoteList element against ToneMapMatrix elements
		for (ToneTimeFrame timeFrame : timeFrames) {

			ToneMapElement element = timeFrame.getElement(noteStatusElement.index);
			element.noteListElement = noteListElement;
		}

		// Add noteList element to noteList object
		noteList.add(noteListElement);
	}

	// Process Harmonic overtones
	private void processOvertones(ToneTimeFrame toneTimeFrame, int pitchIndex) {
		System.out.println(">>processOvertones " + pitchIndex);
		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		double f0 = pitchSet.getFreq(pitchIndex);

		double overToneFTPower;
		double overToneAmplitude;

		double freq;
		int note;
		int n = 2;

		for (double harmonic : harmonics) {
			freq = n * f0;
			note = PitchSet.freqToMidiNote(freq);
			if (note == -1 || note > pitchSet.getHighNote())
				break;
			int index = pitchSet.getIndex(note);

			ToneMapElement[] ttfElements = toneTimeFrame.getElements();

			ToneMapElement toneMapElement = ttfElements[index];
			if (toneMapElement == null || toneMapElement.amplitude == -1)
				continue;
			index = toneMapElement.getIndex();
			if (harmonic == 0.7) {
				System.out.println(">>processOvertones attenuate: " + pitchIndex + ", " + toneMapElement.amplitude);
			}
			attenuate(toneMapElement, toneMapElement.amplitude, harmonic);
			if (index > 0) {
				toneMapElement = ttfElements[index - 1];
				if (toneMapElement != null && toneMapElement.amplitude > -1) {
					// attenuate(toneMapElement, toneMapElement.amplitude,
					// harmonic);
				}
			}
			if (index < ttfElements.length - 1) {
				toneMapElement = ttfElements[index + 1];
				if (toneMapElement != null && toneMapElement.amplitude > -1) {
					// attenuate(toneMapElement, toneMapElement.amplitude,
					// harmonic);
				}
			}

			n++;
		}
	}

	private void processPeak(ToneTimeFrame toneTimeFrame, int startPeak, int endPeak, double trough,
			ToneMapElement thresholdElement, double maxAmp) {

		double ampThres = n3Setting / 100.0;
		int index;

		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();

		for (ToneMapElement toneMapElement : ttfElements) {
			index = toneMapElement.getIndex();
			if (index < startPeak) {
				continue;
			}
			if (index > endPeak) {
				break;
			}
			double amplitude = toneMapElement.amplitude / maxAmp;

			if (amplitude >= normalThreshold(toneTimeFrame, toneMapElement, thresholdElement) && amplitude > ampThres) {
				toneMapElement.amplitude = maxAmp;
				// peakIterator.getElement().postAmplitude = 1.0;
			}

			processOvertones(toneTimeFrame, index);
			// toneMapElement.amplitude = 0;

		}

	}
}