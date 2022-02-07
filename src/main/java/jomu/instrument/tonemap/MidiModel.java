package jomu.instrument.tonemap;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.font.*;
import java.text.*;
import javax.sound.midi.*;

/**
  * This class defines the MIDI Sub System data Model processing 
  * functions for the ToneMap including MIDI sequence and file generation, 
  * Playback implementation and Control settings management through the 
  * MidiPanel class.
  *
  * @version 1.0 01/01/01
  * @author Jim O'Mulloy
  */
public class MidiModel implements PlayerInterface, ToneMapConstants {

	// Inner class handles MIDI data playback event listener
	class ProcessMeta implements MetaEventListener {

		public void meta(MetaMessage message) {

			if (message.getType() == 47 && playState != PAUSED && sequence != null) {
				midiEOM = true;
				if (playState != STOPPED) {
					sequencer.stop();
					playState = EOM;
				}
			}
		}
	}

	// Inner class defines fields associated with a MIDI Track
	class TrackData extends Object {
		Integer chanNum;
		String name;
		Track track;
		public TrackData(int chanNum, String name, Track track) {
			this.chanNum = new Integer(chanNum);
			this.name = name;
			this.track = track;
		}
	}

	/**
	* Inner class contains information associated with a MIDI Channel.
	*/
	class ChannelData {

		MidiChannel channel;
		boolean solo, mono, mute, sustain;
		int velocity, pressure, bend, reverb;
		int num, program;

		public ChannelData(MidiChannel channel, int num) {
			this.channel = channel;
			this.num = num;
			velocity = INIT_VELOCITY_SETTING;
			pressure = INIT_PRESSURE_SETTING;
			bend = INIT_BEND_SETTING;
			reverb = INIT_REVERB_SETTING;
		}

		public void setComponentStates() {
			midiPanel.instrumentCB.setSelectedIndex(program);
			midiPanel.soloCB.setSelected(solo);
			midiPanel.monoCB.setSelected(mono);
			midiPanel.muteCB.setSelected(mute);
			midiPanel.sustCB.setSelected(sustain);

			TmSlider slider[] =
				{ midiPanel.veloS, midiPanel.presS, midiPanel.bendS, midiPanel.revbS };
			int v[] = { velocity, pressure, bend, reverb };
			for (int i = 0; i < slider.length; i++) {
				slider[i].setValue(v[i]);
			}
		}
	}

	public ToneMapFrame toneMapFrame;
	private MidiPanel midiPanel;
	private Player player;

	private ToneMap toneMap;
	private TimeSet timeSet;
	private PitchSet pitchSet;

	private String fileName;
	private File file;

	private String errStr;

	private double duration, seconds;
	
	private int timeRange;
	private int pitchRange;
	
	private NoteSequence noteSequence;
	private NoteSequenceElement noteSequenceElement;
	private NoteList noteList;
	private NoteListElement noteListElement;

	private Sequencer sequencer;
	private Sequence sequence;
	private Synthesizer synthesizer;
	private int numChannels; 
	public boolean levelSwitch=false;

	private Track track;
	private long startTime;

	private int velocity;
	private long tick;
	private int note;

	private int playState = STOPPED;
	private boolean midiEOM;

	public Instrument instruments[];
	public ChannelData channels[];
	public ChannelData cc;

	public double timeStart = INIT_TIME_START;
	public double timeEnd = INIT_TIME_END;

	public int panSetting = INIT_PAN_SETTING;
	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;
	public int volumeSetting = INIT_VOLUME_SETTING;
	public int bpmSetting = INIT_BPM_SETTING;
	public int instrumentSetting = INIT_INSTRUMENT_SETTING;
	public double quantizeBeatSetting = 0;
	public double quantizeDurationSetting = 0;

	/**
 	* MidiModel constructor.
 	* Test Java Sound MIDI System available 
 	* Instantiate MidiPanel
 	*/
	public MidiModel(ToneMapFrame toneMapFrame) {

		this.toneMapFrame = toneMapFrame;
		
		try {
			if (MidiSystem.getSequencer() == null) {
			toneMapFrame.reportStatus(EC_MIDI_SYSTEM);
			return;
			}
		} catch (Exception ex) {
			toneMapFrame.reportStatus(EC_MIDI_SYSTEM);
			return; 
		}
		
		if (!open()) {
			return;
		}
		
		midiPanel = new MidiPanel(this);
		
		programChange(midiPanel.instrumentCB.getSelectedIndex());
	

	}

	/**
 	* Clear current MidiModel objects after Reset
 	*/
	public void clear () {
		playStop();
		close();
		noteSequence = null;
	}

	/**
 	* Use the NoteList object to build a NoteSequence object sorted to 
 	* be used to create a MIDI sequence.
 	* Apply quantization functions on beat and duration
 	*/
	private boolean buildNoteSequence() {

		noteSequence = new NoteSequence();

		double quantizeBeatFactor =
			quantizeBeatSetting * 1000.0 * 60.0 / (double) getBPM();
		double quantizeDurationFactor =
			quantizeDurationSetting * 1000.0 * 60.0 / (double) getBPM();

		for (int i = 0; i < noteList.size(); i++) {
			noteListElement = noteList.get(i);

			if (noteListElement.underTone == true) {
				continue;
			}

			note = noteListElement.note;
		
			if (note < getLowPitch()) continue;
			if (note > getHighPitch()) continue;
			
			double startTime = (double) (noteListElement.startTime);
						
			if (quantizeBeatFactor != 0.0)
				startTime = Math.floor(startTime/quantizeBeatFactor) * quantizeBeatFactor;
			long startTick = 1 + (long) (startTime * getTickRate() / 1000.0);

			
			double endTime = (double) (noteListElement.endTime);

			if (quantizeBeatFactor != 0.0)
				endTime = Math.ceil(endTime/quantizeBeatFactor) * quantizeBeatFactor;
			if (quantizeDurationFactor != 0)
				endTime =
					startTime
						+ (Math.ceil((endTime - startTime)/quantizeDurationFactor) * quantizeDurationFactor);

			long endTick = 1 + (long) (endTime * getTickRate() / 1000.0);

			if ((endTick - startTick) < 1)
				endTick = startTick + 1;
			if (endTime < getStartTime()) continue;
			if (startTime > getEndTime()) continue;
		

			velocity = (int)(noteListElement.avgAmp*127.0);
			noteSequence.add(new NoteSequenceElement(note, ON, startTick, velocity));
			noteSequence.add(new NoteSequenceElement(note, OFF, endTick, velocity));

		}
		
		if (noteSequence.size() == 0)  {
			toneMapFrame.reportStatus(SC_MIDI_EMPTY_SEQUENCE);
			return false;
		} else { 
			noteSequence.sort();
			return true;
		}

	}
	
	/**
 	* Close MIDI Java Sound system objects
 	*/
	public void close() {
		if (synthesizer != null) {
			synthesizer.close();
		}
		if (sequencer != null) {
			sequencer.close();
		}
		sequencer = null;
		synthesizer = null;
		instruments = null;
		channels = null;

	}
	
	/**
 	* Create MIDI message, wrap in time based MIDI event and
 	* add the MIDI event to the current MIDI track
 	*/
	public boolean createEvent(int type, int num, long tick, int velocity) {
		ShortMessage message = new ShortMessage();
		try {
			if (levelSwitch) {
				message.setMessage(type + cc.num, num, velocity);
			} else {
				message.setMessage(type + cc.num, num, cc.velocity);
			}
		
			MidiEvent event = new MidiEvent(message, tick);
			track.add(event);
			return true;
		} catch (Exception ex) {
			toneMapFrame.reportStatus(EC_MIDI_EVENT);
			return false;
		}
	}
	
	public int getBPM() {
		return bpmSetting;
	}
	public double getDuration() {
		return duration;
	}
	public double getEndTime() {
		return timeEnd;
	}
	public File getFile() {
		return file;
	}
	public String getFileName() {
		return fileName;
	}
	public int getHighPitch() {
		return pitchHigh;
	}
	public int getLowPitch() {
		return pitchLow;
	}
	public int getNumChannels() {
		return numChannels;
	}
	public double getStartTime() {
		return timeStart;
	}
	public JPanel getPanel() {
		return midiPanel;
	}
	public double getTickRate() {
		return ((double) (sequence.getResolution() * getBPM() / 60));
	}
	
	/**
 	* Open MIDI Java Sound system objects
 	*/
 	public boolean open() {

		try {
			if (synthesizer == null) {
				if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
					toneMapFrame.reportStatus(EC_MIDI_OPEN);
					return false;
				}
			}
			synthesizer.open();
			sequencer = MidiSystem.getSequencer();
			sequencer.addMetaEventListener(new ProcessMeta());

			sequence = new Sequence(Sequence.PPQ, 10);
		} catch (Exception ex) {
			toneMapFrame.reportStatus(EC_MIDI_OPEN);
			return false;
		}
		Soundbank sb = synthesizer.getDefaultSoundbank();
		if (sb != null) {
			instruments = synthesizer.getDefaultSoundbank().getInstruments();
			if (instruments.length == 0) {
				toneMapFrame.reportStatus(EC_MIDI_OPEN_INSTRUMENTS);
				return false;
			}
			synthesizer.loadInstrument(instruments[0]);
		} else {
			instruments = synthesizer.getAvailableInstruments();
			
		}
		MidiChannel midiChannels[] = synthesizer.getChannels();
		numChannels = midiChannels.length;
		if (numChannels == 0) {
			toneMapFrame.reportStatus(EC_MIDI_OPEN_CHANNELS);
			return false;
		}
		channels = new ChannelData[midiChannels.length];
		if (channels.length == 0) return false;
		for (int i = 0; i < channels.length; i++) {
			channels[i] = new ChannelData(midiChannels[i], i);
		}
		cc = channels[0];
		if (midiPanel != null) { 
		    programChange(midiPanel.instrumentCB.getSelectedIndex());
		}
		
		return true;
	}
	
	public boolean play() {

		try {
			if (playState != STOPPED)
				playStop();
			System.out.println("midi play 1");

			if (sequence == null) return false;
			if (sequencer == null) return false;
			System.out.println("midi play 2");

			sequencer.open();
			sequencer.setSequence(sequence);

			double playStartTime =(player.getSeekTime() / 100) * (playGetLength());
			sequencer.setMicrosecondPosition((long) playStartTime);

			sequencer.start();
			playState = PLAYING;
			System.out.println("midi play 3");

			return true;

		} catch (Exception ex) {
			playState = STOPPED;
			toneMapFrame.reportStatus(EC_MIDI_PLAY);
			return false;
		}
	}
	public double playGetLength() {
		
		return sequence.getMicrosecondLength();
	}
	
	public int playGetState() {

		return playState;

	}
	public double playGetTime() {
		if (sequence != null) {
			return ((double) sequencer.getMicrosecondPosition()) / 1000.0;
		}
		return 0;
	}
	public void playLoop() {

		if (sequence != null) {
			double playStartTime =(player.getSeekTime() / 100) * (playGetLength());
			sequencer.setMicrosecondPosition((long) playStartTime);
			if (playState != PLAYING) {
				sequencer.start();
				playState = PLAYING;
			}

		}
	}
	public void playPause() {

		if (sequence != null) {
			if (playState == PLAYING) {

				sequencer.stop();
				playState = PAUSED;

			}
		}
	}
	public void playResume() {

		if (sequence != null) {
			if (playState == PAUSED) {

				sequencer.start();
				playState = PLAYING;

			}
		}
	}
	public void playSetPlayer(Player player) {

		this.player = player;
	}
	public void playSetSeek(double seekTime) {

		if (sequence != null) {
			sequencer.setMicrosecondPosition((long) (seekTime * 1000.0));
		}
	}
	public void playStop() {

		if (sequence != null) {
			if (playState == PLAYING || playState == PAUSED) {
				playState = STOPPED;
				sequencer.stop();
				sequencer.close();
				}
		}
	}
	public void programChange(int program) {
		cc.program = program;
		if (instruments != null) {
			synthesizer.loadInstrument(instruments[program]);
		}
		cc.channel.programChange(program);
		
	}

	public void setTime(TimeSet timeSet) {
		
		midiPanel.timeControl.setTimeMax((int)(timeSet.getEndTime()-timeSet.getStartTime()));
		
	}
	
	public void setPitch(PitchSet pitchSet) {
		midiPanel.pitchControl.setPitchRange((int)(pitchSet.getLowNote()), (int)(pitchSet.getHighNote()));
		
	}
	
	/**
 	* Write MIDI sequence to MIDI file
 	*/
	public boolean saveMidiFile(File file) {
		try {
			int[] fileTypes = MidiSystem.getMidiFileTypes(sequence);
			if (fileTypes.length == 0) {
				toneMapFrame.reportStatus(EC_MIDI_SAVE_SYSTEM);
				return false;
			} else {
				if (MidiSystem.write(sequence, fileTypes[0], file) == -1) {
					throw new IOException("Problems writing to file");
				}
				return true;	
			}
		} catch (SecurityException ex) {
			toneMapFrame.reportStatus(EC_MIDI_SAVE_WRITE);
			return false;
		} catch (Exception ex) {
			toneMapFrame.reportStatus(EC_MIDI_SAVE_WRITE);
			return false;
		}
	}

	/**
 	* Create a MIDI sequence from the NoteList object
 	*/
	public boolean writeSequence(NoteList noteList) {

		System.out.println("midi write 1");

		this.noteList = noteList;
		
		toneMap = toneMapFrame.getToneMap();
		timeSet = toneMap.getTimeSet();
		pitchSet = toneMap.getPitchSet();
		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		if (!buildNoteSequence()) return false;

		System.out.println("midi write 2");


		try {
			//sequence = new Sequence(Sequence.PPQ, 10);
		} catch (Exception ex) {
			toneMapFrame.reportStatus(EC_MIDI_WRITE_SEQUENCE);
			return false;
		}
		System.out.println("midi write 3");

		track = sequence.createTrack();
		startTime = System.currentTimeMillis();

		// add a program change right at the beginning of 
		// the track for the current instrument
		createEvent(PROGRAM, cc.program + 1, 1, 127);

		System.out.println("midi write 4");


		for (int i = 0; i < noteSequence.size(); i++) {
			noteSequenceElement = noteSequence.get(i);
			if (noteSequenceElement.state == ON)
				if (!createEvent(NOTEON, noteSequenceElement.note, 
					noteSequenceElement.tick, noteSequenceElement.velocity)) return false;
			if (noteSequenceElement.state == OFF)
				if (!createEvent(NOTEOFF, noteSequenceElement.note, 
					noteSequenceElement.tick, noteSequenceElement.velocity)) return false;

		}
		System.out.println("midi write 5");

		return true; 
	}
	
} // End MidiModel