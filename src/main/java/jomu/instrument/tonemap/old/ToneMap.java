package jomu.instrument.tonemap.old;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JPanel;

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
public class ToneMap implements ToneMapConstants {

	private ToneMapFrame toneMapFrame;

	private ToneMapPanel toneMapPanel;

	private AudioModel audioModel;

	private MidiModel midiModel;

	private TunerModel tunerModel;

	private ToneMapMatrix toneMapMatrix;

	private ToneMapElement element;

	private double[] audioFTPower;

	private int matrixLength;

	private TimeSet timeSet;

	private PitchSet pitchSet;

	private double amplitude;

	private double minFTPower;

	private double maxFTPower;

	private double logMinFTPower;

	public boolean audioSwitch = false;

	public boolean midiSwitch = false;

	private File file;

	private ToneMapSerial toneMapSerial;
	private String fileName;

	private int index;
	private ToneMapMatrix.Iterator mapIterator;
	public ToneMap() {
	}

	/**
	 * ToneMap constructor. Instantiate ToneMapPanel Get handles to other ToneMap
	 * SubSystem Data Model and Control objects, Audio, Midi and Tuner
	 */
	public ToneMap(ToneMapFrame toneMapFrame) {
		this();
		this.toneMapFrame = toneMapFrame;
		toneMapPanel = new ToneMapPanel(this);
		audioModel = toneMapFrame.getAudioModel();
		midiModel = toneMapFrame.getMidiModel();
		tunerModel = toneMapFrame.getTunerModel();

	}
	// Create ToneMapMatrix from AudioModel Transformed data
	private boolean buildMap(int matrixLength) {

		// Instantiate new ToneMapMatrix
		toneMapMatrix = new ToneMapMatrix(matrixLength, timeSet, pitchSet);

		mapIterator = toneMapMatrix.newIterator();

		mapIterator.firstPitch();

		// Iterate through ToneMapMatrix and load elements
		do {
			mapIterator.firstTime();
			do {
				index = mapIterator.getIndex();

				mapIterator.newElement(0, audioFTPower[index]);

			} while (mapIterator.nextTime() && index < audioFTPower.length);

		} while (mapIterator.nextPitch() && index < audioFTPower.length);

		toneMapMatrix.setAmpType(audioModel.logSwitch);
		toneMapMatrix.setLowThres(audioModel.powerLow);
		toneMapMatrix.setHighThres(audioModel.powerHigh);

		toneMapMatrix.reset();

		return true;

	}

	/**
	 * Clear current ToneMap objects after Reset
	 */
	public void clear() {
		if (toneMapPanel.worker != null) {
			toneMapPanel.worker.interrupt();
		}
		tunerModel.clear();
		midiModel.clear();
		audioModel.clear();
		toneMapMatrix = null;
		toneMapPanel.init();
	}
	public AudioModel getAudioModel() {
		return audioModel;
	}

	public ToneMapMatrix getMatrix() {
		return toneMapMatrix;
	}
	public JPanel getPanel() {
		return toneMapPanel;
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}
	public TimeSet getTimeSet() {
		return timeSet;
	}
	public TunerModel getTunerModel() {
		return tunerModel;
	}
	public boolean initialise(TimeSet timeSet, PitchSet pitchSet) {

		this.timeSet = timeSet;
		this.pitchSet = pitchSet;

		int timeRange = timeSet.getRange();
		int pitchRange = pitchSet.getRange();

		int matrixLength = timeRange * (pitchRange + 1);

		if (matrixLength < 0) {
			System.out.println(">>WHA!!");
		}
		// Build TomeMapMatrix
		if (!initMap(matrixLength))
			return false;

		tunerModel = new TunerModel(this);
		tunerModel.clear();

		int lowThreshhold = 0;
		int highThreshhold = 100;
		// tunerModel.setThreshhold(lowThreshhold, highThreshhold);
		ToneMapConfig config = new ToneMapConfig();
		config.peakSwitch = true;
		config.formantSwitch = true;
		config.harmonicSwitch = true;
		config.undertoneSwitch = true;
		config.normalizeSwitch = true;
		config.processMode = NONE;

		tunerModel.setConfig(config);
		// tunerModel.setTime(timeSet);
		// tunerModel.setPitch(pitchSet);

		return true;
	}

	private boolean initMap(int matrixLength) {

		// Instantiate new ToneMapMatrix
		toneMapMatrix = new ToneMapMatrix(matrixLength, timeSet, pitchSet);

		mapIterator = toneMapMatrix.newIterator();

		mapIterator.firstPitch();

		// Iterate through ToneMapMatrix and load elements
		do {
			mapIterator.firstTime();
			do {
				index = mapIterator.getIndex();

				mapIterator.newElement(0, 0);

			} while (mapIterator.nextTime() && index < matrixLength);

		} while (mapIterator.nextPitch() && index < matrixLength);

		// toneMapMatrix.setAmpType(audioModel.logSwitch);
		// toneMapMatrix.setLowThres(audioModel.powerLow);
		// toneMapMatrix.setHighThres(audioModel.powerHigh);

		toneMapMatrix.reset();

		return true;

	}
	/**
	 * "LOAD" ToneMap function - load ToneMap objects from AudioModel data Create
	 * ToneMapMatrix, TimeSet and PitchSet to define the Map.
	 */
	public boolean loadAudio() {

		toneMapFrame.reportStatus(SC_TONEMAP_LOADING);

		if (audioModel.getFile() == null) {
			if (!audioModel.openFile())
				return false;
		}
		// Create TimeSet object from AudioModel settings
		timeSet = new TimeSet(audioModel.getStartTime(), audioModel.getEndTime(), audioModel.getSampleRate(),
				audioModel.getSampleTimeSize());

		// Create PicthSet object from AudioModel settings
		pitchSet = new PitchSet(audioModel.getLowPitch(), audioModel.getHighPitch());

		// Transform Audio Model data
		if (!audioModel.transform(toneMapPanel))
			return false;

		// Build TomeMapMatrix

		if (!buildMap(audioModel.getAudioFTPower().length))
			return false;

		// Initialise ToneMap display panel
		toneMapPanel.init();

		// Initialise MIDI and Tuner model objects
		// midiModel.clear();

		// if (!midiModel.open()) return false;

		midiModel.setTime(timeSet);
		tunerModel.setTime(timeSet);
		midiModel.setPitch(pitchSet);
		tunerModel.setPitch(pitchSet);

		toneMapPanel.revalidate();
		toneMapPanel.repaint();

		toneMapFrame.reportStatus(SC_TONEMAP_LOADED);
		return true;
	}

	/**
	 * Open ToneMap file extracting stream of serialized objects
	 */
	public boolean open(File file) {

		this.file = file;
		fileName = file.getName();

		if (file.exists()) {
			try {
				clear();
				FileInputStream fin = new FileInputStream(file);
				ObjectInputStream istrm = new ObjectInputStream(fin);
				ToneMapSerial toneMapSerial = (ToneMapSerial) istrm.readObject();

				toneMapMatrix = toneMapSerial.matrix;
				timeSet = toneMapSerial.timeSet;
				pitchSet = toneMapSerial.pitchSet;
				ToneMapConfig config = toneMapSerial.config;

				toneMapMatrix.reset();

				if (toneMapMatrix == null || timeSet == null || pitchSet == null) {
					toneMapFrame.reportStatus(EC_TONEMAP_OPEN_BADFILE);
					return false;
				}

				midiModel.open();
				audioModel.setConfig(config);
				tunerModel.setConfig(config);
				toneMapPanel.lowThreshholdSlider.setValue(config.lowThreshhold);
				toneMapPanel.highThreshholdSlider.setValue(config.highThreshhold);

				toneMapPanel.init();
				midiModel.setTime(timeSet);
				tunerModel.setTime(timeSet);
				midiModel.setPitch(pitchSet);
				tunerModel.setPitch(pitchSet);
				toneMapPanel.processB.setEnabled(true);
				toneMapPanel.revalidate();
				toneMapPanel.repaint();

				toneMapFrame.reportStatus(SC_TONEMAP_LOADED);
				return true;

			} catch (IOException io) {
				io.printStackTrace();
				toneMapFrame.reportStatus(EC_TONEMAP_OPEN);
				return false;
			} catch (ClassNotFoundException cnf) {
				cnf.printStackTrace();
				toneMapFrame.reportStatus(EC_TONEMAP_OPEN);
				return false;
			}
		} else {
			toneMapFrame.reportStatus(EC_TONEMAP_OPEN_NOFILE);
			return false;
		}
	}
	/**
	 * "PROCESS" ToneMap function - Process ToneMap objects Call TunerModel Execute
	 * function to filter and convert ToneMap Matrix elements and create NoteList
	 * object and call MidiModel to write MIDI sequence
	 */
	public boolean process() {

		toneMapFrame.reportStatus(SC_TONEMAP_PROCESSING);

		// Execute TunerModel filtering and Conversion process
		if (!tunerModel.execute(toneMapPanel))
			return false;

		// Get NoteList object and call MidiModel to write MDI sequence
		if (midiSwitch && tunerModel.getNoteList() != null) {
			System.out.println("doing midi");
			midiModel.writeSequence(tunerModel.getNoteList());
		}

		// Call AudioModel to write Audio out stream
		// Get NoteList object and call MidiModel to write MDI sequence
		if (audioSwitch) {
			System.out.println("doing audio");
			audioModel.writeStream(tunerModel.getNoteList());
		}
		toneMapPanel.setProgress(100);
		toneMapPanel.revalidate();
		toneMapPanel.repaint();

		toneMapFrame.reportStatus(SC_TONEMAP_PROCESSED);
		return true;

	}
	/**
	 * Save ToneMap objects to a file in serialized form
	 */
	public boolean save(File file) {

		this.file = file;
		fileName = file.getName();

		if (toneMapMatrix == null || timeSet == null || pitchSet == null) {
			toneMapFrame.reportStatus(EC_TONEMAP_SAVE_NOMAP);
			return false;
		}
		try {
			FileOutputStream fout = new FileOutputStream(file);
			ObjectOutputStream ostrm = new ObjectOutputStream(fout);
			ToneMapConfig config = new ToneMapConfig();
			audioModel.getConfig(config);
			tunerModel.getConfig(config);
			config.lowThreshhold = toneMapPanel.lowThreshhold;
			config.highThreshhold = toneMapPanel.highThreshhold;
			toneMapSerial = new ToneMapSerial(toneMapMatrix, timeSet, pitchSet, config);
			ostrm.writeObject(toneMapSerial);
			ostrm.flush();
			return true;
		} catch (IOException io) {
			toneMapFrame.reportStatus(EC_TONEMAP_SAVE);
			return false;
		}
	}

	public void setThreshhold(int lowThreshhold, int highThreshhold) {
		tunerModel.setThreshhold(lowThreshhold, highThreshhold);
	}

	public boolean tune() {

		// Execute TunerModel filtering and Conversion process
		if (!tunerModel.tune())
			return false;
		return true;

	}

} // End ToneMap