package jomu.instrument.audio;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.NoteList;
import jomu.instrument.workspace.tonemap.NoteListElement;
import jomu.instrument.workspace.tonemap.NoteSequence;
import jomu.instrument.workspace.tonemap.NoteSequenceElement;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class MidiSynthesizer implements ToneMapConstants {

	public int bpmSetting = INIT_BPM_SETTING;

	public ChannelData cc;

	public ChannelData channels[];

	public Instrument instruments[];

	public int instrumentSetting = INIT_INSTRUMENT_SETTING;
	public boolean levelSwitch = false;

	public int panSetting = INIT_PAN_SETTING;
	public int pitchHigh = INIT_PITCH_HIGH;

	public int pitchLow = INIT_PITCH_LOW;

	public double quantizeBeatSetting = 0;

	public double quantizeDurationSetting = 0;

	public double timeEnd = INIT_TIME_END;
	public double timeStart = INIT_TIME_START;
	public int volumeSetting = INIT_VOLUME_SETTING;
	private AtomicInteger channelCount = new AtomicInteger(0);

	private File file;
	private String fileName;
	private boolean midiEOM;

	private Map<String, MidiStream> midiStreams = new ConcurrentHashMap<>();

	private NoteSequence noteSequence;

	private NoteSequenceElement noteSequenceElement;
	private int numChannels;

	private int playState = STOPPED;
	private Sequence sequence;
	private Sequencer sequencer;

	private Synthesizer synthesizer;

	private Track track;
	private int velocity;

	private ParameterManager parameterManager;

	/**
	 * MidiModel constructor. Test Java Sound MIDI System available Instantiate
	 * MidiPanel
	 */
	public MidiSynthesizer(ParameterManager parameterManager) {
		this.parameterManager = parameterManager;
	}

	// private int timeRange;

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

	public void close(String streamId) {
		if (!midiStreams.containsKey(streamId)) {
			return;
		}
		MidiStream midiStream = midiStreams.get(streamId);

		MidiQueueMessage midiQueueMessage = new MidiQueueMessage();
		midiStream.bq.add(midiQueueMessage);
		midiStreams.remove(streamId);
		System.out.println(">>!!! MIDI close: " + streamId);
	}

	/**
	 * Create MIDI message, wrap in time based MIDI event and add the MIDI event to
	 * the current MIDI track
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

			Info[] midiDevs = MidiSystem.getMidiDeviceInfo();
			for (Info midiDev : midiDevs) {
				System.out.println(">>midi dev: " + midiDev);
			}
			if (synthesizer == null) {
				if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
					return false;
				}
			}
			synthesizer.open();

			Soundbank sb = null;

			try {
				file = new File(
						parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SOUND_FONTS)); // getFileFromResource("FluidR3_GM.sf2");
				if (file.exists()) {
					sb = MidiSystem.getSoundbank(file);
					synthesizer.loadAllInstruments(sb);
					instruments = synthesizer.getLoadedInstruments();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (instruments == null || instruments.length == 0) {
				sb = synthesizer.getDefaultSoundbank();
				if (sb != null) {
					instruments = synthesizer.getDefaultSoundbank().getInstruments();
				} else {
					instruments = synthesizer.getAvailableInstruments();
				}
			}

			if (instruments == null || instruments.length == 0) {
				return false;
			}

			Instrument melodyInstrument = instruments[0];
			for (Instrument instrument : instruments) {
				if (instrument.getName().contains("Church O")) {
					melodyInstrument = instrument;
					break;
				}
			}

			synthesizer.loadInstrument(melodyInstrument);
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
			cc.channel.allNotesOff();
			cc.channel.allSoundOff();
			cc.channel.resetAllControllers();
			// cc.channel.controlChange(controllerNum, ctrlValue);
			// cc.channel.setChannelPressure(pressure);
			// cc.channel.setPolyPressure(noteNum, pressure);
			// cc.channel.programChange(program); // In "currently selected" bank - where
			// does that state live?
			// cc.channel.programChange(bank, program);
			// cc.channel.setPitchBend(bendVal);
			boolean soloState = false, muteState = false, omniState = false, monoState = false,
					localControlState = true;
			cc.channel.setSolo(soloState);
			cc.channel.setMute(muteState);
			cc.channel.setOmni(omniState);
			cc.channel.setMono(monoState);
			cc.channel.localControl(localControlState);

			cc.channel.programChange(melodyInstrument.getPatch().getBank(), melodyInstrument.getPatch().getProgram());

			// sequencer = MidiSystem.getSequencer();
			sequencer = MidiSystem.getSequencer(false);
			sequencer.addMetaEventListener(new ProcessMeta());
			sequence = new Sequence(Sequence.PPQ, 10);

			/*
			 * To free system resources, it is recommended to close the synthesizer and
			 * sequencer properly. To accomplish this, we register a Listener to the
			 * Sequencer. It is called when there are "meta" events. Meta event 47 is end of
			 * track. Thanks to Espen Riskedal for finding this trick.
			 */
			sequencer.addMetaEventListener(new MetaEventListener() {
				public void meta(final MetaMessage event) {
					if (event.getType() == 47) {
						sequencer.close();
						if (synthesizer != null) {
							synthesizer.close();
						}
					}
				}
			});
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	private File getFileFromResource(String fileName) throws URISyntaxException {

		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("file not found! " + fileName);
		} else {

			// failed if files have whitespaces or special characters
			// return new File(resource.getFile());

			return new File(resource.toURI());
		}

	}

	public void playFrameSequence(ToneTimeFrame toneTimeFrame, String streamId, int sequence)
			throws InvalidMidiDataException, MidiUnavailableException {

		if (!midiStreams.containsKey(streamId)) {
			midiStreams.put(streamId, new MidiStream(streamId));
		}
		MidiStream midiStream = midiStreams.get(streamId);

		MidiQueueMessage midiQueueMessage = new MidiQueueMessage(toneTimeFrame);
		midiStream.bq.add(midiQueueMessage);
		return;
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

	/**
	 * Create a MIDI sequence from the NoteList object
	 */
	public boolean writeSequence(ToneMap toneMap, NoteList noteList) {

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		int timeRange = timeSet.getRange();
		int pitchRange = pitchSet.getRange();

		if (!buildNoteSequence(noteList))
			return false;

		try {
			// sequence = new Sequence(Sequence.PPQ, 10);
		} catch (Exception ex) {
			return false;
		}

		track = sequence.createTrack();
		long startTime = System.currentTimeMillis();

		// add a program change right at the beginning of
		// the track for the current instrument
		createEvent(PROGRAM, cc.program + 1, 1, 127);

		for (int i = 0; i < noteSequence.size(); i++) {
			noteSequenceElement = noteSequence.get(i);
			if (noteSequenceElement.state == ON)
				if (!createEvent(NOTEON, noteSequenceElement.note, noteSequenceElement.tick,
						noteSequenceElement.velocity))
					return false;
			if (noteSequenceElement.state == OFF)
				if (!createEvent(NOTEOFF, noteSequenceElement.note, noteSequenceElement.tick,
						noteSequenceElement.velocity))
					return false;

		}
		return true;
	}

	/**
	 * Use the NoteList object to build a NoteSequence object sorted to be used to
	 * create a MIDI sequence. Apply quantization functions on beat and duration
	 */
	private boolean buildNoteSequence(NoteList noteList) {

		noteSequence = new NoteSequence();

		double quantizeBeatFactor = quantizeBeatSetting * 1000.0 * 60.0 / getBPM();
		double quantizeDurationFactor = quantizeDurationSetting * 1000.0 * 60.0 / getBPM();

		for (int i = 0; i < noteList.size(); i++) {
			NoteListElement noteListElement = noteList.get(i);

			if (noteListElement.underTone) {
				continue;
			}

			int note = noteListElement.note;

			if ((note < getLowPitch()) || (note > getHighPitch()))
				continue;

			double startTime = (noteListElement.startTime);

			if (quantizeBeatFactor != 0.0)
				startTime = Math.floor(startTime / quantizeBeatFactor) * quantizeBeatFactor;
			long startTick = 1 + (long) (startTime * getTickRate() / 1000.0);

			double endTime = (noteListElement.endTime);

			if (quantizeBeatFactor != 0.0)
				endTime = Math.ceil(endTime / quantizeBeatFactor) * quantizeBeatFactor;
			if (quantizeDurationFactor != 0)
				endTime = startTime
						+ (Math.ceil((endTime - startTime) / quantizeDurationFactor) * quantizeDurationFactor);

			long endTick = 1 + (long) (endTime * getTickRate() / 1000.0);

			if ((endTick - startTick) < 1)
				endTick = startTick + 1;
			if ((endTime < getStartTime()) || (startTime > getEndTime()))
				continue;

			velocity = (int) (noteListElement.avgAmp * 127.0);
			noteSequence.add(new NoteSequenceElement(note, ON, startTick, velocity));
			noteSequence.add(new NoteSequenceElement(note, OFF, endTick, velocity));

		}

		if (noteSequence.size() == 0) {
			return false;
		} else {
			noteSequence.sort();
			return true;
		}

	}

	private class MidiQueueConsumer implements Runnable {

		private BlockingQueue<MidiQueueMessage> bq;
		private MidiStream midiStream;
		double sampleTime = 0;
		public Set<Integer> lastNotes;

		public MidiQueueConsumer(BlockingQueue<MidiQueueMessage> bq, MidiStream midiStream) {
			this.bq = bq;
			this.midiStream = midiStream;
		}

		@Override
		public void run() {
			try {
				boolean running = true;
				while (running) {
					MidiQueueMessage mqm = bq.take();
					ToneTimeFrame toneTimeFrame = mqm.toneTimeFrame;

					if (toneTimeFrame == null) {
						cc.channel.allNotesOff();
						cc.channel.allSoundOff();
						cc.channel.resetAllControllers();
						this.midiStream.close();
						System.out.println(">> midiStream.close(): " + this.midiStream.streamId);
						running = false;
						break;
					}
					// System.out.println(">>!!! midi QueueConsumer ENTER THREAD: " +
					// Thread.currentThread());

					if (sampleTime != 0) {
						TimeUnit.MILLISECONDS.sleep((long) (sampleTime * 1000));
					}

					double lowVoiceThreshold = parameterManager
							.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
					double highVoiceThreshold = parameterManager
							.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);

					TimeSet timeSet = toneTimeFrame.getTimeSet();
					PitchSet pitchSet = toneTimeFrame.getPitchSet();
					NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
					NoteStatusElement noteStatusElement = null;
					ShortMessage midiMessage = null;

					List<ShortMessage> midiMessages = new ArrayList<>();
					ToneMapElement[] ttfElements = toneTimeFrame.getElements();
					if (lastNotes == null) {
						lastNotes = new HashSet<>();
					}
					double maxAmp = -1;
					for (ToneMapElement toneMapElement : ttfElements) {
						double amp = toneMapElement.amplitude;
						if (maxAmp < amp) {
							maxAmp = amp;
						}
					}

					for (ToneMapElement toneMapElement : ttfElements) {
						int note = pitchSet.getNote(toneMapElement.getPitchIndex());
						noteStatusElement = noteStatus.getNoteStatusElement(note);
						double amplitude = toneMapElement.amplitude;

						int volume = 0;
						if (amplitude > highVoiceThreshold) {
							volume = 120;
						} else if (amplitude <= lowVoiceThreshold) {
							volume = 0;
						} else {
							volume = (int) (Math
									.log1p((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold))
									/ Math.log1p(1.0000001) * 120);
							volume = Math.max(0, volume);
							volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold))
									* 120);
						}

						switch (noteStatusElement.state) {
						case ON:
						case PENDING:
						case CONTINUING:
							if (!lastNotes.contains(note)) {
								midiMessage = new ShortMessage();
								try {
									midiMessage.setMessage(ShortMessage.NOTE_ON, midiStream.channelId, note, volume);
								} catch (InvalidMidiDataException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								midiMessages.add(midiMessage);
								lastNotes.add(note);
							}
							break;
						case OFF:
							midiMessage = new ShortMessage();
							try {
								midiMessage.setMessage(ShortMessage.NOTE_OFF, midiStream.channelId, note, 0);
							} catch (InvalidMidiDataException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							midiMessages.add(midiMessage);
							lastNotes.remove(note);
							break;
						default:
							lastNotes.remove(note);
							break;
						}
					}

					for (ShortMessage mm : midiMessages) {
						try {
							synthesizer.getReceiver().send(mm, -1);
							if (mm.getCommand() == ShortMessage.NOTE_ON && mm.getData2() == 120) {
							}
						} catch (MidiUnavailableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					sampleTime = timeSet.getSampleTimeSize();

				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private class MidiQueueMessage implements Delayed {
		public ToneTimeFrame toneTimeFrame = null;
		private long startTime;

		public MidiQueueMessage(ToneTimeFrame toneTimeFrame, long delayInMilliseconds) {
			this.toneTimeFrame = toneTimeFrame;
			this.startTime = System.currentTimeMillis() + delayInMilliseconds;
		}

		public MidiQueueMessage(ToneTimeFrame toneTimeFrame) {
			this(toneTimeFrame, parameterManager.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		}

		public MidiQueueMessage() {
			this(null, parameterManager.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		}

		@Override
		public int compareTo(Delayed o) {
			return (int) (this.startTime - ((MidiQueueMessage) o).startTime);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long diff = startTime - System.currentTimeMillis();
			return unit.convert(diff, TimeUnit.MILLISECONDS);
		}
	}

	private class MidiStream {
		public DelayQueue<MidiQueueMessage> bq;

		public int channelId;

		public String streamId;

		public MidiStream(String streamId) {
			this.streamId = streamId;
			// this.channelId = channelCount.getAndIncrement();
			this.channelId = 0;
			bq = new DelayQueue<>(); // LinkedBlockingQueue<>();
			Thread.startVirtualThread(new MidiQueueConsumer(bq, this));
		}

		public void close() {
			bq.clear();

		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			MidiStream other = (MidiStream) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return channelId == other.channelId && Objects.equals(streamId, other.streamId);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(channelId, streamId);
			return result;
		}

		private MidiSynthesizer getEnclosingInstance() {
			return MidiSynthesizer.this;
		}
	}

	/**
	 * Inner class contains information associated with a MIDI Channel.
	 */
	class ChannelData {

		MidiChannel channel;
		int num, program;
		boolean solo, mono, mute, sustain;
		int velocity, pressure, bend, reverb;

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

}
// End MidiModel