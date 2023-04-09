package jomu.instrument.audio;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
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

import io.quarkus.runtime.StartupEvent;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.Controller;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.InstrumentSession.InstrumentSessionState;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ChordListElement;
import jomu.instrument.workspace.tonemap.ChordNote;
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

@ApplicationScoped
public class MidiSynthesizer implements ToneMapConstants {

	private static final Logger LOG = Logger.getLogger(MidiSynthesizer.class.getName());

	private static final int VOICE_1_CHANNEL = 0;

	private static final int VOICE_2_CHANNEL = 1;

	private static final int VOICE_3_CHANNEL = 2;

	private static final int VOICE_4_CHANNEL = 3;

	private static final int CHORD_1_CHANNEL = 4;

	private static final int CHORD_2_CHANNEL = 5;

	private static final int PAD_1_CHANNEL = 6;

	private static final int PAD_2_CHANNEL = 7;

	private static final int BASE_1_CHANNEL = 8;

	private static final int BEAT_1_CHANNEL = 10;

	private static final int BEAT_2_CHANNEL = 11;

	private static final int BEAT_3_CHANNEL = 12;

	private static final int BEAT_4_CHANNEL = 13;

	private int bpmSetting = INIT_BPM_SETTING;

	private ChannelData channels[];

	private Instrument instruments[];

	private boolean levelSwitch = true;

	private int panSetting = INIT_PAN_SETTING;
	private int pitchHigh = INIT_PITCH_HIGH;

	private int pitchLow = INIT_PITCH_LOW;

	private double quantizeBeatSetting = 0;

	private double quantizeDurationSetting = 0;

	private double timeEnd = INIT_TIME_END;
	private double timeStart = INIT_TIME_START;

	private String fisleName;
	private boolean midiEOM;

	private Map<String, MidiStream> midiStreams = new ConcurrentHashMap<>();

	private NoteSequence noteSequence;

	private NoteSequenceElement noteSequenceElement;
	private int numChannels;

	private int playState = STOPPED;
	private Sequence sequence;
	private Sequencer sequencer;

	private Synthesizer synthesizer;

	private int velocity;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Controller controller;

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

	public void onStartup(@Observes StartupEvent startupEvent) {
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				for (Entry<String, MidiStream> entry : midiStreams.entrySet()) {
					close(entry.getKey());
					entry.getValue().close();
				}
				close();
			}
		});
	}

	/**
	 * Close MIDI Java Sound system objects
	 */
	public void close() {
		if (synthesizer != null && synthesizer.isOpen()) {
			synthesizer.close();
		}
		if (sequencer != null && sequencer.isOpen()) {
			sequencer.close();
		}
		LOG.severe(">>MIDI close");
		sequence = null;
		sequencer = null;
		synthesizer = null;
		instruments = null;
	}

	public void clear() {
		for (Entry<String, MidiStream> entry : midiStreams.entrySet()) {
			close(entry.getKey());
			entry.getValue().close();
		}
		close();
		open();
	}

	public void reset() {
		LOG.severe(">>MIDI reset");
		close();
		open();
	}

	public void close(String streamId) {
		if (!midiStreams.containsKey(streamId)) {
			return;
		}
		MidiStream midiStream = midiStreams.get(streamId);
		MidiQueueMessage midiQueueMessage = new MidiQueueMessage();
		midiStream.getBq().add(midiQueueMessage);
		midiStreams.remove(streamId);
		LOG.finer(">>MIDI close: " + streamId);
	}

	/**
	 * Create MIDI message, wrap in time based MIDI event and add the MIDI event to
	 * the current MIDI track
	 */
	public boolean createEvent(Track track, ChannelData cc, int type, int number, long tick, int velocity) {

		ShortMessage message = new ShortMessage();
		try {
			if (levelSwitch) {
				message.setMessage(type, cc.num, number, velocity);
			} else {
				message.setMessage(type, cc.num, number, velocity);
			}

			MidiEvent event = new MidiEvent(message, tick);
			track.add(event);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	private long getTrackTick(ToneTimeFrame toneTimeFrame) {
		double quantizeBeatFactor = quantizeBeatSetting * 1000.0 * 60.0 / (double) getBPM();
		double quantizeDurationFactor = quantizeDurationSetting * 1000.0 * 60.0 / (double) getBPM();

		double tickStartTime = toneTimeFrame.getStartTime() * 1000;
		if (quantizeBeatFactor != 0.0) {
			tickStartTime = Math.floor(tickStartTime / quantizeBeatFactor) * quantizeBeatFactor;
		}
		long tick = 1 + (long) (tickStartTime * getTickRate() / 1000.0);
		return tick;
	}

	public int getBPM() {
		return bpmSetting;
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

		boolean useSynthesizer = parameterManager
				.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_USER_SYNTHESIZER_SWITCH);
		try {
			Info[] midiDevs = MidiSystem.getMidiDeviceInfo();
			for (Info midiDev : midiDevs) {
				LOG.finer(">>MidiSynth dev: " + midiDev);
			}

			Soundbank sb = null;
			if (synthesizer == null) {
				if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
					LOG.finer(">>MidiSynth MISSING SYNTH!!");
					return false;
				}
			}

			if (useSynthesizer) {
				synthesizer.open();
			}

			try {
				if (parameterManager.hasParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SOUND_FONTS)) {
					File file = new File(
							parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SOUND_FONTS)); // getFileFromResource("FluidR3_GM.sf2");
					if (file.exists()) {
						sb = MidiSystem.getSoundbank(file);
						synthesizer.loadAllInstruments(sb);
						instruments = synthesizer.getLoadedInstruments();
						LOG.severe(">>MidiSynth CustomSoundbank!!");
					}
				} else {
					LOG.severe(">>MidiSynth Default Soundbank!!");
				}
			} catch (Exception e) {
				LOG.log(Level.SEVERE, ">>MidiSynth open error", e);
				return false;
			}

			if (instruments == null || instruments.length == 0) {
				sb = synthesizer.getDefaultSoundbank();
				if (sb != null) {
					LOG.severe(">>MidiSynth DefaultSoundbank!!");
					instruments = synthesizer.getDefaultSoundbank().getInstruments();
				} else {
					LOG.severe(">>MidiSynth AvailableSoundbank!!");
					instruments = synthesizer.getAvailableInstruments();
				}
			}
			if (instruments == null || instruments.length == 0) {
				LOG.severe(">>MidiSynth MISSING INSTRUMENTS!!");
				return false;
			}

			MidiChannel midiChannels[] = synthesizer.getChannels();
			numChannels = midiChannels.length;
			if (numChannels == 0) {
				LOG.finer(">>MidiSynth MISSING CHANNELS!!");
				return false;
			}
			channels = new ChannelData[midiChannels.length];
			if (channels.length == 0)
				return false;
			for (int i = 0; i < channels.length; i++) {
				channels[i] = new ChannelData(midiChannels[i], i);
			}
			LOG.severe(">>MidiSynth CHANNELS: " + channels.length);
			initChannels();

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
						if (sequencer != null && sequencer.isOpen()) {
							sequencer.close();
						}
						if (synthesizer != null && synthesizer.isOpen()) {
							synthesizer.close();
						}
					}
				}
			});
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ">>MIDI Synthesiser open exception", ex);
			return false;
		}
		return true;
	}

	private void initChannels() {
		initChannel(channels[VOICE_1_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_1));
		initChannel(channels[VOICE_2_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_2));
		initChannel(channels[VOICE_3_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_3));
		initChannel(channels[VOICE_4_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_4));
		initChannel(channels[CHORD_1_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_CHORD_1));
		initChannel(channels[CHORD_2_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_CHORD_2));
		initChannel(channels[PAD_1_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_PAD_1));
		initChannel(channels[PAD_2_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_PAD_2));
		initChannel(channels[BEAT_1_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_1));
		initChannel(channels[BEAT_2_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_2));
		initChannel(channels[BEAT_3_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_3));
		initChannel(channels[BEAT_4_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_4));
		initChannel(channels[BASE_1_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BASE_1));
	}

	private Instrument initChannel(ChannelData channelData, String instrumentName) {
		Instrument channelInstrument = instruments[0];
		if (instrumentName != null) {
			try {
				int cn = Integer.parseInt(instrumentName);
				channelInstrument = instruments[cn - 1];
			} catch (NumberFormatException ex) {
				for (Instrument instrument : instruments) {
					if (instrument.getName().toLowerCase().contains(instrumentName.toLowerCase())) {
						channelInstrument = instrument;
						break;
					}
				}
			}
		}
		if (channelInstrument == null) {
			channelInstrument = instruments[0];
		}

		LOG.severe(">>Load MIDI Instrument: " + instrumentName + ", " + channelInstrument.getName());
		if (synthesizer != null) {
			synthesizer.loadInstrument(channelInstrument);
		}

		channelData.channel.allNotesOff();
		channelData.channel.allSoundOff();
		channelData.channel.resetAllControllers();
		// cc.channel.controlChange(controllerNum, ctrlValue);
		// cc.channel.setChannelPressure(pressure);
		// cc.channel.setPolyPressure(noteNum, pressure);
		// cc.channel.programChange(program); // In "currently selected" bank - where
		// does that state live?
		// cc.channel.programChange(bank, program);
		// cc.channel.setPitchBend(bendVal);
		boolean soloState = false, muteState = false, omniState = false, monoState = false, localControlState = true;
		channelData.channel.setSolo(soloState);
		channelData.channel.setMute(muteState);
		channelData.channel.setOmni(omniState);
		channelData.channel.setMono(monoState);
		channelData.channel.localControl(localControlState);
		channelData.channel.programChange(channelInstrument.getPatch().getBank(),
				channelInstrument.getPatch().getProgram());
		LOG.severe(">> instrumentName: " + instrumentName + ", " + channelInstrument.getPatch().getProgram() + ", "
				+ channelInstrument.getPatch().getBank());
		programChange(channelData, channelInstrument.getPatch().getProgram());
		return channelInstrument;

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

	private void clearTracks() {
		Track[] tracks = sequence.getTracks();
		for (int i = 0; i < tracks.length; i++) {
			sequence.deleteTrack(tracks[i]);
		}
	}

	public void playFrameSequence(ToneTimeFrame toneTimeFrame, String streamId, int sequence)
			throws InvalidMidiDataException, MidiUnavailableException {

		if (!midiStreams.containsKey(streamId)) {
			clearTracks();
			midiStreams.put(streamId, new MidiStream(streamId));
		}
		MidiStream midiStream = midiStreams.get(streamId);
		MidiQueueMessage midiQueueMessage = new MidiQueueMessage(toneTimeFrame, sequence);
		midiStream.getBq().add(midiQueueMessage);
		return;
	}

	public void programChange(ChannelData channelData, int program) {
		channelData.program = program;
		if (instruments != null && synthesizer != null) {
			synthesizer.loadInstrument(instruments[program]);
		}
		channelData.channel.programChange(program);

	}

	/**
	 * Write MIDI sequence to MIDI file
	 */
	public boolean saveMidiFile(File file, Sequence sequence) {
		try {
			int[] fileTypes = MidiSystem.getMidiFileTypes(sequence);
			if (fileTypes.length == 0) {
				return false;
			} else {
				int fileType = fileTypes[0];
				if (fileTypes.length > 1) {
					fileType = fileTypes[1];
				}
				if (MidiSystem.write(sequence, fileType, file) == -1) {
					throw new IOException("Problems writing file to MIDI System");
				}
				return true;
			}
		} catch (SecurityException ex) {
			LOG.log(Level.SEVERE, ">>saveMidiFile Exception writing out stream", ex);
			return false;
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ">>saveMidiFile Exception writing out stream", ex);
			return false;
		}
	}

	/**
	 * Write MIDI sequence to MIDI file
	 */
	public boolean saveMidiFile(File file, Track track) {
		Sequence trackSequence;
		try {
			trackSequence = new Sequence(Sequence.PPQ, 10);
		} catch (InvalidMidiDataException ex) {
			LOG.log(Level.SEVERE, ">>saveMidiFile Exception writing out track", ex);
			return false;
		}
		Track newTrack = trackSequence.createTrack();
		for (int i = 0; i < track.size(); i++) {
			newTrack.add(track.get(i));
		}
		return saveMidiFile(file, trackSequence);
	}

	private class MidiQueueConsumer implements Runnable {

		private BlockingQueue<MidiQueueMessage> bq;
		private MidiStream midiStream;
		double sampleTime = 0;
		public Set<Integer> voiceChannel1LastNotes;
		public Set<Integer> voiceChannel2LastNotes;
		public Set<Integer> voiceChannel3LastNotes;
		public Set<Integer> voiceChannel4LastNotes;
		public Set<Integer> chordsChannel1LastNotes;
		public Set<Integer> chordsChannel2LastNotes;
		public Set<Integer> padsChannel1LastNotes;
		public Set<Integer> padsChannel2LastNotes;
		public Set<Integer> beatsChannel1LastNotes;
		public Set<Integer> beatsChannel2LastNotes;
		public Set<Integer> beatsChannel3LastNotes;
		public Set<Integer> beatsChannel4LastNotes;
		public Set<Integer> baseChannelLastNotes;

		public Track voice1Track;
		public Track voice2Track;
		public Track voice3Track;
		public Track voice4Track;
		public Track chord1Track;
		public Track chord2Track;
		public Track pad1Track;
		public Track pad2Track;
		public Track beat1Track;
		public Track beat2Track;
		public Track beat3Track;
		public Track beat4Track;
		public Track baseTrack;

		private boolean running = true;

		public MidiQueueConsumer(BlockingQueue<MidiQueueMessage> bq, MidiStream midiStream) {
			this.bq = bq;
			this.midiStream = midiStream;
		}

		public void stop() {
			running = false;
		}

		@Override
		public void run() {
			boolean completed = false;
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			try {
				while (running) {
					LOG.severe(">>MidiQueueConsumer running");
					if (midiStream.isClosed()) {
						stop();
						break;
					}
					MidiQueueMessage mqm = bq.take();
					ToneTimeFrame toneTimeFrame = mqm.toneTimeFrame;

					if (toneTimeFrame == null || midiStream.isClosed()) {
						completed = true;
						stop();
						break;
					}

					if (sampleTime != 0 && !silentWrite) {
						TimeUnit.MILLISECONDS.sleep((long) (sampleTime * 1000));
					}

					if (midiStream.isClosed()) {
						stop();
						break;
					}

					LOG.finer(">>MidiQueueConsumer running: " + toneTimeFrame.getStartTime());

					boolean midiPlayVoice1Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE1_SWITCH);
					boolean midiPlayVoice2Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE2_SWITCH);
					boolean midiPlayVoice3Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE3_SWITCH);
					boolean midiPlayVoice4Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOICE4_SWITCH);
					boolean midiPlayChord1Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD1_SWITCH);
					boolean midiPlayChord2Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD2_SWITCH);
					boolean midiPlayPad1Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD1_SWITCH);
					boolean midiPlayPad2Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD2_SWITCH);
					boolean midiPlayBeat1Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT1_SWITCH);
					boolean midiPlayBeat2Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT2_SWITCH);
					boolean midiPlayBeat3Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT3_SWITCH);
					boolean midiPlayBeat4Switch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT4_SWITCH);
					boolean midiPlayBaseSwitch = parameterManager
							.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BASE_SWITCH);

					if (midiPlayVoice1Switch && !playVoiceChannel1(mqm)) {
						stop();
						break;
					}

					if (midiPlayVoice2Switch && !playVoiceChannel2(mqm)) {
						stop();
						break;
					}

					if (midiPlayVoice3Switch && !playVoiceChannel3(mqm)) {
						stop();
						break;
					}

					if (midiPlayVoice4Switch && !playVoiceChannel4(mqm)) {
						stop();
						break;
					}

					if (midiPlayChord1Switch && !playChordChannel1(mqm)) {
						stop();
						break;
					}

					if (midiPlayChord2Switch && !playChordChannel2(mqm)) {
						stop();
						break;
					}

					if (midiPlayPad1Switch && !playPadChannel1(mqm)) {
						stop();
						break;
					}

					if (midiPlayPad2Switch && !playPadChannel2(mqm)) {
						stop();
						break;
					}

					if (midiPlayBeat1Switch && !playBeatChannel1(mqm)) {
						stop();
						break;
					}

					if (midiPlayBeat2Switch && !playBeatChannel2(mqm)) {
						stop();
						break;
					}

					if (midiPlayBeat3Switch && !playBeatChannel3(mqm)) {
						stop();
						break;
					}

					if (midiPlayBeat4Switch && !playBeatChannel4(mqm)) {
						stop();
						break;
					}

					if (midiPlayBaseSwitch && !playBaseChannel(mqm)) {
						stop();
						break;
					}

					TimeSet timeSet = toneTimeFrame.getTimeSet();
					sampleTime = timeSet.getSampleTimeSize();

				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			LOG.severe(">>MidiQueueConsumer run exit");
			clearChannels();
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);

			if (writeTrack && completed) {
				InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();
				String baseDir = storage.getObjectStorage().getBasePath();
				String folder = Paths.get(baseDir,
						parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_DIRECTORY),
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
						.toString();
				String masterFileName = folder + System.getProperty("file.separator")
						+ instrumentSession.getInputAudioFileName() + "_recording_master.midi";
				// String fileName = "/tmp/instrument_recording_" + System.currentTimeMillis() +
				// ".midi";
				LOG.severe(">>Writing MIDI file name: " + masterFileName);
				File file = new File(masterFileName);
				saveMidiFile(file, sequence);

				Track[] tracks = sequence.getTracks();
				for (int i = 0; i < tracks.length; i++) {
					String trackFileName = folder + System.getProperty("file.separator")
							+ instrumentSession.getInputAudioFileName() + "_recording_track_" + getTrackName(tracks[i])
							+ ".midi";
					// String fileName = "/tmp/instrument_recording_" + System.currentTimeMillis() +
					// ".midi";
					LOG.severe(">>Writing MIDI file name: " + trackFileName);
					file = new File(trackFileName);
					saveMidiFile(file, tracks[i]);
					sequence.deleteTrack(tracks[i]);
				}
				instrumentSession.setOutputMidiFileName(
						masterFileName.substring(masterFileName.lastIndexOf(System.getProperty("file.separator"))));
				instrumentSession.setOutputMidiFilePath(masterFileName);
			}
			LOG.severe(">>MidiQueueConsumer close stream");
			this.midiStream.close();
		}

		private String getTrackName(Track track) {
			if (track == voice1Track) {
				return "voice1";
			} else if (track == voice2Track) {
				return "voice2";
			} else if (track == voice3Track) {
				return "voice3";
			} else if (track == voice4Track) {
				return "voice4";
			} else if (track == chord1Track) {
				return "chord1";
			} else if (track == chord2Track) {
				return "chord2";
			} else if (track == pad1Track) {
				return "pad1";
			} else if (track == pad2Track) {
				return "pad2";
			} else if (track == beat1Track) {
				return "beat1";
			} else if (track == beat2Track) {
				return "beat2";
			} else if (track == beat3Track) {
				return "beat3";
			} else if (track == beat4Track) {
				return "beat4";
			} else if (track == baseTrack) {
				return "base";
			} else {
				return "undefined";
			}
		}

		private boolean playVoiceChannel1(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playVoiceChannel1");
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap notateToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));

			ToneTimeFrame toneTimeFrame = notateToneMap.getTimeFrame(mqm.sequence);

			if (toneTimeFrame == null) {
				return false;
			}

			ChannelData voice1Channel = channels[VOICE_1_CHANNEL];

			if (writeTrack && voice1Track == null) {
				voice1Track = sequence.createTrack();
				LOG.severe(">>MIDI VOICE1 progream: " + (voice1Channel.program + 1));
				createEvent(voice1Track, voice1Channel, PROGRAM, voice1Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(toneTimeFrame);

			TimeSet timeSet = toneTimeFrame.getTimeSet();
			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = toneTimeFrame.getElements();
			if (voiceChannel1LastNotes == null) {
				voiceChannel1LastNotes = new HashSet<>();
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
					volume = 127;
				} else if (amplitude <= lowVoiceThreshold) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 127);
				}

				switch (noteStatusElement.state) {
				case ON:
				case PENDING:
				case CONTINUING:
					LOG.finer(">>V1 MIDI NOTE CANDIDATE...: " + mqm.getSequence() + ", " + note + ", " + volume + ", "
							+ amplitude + ", " + highVoiceThreshold + ", " + lowVoiceThreshold);
					if (!voiceChannel1LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, voice1Channel.num, note, volume);
							if (writeTrack) {
								createEvent(voice1Track, voice1Channel, NOTEON, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannel1LastNotes.add(note);
					} else {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.CONTROL_CHANGE, voice1Channel.num, note, volume);
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// midiMessages.add(midiMessage);
					}
					break;
				case OFF:
					if (voiceChannel1LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, voice1Channel.num, note, 0);
							if (writeTrack) {
								createEvent(voice1Track, voice1Channel, NOTEOFF, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannel1LastNotes.remove(note);
					}
					break;
				default:
					voiceChannel1LastNotes.remove(note);
					break;
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel1 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playVoiceChannel2(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playVoiceChannel2");
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap ipToneMap = workspace.getAtlas().getToneMap(
					ToneMap.buildToneMapKey(CellTypes.AUDIO_INTEGRATE.toString() + "_PEAKS", midiStream.getStreamId()));

			ToneTimeFrame toneTimeFrame = ipToneMap.getTimeFrame(mqm.sequence);

			if (toneTimeFrame == null) {
				return false;
			}

			ChannelData voice2Channel = channels[VOICE_2_CHANNEL];

			if (writeTrack && voice2Track == null) {
				voice2Track = sequence.createTrack();
				createEvent(voice2Track, voice2Channel, PROGRAM, voice2Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(toneTimeFrame);

			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = toneTimeFrame.getElements();
			if (voiceChannel2LastNotes == null) {
				voiceChannel2LastNotes = new HashSet<>();
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
				if ((note > 127)) {
					// TODO WHY??
					continue;
				}
				double amplitude = toneMapElement.amplitude;
				boolean isPeak = toneMapElement.isPeak;

				int volume = 0;
				if (amplitude > 1.0 || (isPeak && playPeaks)) {
					volume = 120;
				} else if (amplitude < 0.1) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - 0.1) / (1.0 - 0.1)) * 120);
				}

				if (volume > 0) {
					if (!voiceChannel2LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, voice2Channel.num, note, volume);
							if (writeTrack) {
								createEvent(voice2Track, voice2Channel, NOTEON, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannel2LastNotes.add(note);
					}
				} else {
					if (voiceChannel2LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, voice2Channel.num, note, 0);
							if (writeTrack) {
								createEvent(voice2Track, voice2Channel, NOTEOFF, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannel2LastNotes.remove(note);
					}
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel2 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playVoiceChannel3(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playVoiceChannel3");
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap pitchToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_YIN, midiStream.getStreamId()));

			ToneTimeFrame toneTimeFrame = pitchToneMap.getTimeFrame(mqm.sequence);

			if (toneTimeFrame == null) {
				return false;
			}

			ChannelData voice3Channel = channels[VOICE_3_CHANNEL];

			if (writeTrack && voice3Track == null) {
				voice3Track = sequence.createTrack();
				createEvent(voice3Track, voice3Channel, PROGRAM, voice3Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(toneTimeFrame);

			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = toneTimeFrame.getElements();
			if (voiceChannel3LastNotes == null) {
				voiceChannel3LastNotes = new HashSet<>();
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
				double amplitude = toneMapElement.amplitude;
				boolean isPeak = toneMapElement.isPeak;

				int volume = 0;
				if (amplitude > 1.0 || (isPeak && playPeaks)) {
					volume = 127;
				} else if (amplitude < 0.1) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - 0.1) / (1.0 - 0.1)) * 127);
				}

				if (volume > 0) {
					if (!voiceChannel3LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, voice3Channel.num, note, volume);
							if (writeTrack) {
								createEvent(voice3Track, voice3Channel, NOTEON, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannel3LastNotes.add(note);
					}
				} else {
					if (voiceChannel3LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, voice3Channel.num, note, 0);
							if (writeTrack) {
								createEvent(voice3Track, voice3Channel, NOTEOFF, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannel3LastNotes.remove(note);
					}
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel3 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playVoiceChannel4(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playVoiceChannel4");
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap notateToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));

			ToneTimeFrame toneTimeFrame = notateToneMap.getTimeFrame(mqm.sequence);

			if (toneTimeFrame == null) {
				return false;
			}

			ChannelData voice4Channel = channels[VOICE_4_CHANNEL];

			if (writeTrack && voice4Track == null) {
				voice4Track = sequence.createTrack();
				LOG.severe(">>MIDI VOICE4 progream: " + (voice4Channel.program + 1));
				createEvent(voice4Track, voice4Channel, PROGRAM, voice4Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(toneTimeFrame);

			TimeSet timeSet = toneTimeFrame.getTimeSet();
			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = toneTimeFrame.getElements();
			if (voiceChannel4LastNotes == null) {
				voiceChannel4LastNotes = new HashSet<>();
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
				NoteListElement noteListElement = toneMapElement.noteListElement;

				int volume = 0;
				amplitude = 0;
				if (noteListElement != null) {
					amplitude = noteListElement.avgAmp;

					if (amplitude > highVoiceThreshold) {
						volume = 127;
					} else if (amplitude <= lowVoiceThreshold) {
						volume = 0;
					} else {
						volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold))
								* 127);
					}

				}

				if (volume > 0) {
					if (!voiceChannel4LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, voice4Channel.num, note, volume);
							if (writeTrack) {
								createEvent(voice4Track, voice4Channel, NOTEON, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannel4LastNotes.add(note);
					} else {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.CONTROL_CHANGE, voice4Channel.num, note, volume);
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// midiMessages.add(midiMessage);
					}
				} else {
					if (voiceChannel4LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, voice4Channel.num, note, 0);
							if (writeTrack) {
								createEvent(voice4Track, voice4Channel, NOTEOFF, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannel4LastNotes.remove(note);
					}
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel4 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playChordChannel1(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playChordChannel1");

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap notesToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));
			ToneMap chromaToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame notesTimeFrame = notesToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame chromaTimeFrame = chromaToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			if (chromaTimeFrame == null) {
				return false;
			}

			ChannelData chord1Channel = channels[CHORD_1_CHANNEL];

			if (writeTrack && chord1Track == null) {
				chord1Track = sequence.createTrack();
				createEvent(chord1Track, chord1Channel, PROGRAM, chord1Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(chromaTimeFrame);

			TimeSet timeSet = chromaTimeFrame.getTimeSet();
			PitchSet pitchSet = chromaTimeFrame.getPitchSet();
			NoteStatus noteStatus = chromaTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = chromaTimeFrame.getElements();
			if (chordsChannel1LastNotes == null) {
				chordsChannel1LastNotes = new HashSet<>();
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
				if (amplitude > 1.0) {
					volume = 120;
				} else if (amplitude <= 0.1) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - 0.1) / (1.0 - 0.1)) * 120);
				}

				int octaveAdjust = 3;

				if (amplitude > 0.1) {
					if (!chordsChannel1LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, chord1Channel.num, (note + 12 * octaveAdjust),
									volume);
							if (writeTrack) {
								createEvent(chord1Track, chord1Channel, NOTEON, (note + 12 * octaveAdjust), tick,
										volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						chordsChannel1LastNotes.add(note);
					}
				} else {
					if (chordsChannel1LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, chord1Channel.num, (note + 12 * octaveAdjust),
									0);
							if (writeTrack) {
								createEvent(chord1Track, chord1Channel, NOTEOFF, (note + 12 * octaveAdjust), tick,
										volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						chordsChannel1LastNotes.remove(note);
					}
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Chord Channel1 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playChordChannel2(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playChordChannel2");
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap chromaToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			ToneMap cqToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN, midiStream.getStreamId()));

			ToneTimeFrame chromaTimeFrame = chromaToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame cqTimeFrame = cqToneMap.getTimeFrame(mqm.sequence);

			if (chromaTimeFrame == null) {
				return false;
			}

			ChannelData chord2Channel = channels[CHORD_2_CHANNEL];

			if (writeTrack && chord2Track == null) {
				chord2Track = sequence.createTrack();
				createEvent(chord2Track, chord2Channel, PROGRAM, chord2Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(chromaTimeFrame);

			ChordListElement chord = chromaTimeFrame.getChord();

			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			if (chordsChannel2LastNotes == null) {
				chordsChannel2LastNotes = new HashSet<>();
			}

			for (int note = 0; note < 12; note++) {
				int volume = 0;
				double amplitude = 0;
				int octaveAdjust = 3;
				if (chord != null) {
					if (chord.hasChordNote(note)) {
						ChordNote chordNote = chord.getChordNote(note).orElse(null);
						amplitude = (int) chordNote.getAmplitude();
						amplitude = 1.0;
						if (amplitude >= 1.0) {
							volume = 127;
						} else if (amplitude <= 0.1) {
							volume = 0;
						} else {
							volume = (int) (((amplitude - 0.1) / (1.0 - 0.1)) * 120);
						}
					}
				}

				if (amplitude > 0.1) {
					octaveAdjust = cqTimeFrame.getOctave();
					LOG.severe(">>CC2 octave adjust: " + octaveAdjust);
					if (!chordsChannel2LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, chord2Channel.num, (note + 12 * octaveAdjust),
									volume);
							if (writeTrack) {
								createEvent(chord2Track, chord2Channel, NOTEON, (note + 12 * octaveAdjust), tick,
										volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						chordsChannel2LastNotes.add(note);
					}
				} else {
					if (chordsChannel2LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, chord2Channel.num, (note + 12 * octaveAdjust),
									0);
							if (writeTrack) {
								createEvent(chord2Track, chord2Channel, NOTEOFF, (note + 12 * octaveAdjust), tick,
										volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						chordsChannel2LastNotes.remove(note);
					}
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
						if (mm.getCommand() == ShortMessage.NOTE_ON && mm.getData2() == 120) {
						}
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Chord Channel2 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playPadChannel1(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playPadChannel1");

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap notesToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));
			ToneMap chromaToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame notesTimeFrame = notesToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame chromaTimeFrame = chromaToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			if (chromaTimeFrame == null) {
				return false;
			}

			ChannelData pad1Channel = channels[PAD_1_CHANNEL];

			if (writeTrack && pad1Track == null) {
				pad1Track = sequence.createTrack();
				createEvent(pad1Track, pad1Channel, PROGRAM, pad1Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(chromaTimeFrame);

			TimeSet timeSet = chromaTimeFrame.getTimeSet();
			PitchSet pitchSet = chromaTimeFrame.getPitchSet();
			NoteStatus noteStatus = chromaTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = chromaTimeFrame.getElements();
			if (padsChannel1LastNotes == null) {
				padsChannel1LastNotes = new HashSet<>();
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
				if (amplitude > 1.0) {
					volume = 120;
				} else if (amplitude < 0.1) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - 0.1) / (1.0 - 0.1)) * 120);
				}

				int octaveAdjust = 2;

				if (volume > 0) {
					if (!padsChannel1LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, pad1Channel.num, (note + 12 * octaveAdjust),
									volume);
							if (writeTrack) {
								createEvent(pad1Track, pad1Channel, NOTEON, (note + 12 * octaveAdjust), tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						padsChannel1LastNotes.add(note);
					}
				} else {
					if (padsChannel1LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, pad1Channel.num, (note + 12 * octaveAdjust),
									0);
							if (writeTrack) {
								createEvent(pad1Track, pad1Channel, NOTEOFF, (note + 12 * octaveAdjust), tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						padsChannel1LastNotes.remove(note);
					}
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
						if (mm.getCommand() == ShortMessage.NOTE_ON && mm.getData2() == 120) {
						}
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Pads Channel1 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playPadChannel2(MidiQueueMessage mqm) {
			return true;
		}

		private boolean playBeatChannel1(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playBeatChannel1");

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			if (beatTimeFrame == null) {
				return false;
			}

			ChannelData beat1Channel = channels[BEAT_1_CHANNEL];

			if (writeTrack && beat1Track == null) {
				beat1Track = sequence.createTrack();
				createEvent(beat1Track, beat1Channel, PROGRAM, beat1Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(beatTimeFrame);

			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = beatTimeFrame.getElements();
			if (beatsChannel1LastNotes == null) {
				beatsChannel1LastNotes = new HashSet<>();
			}

			double maxAmp = -1;
			for (ToneMapElement toneMapElement : ttfElements) {
				double amp = toneMapElement.amplitude;
				if (maxAmp < amp) {
					maxAmp = amp;
				}
			}

			double amplitude = maxAmp;

			int volume = 0;
			if (amplitude > highVoiceThreshold) {
				volume = 120;
			} else if (amplitude <= lowVoiceThreshold) {
				volume = 0;
			} else {
				volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 120);
			}

			int beat1Note = 60;

			if (maxAmp > lowVoiceThreshold) {
				if (!beatsChannel1LastNotes.contains(beat1Note)) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_ON, beat1Channel.num, beat1Note, volume);
						if (writeTrack) {
							createEvent(beat1Track, beat1Channel, NOTEON, beat1Note, tick, volume);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					beatsChannel1LastNotes.add(beat1Note);
				}
			} else {
				if (beatsChannel1LastNotes.contains(beat1Note)) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, beat1Channel.num, beat1Note, 0);
						if (writeTrack) {
							createEvent(beat1Track, beat1Channel, NOTEOFF, beat1Note, tick, volume);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					beatsChannel1LastNotes.remove(beat1Note);
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Beat Channel1 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playBeatChannel2(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playBeatChannel2");

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap onsetToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_ONSET, midiStream.getStreamId()));

			ToneTimeFrame onsetTimeFrame = onsetToneMap.getTimeFrame(mqm.sequence);

			if (onsetTimeFrame == null) {
				return false;
			}

			TimeSet timeSet = onsetTimeFrame.getTimeSet();
			PitchSet pitchSet = onsetTimeFrame.getPitchSet();

			ShortMessage midiMessage = null;

			ChannelData beat2Channel = channels[BEAT_2_CHANNEL];

			if (writeTrack && beat2Track == null) {
				beat2Track = sequence.createTrack();
				createEvent(beat2Track, beat2Channel, PROGRAM, beat2Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(onsetTimeFrame);

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = onsetTimeFrame.getElements();

			if (beatsChannel2LastNotes == null) {
				beatsChannel2LastNotes = new HashSet<>();
			}

			double maxAmp = -1;
			for (ToneMapElement toneMapElement : ttfElements) {
				double amp = toneMapElement.amplitude;
				if (maxAmp < amp) {
					maxAmp = amp;
				}
			}

			int beat2Note = 50;

			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				double amplitude = toneMapElement.amplitude;

				int volume = 0;
				if (amplitude > highVoiceThreshold) {
					volume = 120;
				} else if (amplitude <= lowVoiceThreshold) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 120);
				}

				if (volume > 0) {
					if (!beatsChannel2LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, beat2Channel.num, beat2Note, volume);
							if (writeTrack) {
								createEvent(beat2Track, beat2Channel, NOTEON, beat2Note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						beatsChannel2LastNotes.add(note);
					}
				} else {
					if (beatsChannel2LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, beat2Channel.num, beat2Note, 0);
							if (writeTrack) {
								createEvent(beat2Track, beat2Channel, NOTEOFF, beat2Note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						beatsChannel2LastNotes.remove(note);
					}
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Beat Channel2 error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private boolean playBeatChannel3(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playBeatChannel3");

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_PERCUSSION, midiStream.getStreamId()));

			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			if (beatTimeFrame == null) {
				return false;
			}

			ChannelData beat3Channel = channels[BEAT_3_CHANNEL];

			if (writeTrack && beat3Track == null) {
				beat3Track = sequence.createTrack();
				createEvent(beat3Track, beat3Channel, PROGRAM, beat3Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(beatTimeFrame);

			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = beatTimeFrame.getElements();
			if (beatsChannel3LastNotes == null) {
				beatsChannel3LastNotes = new HashSet<>();
			}

			double maxAmp = -1;
			for (ToneMapElement toneMapElement : ttfElements) {
				double amp = toneMapElement.amplitude;
				if (maxAmp < amp) {
					maxAmp = amp;
				}
			}

			double amplitude = maxAmp;

			int volume = 0;
			if (amplitude > highVoiceThreshold) {
				volume = 120;
			} else if (amplitude <= lowVoiceThreshold) {
				volume = 0;
			} else {
				volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 120);
			}

			int beat3Note = 60;

			if (maxAmp > lowVoiceThreshold) {
				if (!beatsChannel3LastNotes.contains(beat3Note)) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_ON, beat3Channel.num, beat3Note, volume);
						if (writeTrack) {
							createEvent(beat3Track, beat3Channel, NOTEON, beat3Note, tick, volume);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					beatsChannel3LastNotes.add(beat3Note);
				}
			} else {
				if (beatsChannel3LastNotes.contains(beat3Note)) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, beat3Channel.num, beat3Note, 0);
						if (writeTrack) {
							createEvent(beat3Track, beat3Channel, NOTEOFF, beat3Note, tick, volume);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					beatsChannel3LastNotes.remove(beat3Note);
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Beat Channel3 error ", e);
						return false;
					}
				}
			}
			return true;

		}

		private boolean playBeatChannel4(MidiQueueMessage mqm) {
			return true;

		}

		private boolean playBaseChannel(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playBaseChannel");
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);

			ToneMap chromaToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, midiStream.getStreamId()));

			ToneTimeFrame chromaTimeFrame = chromaToneMap.getTimeFrame(mqm.sequence);

			ChannelData base1Channel = channels[BASE_1_CHANNEL];

			if (writeTrack && baseTrack == null) {
				baseTrack = sequence.createTrack();
				createEvent(baseTrack, base1Channel, PROGRAM, base1Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(chromaTimeFrame);

			TimeSet timeSet = chromaTimeFrame.getTimeSet();
			PitchSet pitchSet = chromaTimeFrame.getPitchSet();
			NoteStatus noteStatus = chromaTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = chromaTimeFrame.getElements();
			if (baseChannelLastNotes == null) {
				baseChannelLastNotes = new HashSet<>();
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
				if (amplitude > 1.0) {
					volume = 120;
				} else if (amplitude <= 0.1) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - 0.1) / (1.0 - 0.1)) * 120);
				}

				int octaveAdjust = 1;

				if (amplitude >= 0.5) {
					if (!baseChannelLastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, base1Channel.num, (note + 12 * octaveAdjust),
									volume);
							if (writeTrack) {
								createEvent(baseTrack, base1Channel, NOTEON, (note + 12 * octaveAdjust), tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						baseChannelLastNotes.add(note);
					}
				} else {
					if (baseChannelLastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, base1Channel.num, (note + 12 * octaveAdjust),
									0);
							if (writeTrack) {
								createEvent(baseTrack, base1Channel, NOTEOFF, (note + 12 * octaveAdjust), tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						baseChannelLastNotes.remove(note);
					}
				}
			}

			if (!silentWrite && synthesizer != null && synthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						synthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Base Channel error ", e);
						return false;
					}
				}
			}

			return true;

		}

		private void clearChannels() {
			if (channels != null) {
				clearChannel(channels[VOICE_1_CHANNEL]);
				clearChannel(channels[VOICE_2_CHANNEL]);
				clearChannel(channels[VOICE_3_CHANNEL]);
				clearChannel(channels[VOICE_4_CHANNEL]);
				clearChannel(channels[CHORD_1_CHANNEL]);
				clearChannel(channels[CHORD_2_CHANNEL]);
				clearChannel(channels[PAD_1_CHANNEL]);
				clearChannel(channels[PAD_2_CHANNEL]);
				clearChannel(channels[BEAT_1_CHANNEL]);
				clearChannel(channels[BEAT_2_CHANNEL]);
				clearChannel(channels[BEAT_3_CHANNEL]);
				clearChannel(channels[BEAT_4_CHANNEL]);
				clearChannel(channels[BASE_1_CHANNEL]);
			}
			voiceChannel1LastNotes = new HashSet<>();
			voiceChannel2LastNotes = new HashSet<>();
			voiceChannel3LastNotes = new HashSet<>();
			voiceChannel4LastNotes = new HashSet<>();
			chordsChannel1LastNotes = new HashSet<>();
			chordsChannel2LastNotes = new HashSet<>();
			padsChannel1LastNotes = new HashSet<>();
			padsChannel2LastNotes = new HashSet<>();
			beatsChannel1LastNotes = new HashSet<>();
			beatsChannel2LastNotes = new HashSet<>();
			baseChannelLastNotes = new HashSet<>();

		}

		private void clearChannel(ChannelData channelData) {
			channelData.channel.allNotesOff();
			channelData.channel.allSoundOff();
			channelData.channel.resetAllControllers();
		}
	}

	private class MidiQueueMessage implements Delayed {

		private ToneTimeFrame toneTimeFrame = null;
		private long startTime;
		private int sequence;

		public MidiQueueMessage(ToneTimeFrame toneTimeFrame, int sequence, long delayInMilliseconds) {
			this.toneTimeFrame = toneTimeFrame;
			this.sequence = sequence;
			this.startTime = System.currentTimeMillis() + delayInMilliseconds;
		}

		public MidiQueueMessage(ToneTimeFrame toneTimeFrame, int sequence) {
			this(toneTimeFrame, sequence,
					parameterManager.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		}

		public MidiQueueMessage() {
			this(null, -1, parameterManager.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		}

		public ToneTimeFrame getToneTimeFrame() {
			return toneTimeFrame;
		}

		public long getStartTime() {
			return startTime;
		}

		public int getSequence() {
			return sequence;
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

		private BlockingQueue<MidiQueueMessage> bq;

		private String streamId;

		boolean closed = false;

		private MidiQueueConsumer consumer;

		public MidiStream(String streamId) {
			this.streamId = streamId;
			bq = new LinkedBlockingQueue<>();
			consumer = new MidiQueueConsumer(bq, this);
			// TODO LOOM Thread.startVirtualThread(new MidiQueueConsumer(bq, this));
			new Thread(new MidiQueueConsumer(bq, this)).start();
		}

		public void close() {
			bq.clear();
			bq.drainTo(new ArrayList<Object>());
			closed = true;
			consumer.stop();
			LOG.severe(">>MidiStream close stop and reset!!");
			InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();
			instrumentSession.setState(InstrumentSessionState.STOPPED);
			MidiSynthesizer.this.reset();
			if (controller.isCountDownLatch()) {
				LOG.finer(">>MidiStream close controller");
				controller.getCountDownLatch().countDown();
			}
		}

		public boolean isClosed() {
			return closed;
		}

		public BlockingQueue<MidiQueueMessage> getBq() {
			return bq;
		}

		public String getStreamId() {
			return streamId;
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
			return Objects.equals(streamId, other.streamId);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(streamId);
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

			if (message.getType() == 47 && playState != PAUSED && sequencer != null) {
				midiEOM = true;
				if (playState != STOPPED) {
					sequencer.stop();
					playState = EOM;
				}
			}
		}
	}

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
