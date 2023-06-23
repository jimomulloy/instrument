package jomu.instrument.audio;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import jomu.instrument.InstrumentException;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.BeatListElement;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.ChordListElement;
import jomu.instrument.workspace.tonemap.ChordNote;
import jomu.instrument.workspace.tonemap.NoteListElement;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.NoteTracker.NoteTrack;
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

	private static final int BEATS_CHANNEL = 9;

	private int bpmSetting = INIT_BPM_SETTING;

	private ChannelData channels[];

	private Instrument instruments[];

	private int pitchHigh = INIT_PITCH_HIGH;

	private int pitchLow = INIT_PITCH_LOW;

	private double quantizeBeatSetting = 0;

	private double quantizeDurationSetting = 0;

	private double timeEnd = INIT_TIME_END;
	private double timeStart = INIT_TIME_START;

	private Map<String, MidiStream> midiStreams = new ConcurrentHashMap<>();

	private int numChannels;

	private int playState = STOPPED;
	private Sequence midiSequence;

	private Synthesizer midiSynthesizer;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Controller controller;

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

	@Inject
	Coordinator coordinator;

	boolean synthesizerRunning = false;

	public void onStartup(@Observes Startup startupEvent) {
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
		LOG.severe(">>MIDI closing");
		clearChannels();
		if (midiSynthesizer != null && midiSynthesizer.isOpen()) {
			midiSynthesizer.close();
		}
		LOG.severe(">>MIDI close");
		midiSequence = null;
		midiSynthesizer = null;
		instruments = null;
	}

	public void clear(String streamId) {
		LOG.finer(">>MIDI clear");
		if (midiStreams.containsKey(streamId)) {
			MidiStream ms = midiStreams.get(streamId);
			ms.close();
		}
		MidiSynthesizer.this.reset();
		if (controller.isCountDownLatch()) {
			controller.getCountDownLatch().countDown();
		}
		LOG.finer(">>MIDI cleared");
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
			message.setMessage(type, cc.num, number, velocity);
			MidiEvent event = new MidiEvent(message, tick);
			track.add(event);
			return true;
		} catch (Exception ex) {
			throw new InstrumentException("Midi createEvent exception: " + ex.getMessage(), ex);
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
		return (midiSequence.getResolution() * getBPM() / 60);
	}

	/**
	 * Open MIDI Java Sound system objects
	 */
	public boolean open() {

		boolean useSynthesizer = parameterManager
				.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_USER_SYNTHESIZER_SWITCH);
		try {
			Info[] midiDevs = MidiSystem.getMidiDeviceInfo();
			LOG.finer(">>MidiSynth dev searched");
			for (Info midiDev : midiDevs) {
				LOG.finer(">>MidiSynth dev: " + midiDev);
			}

			MidiChannel[] midiChannels = new MidiChannel[0];
			Soundbank sb = null;
			if (useSynthesizer) {
				if (midiSynthesizer == null) {
					Properties ps = System.getProperties();
					ps.setProperty("javax.sound.config.file", "sound.properties");
					if ((midiSynthesizer = MidiSystem.getSynthesizer()) == null) {
						LOG.finer(">>MidiSynth MISSING SYNTH!!");
						throw new InstrumentException("Midi open error, MISSING SYNTH");
					}
				}

				midiSynthesizer.open();

				try {
					if (parameterManager.hasParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SOUND_FONTS)) {
						String fileName = storage.getObjectStorage().getBasePath()
								+ System.getProperty("file.separator") + parameterManager
										.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SOUND_FONTS);

						LOG.severe(">>midi file: " + fileName);
						File file = new File(fileName);
						if (file.exists()) {
							sb = MidiSystem.getSoundbank(file);
							midiSynthesizer.loadAllInstruments(sb);
							instruments = midiSynthesizer.getLoadedInstruments();
							LOG.severe(">>MidiSynth CustomSoundbank!!: " + instruments.length);
						}
					} else {
						LOG.finer(">>MidiSynth Default Soundbank!!");
					}
				} catch (Exception e) {
					LOG.log(Level.SEVERE, ">>MidiSynth open error", e);
					throw new InstrumentException("Midi open error: " + e.getMessage(), e);
				}

				if (instruments == null || instruments.length == 0) {
					sb = midiSynthesizer.getDefaultSoundbank();
					if (sb != null) {
						LOG.finer(">>MidiSynth DefaultSoundbank!!");
						instruments = midiSynthesizer.getDefaultSoundbank().getInstruments();
					} else {
						LOG.finer(">>MidiSynth AvailableSoundbank!!");
						instruments = midiSynthesizer.getAvailableInstruments();
					}
				}
				if (instruments == null || instruments.length == 0) {
					LOG.finer(">>MidiSynth MISSING INSTRUMENTS!!");
					throw new InstrumentException("Midi open error: MISSING INSTRUMENTS");
				}

				midiChannels = midiSynthesizer.getChannels();
			}

			numChannels = midiChannels.length;
			if (numChannels == 0) {
				LOG.finer(">>MidiSynth MISSING CHANNELS!!");
				// return false;
				channels = new ChannelData[16];
				for (int i = 0; i < channels.length; i++) {
					channels[i] = new ChannelData(null, i);
				}
			} else {
				channels = new ChannelData[midiChannels.length];
				for (int i = 0; i < channels.length; i++) {
					channels[i] = new ChannelData(midiChannels[i], i);
				}
			}

			LOG.severe(">>MidiSynth INIT CHANNELS: " + channels.length);
			initChannels();
			midiSequence = new Sequence(Sequence.PPQ, 10);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ">>MIDI Synthesiser open exception", ex);
			throw new InstrumentException("Midi open exception: " + ex.getMessage(), ex);
		}
		return true;
	}

	private void initChannels() {
		initChannel(channels[VOICE_1_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_1));
		LOG.severe(">>MidiSynth INIT CHANNELS V1: " + InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_1);
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
		initChannel(channels[BASE_1_CHANNEL],
				parameterManager.getParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BASE_1));
		initDrumChannel(channels[BEATS_CHANNEL]);
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
			clearChannel(channels[BASE_1_CHANNEL]);
			clearChannel(channels[BEATS_CHANNEL]);
		}
	}

	private void clearChannel(ChannelData channelData) {
		if (channelData.channel != null) {
			channelData.channel.allNotesOff();
			channelData.channel.allSoundOff();
			channelData.channel.resetAllControllers();
		}
	}

	private void initChannel(ChannelData channelData, String instrumentName) {
		Instrument channelInstrument = null;
		int instumentNumber = 0;

		if (instrumentName != null) {
			try {
				instumentNumber = Integer.parseInt(instrumentName);
				if (instruments != null && instruments.length > instumentNumber - 1) {
					channelInstrument = instruments[instumentNumber - 1];
				}
			} catch (NumberFormatException ex) {
				if (instruments != null) {
					for (Instrument instrument : instruments) {
						if (instrument.getName().toLowerCase().contains(instrumentName.toLowerCase())) {
							channelInstrument = instrument;
							break;
						}
					}
				}
			}
		}
		if (channelInstrument == null && instruments != null && instruments.length > 0) {
			channelInstrument = instruments[0];
		}

		if (midiSynthesizer != null) {
			midiSynthesizer.loadInstrument(channelInstrument);
		}

		if (channelData.channel != null) {
			channelData.channel.allNotesOff();
			channelData.channel.allSoundOff();
			channelData.channel.resetAllControllers();
			boolean soloState = false, muteState = false, omniState = false, monoState = false,
					localControlState = true;
			channelData.channel.setSolo(soloState);
			channelData.channel.setMute(muteState);
			channelData.channel.setOmni(omniState);
			channelData.channel.setMono(monoState);
			channelData.channel.localControl(localControlState);
			channelData.channel.programChange(channelInstrument.getPatch().getBank(),
					channelInstrument.getPatch().getProgram());
			programChange(channelData, channelInstrument.getPatch().getProgram());
		} else {
			channelData.program = instumentNumber;
		}
	}

	private void initDrumChannel(ChannelData channelData) {
		channelData.program = 1;
		if (channelData.channel != null) {
			channelData.channel.allNotesOff();
			channelData.channel.allSoundOff();
			channelData.channel.resetAllControllers();
			boolean soloState = false, muteState = false, omniState = false, monoState = false,
					localControlState = true;
			channelData.channel.setSolo(soloState);
			channelData.channel.setMute(muteState);
			channelData.channel.setOmni(omniState);
			channelData.channel.setMono(monoState);
			channelData.channel.localControl(localControlState);
			channelData.channel.programChange(0, 1);
		}
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
		Track[] tracks = midiSequence.getTracks();
		for (int i = 0; i < tracks.length; i++) {
			midiSequence.deleteTrack(tracks[i]);
		}
	}

	public void playFrameSequence(ToneTimeFrame toneTimeFrame, String streamId, int sequence)
			throws InvalidMidiDataException, MidiUnavailableException {

		MidiStream midiStream = midiStreams.get(streamId);
		LOG.severe(">>playFrameSequence for MidiStream: " + streamId);
		if (midiStream == null || midiStream.isClosed()) {
			LOG.severe(">>playFrameSequence reset MidiStream: " + streamId);
			clearTracks();
			midiStreams.put(streamId, new MidiStream(streamId));
			midiStream = midiStreams.get(streamId);
		}
		MidiQueueMessage midiQueueMessage = new MidiQueueMessage(toneTimeFrame, sequence);
		midiStream.getBq().add(midiQueueMessage);
		return;
	}

	public void programChange(ChannelData channelData, int program) {
		channelData.program = program;
		if (channelData.channel != null) {
			channelData.channel.programChange(program);
		}
		if (instruments != null && midiSynthesizer != null) {
			midiSynthesizer.loadInstrument(instruments[program]);
		}
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
			throw new InstrumentException("Midi saveMidiFile Exception writing out stream: " + ex.getMessage(), ex);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ">>saveMidiFile Exception writing out stream", ex);
			throw new InstrumentException("Midi saveMidiFile Exception writing out stream: " + ex.getMessage(), ex);
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
			throw new InstrumentException("Midi saveMidiFile Exception writing track: " + ex.getMessage(), ex);
		}
		Track newTrack = trackSequence.createTrack();
		for (int i = 0; i < track.size(); i++) {
			newTrack.add(track.get(i));
		}
		return saveMidiFile(file, trackSequence);
	}

	/**
	 * Write MIDI sequence to MIDI file
	 */
	public boolean saveMidiFile(File file, List<Track> tracks) {
		Sequence trackSequence;
		try {
			trackSequence = new Sequence(Sequence.PPQ, 10);
		} catch (InvalidMidiDataException ex) {
			LOG.log(Level.SEVERE, ">>saveMidiFile Exception writing out track", ex);
			throw new InstrumentException("Midi saveMidiFile Exception writing tracks: " + ex.getMessage(), ex);
		}
		for (Track track : tracks) {
			if (track != null) {
				Track newTrack = trackSequence.createTrack();
				for (int i = 0; i < track.size(); i++) {
					newTrack.add(track.get(i));
				}
			}
		}
		return saveMidiFile(file, trackSequence);
	}

	public boolean isSynthesizerRunning() {
		return synthesizerRunning;
	}

	public void setSynthesizerRunning(boolean synthesizerRunning) {
		this.synthesizerRunning = synthesizerRunning;
	}

	private class MidiQueueConsumer implements Runnable {

		private BlockingQueue<MidiQueueMessage> bq;
		private MidiStream midiStream;
		double sampleTime = 0;
		ToneTimeFrame lastTimeFrame = null;
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
		public Map<Integer, Double> padsChannel1LastNoteTimes;
		public Map<Integer, Double> padsChannel2LastNoteTimes;

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
			setSynthesizerRunning(true);
			LOG.severe(">>MidiQueueConsumer create");
		}

		public void stop() {
			running = false;
		}

		@Override
		public void run() {
			LOG.severe(">>MidiQueueConsumer start run");
			try {
				MidiSynthesizer.this.setSynthesizerRunning(true);
				boolean completed = false;
				boolean silentWrite = parameterManager
						.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);
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
				boolean midiSynthTracksSwitch = parameterManager
						.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SYNTH_TRACKS_SWITCH);
				int playDelay = parameterManager.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY);
				boolean writeTrack = parameterManager
						.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);

				boolean doneDelay = false;

				try {

					while (running) {

						if (playDelay > 0 && !doneDelay) {
							LOG.finer(">>MidiStream IN PLAY DELAY: " + System.currentTimeMillis());
							TimeUnit.MILLISECONDS.sleep((long) playDelay);
							LOG.finer(">>MidiStream AFTER PLAY DELAY: " + System.currentTimeMillis());
							doneDelay = true;
						}

						LOG.finer(">>MidiQueueConsumer running");
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

						lastTimeFrame = toneTimeFrame;

						if (sampleTime != 0 && !silentWrite) {
							TimeUnit.MILLISECONDS.sleep((long) (sampleTime * 1000));
						}

						if (midiStream.isClosed()) {
							stop();
							break;
						}
						if (midiSequence == null) {
							stop();
							break;
						}
						LOG.finer(">>MidiQueueConsumer running: " + toneTimeFrame.getStartTime());

						if (midiSynthTracksSwitch) {
							if (!playSynthTracks(mqm)) {
								stop();
								break;
							}
						} else {
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
						}

						TimeSet timeSet = toneTimeFrame.getTimeSet();
						sampleTime = timeSet.getSampleTimeSize();

					}
					if (midiSequence != null && midiSynthTracksSwitch && lastTimeFrame != null) {
						switchOffSynthTracks(lastTimeFrame, silentWrite, writeTrack);
					}
				} catch (InterruptedException e) {
					LOG.severe(">>MidiStream Interrupted: " + System.currentTimeMillis());
					Thread.currentThread().interrupt();
				}
				LOG.severe(">>MidiQueueConsumer run exit");
				clearChannels();

				if (writeTrack && completed) {
					InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();
					String baseDir = storage.getObjectStorage().getBasePath();
					String folder = Paths
							.get(baseDir,
									parameterManager.getParameter(
											InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
							.toString();
					String masterFileName = folder + System.getProperty("file.separator")
							+ instrumentSession.getInputAudioFileName() + "_recording_master.midi";

					LOG.finer(">>Writing MIDI file name: " + masterFileName);
					File file = new File(masterFileName);
					saveMidiFile(file, midiSequence);
					LOG.finer(">>Saved MIDI file name: " + masterFileName);

					if (midiSynthTracksSwitch) {

						List<Track> trackList = new ArrayList<>();
						addTrackToList(voice1Track, trackList);
						addTrackToList(voice2Track, trackList);
						addTrackToList(voice3Track, trackList);
						addTrackToList(voice4Track, trackList);
						String trackFileName = folder + System.getProperty("file.separator")
								+ instrumentSession.getInputAudioFileName() + "_recording_track_voices.midi";
						file = new File(trackFileName);
						saveMidiFile(file, trackList);

						trackList.clear();
						addTrackToList(voice1Track, trackList);
						addTrackToList(voice2Track, trackList);
						addTrackToList(voice3Track, trackList);
						addTrackToList(voice4Track, trackList);
						addTrackToList(chord1Track, trackList);
						addTrackToList(chord2Track, trackList);
						addTrackToList(pad1Track, trackList);
						addTrackToList(pad2Track, trackList);
						addTrackToList(baseTrack, trackList);
						trackFileName = folder + System.getProperty("file.separator")
								+ instrumentSession.getInputAudioFileName() + "_recording_track_ensemble.midi";
						file = new File(trackFileName);
						saveMidiFile(file, trackList);

						trackList.clear();
						addTrackToList(beat1Track, trackList);
						addTrackToList(beat2Track, trackList);
						addTrackToList(beat3Track, trackList);
						addTrackToList(beat4Track, trackList);
						trackFileName = folder + System.getProperty("file.separator")
								+ instrumentSession.getInputAudioFileName() + "_recording_track_beats.midi";
						file = new File(trackFileName);
						saveMidiFile(file, trackList);

						trackList.clear();

						if (chord1Track != null && chord1Track.size() > 2) {
							trackList.add(chord1Track);
						}
						if (chord2Track != null && chord2Track.size() > 2) {
							trackList.add(chord2Track);
						}
						if (trackList.size() > 0) {
							trackFileName = folder + System.getProperty("file.separator")
									+ instrumentSession.getInputAudioFileName() + "_recording_track_chords.midi";
							file = new File(trackFileName);
							saveMidiFile(file, trackList);
						}

						trackList.clear();
						if (pad1Track != null && pad1Track.size() > 2) {
							trackList.add(pad1Track);
						}
						if (pad2Track != null && pad2Track.size() > 2) {
							trackList.add(pad2Track);
						}
						if (trackList.size() > 0) {
							trackFileName = folder + System.getProperty("file.separator")
									+ instrumentSession.getInputAudioFileName() + "_recording_track_pads.midi";
							file = new File(trackFileName);
							saveMidiFile(file, trackList);
						}
					} else {
						Track[] tracks = midiSequence.getTracks();
						for (int i = 0; i < tracks.length; i++) {
							String trackFileName = folder + System.getProperty("file.separator")
									+ instrumentSession.getInputAudioFileName() + "_recording_track_"
									+ getTrackName(tracks[i]) + ".midi";
							LOG.finer(">>Writing MIDI file name: " + trackFileName);
							file = new File(trackFileName);
							saveMidiFile(file, tracks[i]);
						}
					}

					Track[] tracks = midiSequence.getTracks();
					for (int i = 0; i < tracks.length; i++) {
						midiSequence.deleteTrack(tracks[i]);
					}

					instrumentSession.setOutputMidiFileName(
							masterFileName.substring(masterFileName.lastIndexOf(System.getProperty("file.separator"))));
					instrumentSession.setOutputMidiFilePath(masterFileName);
					try {
						TimeUnit.SECONDS.sleep(2);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				LOG.finer(">>MidiQueueConsumer setSynthesizerRunning(false)");
				MidiSynthesizer.this.setSynthesizerRunning(false);
				LOG.finer(">>MidiQueueConsumer close and exit stream");
				this.midiStream.close();
			} catch (Exception ex) {
				LOG.log(Level.SEVERE, ">>MidiQueueConsumer Thread run exception: " + ex.getMessage(), ex);
				coordinator.handleException(
						new InstrumentException(">>MidiQueueConsumer Thread run exception: " + ex.getMessage(), ex));
			}
		}

		private void switchOffSynthTracks(ToneTimeFrame toneTimeFrame, boolean silentWrite, boolean writeTrack) {

			List<ShortMessage> midiMessages = new ArrayList<>();

			long tick = getTrackTick(toneTimeFrame);

			switchOffSynthTrack(tick, midiMessages, writeTrack, voice1Track, voiceChannel1LastNotes,
					channels[VOICE_1_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, voice2Track, voiceChannel2LastNotes,
					channels[VOICE_2_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, voice3Track, voiceChannel3LastNotes,
					channels[VOICE_3_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, voice4Track, voiceChannel4LastNotes,
					channels[VOICE_4_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, baseTrack, baseChannelLastNotes,
					channels[BASE_1_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, chord1Track, chordsChannel1LastNotes,
					channels[CHORD_1_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, chord2Track, chordsChannel2LastNotes,
					channels[CHORD_2_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, pad1Track, padsChannel1LastNotes,
					channels[PAD_1_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, pad2Track, padsChannel2LastNotes,
					channels[PAD_2_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, beat1Track, beatsChannel1LastNotes,
					channels[BEATS_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, beat2Track, beatsChannel2LastNotes,
					channels[BEATS_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, beat3Track, beatsChannel3LastNotes,
					channels[BEATS_CHANNEL]);
			switchOffSynthTrack(tick, midiMessages, writeTrack, beat4Track, beatsChannel4LastNotes,
					channels[BEATS_CHANNEL]);

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel1 error ", e);
						throw new InstrumentException("Send MIDI Voice Channel1 error: " + e.getMessage(), e);
					}
				}
			}

		}

		private void switchOffSynthTrack(long tick, List<ShortMessage> midiMessages, boolean writeTrack, Track track,
				Set<Integer> channelNotes, ChannelData channel) {
			if (channelNotes != null && channel != null) {
				for (int note : channelNotes) {
					ShortMessage midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, channel.num, note, 0);
						if (writeTrack && track != null) {
							createEvent(track, channel, NOTEOFF, note, tick, 0);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
				}
			}
		}

		private void addTrackToList(Track track, List<Track> trackList) {
			if (track != null) {
				trackList.add(track);
			}
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

		private boolean playSynthTracks(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playSynthTracks");
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);
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
			boolean midiPlayPadChordsSwitch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD_CHORDS_SWITCH);
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
			int maxTracksLower = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOTETRACKER_MAX_TRACKS_LOWER);
			int voice1VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_VOICE_1);
			int voice2VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_VOICE_2);
			int voice3VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_VOICE_3);
			int voice4VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_VOICE_4);
			int baseVolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BASE_1);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);

			ToneTimeFrame toneTimeFrame = mqm.toneTimeFrame;
			if (toneTimeFrame == null) {
				LOG.log(Level.SEVERE, "playSynthTracks error, missing ToneTimeFrame");
				throw new InstrumentException("playSynthTracks error, missing ToneTimeFrame");
			}

			ToneMap toneMap = toneTimeFrame.getToneMap();
			List<ShortMessage> midiMessages = new ArrayList<>();
			NoteTrack[] tracks = toneMap.getNoteTracker().getTracks();
			ChannelData noteTrackChannel = null;
			Set<Integer> noteTrackChannelLastNotes = null;
			Track midiTrack = null;

			Set<NoteTrack> extraTracks = new HashSet<>();
			for (NoteTrack track : tracks) {
				if ((track.getNumber() == 1 && midiPlayVoice1Switch)) {
					noteTrackChannel = channels[VOICE_1_CHANNEL];
					if (voiceChannel1LastNotes == null) {
						voiceChannel1LastNotes = new HashSet<>();
					}
					noteTrackChannelLastNotes = voiceChannel1LastNotes;
					if (writeTrack && voice1Track == null) {
						voice1Track = midiSequence.createTrack();
						createEvent(voice1Track, noteTrackChannel, PROGRAM, noteTrackChannel.program + 1, 1L, 127);
					}
					midiTrack = voice1Track;
					playSynthNoteTracks(new NoteTrack[] { track }, noteTrackChannel, noteTrackChannelLastNotes,
							midiTrack, toneTimeFrame, midiMessages, voice1VolumeFactor, true);
				}
				if ((track.getNumber() == 2 && midiPlayVoice2Switch)) {
					noteTrackChannel = channels[VOICE_2_CHANNEL];
					if (voiceChannel2LastNotes == null) {
						voiceChannel2LastNotes = new HashSet<>();
					}
					noteTrackChannelLastNotes = voiceChannel2LastNotes;
					if (writeTrack && voice2Track == null) {
						voice2Track = midiSequence.createTrack();
						midiTrack = voice2Track;
						createEvent(voice2Track, noteTrackChannel, PROGRAM, noteTrackChannel.program + 1, 1L, 127);
					}
					midiTrack = voice2Track;
					playSynthNoteTracks(new NoteTrack[] { track }, noteTrackChannel, noteTrackChannelLastNotes,
							midiTrack, toneTimeFrame, midiMessages, voice2VolumeFactor, true);
				}
				if ((track.getNumber() == 3 && midiPlayVoice3Switch)) {
					noteTrackChannel = channels[VOICE_3_CHANNEL];
					if (voiceChannel3LastNotes == null) {
						voiceChannel3LastNotes = new HashSet<>();
					}
					noteTrackChannelLastNotes = voiceChannel3LastNotes;
					if (writeTrack && voice3Track == null) {
						voice3Track = midiSequence.createTrack();
						midiTrack = voice3Track;
						createEvent(voice3Track, noteTrackChannel, PROGRAM, noteTrackChannel.program + 1, 1L, 127);
					}
					midiTrack = voice3Track;
					playSynthNoteTracks(new NoteTrack[] { track }, noteTrackChannel, noteTrackChannelLastNotes,
							midiTrack, toneTimeFrame, midiMessages, voice3VolumeFactor, true);
				}
				if ((track.getNumber() >= 4 && track.getNumber() <= maxTracksLower && midiPlayVoice4Switch)) {
					extraTracks.add(track);
				}
			}

			if (!extraTracks.isEmpty()) {
				noteTrackChannel = channels[VOICE_4_CHANNEL];
				if (voiceChannel4LastNotes == null) {
					voiceChannel4LastNotes = new HashSet<>();
				}
				noteTrackChannelLastNotes = voiceChannel4LastNotes;
				if (writeTrack && voice4Track == null) {
					voice4Track = midiSequence.createTrack();
					midiTrack = voice4Track;
					createEvent(voice4Track, noteTrackChannel, PROGRAM, noteTrackChannel.program + 1, 1L, 127);
				}
				midiTrack = voice4Track;
				playSynthNoteTracks(extraTracks.toArray(new NoteTrack[extraTracks.size()]), noteTrackChannel,
						noteTrackChannelLastNotes, midiTrack, toneTimeFrame, midiMessages, voice4VolumeFactor, true);
			}

			if (midiPlayChord1Switch || midiPlayChord2Switch
					|| ((midiPlayPad1Switch || midiPlayPad2Switch) && midiPlayPadChordsSwitch)) {
				playSynthChords(toneTimeFrame, mqm.sequence, tracks, midiMessages);
			}

			if ((midiPlayPad1Switch || midiPlayPad2Switch) && !midiPlayPadChordsSwitch) {
				playSynthPads(toneTimeFrame, mqm.sequence, tracks, midiMessages);
			}

			if (midiPlayBaseSwitch) {
				NoteTrack track = toneMap.getNoteTracker().getBaseTrack();
				if (track != null) {
					noteTrackChannel = channels[BASE_1_CHANNEL];
					if (baseChannelLastNotes == null) {
						baseChannelLastNotes = new HashSet<>();
					}
					noteTrackChannelLastNotes = baseChannelLastNotes;
					if (writeTrack && baseTrack == null) {
						baseTrack = midiSequence.createTrack();
						createEvent(baseTrack, noteTrackChannel, PROGRAM, noteTrackChannel.program + 1, 1L, 127);
					}
					midiTrack = baseTrack;
					playSynthNoteTracks(new NoteTrack[] { track }, noteTrackChannel, noteTrackChannelLastNotes,
							midiTrack, toneTimeFrame, midiMessages, baseVolumeFactor, false);
				}
			}

			if (midiPlayBeat1Switch || midiPlayBeat2Switch || midiPlayBeat3Switch || midiPlayBeat4Switch) {
				playSynthBeats(toneTimeFrame, mqm.sequence, tracks, midiMessages);
			}

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel1 error ", e);
						throw new InstrumentException("Send MIDI Voice Channel1 error: " + e.getMessage(), e);
					}
				}
			}

			return true;

		}

		private boolean playSynthChords(ToneTimeFrame toneTimeFrame, int sequence, NoteTrack[] tracks,
				List<ShortMessage> midiMessages) {
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean midiPlayChord1Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD1_SWITCH);
			boolean midiPlayChord2Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_CHORD2_SWITCH);
			boolean midiPlayChord3Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD1_SWITCH);
			boolean midiPlayChord4Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD2_SWITCH);
			boolean midiPlayPadChordsSwitch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD_CHORDS_SWITCH);
			int chord1VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_CHORD_1);
			int chord2VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_CHORD_2);
			int chord3VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_PAD_1);
			int chord4VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_PAD_2);

			if (midiPlayChord1Switch) {
				ChannelData chord1Channel = channels[CHORD_1_CHANNEL];
				if (writeTrack && chord1Track == null) {
					chord1Track = midiSequence.createTrack();
					createEvent(chord1Track, chord1Channel, PROGRAM, chord1Channel.program + 1, 1L, 127);
				}

				if (chordsChannel1LastNotes == null) {
					chordsChannel1LastNotes = new HashSet<>();
				}

				NoteTrack track = toneTimeFrame.getToneMap().getNoteTracker().getChordTrack(1);
				if (track != null) {
					playSynthNoteTracks(new NoteTrack[] { track }, chord1Channel, chordsChannel1LastNotes, chord1Track,
							toneTimeFrame, midiMessages, chord1VolumeFactor, false);
				}
			}

			if (midiPlayChord2Switch) {
				ChannelData chord2Channel = channels[CHORD_2_CHANNEL];
				if (writeTrack && chord2Track == null) {
					chord2Track = midiSequence.createTrack();
					createEvent(chord2Track, chord2Channel, PROGRAM, chord2Channel.program + 1, 1L, 127);
				}

				if (chordsChannel2LastNotes == null) {
					chordsChannel2LastNotes = new HashSet<>();
				}

				NoteTrack track = toneTimeFrame.getToneMap().getNoteTracker().getChordTrack(2);
				if (track != null) {
					playSynthNoteTracks(new NoteTrack[] { track }, chord2Channel, chordsChannel2LastNotes, chord2Track,
							toneTimeFrame, midiMessages, chord2VolumeFactor, false);
				}
			}

			if (midiPlayPadChordsSwitch && midiPlayChord3Switch) {
				ChannelData pad1Channel = channels[PAD_1_CHANNEL];
				if (writeTrack && pad1Track == null) {
					pad1Track = midiSequence.createTrack();
					createEvent(pad1Track, pad1Channel, PROGRAM, pad1Channel.program + 1, 1L, 127);
				}

				if (padsChannel1LastNotes == null) {
					padsChannel1LastNotes = new HashSet<>();
				}

				NoteTrack track = toneTimeFrame.getToneMap().getNoteTracker().getChordTrack(3);
				if (track != null) {
					playSynthNoteTracks(new NoteTrack[] { track }, pad1Channel, padsChannel1LastNotes, pad1Track,
							toneTimeFrame, midiMessages, chord3VolumeFactor, false);
				}
			}

			if (midiPlayPadChordsSwitch && midiPlayChord4Switch) {
				ChannelData pad2Channel = channels[PAD_2_CHANNEL];
				if (writeTrack && pad2Track == null) {
					pad2Track = midiSequence.createTrack();
					createEvent(pad2Track, pad2Channel, PROGRAM, pad2Channel.program + 1, 1L, 127);
				}

				if (padsChannel2LastNotes == null) {
					padsChannel2LastNotes = new HashSet<>();
				}

				NoteTrack track = toneTimeFrame.getToneMap().getNoteTracker().getChordTrack(4);
				if (track != null) {
					playSynthNoteTracks(new NoteTrack[] { track }, pad2Channel, padsChannel2LastNotes, pad2Track,
							toneTimeFrame, midiMessages, chord4VolumeFactor, false);
				}
			}
			return true;

		}

		private boolean playSynthBeats(ToneTimeFrame toneTimeFrame, int sequence, NoteTrack[] tracks,
				List<ShortMessage> midiMessages) {

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean midiPlayBeat1Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT1_SWITCH);
			boolean midiPlayBeat2Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT2_SWITCH);
			boolean midiPlayBeat3Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT3_SWITCH);
			boolean midiPlayBeat4Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_BEAT4_SWITCH);
			int drum1 = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_1);
			int drum2 = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_2);
			int drum3 = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_3);
			int drum4 = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_4);
			int beat1VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BEAT_1);
			int beat2VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BEAT_2);
			int beat3VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BEAT_3);
			int beat4VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_BEAT_4);

			long tick = getTrackTick(toneTimeFrame);

			ShortMessage midiMessage = null;

			ChannelData beatChannel = channels[BEATS_CHANNEL];

			if (midiPlayBeat1Switch) {
				ToneMap toneMap = toneTimeFrame.getToneMap();
				NoteTrack track = toneMap.getNoteTracker().getBeatTrack(1);
				if (track != null) {
					if (writeTrack && beat1Track == null) {
						beat1Track = midiSequence.createTrack();
						createEvent(beat1Track, beatChannel, PROGRAM, 1, 1L, 127);
					}

					if (beatsChannel1LastNotes == null) {
						beatsChannel1LastNotes = new HashSet<>();
					}
					playSynthNoteTracks(new NoteTrack[] { track }, beatChannel, beatsChannel1LastNotes, beat1Track,
							toneTimeFrame, midiMessages, beat1VolumeFactor, false);
				}
			}

			if (midiPlayBeat2Switch) {
				ToneMap toneMap = toneTimeFrame.getToneMap();
				NoteTrack track = toneMap.getNoteTracker().getBeatTrack(2);
				if (track != null) {
					if (writeTrack && beat2Track == null) {
						beat2Track = midiSequence.createTrack();
						createEvent(beat2Track, beatChannel, PROGRAM, 1, 1L, 127);
					}

					if (beatsChannel2LastNotes == null) {
						beatsChannel2LastNotes = new HashSet<>();
					}
					playSynthNoteTracks(new NoteTrack[] { track }, beatChannel, beatsChannel2LastNotes, beat2Track,
							toneTimeFrame, midiMessages, beat2VolumeFactor, false);
				}
			}

			if (midiPlayBeat3Switch) {
				ToneMap toneMap = toneTimeFrame.getToneMap();
				NoteTrack track = toneMap.getNoteTracker().getBeatTrack(3);
				if (track != null) {
					if (writeTrack && beat3Track == null) {
						beat3Track = midiSequence.createTrack();
						createEvent(beat3Track, beatChannel, PROGRAM, 1, 1L, 127);
					}

					if (beatsChannel3LastNotes == null) {
						beatsChannel3LastNotes = new HashSet<>();
					}
					playSynthNoteTracks(new NoteTrack[] { track }, beatChannel, beatsChannel3LastNotes, beat3Track,
							toneTimeFrame, midiMessages, beat3VolumeFactor, false);
				}
			}

			if (midiPlayBeat4Switch) {
				ToneMap toneMap = toneTimeFrame.getToneMap();
				NoteTrack track = toneMap.getNoteTracker().getBeatTrack(4);
				if (track != null) {
					if (writeTrack && beat4Track == null) {
						beat4Track = midiSequence.createTrack();
						createEvent(beat4Track, beatChannel, PROGRAM, 1, 1L, 127);
					}

					if (beatsChannel4LastNotes == null) {
						beatsChannel4LastNotes = new HashSet<>();
					}
					playSynthNoteTracks(new NoteTrack[] { track }, beatChannel, beatsChannel4LastNotes, beat4Track,
							toneTimeFrame, midiMessages, beat4VolumeFactor, false);
				}
			}

			return true;
		}

		private boolean playSynthPads(ToneTimeFrame toneTimeFrame, int sequence, NoteTrack[] tracks,
				List<ShortMessage> midiMessages) {
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean midiPlayPad1Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD1_SWITCH);
			boolean midiPlayPad2Switch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_PAD2_SWITCH);
			int pad1VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_PAD_1);
			int pad2VolumeFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_VOLUME_PAD_2);
			int synthPadBeat = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_CHORD1_MEASURE);
			int synthPad1Octave = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_PAD1_OCTAVE);
			int synthPad2Octave = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SYNTHESIS_PAD2_OCTAVE);

			long tick = getTrackTick(toneTimeFrame);

			ShortMessage midiMessage = null;

			double beatAmp = 0;
			BeatListElement ble = null;
			Optional<BeatListElement> ob = toneTimeFrame.getBeat(CellTypes.AUDIO_BEAT.name());
			if (ob.isPresent()) {
				ble = ob.get();
				beatAmp = ble.getAmplitude();
			}

			if (midiPlayPad1Switch) {
				ChannelData pad1Channel = channels[PAD_1_CHANNEL];
				if (writeTrack && pad1Track == null) {
					pad1Track = midiSequence.createTrack();
					createEvent(pad1Track, pad1Channel, PROGRAM, pad1Channel.program + 1, 1L, 127);
				}
				if (padsChannel1LastNotes == null) {
					padsChannel1LastNotes = new HashSet<>();
					padsChannel1LastNoteTimes = new HashMap<>();
				}

				ChordListElement chord = null;
				Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_ONSET.name() + "_PADS");
				if (oc.isPresent()) {
					chord = oc.get();
				}

				List<Integer> volumes = new ArrayList<>();
				List<Integer> notes = new ArrayList<>();

				if (chord != null) {
					toneTimeFrame.sharpenChord(chord);
					int octaveAdjust = synthPad1Octave;
					ChordNote[] chordNotes = chord.getChordNotes().toArray(new ChordNote[chord.getChordNotes().size()]);
					Arrays.sort(chordNotes, new Comparator<ChordNote>() {
						public int compare(ChordNote c1, ChordNote c2) {
							return Double.valueOf(c2.getAmplitude()).compareTo(Double.valueOf(c1.getAmplitude()));
						}
					});
					int rootNote = -1;
					for (ChordNote chordNote : chordNotes) {
						int volume = 0;
						int note = 0;
						double amplitude = 0;
						amplitude = chordNote.getAmplitude();

						note = chordNote.getPitchClass();
						if (rootNote < 0) {
							rootNote = note;
						}
						if (note < rootNote) {
							note += 12;
						}
						note += octaveAdjust * 12;

						volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, false, false,
								amplitude, pad1VolumeFactor);
						if (volume > 0) {
							volumes.add(volume);
							notes.add(note);
						}
					}
				}

				Set<Integer> oldNotes = padsChannel1LastNotes.stream().filter(n -> !notes.contains(n))
						.collect(Collectors.toSet());

				for (int note : oldNotes) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, pad1Channel.num, note, 0);
						if (writeTrack) {
							createEvent(pad1Track, pad1Channel, NOTEOFF, note, tick, 0);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					padsChannel1LastNotes.remove(note);
					padsChannel1LastNoteTimes.remove(note);
				}

				for (int i = 0; i < notes.size(); i++) {
					int note = notes.get(i);
					int volume = volumes.get(i);
					double lastNoteTime = 0;
					if (padsChannel1LastNoteTimes.containsKey(note)) {
						lastNoteTime = padsChannel1LastNoteTimes.get(note);
					}

					if (beatAmp > ToneTimeFrame.AMPLITUDE_FLOOR && (lastNoteTime == 0
							|| ble.getStartTime() <= lastNoteTime
							|| toneTimeFrame.getStartTime() >= (lastNoteTime + ble.getTimeRange() * synthPadBeat))) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, pad1Channel.num, note, volume);
							if (writeTrack) {
								createEvent(pad1Track, pad1Channel, NOTEON, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						padsChannel1LastNotes.add(note);
						padsChannel1LastNoteTimes.put(note, toneTimeFrame.getStartTime());
					}
				}
			}

			if (midiPlayPad2Switch) {

				ChannelData pad2Channel = channels[PAD_2_CHANNEL];

				if (writeTrack && pad2Track == null) {
					pad2Track = midiSequence.createTrack();
					createEvent(pad2Track, pad2Channel, PROGRAM, pad2Channel.program + 1, 1L, 127);
				}
				if (padsChannel2LastNotes == null) {
					padsChannel2LastNotes = new HashSet<>();
					padsChannel2LastNoteTimes = new HashMap<>();
				}

				ChordListElement chord = null;
				Optional<ChordListElement> oc = toneTimeFrame.getChordList(CellTypes.AUDIO_HPS.name() + "_PADS");
				if (oc.isPresent()) {
					chord = oc.get();
				}

				List<Integer> volumes = new ArrayList<>();
				List<Integer> notes = new ArrayList<>();

				if (chord != null) {
					toneTimeFrame.sharpenChord(chord);
					int octaveAdjust = synthPad2Octave;
					ChordNote[] chordNotes = chord.getChordNotes().toArray(new ChordNote[chord.getChordNotes().size()]);
					Arrays.sort(chordNotes, new Comparator<ChordNote>() {
						public int compare(ChordNote c1, ChordNote c2) {
							return Double.valueOf(c2.getAmplitude()).compareTo(Double.valueOf(c1.getAmplitude()));
						}
					});
					int rootNote = -1;
					for (ChordNote chordNote : chordNotes) {
						int volume = 0;
						int note = 0;
						double amplitude = 0;
						amplitude = chordNote.getAmplitude();

						note = chordNote.getPitchClass();
						if (rootNote < 0) {
							rootNote = note;
						}
						if (note < rootNote) {
							note += 12;
						}
						note += octaveAdjust * 12;

						volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, false, false,
								amplitude, pad2VolumeFactor);
						if (volume > 0) {
							volumes.add(volume);
							notes.add(note);
						}
					}
				}

				Set<Integer> oldNotes = padsChannel2LastNotes.stream().filter(n -> !notes.contains(n))
						.collect(Collectors.toSet());

				for (int note : oldNotes) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, pad2Channel.num, note, 0);
						if (writeTrack) {
							createEvent(pad2Track, pad2Channel, NOTEOFF, note, tick, 0);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					padsChannel2LastNotes.remove(note);
					padsChannel2LastNoteTimes.remove(note);
				}

				for (int i = 0; i < notes.size(); i++) {
					int note = notes.get(i);
					int volume = volumes.get(i);
					double lastNoteTime = 0;
					if (beatAmp > ToneTimeFrame.AMPLITUDE_FLOOR && (lastNoteTime == 0
							|| ble.getStartTime() <= lastNoteTime
							|| toneTimeFrame.getStartTime() >= (lastNoteTime + ble.getTimeRange() * synthPadBeat))) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, pad2Channel.num, note, volume);
							if (writeTrack) {
								createEvent(pad2Track, pad2Channel, NOTEON, note, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						padsChannel2LastNotes.add(note);
						padsChannel2LastNoteTimes.put(note, toneTimeFrame.getStartTime());
					}
				}
			}

			return true;

		}

		private void playSynthNoteTracks(NoteTrack[] tracks, ChannelData noteTrackChannel,
				Set<Integer> noteTrackChannelLastNotes, Track midiTrack, ToneTimeFrame toneTimeFrame,
				List<ShortMessage> midiMessages, int volumeFactor, boolean followAmp) {
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean playVolume = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOLUME_SWITCH);
			int glissandoRange = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_GLISSANDO_RANGE);
			boolean playGlissando = true;

			double playTime = toneTimeFrame.getStartTime() * 1000;
			long tick = getTrackTick(toneTimeFrame);

			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			ShortMessage midiMessage = null;
			int note = 0;
			int volume = 0;

			Set<NoteListElement> nles = new HashSet<>();
			for (NoteTrack track : tracks) {
				nles.addAll(Arrays.asList(track.getNotes(playTime)));
			}
			Set<NoteListElement> snles = new HashSet<>();
			Set<NoteListElement> enles = new HashSet<>();
			for (NoteListElement nle : nles) {
				if (nle.startTime == playTime) {
					snles.add(nle);
				}
				if (nle.endTime < playTime) {
					enles.add(nle);
				}
				boolean noteFound = false;
				for (int ln : noteTrackChannelLastNotes) {
					if (nle.note == ln) {
						noteFound = true;
						break;
					}
				}
				if (!noteFound) {
					enles.add(nle);
				}
			}

			Set<Integer> discardNotes = new HashSet<>();

			for (int ln : noteTrackChannelLastNotes) {
				note = ln;
				boolean noteFound = false;
				for (NoteListElement nle : nles) {
					if (nle.note == ln) {
						noteFound = true;
						break;
					}
				}
				if (!noteFound) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, noteTrackChannel.num, note, 0);
						if (writeTrack) {
							createEvent(midiTrack, noteTrackChannel, NOTEOFF, note, tick, 0);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					discardNotes.add(note);
				}
			}

			for (int dn : discardNotes) {
				noteTrackChannelLastNotes.remove(dn);
			}

			for (NoteListElement enle : enles) {
				if (enle != null) {
					note = enle.note;
					if (noteTrackChannelLastNotes.contains(note)) {
						MidiPitchBend pitchBend = new MidiPitchBend();
						pitchBend.setBendAmount(0);
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.PITCH_BEND, noteTrackChannel.num,
									pitchBend.getLeastSignificantBits(), pitchBend.getMostSignificantBits());
							if (writeTrack) {
								createEvent(midiTrack, noteTrackChannel, ShortMessage.PITCH_BEND,
										pitchBend.getLeastSignificantBits(), tick, pitchBend.getMostSignificantBits());
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);

						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, noteTrackChannel.num, note, 0);
							if (writeTrack) {
								createEvent(midiTrack, noteTrackChannel, NOTEOFF, note, tick, 0);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						noteTrackChannelLastNotes.remove(note);
					}

					if (playGlissando && enle.legatoAfter != null && enle.legatoAfter.startTime < playTime) {
						note = enle.legatoAfter.note;
						if (!noteTrackChannelLastNotes.contains(note)) {
							volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
									false, enle.legatoAfter.maxAmp, volumeFactor);
							midiMessage = new ShortMessage();
							try {
								midiMessage.setMessage(ShortMessage.NOTE_ON, noteTrackChannel.num, note, volume);
								if (writeTrack) {
									createEvent(midiTrack, noteTrackChannel, NOTEON, note, tick, volume);
								}
							} catch (InvalidMidiDataException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							midiMessages.add(midiMessage);
							noteTrackChannelLastNotes.add(note);
						}
					}
				}
			}

			for (NoteListElement snle : snles) {
				if (snle != null) {
					note = snle.note;
					if (followAmp && playVolume) {
						volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
								false, toneTimeFrame.getElement(pitchSet.getIndex(note)).amplitude, volumeFactor);
					} else {
						volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
								false, snle.maxAmp, volumeFactor);
					}

					if (!playGlissando || snle.legatoBefore == null || snle.legatoBefore.endTime <= playTime) {
						if (!noteTrackChannelLastNotes.contains(note)) {
							if (playVolume) {
								midiMessage = new ShortMessage();
								try {
									midiMessage.setMessage(ShortMessage.CONTROL_CHANGE, noteTrackChannel.num, 7,
											volume);
									if (writeTrack) {
										createEvent(midiTrack, noteTrackChannel, ShortMessage.CONTROL_CHANGE, 7, tick,
												volume);
									}
								} catch (InvalidMidiDataException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								midiMessages.add(midiMessage);
							}
							midiMessage = new ShortMessage();
							try {
								midiMessage.setMessage(ShortMessage.NOTE_ON, noteTrackChannel.num, note, volume);
								if (writeTrack) {
									createEvent(midiTrack, noteTrackChannel, NOTEON, note, tick, volume);
								}
							} catch (InvalidMidiDataException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							midiMessages.add(midiMessage);
							noteTrackChannelLastNotes.add(note);
						}
					}
				}
			}

			NoteListElement pnle = null;
			for (NoteListElement nle : nles) {
				if (pnle == null || (nle.startTime < pnle.startTime)) {
					pnle = nle;
				}

			}
			if (playGlissando && pnle != null) {
				note = pnle.note;
				if (noteTrackChannelLastNotes.contains(note)) {
					MidiPitchBend pitchBend = glissando(toneTimeFrame, pnle, glissandoRange);
					if (pitchBend != null) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.PITCH_BEND, noteTrackChannel.num,
									pitchBend.getLeastSignificantBits(), pitchBend.getMostSignificantBits());
							if (writeTrack) {
								createEvent(midiTrack, noteTrackChannel, ShortMessage.PITCH_BEND,
										pitchBend.getLeastSignificantBits(), tick, pitchBend.getMostSignificantBits());
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
					}
				}
			}

			if (pnle != null) {
				note = pnle.note;
				int increment = (int) ((playTime - pnle.startTime));
				// if (increment >= 300 && increment % 300 < 100) {
				if (noteTrackChannelLastNotes.contains(note)) {
					if (followAmp && playVolume) {
						volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
								false, toneTimeFrame.getElement(pitchSet.getIndex(note)).amplitude, volumeFactor);
					} else {
						volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
								false, pnle.maxAmp, volumeFactor);
					}
					if (playVolume) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.CONTROL_CHANGE, noteTrackChannel.num, 7, volume);
							if (writeTrack) {
								createEvent(midiTrack, noteTrackChannel, ShortMessage.CONTROL_CHANGE, 7, tick, volume);
							}
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
					}
				}
				// }
			}

		}

		private boolean playVoiceChannel1(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playVoiceChannel1");
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean playVolume = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOLUME_SWITCH);

			ToneMap toneMap = null;
			ToneTimeFrame toneTimeFrame = null;
			toneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));

			toneTimeFrame = toneMap.getTimeFrame(mqm.sequence);
			if (toneTimeFrame == null) {
				return false;
			}

			ChannelData voice1Channel = channels[VOICE_1_CHANNEL];

			if (writeTrack && voice1Track == null) {
				voice1Track = midiSequence.createTrack();
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

			LOG.finer(">>Midi playVoiceChannel1: " + toneTimeFrame.getStartTime());
			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				noteStatusElement = noteStatus.getNoteStatusElement(note);
				double amplitude = toneMapElement.amplitude;

				int volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
						toneMapElement.isPeak, amplitude);

				switch (noteStatusElement.state) {
				case ON:
				case PENDING:
				case CONTINUING:
					LOG.severe(">>V1 MIDI NOTE CANDIDATE...: " + mqm.getSequence() + ", " + note + ", " + volume + ", "
							+ amplitude + ", " + highVoiceThreshold + ", " + lowVoiceThreshold);
					if (!voiceChannel1LastNotes.contains(note)) {
						LOG.severe(">>V1 NOTE_ON: " + mqm.getSequence() + ", " + note + ", " + volume + ", " + amplitude
								+ ", " + highVoiceThreshold + ", " + lowVoiceThreshold);
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
						if (playVolume) {
							midiMessage = new ShortMessage();
							try {
								midiMessage.setMessage(ShortMessage.CONTROL_CHANGE, voice1Channel.num, note, volume);
							} catch (InvalidMidiDataException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							midiMessages.add(midiMessage);
						}
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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel1 error ", e);
						throw new InstrumentException("Send MIDI Voice Channel1 error: " + e.getMessage(), e);
					}
				}
			}

			return true;

		}

		private MidiPitchBend glissando(ToneTimeFrame toneTimeFrame, NoteListElement noteListElement,
				int glissandoRange) {
			MidiPitchBend pitchBend = null;
			if (glissandoRange == 0) {
				return pitchBend;
			}
			double pitchBendAmount = 0;
			double currentTime = toneTimeFrame.getStartTime() * 1000;
			double glissandoNoteRange = 0;
			double glissandoMidTime = 0;
			double glissandoStartTime = 0;

			if (noteListElement.legatoAfter != null) {
				NoteListElement nleo = noteListElement.legatoAfter;
				glissandoMidTime = noteListElement.endTime + noteListElement.incrementTime;
				LOG.finer(">>Midi glissando C: " + ", " + currentTime + ", " + glissandoMidTime + ", "
						+ noteListElement.startTime + ", " + noteListElement.endTime + ", " + nleo.startTime + ", "
						+ nleo.endTime + ", " + noteListElement.note + ", " + nleo.note);
				if (currentTime > glissandoMidTime) {
					LOG.finer(">>Midi glissando C -1:");
					return pitchBend;
				} else {
					glissandoStartTime = glissandoMidTime - glissandoRange > noteListElement.startTime
							? glissandoMidTime - glissandoRange
							: noteListElement.startTime;
					if (currentTime >= glissandoStartTime) {
						if (nleo.note >= noteListElement.note) {
							glissandoNoteRange = (nleo.note - noteListElement.note) > 2 ? 2
									: (double) (nleo.note - noteListElement.note);
						} else {
							glissandoNoteRange = (noteListElement.note - nleo.note) > 2 ? -2
									: (double) (nleo.note - noteListElement.note);
						}
						pitchBendAmount += ((double) glissandoNoteRange / 2)
								* ((currentTime - glissandoStartTime) / (glissandoMidTime - glissandoStartTime));
						LOG.finer(">>Midi glissando C pitchBendAmount: " + pitchBendAmount + " ," + glissandoNoteRange
								+ ", " + glissandoStartTime);
						pitchBend = new MidiPitchBend();
						pitchBend.setBendAmount(pitchBendAmount);
					}
				}
			}
			return pitchBend;
		}

		private int getNoteVolume(double lowVoiceThreshold, double highVoiceThreshold, boolean playLog,
				double logFactor, boolean playPeaks, boolean isPeak, double amplitude) {
			return getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks, isPeak,
					amplitude);
		}

		private int getNoteVolume(double lowVoiceThreshold, double highVoiceThreshold, boolean playLog,
				double logFactor, boolean playPeaks, boolean isPeak, double amplitude, int volumeFactor) {
			double logAmplitude;
			double highLogThreshold;
			double lowLogThreshold;
			int volume;
			if (playPeaks && !isPeak) {
				amplitude = 0;
			}
			if (playLog) {
				highLogThreshold = (float) Math.log10(1 + (logFactor * highVoiceThreshold));
				lowLogThreshold = (float) Math.log10(1 + (logFactor * lowVoiceThreshold));
				logAmplitude = (float) Math.log10(1 + (logFactor * amplitude));
				volume = (int) (((logAmplitude - lowLogThreshold) / (highLogThreshold - lowLogThreshold)) * 127);
				if (volume > highLogThreshold * 127) {
					volume = 127;
				} else if (volume < lowLogThreshold * 127) {
					volume = 0;
				}
			} else {
				volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 127);
				if (volume > highVoiceThreshold * 127) {
					volume = 127;
				} else if (volume <= lowVoiceThreshold * 127) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 127);
				}
			}
			volume *= (double) volumeFactor / 100;
			if (volume > 127) {
				volume = 127;
			}
			return volume;
		}

		private boolean playVoiceChannel2(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playVoiceChannel2");
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean playVolume = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOLUME_SWITCH);

			ToneMap toneMap = null;
			ToneTimeFrame toneTimeFrame = null;

			toneMap = workspace.getAtlas().getToneMap(
					ToneMap.buildToneMapKey(CellTypes.AUDIO_INTEGRATE.toString() + "_PEAKS", midiStream.getStreamId()));
			toneTimeFrame = toneMap.getTimeFrame(mqm.sequence);
			if (toneTimeFrame == null) {
				return false;
			}

			ChannelData voice2Channel = channels[VOICE_2_CHANNEL];

			if (writeTrack && voice2Track == null) {
				voice2Track = midiSequence.createTrack();
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

			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				if ((note > 120 || note < 12)) {
					continue;
				}
				double amplitude = toneMapElement.amplitude;
				boolean isPeak = toneMapElement.isPeak;

				int volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, true, isPeak,
						amplitude);

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
					} else {
						if (playVolume) {
							midiMessage = new ShortMessage();
							try {
								midiMessage.setMessage(ShortMessage.CONTROL_CHANGE, voice2Channel.num, note, volume);
							} catch (InvalidMidiDataException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							midiMessages.add(midiMessage);
						}
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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel2 error ", e);
						throw new InstrumentException("Send MIDI Voice Channel2 error: " + e.getMessage(), e);
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
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean playVolume = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOLUME_SWITCH);

			ToneMap toneMap = null;
			ToneTimeFrame toneTimeFrame = null;

			toneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_INTEGRATE, midiStream.getStreamId()));
			toneTimeFrame = toneMap.getTimeFrame(mqm.sequence);
			if (toneTimeFrame == null) {
				return false;
			}
			ChannelData voice3Channel = channels[VOICE_3_CHANNEL];

			if (writeTrack && voice3Track == null) {
				voice3Track = midiSequence.createTrack();
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

			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				if ((note > 120 || note < 12)) {
					continue;
				}
				double amplitude = toneMapElement.amplitude;

				int volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
						toneMapElement.isPeak, amplitude);

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
					} else {
						if (playVolume) {
							midiMessage = new ShortMessage();
							try {
								midiMessage.setMessage(ShortMessage.CONTROL_CHANGE, voice3Channel.num, note, volume);
							} catch (InvalidMidiDataException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							midiMessages.add(midiMessage);
						}
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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel3 error ", e);
						throw new InstrumentException("Send MIDI Voice Channel3 error: " + e.getMessage(), e);
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
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			boolean playVolume = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_VOLUME_SWITCH);

			ToneMap toneMap = null;
			ToneTimeFrame toneTimeFrame = null;

			toneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));
			toneTimeFrame = toneMap.getTimeFrame(mqm.sequence);
			if (toneTimeFrame == null) {
				return false;
			}

			ChannelData voice4Channel = channels[VOICE_4_CHANNEL];

			if (writeTrack && voice4Track == null) {
				voice4Track = midiSequence.createTrack();
				createEvent(voice4Track, voice4Channel, PROGRAM, voice4Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(toneTimeFrame);

			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = toneTimeFrame.getElements();
			if (voiceChannel4LastNotes == null) {
				voiceChannel4LastNotes = new HashSet<>();
			}

			double logAmplitude = 0;
			double highLogThreshold = 0;
			double lowLogThreshold = 0;
			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				double amplitude = toneMapElement.amplitude;
				NoteListElement noteListElement = toneMapElement.noteListElement;

				int volume = 0;
				amplitude = 0;

				if (noteListElement != null) {

					amplitude = noteListElement.maxAmp;
					volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
							toneMapElement.isPeak, amplitude);

				}

				if (volume > 0) {
					if (!voiceChannel4LastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, voice4Channel.num, note, volume);
							LOG.finer(">>MIDI V4 NOTEON: " + toneTimeFrame.getStartTime() + ", " + note + ", " + volume
									+ ", " + logAmplitude + ", " + amplitude + ", " + highLogThreshold + ", "
									+ lowLogThreshold);
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
						if (playVolume) {
							midiMessage = new ShortMessage();
							try {
								midiMessage.setMessage(ShortMessage.CONTROL_CHANGE, voice4Channel.num, note, volume);
							} catch (InvalidMidiDataException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							midiMessages.add(midiMessage);
						}
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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Voice Channel4 error ", e);
						throw new InstrumentException("Send MIDI Voice Channel4 error: " + e.getMessage(), e);
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
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			boolean playPeaks = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PLAY_PEAKS);

			ToneMap toneMap = null;
			ToneTimeFrame toneTimeFrame = null;

			toneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			toneTimeFrame = toneMap.getTimeFrame(mqm.sequence);

			if (toneTimeFrame == null) {
				return false;
			}

			ChannelData chord1Channel = channels[CHORD_1_CHANNEL];

			if (writeTrack && chord1Track == null) {
				chord1Track = midiSequence.createTrack();
				createEvent(chord1Track, chord1Channel, PROGRAM, chord1Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(toneTimeFrame);

			TimeSet timeSet = toneTimeFrame.getTimeSet();
			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = toneTimeFrame.getElements();

			if (chordsChannel1LastNotes == null) {
				chordsChannel1LastNotes = new HashSet<>();
			}

			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				double amplitude = toneMapElement.amplitude;
				int volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, playPeaks,
						toneMapElement.isPeak, toneMapElement.amplitude);

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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Chord Channel1 error ", e);
						throw new InstrumentException("Send MIDI Chord Channel1 error: " + e.getMessage(), e);
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
			boolean midiSynthTracksSwitch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SYNTH_TRACKS_SWITCH);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);

			ToneMap toneMap = null;
			ToneTimeFrame toneTimeFrame = null;
			ChordListElement chord = null;

			if (midiSynthTracksSwitch) {
				toneMap = workspace.getAtlas()
						.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, midiStream.getStreamId()));
			} else {
				toneMap = workspace.getAtlas()
						.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			}

			toneTimeFrame = toneMap.getTimeFrame(mqm.sequence);

			if (toneTimeFrame == null) {
				return false;
			}

			if (midiSynthTracksSwitch) {
				Optional<ChordListElement> oc = toneMap.getToneSynthesiser().getChord(toneTimeFrame.getStartTime());
				if (oc.isPresent()) {
					chord = oc.get();
				}
			} else {
				chord = toneTimeFrame.getChord();
			}

			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			ChannelData chord2Channel = channels[CHORD_2_CHANNEL];

			if (writeTrack && chord2Track == null) {
				chord2Track = midiSequence.createTrack();
				createEvent(chord2Track, chord2Channel, PROGRAM, chord2Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(toneTimeFrame);
			PitchSet pitchSet = toneTimeFrame.getPitchSet();

			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			if (chordsChannel2LastNotes == null) {
				chordsChannel2LastNotes = new HashSet<>();
			}

			String tmKey = beatToneMap.getKey();
			String streamId = tmKey.substring(tmKey.lastIndexOf(":") + 1);
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);

			boolean hasNewChord = false;
			if (chord != null && (cm.getBeatAfterTime(beatTimeFrame.getStartTime(), 110) != -1)) {
				double beatRange = cm.getBeatRange(beatTimeFrame.getStartTime());
				double time = beatTimeFrame.getStartTime();
				if (beatRange != -1 && ((time - beatRange / 2) > chord.getStartTime())
						&& ((time + beatRange / 2) < chord.getEndTime())) {
					hasNewChord = true;
				}
			}

			List<Integer> volumes = new ArrayList<>();
			List<Integer> notes = new ArrayList<>();

			if (chord != null) {
				int sm = toneTimeFrame.getSpectralMean();
				int octaveAdjust = 4;
				ChordNote[] chordNotes = chord.getChordNotes().toArray(new ChordNote[chord.getChordNotes().size()]);
				Arrays.sort(chordNotes, new Comparator<ChordNote>() {
					public int compare(ChordNote c1, ChordNote c2) {
						return Double.valueOf(c2.getAmplitude()).compareTo(Double.valueOf(c1.getAmplitude()));
					}
				});

				int rootNote = -1;
				for (ChordNote chordNote : chordNotes) {
					int volume = 0;
					int note = 0;
					double amplitude = 0;
					amplitude = chordNote.getAmplitude();

					note = chordNote.getPitchClass();
					if (rootNote < 0) {
						rootNote = note;
					}
					if (note < rootNote) {
						note += 12;
					}
					note += octaveAdjust * 12;

					volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, false, false,
							amplitude);
					if (volume > 0) {
						volumes.add(volume);
						notes.add(note);
					}
				}

				HashSet<Integer> newNotes = new HashSet<Integer>(notes);
				if (!newNotes.equals(chordsChannel2LastNotes)) {
					hasNewChord = true;
				}
			}

			LOG.finer(">>MIDI C4 PLAY: " + toneTimeFrame.getStartTime());
			if ((chord == null && !chordsChannel2LastNotes.isEmpty()) || hasNewChord) {
				for (int note : chordsChannel2LastNotes) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, chord2Channel.num, note, 0);
						LOG.finer(">>MIDI C4 NOTE_OFF: " + note);
						if (writeTrack) {
							createEvent(chord2Track, chord2Channel, NOTEOFF, note, tick, 0);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
				}
				chordsChannel2LastNotes.clear();
			}

			if (hasNewChord) {
				for (int i = 0; i < notes.size(); i++) {
					int note = notes.get(i);
					int volume = volumes.get(i);
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_ON, chord2Channel.num, note, volume);
						LOG.finer(">>MIDI C4 NOTE_ON: " + note + ", " + volume);
						if (writeTrack) {
							createEvent(chord2Track, chord2Channel, NOTEON, note, tick, volume);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					chordsChannel2LastNotes.add(note);
				}
			}

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen())

			{
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Chord Channel2 error ", e);
						throw new InstrumentException("Send MIDI Chord Channel2 error: " + e.getMessage(), e);
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
				pad1Track = midiSequence.createTrack();
				createEvent(pad1Track, pad1Channel, PROGRAM, pad1Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(chromaTimeFrame);

			TimeSet timeSet = chromaTimeFrame.getTimeSet();
			PitchSet pitchSet = chromaTimeFrame.getPitchSet();
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = chromaTimeFrame.getElements();
			if (padsChannel1LastNotes == null) {
				padsChannel1LastNotes = new HashSet<>();
			}

			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				double amplitude = toneMapElement.amplitude;

				int volume = 0;
				if (amplitude > 1.0) {
					volume = 127;
				} else if (amplitude < 0.1) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - 0.1) / (1.0 - 0.1)) * 127);
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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Pads Channel1 error ", e);
						throw new InstrumentException("Send MIDI Pads Channel1 error: " + e.getMessage(), e);
					}
				}
			}

			return true;

		}

		private boolean playPadChannel2(MidiQueueMessage mqm) {
			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);
			boolean midiSynthTracksSwitch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SYNTH_TRACKS_SWITCH);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);

			ToneMap toneMap = null;
			ToneTimeFrame toneTimeFrame = null;
			ChordListElement chord = null;

			if (midiSynthTracksSwitch) {
				toneMap = workspace.getAtlas()
						.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, midiStream.getStreamId()));
			} else {
				toneMap = workspace.getAtlas()
						.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			}

			toneTimeFrame = toneMap.getTimeFrame(mqm.sequence);

			if (toneTimeFrame == null) {
				return false;
			}

			if (midiSynthTracksSwitch) {
				Optional<ChordListElement> oc = toneMap.getToneSynthesiser().getChord(toneTimeFrame.getStartTime());
				if (oc.isPresent()) {
					chord = oc.get();
				}
			} else {
				chord = toneTimeFrame.getChord();
			}

			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			ChannelData pad2Channel = channels[PAD_2_CHANNEL];

			if (writeTrack && pad2Track == null) {
				pad2Track = midiSequence.createTrack();
				createEvent(pad2Track, pad2Channel, PROGRAM, pad2Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(toneTimeFrame);
			PitchSet pitchSet = toneTimeFrame.getPitchSet();

			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			if (padsChannel2LastNotes == null) {
				padsChannel2LastNotes = new HashSet<>();
			}

			String tmKey = beatToneMap.getKey();
			String streamId = tmKey.substring(tmKey.lastIndexOf(":") + 1);

			List<Integer> volumes = new ArrayList<>();
			List<Integer> notes = new ArrayList<>();

			if (chord != null) {
				int octaveAdjust = 4;
				ChordNote[] chordNotes = chord.getChordNotes().toArray(new ChordNote[chord.getChordNotes().size()]);
				Arrays.sort(chordNotes, new Comparator<ChordNote>() {
					public int compare(ChordNote c1, ChordNote c2) {
						return Double.valueOf(c2.getAmplitude()).compareTo(Double.valueOf(c1.getAmplitude()));
					}
				});

				int rootNote = -1;
				for (ChordNote chordNote : chordNotes) {
					int volume = 0;
					int note = 0;
					double amplitude = 0;
					amplitude = chordNote.getAmplitude();

					note = chordNote.getPitchClass();
					if (rootNote < 0) {
						rootNote = note;
					}
					if (note < rootNote) {
						note += 12;
					}
					note += octaveAdjust * 12;

					volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, false, false,
							amplitude);
					if (volume > 0) {
						volumes.add(volume);
						notes.add(note);
					}
				}
			}

			Set<Integer> oldNotes = padsChannel2LastNotes.stream().filter(n -> !notes.contains(n))
					.collect(Collectors.toSet());

			for (int note : oldNotes) {
				midiMessage = new ShortMessage();
				try {
					midiMessage.setMessage(ShortMessage.NOTE_OFF, pad2Channel.num, note, 0);
					if (writeTrack) {
						createEvent(pad2Track, pad2Channel, NOTEOFF, note, tick, 0);
					}
				} catch (InvalidMidiDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				midiMessages.add(midiMessage);
				padsChannel2LastNotes.remove(note);
			}

			for (int i = 0; i < notes.size(); i++) {
				int note = notes.get(i);
				int volume = volumes.get(i);
				// if (!padsChannel2LastNotes.contains(note)) {
				midiMessage = new ShortMessage();
				try {
					midiMessage.setMessage(ShortMessage.NOTE_ON, pad2Channel.num, note, volume);
					if (writeTrack) {
						createEvent(pad2Track, pad2Channel, NOTEON, note, tick, volume);
					}
				} catch (InvalidMidiDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				midiMessages.add(midiMessage);
				padsChannel2LastNotes.add(note);
				// }
			}

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Pad Channel2 error ", e);
						throw new InstrumentException("Send MIDI Pad Channel2 error: " + e.getMessage(), e);
					}
				}
			}

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
			int drum = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_1);

			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			if (beatTimeFrame == null) {
				return false;
			}

			ChannelData beat1Channel = channels[BEATS_CHANNEL];

			if (writeTrack && beat1Track == null) {
				beat1Track = midiSequence.createTrack();
				createEvent(beat1Track, beat1Channel, PROGRAM, 1, 1L, 127);
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
				volume = 127;
			} else if (amplitude <= lowVoiceThreshold) {
				volume = 0;
			} else {
				volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 127);
			}

			int beat1Note = drum;

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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Beat Channel1 error ", e);
						throw new InstrumentException("Send MIDI Beat Channel1 error: " + e.getMessage(), e);
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
			int drum = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_2);

			ToneMap onsetToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_ONSET, midiStream.getStreamId()));

			ToneTimeFrame onsetTimeFrame = onsetToneMap.getTimeFrame(mqm.sequence);

			if (onsetTimeFrame == null) {
				return false;
			}

			TimeSet timeSet = onsetTimeFrame.getTimeSet();
			PitchSet pitchSet = onsetTimeFrame.getPitchSet();

			ShortMessage midiMessage = null;

			ChannelData beat2Channel = channels[BEATS_CHANNEL];

			if (writeTrack && beat2Track == null) {
				beat2Track = midiSequence.createTrack();
				createEvent(beat2Track, beat2Channel, PROGRAM, 1, 1L, 127);
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

			int beat2Note = drum;

			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				double amplitude = toneMapElement.amplitude;

				int volume = 0;
				if (amplitude > highVoiceThreshold) {
					volume = 127;
				} else if (amplitude <= lowVoiceThreshold) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 127);
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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Beat Channel2 error ", e);
						throw new InstrumentException("Send MIDI Beat Channel2 error: " + e.getMessage(), e);
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
			int drum = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_3);

			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_PERCUSSION, midiStream.getStreamId()));

			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			if (beatTimeFrame == null) {
				return false;
			}

			ChannelData beat3Channel = channels[BEATS_CHANNEL];

			if (writeTrack && beat3Track == null) {
				beat3Track = midiSequence.createTrack();
				createEvent(beat3Track, beat3Channel, PROGRAM, 1, 1L, 127);
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
				volume = 127;
			} else if (amplitude <= lowVoiceThreshold) {
				volume = 0;
			} else {
				volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 127);
			}

			int beat3Note = drum;

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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Beat Channel3 error ", e);
						throw new InstrumentException("Send MIDI Beat Channel3 error: " + e.getMessage(), e);
					}
				}
			}
			return true;

		}

		private boolean playBeatChannel4(MidiQueueMessage mqm) {
			LOG.finer(">>MIDI CHANNEL playBeatChannel1");

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
			boolean writeTrack = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH);
			boolean silentWrite = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE);
			boolean midiSynthTracksSwitch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_SYNTH_TRACKS_SWITCH);
			boolean playLog = parameterManager
					.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_LOG_SWITCH);
			double logFactor = parameterManager.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOG_FACTOR);
			int drum = parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_BEAT_4);

			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			if (beatTimeFrame == null) {
				return false;
			}

			ChannelData beat4Channel = channels[BEATS_CHANNEL];

			if (writeTrack && beat4Track == null) {
				beat4Track = midiSequence.createTrack();
				createEvent(beat4Track, beat4Channel, PROGRAM, 1, 1L, 127);
			}

			long tick = getTrackTick(beatTimeFrame);

			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = beatTimeFrame.getElements();
			if (beatsChannel4LastNotes == null) {
				beatsChannel4LastNotes = new HashSet<>();
			}

			String tmKey = beatToneMap.getKey();
			String streamId = tmKey.substring(tmKey.lastIndexOf(":") + 1);
			CalibrationMap cm = workspace.getAtlas().getCalibrationMap(streamId);

			int volume = 127;
			int beat4Note = drum;
			boolean hasBeat = false;
			double beatTime = cm.getBeatAfterTime(beatTimeFrame.getStartTime(), 110);
			if (beatTime != -1) {
				ToneTimeFrame ttf = beatToneMap.getTimeFrame(beatTime);
				if (ttf != null) {
					volume = getNoteVolume(lowVoiceThreshold, highVoiceThreshold, playLog, logFactor, false, false,
							ttf.getMaxAmplitude());
				}
				hasBeat = true;
				LOG.finer(">>MIDI C4 has beat: " + beatTimeFrame.getStartTime() + ", " + volume);
			}

			if (hasBeat) {
				if (!beatsChannel4LastNotes.contains(beat4Note)) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_ON, beat4Channel.num, beat4Note, volume);
						if (writeTrack) {
							createEvent(beat4Track, beat4Channel, NOTEON, beat4Note, tick, volume);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					beatsChannel4LastNotes.add(beat4Note);
				}
			} else {
				if (beatsChannel4LastNotes.contains(beat4Note)) {
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, beat4Channel.num, beat4Note, 0);
						if (writeTrack) {
							createEvent(beat4Track, beat4Channel, NOTEOFF, beat4Note, tick, volume);
						}
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					beatsChannel4LastNotes.remove(beat4Note);
				}
			}

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Beat Channel4 error 4 error: " + e.getMessage(), e);
					}
				}
			}

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
				baseTrack = midiSequence.createTrack();
				createEvent(baseTrack, base1Channel, PROGRAM, base1Channel.program + 1, 1L, 127);
			}

			long tick = getTrackTick(chromaTimeFrame);

			TimeSet timeSet = chromaTimeFrame.getTimeSet();
			PitchSet pitchSet = chromaTimeFrame.getPitchSet();
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = chromaTimeFrame.getElements();
			if (baseChannelLastNotes == null) {
				baseChannelLastNotes = new HashSet<>();
			}

			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				double amplitude = toneMapElement.amplitude;

				int volume = 0;
				if (amplitude > 1.0) {
					volume = 127;
				} else if (amplitude <= 0.1) {
					volume = 0;
				} else {
					volume = (int) (((amplitude - 0.1) / (1.0 - 0.1)) * 127);
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

			if (!silentWrite && midiSynthesizer != null && midiSynthesizer.isOpen()) {
				for (ShortMessage mm : midiMessages) {
					try {
						midiSynthesizer.getReceiver().send(mm, -1);
					} catch (MidiUnavailableException e) {
						LOG.log(Level.SEVERE, "Send MIDI Base Channel error ", e);
						throw new InstrumentException("Send MIDI Base Channel error: " + e.getMessage(), e);
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
				clearChannel(channels[BEATS_CHANNEL]);
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
			padsChannel1LastNoteTimes = new HashMap<>();
			padsChannel2LastNoteTimes = new HashMap<>();
			beatsChannel1LastNotes = new HashSet<>();
			beatsChannel2LastNotes = new HashSet<>();
			beatsChannel3LastNotes = new HashSet<>();
			beatsChannel4LastNotes = new HashSet<>();
			baseChannelLastNotes = new HashSet<>();

		}

		private void clearChannel(ChannelData channelData) {
			if (channelData.channel != null) {
				channelData.channel.allNotesOff();
				channelData.channel.allSoundOff();
				channelData.channel.resetAllControllers();
			}
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

		boolean paused = false;

		private MidiQueueConsumer consumer;

		public MidiStream(String streamId) {
			this.streamId = streamId;
			bq = new LinkedBlockingQueue<>();
			consumer = new MidiQueueConsumer(bq, this);
			// TODO LOOM Thread.startVirtualThread(consumer);
			LOG.severe(">>Start MidiQueueConsumer Stream: " + streamId);
			new Thread(consumer).start();
		}

		public void setPaused(boolean paused) {
			this.paused = paused;
		}

		public boolean isPaused() {
			return paused;
		}

		public void close() {
			closed = true;
			bq.clear();
			bq.drainTo(new ArrayList<Object>());
			consumer.stop();
			LOG.finer(">>MidiStream close stop and reset!!");
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
		int drum;

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

			if (message.getType() == 47 && playState != PAUSED) {
				if (playState != STOPPED) {
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
			this.chanNum = Integer.valueOf(chanNum);
			this.name = name;
			this.track = track;
		}
	}

	class MidiPitchBend {

		protected int mValue1;
		protected int mValue2;
		protected double amount;

		public int getLeastSignificantBits() {
			return mValue1;
		}

		public int getMostSignificantBits() {
			return mValue2;
		}

		public int getBendValue() {
			int y = (mValue2 & 0x7F) << 7;
			int x = (mValue1);

			return y + x;
		}

		public double getAmount() {
			return amount;
		}

		public void setLeastSignificantBits(int p) {
			mValue1 = p & 0x7F;
		}

		public void setMostSignificantBits(int p) {
			mValue2 = p & 0x7F;
		}

		public void setBendAmount(double amount) {
			this.amount = amount;
			if (amount > 1 || amount < -1) {
				LOG.finer(">>MidiPitchBend invalid amount: " + amount);
				// new InstrumentException(">>MidiPitchBend invalid amount: " + amount);
			}
			double value = amount + 1;
			value = (16383 * value) / 2;
			value = (int) value & 0x3FFF;
			mValue1 = ((int) value & 0x7F);
			mValue2 = (int) value >> 7;
		}

		public void setBendAmountHighd(double amount) {
			this.amount = amount;
			if (amount > 1 || amount < -1) {
				LOG.finer(">>MidiPitchBend invalid amount: " + amount);
				// new InstrumentException(">>MidiPitchBend invalid amount: " + amount);
			}
			double value = (amount + 1) / 2;
			value = (126 * value);
			LOG.finer(">>MidiPitchBend set amount high: " + value + ", " + amount);
			mValue1 = 0;
			mValue2 = (int) value;
		}

	}

}
