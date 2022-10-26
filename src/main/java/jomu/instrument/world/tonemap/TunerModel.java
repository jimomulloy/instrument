
package jomu.instrument.world.tonemap;

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

	private ToneMap toneMap;

	private double sampleRate;
	private int numChannels;
	private double sampleBitSize;
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

	public int harmonic1Setting = 100;
	public int harmonic2Setting = 100;
	public int harmonic3Setting = 100;
	public int harmonic4Setting = 100;
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

	public int processMode = NOTE_MODE;

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
	private ToneMapElement toneMapElement;

	double[] harmonics;
	double[][] formants;

	private long tick;
	private double time;

	private int note;
	private int velocity;

	private double formantLowFreq;
	private double formantHighFreq;
	private double formantMidFreq;

	/**
	 * TunerModel constructor. Instantiate TunerPanel
	 */
	public TunerModel(ToneMap toneMap) {

		this.toneMap = toneMap;
	}

	/**
	 * Clear current TunerModel objects after Reset
	 */
	public void clear() {
		noteList = null;
	}

	/**
	 * Scan through ToneMapMatrix extracting MIDI note data into NoteList object
	 * Apply filtering and conversion processing on basis of Tuner Parameters
	 */
	public boolean noteScan(ToneMap toneMap) {

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();
		ToneTimeFrame previousToneTimeFrame = toneMap.getPreviousTimeFrame();
		NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
		NoteStatus previousNoteStatus = noteStatus;
		if (previousToneTimeFrame != null) {
			previousNoteStatus = previousToneTimeFrame.getNoteStatus();
		}
		// timeRange = toneTimeFrame.getTimeSet().getRange();
		// pitchRange = toneTimeFrame.getPitchSet().getRange();

		// Initialise noteList object
		noteList = new NoteList();
		// Initialise noteStatus object

		NoteStatusElement noteStatusElement = null;
		NoteStatusElement previousNoteStatusElement = null;

		// Iterate through ToneTimeFrame processing data elements to derive NoteList
		// elements
		// Scan through each Pitch coordinate from Low to High limit.
		ToneMapElement[] ttfElements = toneTimeFrame.getElements();
		for (ToneMapElement toneMapElement : ttfElements) {
			note = pitchSet.getNote(toneMapElement.getPitchIndex());
			previousNoteStatusElement = previousNoteStatus.getNote(note);
			noteStatusElement = noteStatus.getNote(note);
			if (toneMapElement == null || toneMapElement.amplitude == -1)
				continue;

			amplitude = toneMapElement.amplitude;

			time = timeSet.getTime(toneMapElement.getTimeIndex());
			// Establish range of Matrix entries within a sequence constituting
			// a continuous note within the bounds of the Tuner parameters
			switch (previousNoteStatusElement.state) {
			case OFF:
				if (amplitude >= (double) noteLow / 100.0) {
					noteStatusElement.state = ON;
					noteStatusElement.onTime = time;
					noteStatusElement.offTime = 0.0;
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
						if (amplitude >= (double) noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
					} else {
						// Process candididate note
						processNote(toneMap, noteStatusElement);
						noteStatusElement.state = ON;
						noteStatusElement.onTime = time;
						noteStatusElement.offTime = 0.0;
						if (amplitude >= (double) noteHigh / 100.0) {
							noteStatusElement.highFlag = true;
						}
					}
				} else {
					if ((time - noteStatusElement.offTime) >= (noteSustain)
							|| (noteStatusElement.offTime - noteStatusElement.onTime) > (double) noteMaxDuration) {
						// Process candidate note
						processNote(toneMap, noteStatusElement);
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

	// Process individual Note across sequence of ToneMapMatrix elements
	private void processNote(ToneMap toneMap, NoteStatusElement noteStatusElement) {

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
		int pitchIndex, startTimeIndex, endTimeIndex;

		ToneTimeFrame[] timeFrames = toneMap.getTimeFramesFrom(noteStatusElement.onTime);
		ToneMapElement startElement = null;
		ToneMapElement endElement = null;
		startTime = noteStatusElement.onTime;
		endTime = noteStatusElement.offTime;

		// across range of note
		for (ToneTimeFrame timeFrame : timeFrames) {

			ToneMapElement element = timeFrame.getElement(noteStatusElement.index);
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
					startTime = timeFrame.getStartTime();
					startElement = element;
				}
			}
			if ((minAmp == 0) || (minAmp > amplitude))
				minAmp = amplitude;

			if (amplitude < (double) noteLow / 100.0)
				numLowSlots++;
			if (peakSwitch && (amplitude >= (double) noteHigh / 100.0)) {
				endTime = timeFrame.getStartTime();
				endElement = element;
				break;
			}
		}

		if (startTime >= endTime)
			return;

		startElement.noteState = START;

		pitchIndex = startElement.getPitchIndex();

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
		System.out.println("New Note: " + avgAmp + ", " + maxAmp + ", " + minAmp);

		// Cross-Register NoteList element against ToneMapMatrix elements
		for (ToneTimeFrame timeFrame : timeFrames) {

			ToneMapElement element = timeFrame.getElement(noteStatusElement.index);
			element.noteListElement = noteListElement;
		}

		// Add noteList element to noteList object
		noteList.add(noteListElement);
	}
}