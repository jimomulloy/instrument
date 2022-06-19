
package jomu.instrument.tonemap;

import javax.swing.JPanel;

/**
 * This class defines the Tuner Sub System Data Model processing functions for
 * the ToneMap including execution of ToneMapMatrix data filtering and
 * conversion, generation of NoteList objects of MIDI notes and Control Settings
 * management through the TunerPanel class.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class TunerModel implements ToneMapConstants {

	/**
	 * TunerModel constructor. Instantiate TunerPanel
	 */
	public TunerModel(ToneMapFrame toneMapFrame) {

		this.toneMapFrame = toneMapFrame;
		tunerPanel = new TunerPanel(this);

	}

	public TunerModel(ToneMap toneMap) {
		this.toneMap = toneMap;
	}

	/**
	 * Clear current TunerModel objects after Reset
	 */
	public void clear() {
		noteList = null;
		noteStatus = null;
	}

	/**
	 * Get configuration parameters
	 */
	public void getConfig(ToneMapConfig config) {
		config.noteSustain = this.noteSustain;
		config.noteMinDuration = this.noteMinDuration;
		config.noteMaxDuration = this.noteMaxDuration;
		config.harmonic1Setting = this.harmonic1Setting;
		config.harmonic2Setting = this.harmonic2Setting;
		config.harmonic3Setting = this.harmonic3Setting;
		config.harmonic4Setting = this.harmonic4Setting;
		config.formantLowSetting = this.formantLowSetting;
		config.formantMiddleSetting = this.formantMiddleSetting;
		config.formantHighSetting = this.formantHighSetting;
		config.formantFactor = this.formantFactor;
		config.harmonicSwitch = this.harmonicSwitch;
		config.formantSwitch = this.formantSwitch;
		config.undertoneSwitch = this.undertoneSwitch;
		config.peakSwitch = this.peakSwitch;
		config.normalizeSwitch = this.normalizeSwitch;
		config.normalizeSetting = this.normalizeSetting;
		config.noteLow = this.noteLow;
		config.noteHigh = this.noteHigh;
		config.processMode = this.processMode;
	}

	/**
	 * Set configuration parameters
	 */
	public void setConfig(ToneMapConfig config) {
		// tunerPanel.noteSustainSlider.setValue(config.noteSustain);
		// tunerPanel.noteMinDurationSlider.setValue(config.noteMinDuration);
		// tunerPanel.noteMaxDurationSlider.setValue(config.noteMaxDuration);
		// tunerPanel.noteLowSlider.setValue(config.noteLow);
		// tunerPanel.noteHighSlider.setValue(config.noteHigh);
		// tunerPanel.normalizeS.setValue(config.normalizeSetting);
		// tunerPanel.harmonic1S.setValue(config.harmonic1Setting);
		// tunerPanel.harmonic2S.setValue(config.harmonic2Setting);
		// tunerPanel.harmonic3S.setValue(config.harmonic3Setting);
		// tunerPanel.harmonic4S.setValue(config.harmonic4Setting);
		// tunerPanel.formantLowS.setValue(config.formantLowSetting);
		// tunerPanel.formantMiddleS.setValue(config.formantMiddleSetting);
		// tunerPanel.formantHighS.setValue(config.formantHighSetting);
		// tunerPanel.formantFreqS.setValue(config.formantFactor);
		// tunerPanel.harmonicCB.setSelected(config.harmonicSwitch);
		harmonicSwitch = config.harmonicSwitch;
		// tunerPanel.formantCB.setSelected(config.formantSwitch);
		formantSwitch = config.formantSwitch;
		// tunerPanel.undertoneCB.setSelected(config.undertoneSwitch);
		undertoneSwitch = config.undertoneSwitch;
		// tunerPanel.normalizeCB.setSelected(config.normalizeSwitch);
		normalizeSwitch = config.normalizeSwitch;
		// tunerPanel.peakCB.setSelected(config.peakSwitch);
		peakSwitch = config.peakSwitch;
		processMode = config.processMode;
		if (processMode == NOTE_MODE) {
			// tunerPanel.noteModeB.setSelected(true);
			// tunerPanel.beatModeB.setSelected(false);

		}
		if (processMode == BEAT_MODE) {
			// tunerPanel.beatModeB.setSelected(true);
			// tunerPanel.noteModeB.setSelected(false);
		}

	}

	/**
	 * Execute Tuner processing function on current ToneMap objects Involves
	 * filtering and conversion of data to produce NoteList object of MIDI note data
	 */
	public boolean execute(ProgressListener progressListener) {

		// Get curent ToneMap objects
		this.progressListener = progressListener;
		toneMap = toneMapFrame.getToneMap();

		timeSet = toneMap.getTimeSet();
		pitchSet = toneMap.getPitchSet();
		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		toneMapMatrix = toneMap.getMatrix();
		toneMapMatrix.reset();

		initOvertoneSet();
		harmonics = overtoneSet.getHarmonics();
		formants = overtoneSet.getFormants();

		initFormants();

		if (formantSwitch == true) {
			applyFormants(toneMapMatrix);
			toneMapMatrix.update();
		}

		if (normalizeSwitch == true) {
			if (!normalize())
				return false;
			toneMapMatrix.update();
		}

		// Switch to selected processing mode
		switch (processMode) {
		case NOTE_MODE:
			if (!noteScan())
				return false;
			break;

		case BEAT_MODE:
			if (!beatScan())
				return false;
			break;

		default:
			break;
		}

		return true;
	}

	public boolean tune() {

		timeSet = toneMap.getTimeSet();
		pitchSet = toneMap.getPitchSet();
		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		toneMapMatrix = toneMap.getMatrix();
		System.out.println("Matrix reset");
		toneMapMatrix.reset();

		initOvertoneSet();
		harmonics = overtoneSet.getHarmonics();
		formants = overtoneSet.getFormants();

		initFormants();

		if (formantSwitch == true) {
			applyFormants(toneMapMatrix);
			toneMapMatrix.update();
			System.out.println("Matrix update");

		}

		if (normalizeSwitch == true) {
			if (!normalize())
				return false;
			toneMapMatrix.update();
		}

		if (harmonicSwitch == true) {
			// Process harmonic overtones
			processOvertones();
			toneMapMatrix.update();
		}

		// Switch to selected processing mode
		switch (processMode) {
		case NOTE_MODE:
			if (!noteScan())
				return false;
			break;

		case BEAT_MODE:
			if (!beatScan())
				return false;
			break;

		default:
			break;
		}

		return true;
	}

	public void setThreshhold(int lowThreshhold, int highThreshhold) {
		tunerPanel.noteLowSlider.setValue(lowThreshhold);
		tunerPanel.noteHighSlider.setValue(highThreshhold);
	}

	public double getDuration() {
		return duration;
	}

	public double getEndTime() {
		return timeEnd;
	}

	public int getHighPitch() {
		return pitchHigh;
	}

	public int getLowPitch() {
		return pitchLow;
	}

	public double getStartTime() {
		return timeStart;
	}

	public JPanel getPanel() {
		return tunerPanel;
	}

	public void setTime(TimeSet timeSet) {

		tunerPanel.timeControl.setTimeMax((int) (timeSet.getEndTime() - timeSet.getStartTime()));

	}

	public void setPitch(PitchSet pitchSet) {
		tunerPanel.pitchControl.setPitchRange((int) (pitchSet.getLowNote()), (int) (pitchSet.getHighNote()));

	}

	/**
	 * Scan through ToneMapMatrix extracting MIDI note data into NoteList object
	 * Apply filtering and conversion processing on basis of Tuner Parameters
	 */
	private boolean noteScan() {

		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		// Initialise noteList object
		noteList = new NoteList();
		// Initialise noteStatus object
		noteStatus = new NoteStatus(pitchSet);

		// Iterate through ToneMapMatrix processing data elements to derive NoteList
		// elements
		// Scan through each Matrix Time coordinate from Start to End limit
		// for each MAtrix Pitch coordinate from Low to High limit.
		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();
		mapIterator.firstPitch();
		mapIterator.setPitchIndex(pitchSet.pitchToIndex(getLowPitch()));
		do {
			progressListener.setProgress(
					(int) (((double) mapIterator.getIndex() / (double) toneMapMatrix.getMatrixSize()) * 100.0));
			note = pitchSet.getNote(mapIterator.getPitchIndex());
			noteStatusElement = noteStatus.getNote(note);
			mapIterator.firstTime();
			// mapIterator.setTimeIndex(timeSet.timeToIndex(getStartTime()));
			do {
				index = mapIterator.getIndex();
				toneMapElement = mapIterator.getElement();
				if (toneMapElement == null || toneMapElement.preAmplitude == -1)
					continue;

				if (formantSwitch == true) {
					// Apply formant conversion
					// applyFormant(toneMapElement, note);
				}

				amplitude = mapIterator.getElement().postAmplitude;

				time = timeSet.getTime(mapIterator.getTimeIndex());
				// Establish range of Matrix entries within a sequence constituting
				// a continuous note within the bounds of the Tuner parameters
				switch (noteStatusElement.state) {
				case OFF:
					if (amplitude >= (double) noteLow / 100.0) {
						noteStatusElement.state = ON;
						noteStatusElement.onTime = time;
						noteStatusElement.onIndex = index;
						noteStatusElement.offTime = 0.0;
						noteStatusElement.offIndex = 0;
						if (amplitude >= (double) noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
					}
					break;

				case ON:
					if (amplitude < (double) noteLow / 100.0
							|| (time - noteStatusElement.onTime) > (double) noteMaxDuration) {
						noteStatusElement.state = PENDING;
						noteStatusElement.offTime = time;
						noteStatusElement.offIndex = index; // ??
					} else {
						if (amplitude >= (double) noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
					}
					break;

				case PENDING:
					if (amplitude >= (double) noteLow / 100.0) {
						if ((time - noteStatusElement.offTime) < (noteSustain)
								&& (noteStatusElement.offTime - noteStatusElement.onTime) <= (double) noteMaxDuration) {
							noteStatusElement.state = ON;
							noteStatusElement.offTime = 0.0;
							noteStatusElement.offIndex = 0;
							if (amplitude >= (double) noteHigh / 100.0) {
								noteStatusElement.highFlag = true;
							}
						} else {
							// Process candididate note
							processNote();
							noteStatusElement.state = ON;
							noteStatusElement.onTime = time;
							noteStatusElement.onIndex = index;
							noteStatusElement.offTime = 0.0;
							noteStatusElement.offIndex = 0;
							if (amplitude >= (double) noteHigh / 100.0) {
								noteStatusElement.highFlag = true;
							}
						}
					} else {
						if ((time - noteStatusElement.offTime) >= (noteSustain)
								|| (noteStatusElement.offTime - noteStatusElement.onTime) > (double) noteMaxDuration) {
							// Process candidate note
							processNote();
							noteStatusElement.state = OFF;
							noteStatusElement.onTime = 0.0;
							noteStatusElement.onIndex = 0;
							noteStatusElement.offTime = 0.0;
							noteStatusElement.offIndex = 0;
							noteStatusElement.highFlag = false;
						}
					}

					break;

				default:
					break;
				}

			} while (mapIterator.nextTime());

			switch (noteStatusElement.state) {
			case OFF:
				break;

			case ON:
				noteStatusElement.offTime = time;
				noteStatusElement.offIndex = index; // ??

			case PENDING:

				// Process candidate note
				processNote();
				noteStatusElement.state = OFF;
				noteStatusElement.onTime = 0.0;
				noteStatusElement.onIndex = 0;
				noteStatusElement.offTime = 0.0;
				noteStatusElement.offIndex = 0;
				noteStatusElement.highFlag = false;
				break;

			default:
				break;
			}

		} while (mapIterator.nextPitch() && pitchSet.pitchToIndex(getHighPitch()) >= mapIterator.getPitchIndex());

		return true;
	}

	// Process individual Note across sequence of ToneMapMatrix elements
	private void processNote() {

		if (noteStatusElement.highFlag == false) {
			// Discard note - no high flag
			return;
		}

		if ((noteStatusElement.offTime - noteStatusElement.onTime) < (double) noteMinDuration) {
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
		int pitchIndex, startTimeIndex, endTimeIndex, startIndex, endIndex;

		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();
		mapIterator.setIndex(noteStatusElement.onIndex);
		startIndex = noteStatusElement.onIndex;
		endIndex = noteStatusElement.offIndex;

		// Extract parameters from ToneMapMatrix elements
		// across range of note
		do {

			numSlots++;

			FTPower = mapIterator.getElement().postFTPower;
			FTPowerSum = FTPowerSum + FTPower;
			if (maxFTPower < FTPower) {
				maxFTPower = FTPower;
				if (peakSwitch) {
					startIndex = mapIterator.getIndex();
				}
			}
			if ((minFTPower == 0) || (minFTPower > FTPower))
				minFTPower = FTPower;

			amplitude = mapIterator.getElement().postAmplitude;
			ampSum = ampSum + amplitude;
			if (maxAmp < amplitude)
				maxAmp = amplitude;
			if ((minAmp == 0) || (minAmp > amplitude))
				minAmp = amplitude;

			if (amplitude < (double) noteLow / 100.0)
				numLowSlots++;
			if (peakSwitch && (amplitude >= (double) noteHigh / 100.0))
				endIndex = mapIterator.getIndex();

			mapIterator.getElement().noteState = ON;

		} while (mapIterator.nextTime() && mapIterator.getIndex() <= noteStatusElement.offIndex);

		if (startIndex > endIndex)
			return;
		if (startIndex == endIndex)
			endIndex++;

		mapIterator.setIndex(startIndex);
		mapIterator.getElement().noteState = START;

		pitchIndex = mapIterator.getPitchIndex();

		startTime = timeSet.getTime(mapIterator.getTimeIndex());
		startTimeIndex = mapIterator.getTimeIndex();

		mapIterator.setIndex(endIndex);
		mapIterator.getElement().noteState = END;
		endTime = timeSet.getTime(mapIterator.getTimeIndex());
		endTimeIndex = mapIterator.getTimeIndex();

		avgFTPower = FTPowerSum / numSlots;
		avgAmp = ampSum / numSlots;
		percentMin = numLowSlots / numSlots;

		// Create noteList element object
		NoteListElement noteListElement = new NoteListElement(note, pitchIndex, startTime, endTime, startTimeIndex,
				endTimeIndex, avgFTPower, maxFTPower, minFTPower, avgAmp, maxAmp, minAmp, percentMin);
		System.out.println("New Note: " + avgAmp + ", " + maxAmp + ", " + minAmp);

		mapIterator.setIndex(startIndex);

		// Cross-Register NoteList element against ToneMapMatrix elements
		do {

			mapIterator.getElement().noteListElement = noteListElement;

		} while (mapIterator.nextTime() && mapIterator.getIndex() <= endIndex);

		if (harmonicSwitch == true) {
			// Process harmonic overtones
			processOvertones(noteListElement);
		}

		if (undertoneSwitch == true) {
			// Process undertones
			processUndertones(noteListElement);
		}

		// Add noteList element to noteList object
		noteList.add(noteListElement);
	}

	// Process Harmonic overtones
	private void processOvertones(NoteListElement noteListElement) {

		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();

		mapIterator.first();
		mapIterator.setPitchIndex(noteListElement.pitchIndex);
		mapIterator.setTimeIndex(noteListElement.startTimeIndex);

		double f0 = pitchSet.getFreq(mapIterator.getPitchIndex());
		int lastNote = pitchSet.getNote(mapIterator.getPitchIndex());

		double overToneFTPower;
		double overToneAmplitude;

		double freq;
		int note;
		int n = 2;

		for (int i = 0; i < harmonics.length; i++) {
			freq = n * f0;
			note = pitchSet.freqToMidiNote(freq);
			if (note == -1 || note > pitchSet.getHighNote())
				break;
			mapIterator.setTimeIndex(noteListElement.startTimeIndex);
			mapIterator.setPitchIndex(mapIterator.getPitchIndex() + note - lastNote);
			do {

				if (mapIterator.getElement() == null || mapIterator.getElement().preAmplitude == -1)
					continue;

				attenuate(mapIterator.getElement(), noteListElement.avgFTPower, harmonics[i]);
			} while (mapIterator.nextTime() && mapIterator.getTimeIndex() <= noteListElement.endTimeIndex);

			lastNote = note;
			n++;

		}
	}

	// Attenuate audio data power values for given Harmonic overtone
	private void attenuate(ToneMapElement overToneElement, double fundamental, double harmonic) {

		double overToneData = fundamental * harmonic;

		if (overToneElement.postFTPower <= overToneData) {
			overToneElement.postFTPower = 0;
		} else {
			overToneElement.postFTPower -= overToneData;
		}

		overToneElement.postAmplitude = toneMapMatrix.FTPowerToAmp(overToneElement.postFTPower);

		// overToneElement.postAmplitude =
		// (overToneElement.postFTPower-toneMapMatrix.getMinFTPower()) /
		// (toneMapMatrix.getMaxFTPower()-toneMapMatrix.getMinFTPower());

	}

	// process undertones
	private void processUndertones(NoteListElement noteListElement) {

		NoteListElement underNote = null;

		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();

		mapIterator.first();
		mapIterator.setPitchIndex(noteListElement.pitchIndex);
		mapIterator.setTimeIndex(noteListElement.startTimeIndex);
		if (mapIterator.prevPitch()) {

			int startTime = noteListElement.startTimeIndex;
			int endTime = noteListElement.endTimeIndex;

			do {
				underNote = mapIterator.getElement().noteListElement;
				if (underNote != null) {

					if (startTime == underNote.startTimeIndex && endTime == underNote.endTimeIndex) {
						// check this note note higher amplitude;
						if (noteListElement.avgAmp < underNote.avgAmp) {
							noteListElement.underTone = true;
							// this note is undertone

						} else {
							underNote.underTone = true;
							// lower note is undertone

						}
						break;
					}

					if (startTime >= underNote.startTimeIndex && endTime <= underNote.endTimeIndex) {
						// check this note note higher amplitude;
						noteListElement.underTone = true;
						// this note is undertone
						break;
					}

					if (endTime < underNote.endTimeIndex) {
						// end < lower end
						break;
					}

					if (startTime <= underNote.startTimeIndex) {
						underNote.underTone = true;
						// lower note is undertone

					}

					mapIterator.setTimeIndex(underNote.endTimeIndex);

				}

			} while (mapIterator.nextTime() && mapIterator.getTimeIndex() <= endTime);

		}

	}

	/**
	 * Scan through ToneMapMatrix extracting MIDI beats of dominant rythmic note
	 * data
	 */
	private boolean beatScan() {

		double amplitude;
		int numSlots;
		double ampSum = 0;
		double avgAmp = 0;
		int startTimeIndex = 0, endTimeIndex = 0;
		boolean powerON = false, powerHigh;
		double time = 0, lastTime = 0;

		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		noteList = new NoteList();

		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();
		mapIterator.firstTime();
		// mapIterator.setTimeIndex(timeSet.timeToIndex(getStartTime()));

		do {
			progressListener.setProgress(
					(int) (((double) mapIterator.getIndex() / (double) toneMapMatrix.getMatrixSize()) * 100.0));
			time = timeSet.getTime(mapIterator.getTimeIndex());
			mapIterator.firstPitch();
			mapIterator.setPitchIndex(pitchSet.pitchToIndex(getLowPitch()));
			numSlots = 0;
			ampSum = 0;

			do {
				numSlots++;
				index = mapIterator.getIndex();
				note = pitchSet.getNote(mapIterator.getPitchIndex());
				toneMapElement = mapIterator.getElement();
				if (toneMapElement == null || toneMapElement.preAmplitude == -1)
					continue;

				if (formantSwitch == true) {
					// Apply formant conversion
					// applyFormant(toneMapElement, note);
				}
				amplitude = mapIterator.getElement().postAmplitude;
				if (amplitude > (double) noteHigh / 100.0)
					amplitude = (double) noteHigh / 100.0;
				ampSum = ampSum + amplitude;

			} while (mapIterator.nextPitch() && pitchSet.pitchToIndex(getHighPitch()) >= mapIterator.getPitchIndex());
			time = timeSet.getTime(mapIterator.getTimeIndex());
			avgAmp = ampSum / numSlots;
			if (powerON) {
				if ((avgAmp < (double) noteLow / 100.0) || ((time - lastTime) > ((double) noteMaxDuration))) {
					lastTime = time;
					powerON = false;
					endTimeIndex = mapIterator.getTimeIndex();
					processBeat(startTimeIndex, endTimeIndex);
				}
			} else {
				if (avgAmp >= (double) noteLow / 100.0) {
					lastTime = time;
					powerON = true;
					startTimeIndex = mapIterator.getTimeIndex();
				}
			}

		} while (mapIterator.nextTime());

		if (powerON) {
			powerON = false;
			endTimeIndex = mapIterator.getTimeIndex();
			processBeat(startTimeIndex, endTimeIndex);
		}

		return true;
	}

	private void processBeat(int startTimeIndex, int endTimeIndex) {

		int index;
		int numSlots = 0;
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
		int pitchIndex = 0, startIndex = 0, endIndex = 0;
		double maxAmpSum = 0;
		int maxNote = 0;

		if (startTimeIndex > endTimeIndex)
			return;
		if (startTimeIndex == endTimeIndex)
			endTimeIndex++;

		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();

		mapIterator.firstPitch();
		mapIterator.setPitchIndex(pitchSet.pitchToIndex(getLowPitch()));

		do {
			mapIterator.firstTime();
			mapIterator.setTimeIndex(startTimeIndex);
			ampSum = 0;

			do {
				index = mapIterator.getIndex();
				note = pitchSet.getNote(mapIterator.getPitchIndex());
				numSlots++;
				amplitude = mapIterator.getElement().postAmplitude;
				ampSum = ampSum + amplitude;

			} while (mapIterator.nextTime() && endTimeIndex >= mapIterator.getTimeIndex());

			if (ampSum > maxAmpSum) {
				maxAmpSum = ampSum;
				maxNote = note;
				pitchIndex = mapIterator.getPitchIndex();
			}

		} while (mapIterator.nextPitch() && pitchSet.pitchToIndex(getHighPitch()) >= mapIterator.getPitchIndex());

		mapIterator.firstPitch();
		mapIterator.firstTime();
		mapIterator.setPitchIndex(pitchIndex);
		mapIterator.setTimeIndex(startTimeIndex);
		startIndex = mapIterator.getIndex();
		startTime = timeSet.getTime(mapIterator.getTimeIndex());
		mapIterator.setTimeIndex(endTimeIndex);
		endIndex = mapIterator.getIndex();
		endTime = timeSet.getTime(mapIterator.getTimeIndex());

		avgFTPower = toneMapMatrix.getAvgFTPower();
		avgAmp = toneMapMatrix.getAvgAmplitude();
		maxFTPower = toneMapMatrix.getMaxFTPower();
		minFTPower = toneMapMatrix.getMinFTPower();
		maxAmp = toneMapMatrix.getMaxAmplitude();
		minAmp = toneMapMatrix.getMinAmplitude();

		percentMin = 0;

		NoteListElement noteListElement = new NoteListElement(maxNote, pitchIndex, startTime, endTime, startTimeIndex,
				endTimeIndex, avgFTPower, maxFTPower, minFTPower, avgAmp, maxAmp, minAmp, percentMin);

		mapIterator.setIndex(startIndex);

		do {

			mapIterator.getElement().noteListElement = noteListElement;

		} while (mapIterator.nextTime() && mapIterator.getIndex() <= endIndex);

		noteList.add(noteListElement);
	}

	public NoteList getNoteList() {
		return noteList;
	}

	/**
	 * Normalise peak amplitudes
	 */
	private boolean normalize() {

		double amplitude, maxAmp = 0;
		int startPeak, endPeak, lastStartPeak, lastEndPeak;
		int index, thresholdIndex = 0;
		int note;
		double troughAmp, peakAmp, lastAmp, lastPeakAmp;

		// System.out.println("In normalize: " + normalizeSetting + ", " + noteHigh);

		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();
		mapIterator.firstTime();

		// mapIterator.setTimeIndex(timeSet.timeToIndex(getStartTime()));

		do {
			time = timeSet.getTime(mapIterator.getTimeIndex());
			mapIterator.firstPitch();
			mapIterator.setPitchIndex(pitchSet.pitchToIndex(getLowPitch()));
			maxAmp = 0;

			do {
				index = mapIterator.getIndex();
				note = pitchSet.getNote(mapIterator.getPitchIndex());
				amplitude = mapIterator.getElement().postAmplitude;

				if (amplitude > maxAmp) {
					maxAmp = amplitude;
					thresholdIndex = index;
				}

			} while (mapIterator.nextPitch() && pitchSet.pitchToIndex(getHighPitch()) >= mapIterator.getPitchIndex());

			// System.out.println("max amp: " + maxAmp + ", " + time);

			troughAmp = toneMapMatrix.getMaxAmplitude();
			time = timeSet.getTime(mapIterator.getTimeIndex());
			mapIterator.firstPitch();
			mapIterator.setPitchIndex(pitchSet.pitchToIndex(getLowPitch()));
			startPeak = 0;
			endPeak = 0;
			lastAmp = 0;
			maxAmp = 0;
			if (!n1Switch) {
				thresholdIndex = 0;
			}
			lastStartPeak = 0;
			lastEndPeak = 0;
			lastPeakAmp = 0;
			int peakcount = (int) n1Setting;
			double peakFactor = (double) n2Setting;

			do {

				index = mapIterator.getIndex();
				note = pitchSet.getNote(mapIterator.getPitchIndex());
				amplitude = mapIterator.getElement().postAmplitude;
				if (amplitude > lastAmp)
					startPeak = index;
				if (amplitude >= lastAmp)
					endPeak = index;
				if (amplitude < lastAmp) {
					if (lastStartPeak != 0) {
						if (troughAmp == 0 || (lastAmp / troughAmp) > peakFactor) {
							if ((troughAmp == 0 || (lastPeakAmp / troughAmp) > peakFactor) && peakcount > 0) {
								peakcount = peakcount - 1;
								processPeak(lastStartPeak, lastEndPeak, troughAmp, thresholdIndex);
							}
							lastPeakAmp = lastAmp;
							lastStartPeak = startPeak;
							lastEndPeak = endPeak;
							startPeak = 0;
							endPeak = 0;
							troughAmp = toneMapMatrix.getMaxAmplitude();
						}
					} else {
						if (startPeak != 0) {
							if (troughAmp == 0 || (lastAmp / troughAmp) > peakFactor) {
								lastPeakAmp = lastAmp;
								lastStartPeak = startPeak;
								lastEndPeak = endPeak;
								startPeak = 0;
								endPeak = 0;
								troughAmp = toneMapMatrix.getMaxAmplitude();
							}
						}
					}

				}
				if (amplitude < troughAmp)
					troughAmp = amplitude;
				lastAmp = amplitude;

			} while (mapIterator.nextPitch() && pitchSet.pitchToIndex(getHighPitch()) >= mapIterator.getPitchIndex());

			if (lastStartPeak != 0 && peakcount > 0) {
				if (troughAmp == 0 || (lastPeakAmp / troughAmp) > peakFactor) {
					processPeak(lastStartPeak, lastEndPeak, troughAmp, thresholdIndex);
				}
			}

		} while (mapIterator.nextTime());

		return true;
	}

	private void processPeak(int startPeak, int endPeak, double trough, int thresholdIndex) {

		double ampThres = (double) n3Setting / 100.0;
		int index, note;
		ToneMapMatrix.Iterator peakIterator = toneMapMatrix.newIterator();
		peakIterator.setIndex(startPeak);
		note = pitchSet.getNote(peakIterator.getPitchIndex());
		System.out.println("In processPeak: " + note + ", " + startPeak + ", " + endPeak);
		do {
			note = pitchSet.getNote(peakIterator.getPitchIndex());
			index = peakIterator.getIndex();
			amplitude = peakIterator.getElement().postAmplitude;
			System.out.println("In processPeak loop: " + note + ", " + startPeak + ", " + endPeak + ", " + amplitude);

			if (amplitude >= normalThreshold(index, thresholdIndex) && amplitude > ampThres) {
				peakIterator.getElement().postFTPower = toneMapMatrix.getMaxFTPower();
				peakIterator.getElement().postAmplitude = toneMapMatrix
						.FTPowerToAmp(peakIterator.getElement().postFTPower);
				// peakIterator.getElement().postAmplitude = 1.0;
				System.out.println("New amp: " + peakIterator.getElement().postAmplitude);

			}

		} while (peakIterator.nextPitch() && endPeak >= peakIterator.getIndex());

	}

	private double normalThreshold(int normalIndex, int thresholdIndex) {

		double thresholdAmp;
		double thresholdFreq, normalFreq;
		int thresholdNote, normalNote;

		ToneMapMatrix.Iterator normalIterator = toneMapMatrix.newIterator();
		normalIterator.setIndex(normalIndex);
		if (thresholdIndex == 0 || normalIndex < thresholdIndex) {
			thresholdAmp = 1.0;
			thresholdFreq = pitchSet.getFreq(pitchSet.pitchToIndex(getLowPitch()));
			thresholdNote = pitchSet.getNote(pitchSet.pitchToIndex(getLowPitch()));

		} else {

			ToneMapMatrix.Iterator thresholdIterator = toneMapMatrix.newIterator();
			thresholdIterator.setIndex(thresholdIndex);

			thresholdAmp = thresholdIterator.getElement().postAmplitude;
			thresholdFreq = pitchSet.getFreq(thresholdIterator.getPitchIndex());
			thresholdNote = pitchSet.getNote(thresholdIterator.getPitchIndex());
		}

		normalFreq = pitchSet.getFreq(normalIterator.getPitchIndex());
		normalNote = pitchSet.getNote(normalIterator.getPitchIndex());
		note = pitchSet.getNote(normalIterator.getPitchIndex());
		double threshold = 0;
		if (n2Switch) {
			threshold = ((double) noteHigh / 100.0) * (thresholdAmp) / ((double) n5Setting / 10.0
					+ (((double) n4Setting / (double) normalizeSetting) * (double) (normalNote - thresholdNote)));
		} else {
			threshold = ((double) noteHigh / 100.0) * (thresholdAmp) / ((double) n5Setting / 10.0
					+ (((double) n4Setting / (double) normalizeSetting) * (double) (normalFreq - thresholdFreq)));
		}

		System.out.println("In normalThreshold: " + note + ", " + threshold + ", " + thresholdAmp + ", " + thresholdFreq
				+ ", " + normalFreq);
		return threshold;
	}

	public ToneMapFrame toneMapFrame;
	private ToneMap toneMap;

	private TunerPanel tunerPanel;

	private double sampleRate;
	private int numChannels;
	private double sampleBitSize;
	private ProgressListener progressListener;
	private String errStr;

	private double duration, seconds;

	public double timeStart = INIT_TIME_START;
	public double timeEnd = INIT_TIME_END;

	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;

	public int noteLow = INIT_NOTE_LOW;
	public int noteHigh = INIT_NOTE_HIGH;
	public int noteSustain = INIT_NOTE_SUSTAIN;
	public int noteMinDuration = INIT_NOTE_MIN_DURATION;
	public int noteMaxDuration = INIT_NOTE_MAX_DURATION;

	public int noiseLow = INIT_NOISE_LOW;
	public int noiseHigh = INIT_NOISE_HIGH;

	public int n1Setting = 100;
	public int n2Setting = 100;
	public int n3Setting = 100;
	public int n4Setting = 100;
	public int n5Setting = 100;
	public int n6Setting = 100;

	public int harmonic1Setting = 60;
	public int harmonic2Setting = 50;
	public int harmonic3Setting = 30;
	public int harmonic4Setting = 20;
	public int formantLowSetting = 0;
	public int formantMiddleSetting = 50;
	public int formantHighSetting = 100;
	public int formantFactor = 0;

	public int droneSetting = 100;
	public int undertoneSetting = 100;
	public int normalizeSetting = 100;
	public int spikeSetting = 100;

	public boolean harmonicSwitch;
	public boolean formantSwitch;
	public boolean undertoneSwitch;
	public boolean normalizeSwitch;
	public boolean droneSwitch;
	public boolean spikeSwitch;
	public boolean peakSwitch;
	public boolean n1Switch;
	public boolean n2Switch;
	public boolean formantAdd;
	public boolean harmonicAdd;

	public int processMode = NONE;

	private ToneMapMatrix toneMapMatrix;
	private ToneMapElement element;

	private int matrixLength;

	private TimeSet timeSet;
	private PitchSet pitchSet;

	private int timeRange;
	private int pitchRange;
	private double amplitude;

	private int index;
	private NoteSequence noteSequence;
	private NoteSequenceElement noteSequenceElement;
	private NoteList noteList;
	private NoteListElement noteListElement;
	private ToneMapMatrix.Iterator mapIterator;
	private ToneMapElement toneMapElement;

	OvertoneSet overtoneSet;
	double[] harmonics;
	double[][] formants;

	private long tick;
	private double time;

	private NoteStatus noteStatus;
	private NoteStatusElement noteStatusElement;
	private int note;
	private int velocity;

	private double formantLowFreq;
	private double formantHighFreq;
	private double formantMidFreq;

	private void initOvertoneSet() {

		overtoneSet = new OvertoneSet();

		double[] initHarmonics = { (double) harmonic1Setting / 100.0, (double) harmonic2Setting / 100.0,
				(double) harmonic3Setting / 100.0, (double) harmonic4Setting / 100.0, (double) harmonic4Setting / 100.0,
				(double) harmonic4Setting / 100.0 };

		overtoneSet.setHarmonics(initHarmonics);

	}

	private void initFormants() {

		formantLowFreq = PitchSet.getMidiFreq(getLowPitch()) + (formantLowSetting / 100.0)
				* (PitchSet.getMidiFreq(getHighPitch()) - PitchSet.getMidiFreq(getLowPitch()));

		formantHighFreq = PitchSet.getMidiFreq(getLowPitch()) + (formantHighSetting / 100.0)
				* (PitchSet.getMidiFreq(getHighPitch()) - PitchSet.getMidiFreq(getLowPitch()));
		formantMidFreq = PitchSet.getMidiFreq(getLowPitch()) + (formantMiddleSetting / 100.0)
				* (PitchSet.getMidiFreq(getHighPitch()) - PitchSet.getMidiFreq(getLowPitch()));

	}

	// Apply formant conversion to ToneMapMatrix element data
	private void applyFormant(ToneMapElement element, int note) {

		if (formantMidFreq < formantLowFreq || formantMidFreq > formantHighFreq)
			return;

		double noteFreq = pitchSet.getMidiFreq(note);

		if (noteFreq < formantLowFreq || noteFreq > formantHighFreq)
			return;

		if (noteFreq <= formantMidFreq) {
			element.postFTPower = element.postFTPower * (1.0 - (((double) formantFactor / 100.0)
					* ((noteFreq - formantLowFreq) / (formantMidFreq - formantLowFreq))));

		} else {
			element.postFTPower = element.postFTPower * (1.0 - (((double) formantFactor / 100.0)
					* ((formantHighFreq - noteFreq) / (formantHighFreq - formantMidFreq))));

		}

		// element.postAmplitude = toneMapMatrix.FTPowerToAmp(element.postFTPower);

	}

	// Apply formant conversion to ToneMapMatrix element data
	private void applyFormants(ToneMapMatrix matrix) {

		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		ToneMapMatrix.Iterator mapIterator = matrix.newIterator();
		mapIterator.firstPitch();
		mapIterator.setPitchIndex(pitchSet.pitchToIndex(getLowPitch()));
		do {
			note = pitchSet.getNote(mapIterator.getPitchIndex());
			mapIterator.firstTime();
			// mapIterator.setTimeIndex(timeSet.timeToIndex(getStartTime()));
			do {
				index = mapIterator.getIndex();
				toneMapElement = mapIterator.getElement();
				if (toneMapElement == null || toneMapElement.preAmplitude == -1)
					continue;
				applyFormant(toneMapElement, note);

			} while (mapIterator.nextTime());

		} while (mapIterator.nextPitch() && pitchSet.pitchToIndex(getHighPitch()) >= mapIterator.getPitchIndex());

		return;
	}

	private void processOvertones() {

		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();
		mapIterator.firstPitch();
		mapIterator.setPitchIndex(pitchSet.pitchToIndex(getLowPitch()));
		do {
			note = pitchSet.getNote(mapIterator.getPitchIndex());
			mapIterator.firstTime();
			// mapIterator.setTimeIndex(timeSet.timeToIndex(getStartTime()));
			do {
				index = mapIterator.getIndex();
				toneMapElement = mapIterator.getElement();
				if (toneMapElement == null || toneMapElement.preAmplitude == -1)
					continue;
				applyHarmonics(mapIterator.getPitchIndex(), mapIterator.getTimeIndex());

			} while (mapIterator.nextTime());

		} while (mapIterator.nextPitch() && pitchSet.pitchToIndex(getHighPitch()) >= mapIterator.getPitchIndex());

		return;
	}

	// Process Harmonic overtones
	private void applyHarmonics(int pitchIndex, int timeIndex) {

		ToneMapMatrix.Iterator mapIterator = toneMapMatrix.newIterator();

		mapIterator.first();
		mapIterator.setPitchIndex(pitchIndex);
		mapIterator.setTimeIndex(timeIndex);
		ToneMapElement fundamentalElement = mapIterator.getElement();

		double f0 = pitchSet.getFreq(mapIterator.getPitchIndex());
		int lastNote = pitchSet.getNote(mapIterator.getPitchIndex());

		double freq;
		int note;
		int n = 2;

		for (int i = 0; i < harmonics.length; i++) {
			freq = n * f0;
			note = PitchSet.freqToMidiNote(freq);
			if (note == -1 || note > pitchSet.getHighNote())
				break;
			mapIterator.setTimeIndex(timeIndex);
			mapIterator.setPitchIndex(mapIterator.getPitchIndex() + note - lastNote);

			if (mapIterator.getElement() != null && mapIterator.getElement().preAmplitude != -1) {
				attenuate(mapIterator.getElement(), fundamentalElement.postFTPower, harmonics[i]);
			}

			lastNote = note;
			n++;

		}
	}

}