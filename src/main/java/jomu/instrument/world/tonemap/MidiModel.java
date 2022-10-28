package jomu.instrument.world.tonemap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

/**
 * This class defines the MIDI Sub System data Model processing functions for
 * the ToneMap including MIDI sequence and file generation, Playback
 * implementation and Control settings management through the MidiPanel class.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class MidiModel implements ToneMapConstants {

	public class MidiQueueMessages {
		public List<ShortMessage> midiMessages;
		public double sampleTime;

		public MidiQueueMessages(List<ShortMessage> midiMessages,
				double sampleTime) {
			this.midiMessages = midiMessages;
			this.sampleTime = sampleTime;
		}
	}

	private class QueueConsumer implements Runnable {

		double sampleTime = 0;

		@Override
		public void run() {
			try {
				while (true) {
					MidiQueueMessages mqm = (MidiQueueMessages) bq.take();

					System.out.println(">>!!! midi QueueConsumer");

					if (sampleTime != 0) {
						TimeUnit.MILLISECONDS.sleep((long) sampleTime);
					}
					for (ShortMessage midiMessage : mqm.midiMessages) {
						try {
							// System.out.println(">>!!! midi send");
							synthesizer.getReceiver().send(midiMessage, -1);
						} catch (MidiUnavailableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					sampleTime = mqm.sampleTime;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
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
		}
	}

	// Inner class handles MIDI data playback event listener
	class ProcessMeta implements MetaEventListener {

		@Override
		public void meta(MetaMessage message) {

			if (message.getType() == 47 && playState != PAUSED
					&& sequence != null) {
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

	public boolean levelSwitch = false;
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

	private BlockingQueue<Object> bq;

	public Set<Integer> lastNotes;

	/**
	 * MidiModel constructor. Test Java Sound MIDI System available Instantiate
	 * MidiPanel
	 */
	public MidiModel() {
		bq = new LinkedBlockingQueue<>();
		Thread.startVirtualThread(new QueueConsumer());
	}

	/**
	 * Clear current MidiModel objects after Reset
	 */
	public void clear() {
		close();
		noteSequence = null;
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
	}
	/**
	 * Create MIDI message, wrap in time based MIDI event and add the MIDI event
	 * to the current MIDI track
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

	public double getTickRate() {
		return (sequence.getResolution() * getBPM() / 60);
	}

	/**
	 * Open MIDI Java Sound system objects
	 */
	public boolean open() {

		try {
			if (synthesizer == null) {
				if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
					return false;
				}
			}
			synthesizer.open();
			sequencer = MidiSystem.getSequencer();
			sequencer.addMetaEventListener(new ProcessMeta());

			sequence = new Sequence(Sequence.PPQ, 10);
		} catch (Exception ex) {
			return false;
		}
		Soundbank sb = synthesizer.getDefaultSoundbank();
		if (sb != null) {
			instruments = synthesizer.getDefaultSoundbank().getInstruments();
			if (instruments.length == 0) {
				return false;
			}
			synthesizer.loadInstrument(instruments[0]);
		} else {
			instruments = synthesizer.getAvailableInstruments();

		}
		MidiChannel midiChannels[] = synthesizer.getChannels();
		numChannels = midiChannels.length;
		if (numChannels == 0) {
			return false;
		}
		channels = new ChannelData[midiChannels.length];
		if (channels.length == 0)
			return false;
		for (int i = 0; i < channels.length; i++) {
			channels[i] = new ChannelData(midiChannels[i], i);
		}
		cc = channels[0];

		return true;
	}

	public void programChange(int program) {
		cc.program = program;
		if (instruments != null) {
			synthesizer.loadInstrument(instruments[program]);
		}
		cc.channel.programChange(program);

	}

	/**
	 * Write MIDI sequence to MIDI file
	 */
	public boolean saveMidiFile(File file) {
		try {
			int[] fileTypes = MidiSystem.getMidiFileTypes(sequence);
			if (fileTypes.length == 0) {
				return false;
			} else {
				if (MidiSystem.write(sequence, fileTypes[0], file) == -1) {
					throw new IOException("Problems writing to file");
				}
				return true;
			}
		} catch (SecurityException ex) {
			return false;
		} catch (Exception ex) {
			return false;
		}
	}

	public void writeFrameSequence(ToneTimeFrame toneTimeFrame)
			throws InvalidMidiDataException, MidiUnavailableException {

		System.out.println("midi write 1");

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();
		NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
		NoteStatusElement noteStatusElement = null;
		ShortMessage midiMessage = null;

		List<ShortMessage> midiMessages = new ArrayList<ShortMessage>();
		ToneMapElement[] ttfElements = toneTimeFrame.getElements();
		int counter = 0;
		if (lastNotes == null) {
			lastNotes = new HashSet<Integer>();
		}
		for (ToneMapElement toneMapElement : ttfElements) {
			note = pitchSet.getNote(toneMapElement.getPitchIndex());
			noteStatusElement = noteStatus.getNote(note);

			switch (noteStatusElement.state) {
				case ON :
				case PENDING :
					if (!lastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						midiMessage.setMessage(ShortMessage.NOTE_ON, 0, note,
								(int) (120 * toneMapElement.amplitude));
						midiMessages.add(midiMessage);
						System.out.println("!!?? midi add: " + note + ", "
								+ toneTimeFrame.getStartTime() + ", "
								+ toneMapElement.amplitude);
						lastNotes.add(note);
					}
					counter++;
					break;
				case OFF :
					midiMessage = new ShortMessage();
					midiMessage.setMessage(ShortMessage.NOTE_OFF, 0, note,
							(int) (120 * toneMapElement.amplitude));
					midiMessages.add(midiMessage);
					lastNotes.remove(note);
					break;
				default :
					lastNotes.remove(note);
					break;
			}
		}
		System.out.println("!!?? midi count: " + counter);
		MidiQueueMessages midiQueueMessages = new MidiQueueMessages(
				midiMessages, timeSet.getSampleTimeSize());

		bq.add(midiQueueMessages);

		return;
	}

	/**
	 * Create a MIDI sequence from the NoteList object
	 */
	public boolean writeSequence(ToneMap toneMap, NoteList noteList) {

		System.out.println("midi write 1");

		this.noteList = noteList;

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		if (!buildNoteSequence())
			return false;

		System.out.println("midi write 2");

		try {
			// sequence = new Sequence(Sequence.PPQ, 10);
		} catch (Exception ex) {
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
						noteSequenceElement.tick, noteSequenceElement.velocity))
					return false;
			if (noteSequenceElement.state == OFF)
				if (!createEvent(NOTEOFF, noteSequenceElement.note,
						noteSequenceElement.tick, noteSequenceElement.velocity))
					return false;

		}
		System.out.println("midi write 5");
		return true;
	}

	/**
	 * Use the NoteList object to build a NoteSequence object sorted to be used
	 * to create a MIDI sequence. Apply quantization functions on beat and
	 * duration
	 */
	private boolean buildNoteSequence() {

		noteSequence = new NoteSequence();

		double quantizeBeatFactor = quantizeBeatSetting * 1000.0 * 60.0
				/ getBPM();
		double quantizeDurationFactor = quantizeDurationSetting * 1000.0 * 60.0
				/ getBPM();

		for (int i = 0; i < noteList.size(); i++) {
			noteListElement = noteList.get(i);

			if (noteListElement.underTone == true) {
				continue;
			}

			note = noteListElement.note;

			if (note < getLowPitch())
				continue;
			if (note > getHighPitch())
				continue;

			double startTime = (noteListElement.startTime);

			if (quantizeBeatFactor != 0.0)
				startTime = Math.floor(startTime / quantizeBeatFactor)
						* quantizeBeatFactor;
			long startTick = 1 + (long) (startTime * getTickRate() / 1000.0);

			double endTime = (noteListElement.endTime);

			if (quantizeBeatFactor != 0.0)
				endTime = Math.ceil(endTime / quantizeBeatFactor)
						* quantizeBeatFactor;
			if (quantizeDurationFactor != 0)
				endTime = startTime + (Math
						.ceil((endTime - startTime) / quantizeDurationFactor)
						* quantizeDurationFactor);

			long endTick = 1 + (long) (endTime * getTickRate() / 1000.0);

			if ((endTick - startTick) < 1)
				endTick = startTick + 1;
			if (endTime < getStartTime())
				continue;
			if (startTime > getEndTime())
				continue;

			velocity = (int) (noteListElement.avgAmp * 127.0);
			noteSequence.add(
					new NoteSequenceElement(note, ON, startTick, velocity));
			noteSequence
					.add(new NoteSequenceElement(note, OFF, endTick, velocity));

		}

		if (noteSequence.size() == 0) {
			return false;
		} else {
			noteSequence.sort();
			return true;
		}

	}

} // End MidiModel