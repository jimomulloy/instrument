package jomu.instrument.tonemap;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;


/**
  * This class is the main centre of control of program flow for the 
  * ToneMap. This manages the data held in internal structures that define 
  * the "map" including classes ToneMapMatrix, TimeSet and PitchSet.
  * Functions include Loading of the Map from the Audio data, Processing 
  * of the Map through the Tuner functions to produce MIDI sequences and  
  * Saving and Opening of the data objects in Serialised form
  *
  * @version 1.0 01/01/01
  * @author Jim O'Mulloy
  */
public class ToneMap implements ToneMapConstants {

	/**
 	* ToneMap constructor.
 	* Instantiate ToneMapPanel
 	* Get handles to other ToneMap SubSystem Data Model and Control objects, 
 	* Audio, Midi and Tuner 
 	*/
	public ToneMap(ToneMapFrame toneMapFrame ) {
	
		this.toneMapFrame = toneMapFrame;
		toneMapPanel = new ToneMapPanel(this);
		audioModel = toneMapFrame.getAudioModel();
		midiModel = toneMapFrame.getMidiModel();
		tunerModel = toneMapFrame.getTunerModel();
				
	}
	
	/**
 	* Clear current ToneMap objects after Reset
 	*/
	public void clear(){
		if (toneMapPanel.worker != null) {
			toneMapPanel.worker.interrupt();
		}
		tunerModel.clear();
		midiModel.clear();
		audioModel.clear();
		toneMapMatrix = null;
		toneMapPanel.init();
	}
	
	public JPanel getPanel(){
		return toneMapPanel;
	}

	public void setThreshhold(int lowThreshhold, int highThreshhold) {
		tunerModel.setThreshhold(lowThreshhold, highThreshhold);
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
				ToneMapSerial toneMapSerial = (ToneMapSerial)istrm.readObject();
				
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
				
			} catch (IOException io ){
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
		} catch (IOException io ){
			toneMapFrame.reportStatus(EC_TONEMAP_SAVE);
			return false;
		} 
	}

	/**
 	* "LOAD" ToneMap function - load ToneMap objects from AudioModel data
 	* Create ToneMapMatrix, TimeSet and PitchSet to define the Map.
 	*/	
	public boolean loadAudio() {

		toneMapFrame.reportStatus(SC_TONEMAP_LOADING);

		if (audioModel.getFile() == null) {
			if (!audioModel.openFile()) return false;
		}
		//Create TimeSet object from AudioModel settings 
		timeSet =
				new TimeSet(audioModel.getStartTime(), 
							audioModel.getEndTime(), 
							audioModel.getSampleRate(), 
							audioModel.getSampleTimeSize());
				
		//Create PicthSet object from AudioModel settings
		pitchSet = new PitchSet(audioModel.getLowPitch(), 
							audioModel.getHighPitch());

		//Transform Audio Model data
		if (!audioModel.transform(toneMapPanel)) return false;
		
		//Build TomeMapMatrix
		if (!buildMap()) return false;

		//Initialise ToneMap display panel
		toneMapPanel.init();

		//Initialise MIDI and Tuner model objects
		//midiModel.clear();
		
		//if (!midiModel.open()) return false;

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
 	* "PROCESS" ToneMap function - Process ToneMap objects 
 	* Call TunerModel Execute function to filter and convert ToneMap Matrix elements
 	* and create NoteList object and call MidiModel to write MIDI sequence
 	*/	
	public boolean process() {

		toneMapFrame.reportStatus(SC_TONEMAP_PROCESSING);

		//Execute TunerModel filtering and Conversion process	
		if (!tunerModel.execute(toneMapPanel)) return false;

		//Get NoteList object and call MidiModel to write MDI sequence
		if (midiSwitch && tunerModel.getNoteList() != null ) {
			System.out.println("doing midi");
			midiModel.writeSequence(tunerModel.getNoteList());
		}
		
		//Call AudioModel to write Audio out stream
		//Get NoteList object and call MidiModel to write MDI sequence
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

	//Create ToneMapMatrix from AudioModel Transformed data
	private boolean buildMap() {

		//Load ToneMapMatrix elements from AudioModel transformed data buffer
		//audioFTPower
		audioFTPower = audioModel.getAudioFTPower();
		matrixLength = audioFTPower.length;

		//Instantiate new ToneMapMatrix
		toneMapMatrix = new ToneMapMatrix(matrixLength, 
									timeSet, pitchSet);
		
		mapIterator = toneMapMatrix.newIterator();
		
		mapIterator.firstPitch();

		//Iterate through ToneMapMatrix and load elements	
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
	
	public ToneMapMatrix getMatrix(){
		return toneMapMatrix;
	}
	
	public TimeSet getTimeSet() {
		return timeSet;
	}
	
	public PitchSet getPitchSet() {
		return pitchSet;
	}
	
	
	public AudioModel getAudioModel() {
		return audioModel;
	}
		
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
	
	public boolean audioSwitch=false;
	public boolean midiSwitch=false;
	
	private File file;
	private ToneMapSerial toneMapSerial;
	private String fileName;
	
	private int index;
	
	private ToneMapMatrix.Iterator mapIterator;
	
	
} // End ToneMap