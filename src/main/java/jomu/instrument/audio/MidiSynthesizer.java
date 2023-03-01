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
import java.util.logging.Logger;

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

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.Workspace;
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

	private static final Logger LOG = Logger.getLogger(MidiSynthesizer.class.getName());

	private static final int VOICE_1_CHANNEL = 0;

	private static final int VOICE_2_CHANNEL = 1;

	private static final int VOICE_3_CHANNEL = 2;

	private static final int VOICE_4_CHANNEL = 3;

	private static final int CHORD_1_CHANNEL = 4;

	private static final int CHORD_2_CHANNEL = 5;

	private static final int PAD_1_CHANNEL = 6;

	private static final int PAD_2_CHANNEL = 7;

	private static final int BEAT_1_CHANNEL = 8;

	private static final int BEAT_2_CHANNEL = 9;

	private static final int BEAT_3_CHANNEL = 10;

	private static final int BEAT_4_CHANNEL = 11;

	private static final int BASE_1_CHANNEL = 12;

	private int bpmSetting = INIT_BPM_SETTING;

	private ChannelData channels[];

	private Instrument instruments[];

	private boolean levelSwitch = false;

	private int panSetting = INIT_PAN_SETTING;
	private int pitchHigh = INIT_PITCH_HIGH;

	private int pitchLow = INIT_PITCH_LOW;

	private double quantizeBeatSetting = 0;

	private double quantizeDurationSetting = 0;

	private double timeEnd = INIT_TIME_END;
	private double timeStart = INIT_TIME_START;

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

	private Workspace workspace;

	/**
	 * MidiModel constructor. Test Java Sound MIDI System available Instantiate
	 * MidiPanel
	 */
	public MidiSynthesizer(Workspace workspace, ParameterManager parameterManager) {
		this.workspace = workspace;
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
	public boolean createEvent(ChannelData cc, int type, int num, long tick, int velocity) {
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

			initChannels();

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

	private void initChannel(ChannelData channelData, String instrumentName) {

		Instrument channelInstrument = instruments[0];
		for (Instrument instrument : instruments) {
			if (instrument.getName().toLowerCase().contains(instrumentName.toLowerCase())) {
				channelInstrument = instrument;
				break;
			}
		}

		synthesizer.loadInstrument(channelInstrument);

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

		MidiQueueMessage midiQueueMessage = new MidiQueueMessage(toneTimeFrame, sequence);
		midiStream.bq.add(midiQueueMessage);
		return;
	}

	public void programChange(ChannelData channelData, int program) {
		channelData.program = program;
		if (instruments != null) {
			synthesizer.loadInstrument(instruments[program]);
		}
		channelData.channel.programChange(program);

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

	private class MidiQueueConsumer implements Runnable {

		private BlockingQueue<MidiQueueMessage> bq;
		private MidiStream midiStream;
		double sampleTime = 0;
		public Set<Integer> voiceChannelLastNotes;
		public Set<Integer> chordChannelLastNotes;
		public Set<Integer> padChannelLastNotes;
		public Set<Integer> beatChannelLastNotes;
		public Set<Integer> baseChannelLastNotes;

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
						clearChannels();
						this.midiStream.close();
						System.out.println(">> midiStream.close(): " + this.midiStream.streamId);
						running = false;
						break;
					}

					if (sampleTime != 0) {
						TimeUnit.MILLISECONDS.sleep((long) (sampleTime * 1000));
					}

					TimeSet timeSet = toneTimeFrame.getTimeSet();

					playChannelVoices(mqm);

					playChannelChords(mqm);

					playChannelPads(mqm);

					playChannelBeats(mqm);

					playChannelBase(mqm);

					sampleTime = timeSet.getSampleTimeSize();

				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		private void playChannelVoices(MidiQueueMessage mqm) {

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);

			ToneMap notateToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));

			ToneTimeFrame toneTimeFrame = notateToneMap.getTimeFrame(mqm.sequence);

			ChannelData voice1Channel = channels[VOICE_1_CHANNEL];

			TimeSet timeSet = toneTimeFrame.getTimeSet();
			PitchSet pitchSet = toneTimeFrame.getPitchSet();
			NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = toneTimeFrame.getElements();
			if (voiceChannelLastNotes == null) {
				voiceChannelLastNotes = new HashSet<>();
			}
			double maxAmp = -1;
			for (ToneMapElement toneMapElement : ttfElements) {
				double amp = toneMapElement.amplitude;
				if (maxAmp < amp) {
					maxAmp = amp;
				}
			}

			// LOG.info(">>IN MIDI VOICES MAX: " + maxAmp);

			for (ToneMapElement toneMapElement : ttfElements) {
				int note = pitchSet.getNote(toneMapElement.getPitchIndex());
				noteStatusElement = noteStatus.getNoteStatusElement(note);
				double amplitude = toneMapElement.amplitude;

				int volume = 0;
				int nonlogVolume = 0;
				if (amplitude > highVoiceThreshold) {
					volume = 120;
				} else if (amplitude <= lowVoiceThreshold) {
					volume = 0;
				} else {
					// volume = (int) (Math
					// .log1p((amplitude - lowVoiceThreshold) / (highVoiceThreshold -
					// lowVoiceThreshold))
					// / Math.log1p(1.0000001) * 120);
					// volume = Math.max(0, volume);
					volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 120);
				}

				switch (noteStatusElement.state) {
				case ON:
				case PENDING:
				case CONTINUING:
					if (!voiceChannelLastNotes.contains(note)) {
						System.out.println(">>VOICE NOTE ON: " + note + ", " + amplitude + ", " + volume);
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, voice1Channel.num, note, volume);
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannelLastNotes.add(note);
					}
					break;
				case OFF:
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, voice1Channel.num, note, 0);
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					voiceChannelLastNotes.remove(note);
					break;
				default:
					voiceChannelLastNotes.remove(note);
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

		}

		private void playChannelChords(MidiQueueMessage mqm) {

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);

			ToneMap notesToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));
			ToneMap chromaToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame notesTimeFrame = notesToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame chromaTimeFrame = chromaToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			ChannelData chord1Channel = channels[CHORD_1_CHANNEL];

			TimeSet timeSet = chromaTimeFrame.getTimeSet();
			PitchSet pitchSet = chromaTimeFrame.getPitchSet();
			NoteStatus noteStatus = chromaTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = chromaTimeFrame.getElements();
			if (chordChannelLastNotes == null) {
				chordChannelLastNotes = new HashSet<>();
			}
			double maxAmp = -1;
			for (ToneMapElement toneMapElement : ttfElements) {
				double amp = toneMapElement.amplitude;
				if (maxAmp < amp) {
					maxAmp = amp;
				}
			}

			// LOG.info(">>IN MIDI CHORD MAX: " + maxAmp);

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
					if (!chordChannelLastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, chord1Channel.num, (note + 12 * octaveAdjust),
									volume);
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						chordChannelLastNotes.add(note);
					}
				} else {
					if (chordChannelLastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, chord1Channel.num, (note + 12 * octaveAdjust),
									0);
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						chordChannelLastNotes.remove(note);
					}
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

		}

		private void playChannelPads(MidiQueueMessage mqm) {

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);

			ToneMap notesToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));
			ToneMap chromaToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame notesTimeFrame = notesToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame chromaTimeFrame = chromaToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			ChannelData pad1Channel = channels[PAD_1_CHANNEL];

			TimeSet timeSet = chromaTimeFrame.getTimeSet();
			PitchSet pitchSet = chromaTimeFrame.getPitchSet();
			NoteStatus noteStatus = chromaTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = chromaTimeFrame.getElements();
			if (padChannelLastNotes == null) {
				padChannelLastNotes = new HashSet<>();
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
					if (!padChannelLastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, pad1Channel.num, (note + 12 * octaveAdjust),
									volume);
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						padChannelLastNotes.add(note);
					}
				} else {
					if (padChannelLastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_OFF, pad1Channel.num, (note + 12 * octaveAdjust),
									0);
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						padChannelLastNotes.remove(note);
					}
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

		}

		private void playChannelBeats(MidiQueueMessage mqm) {

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);

			ToneMap notesToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));
			ToneMap chromaToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, midiStream.getStreamId()));
			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame notesTimeFrame = notesToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame chromaTimeFrame = chromaToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			ChannelData beat1Channel = channels[BEAT_1_CHANNEL];

			TimeSet timeSet = chromaTimeFrame.getTimeSet();
			PitchSet pitchSet = chromaTimeFrame.getPitchSet();
			NoteStatus noteStatus = chromaTimeFrame.getNoteStatus();
			NoteStatusElement noteStatusElement = null;
			ShortMessage midiMessage = null;

			List<ShortMessage> midiMessages = new ArrayList<>();
			ToneMapElement[] ttfElements = chromaTimeFrame.getElements();
			if (voiceChannelLastNotes == null) {
				voiceChannelLastNotes = new HashSet<>();
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
					volume = (int) (((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold)) * 120);
				}

				switch (noteStatusElement.state) {
				case ON:
				case PENDING:
				case CONTINUING:
					if (!voiceChannelLastNotes.contains(note)) {
						midiMessage = new ShortMessage();
						try {
							midiMessage.setMessage(ShortMessage.NOTE_ON, beat1Channel.num, note, volume);
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						voiceChannelLastNotes.add(note);
					}
					break;
				case OFF:
					midiMessage = new ShortMessage();
					try {
						midiMessage.setMessage(ShortMessage.NOTE_OFF, beat1Channel.num, note, 0);
					} catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					midiMessages.add(midiMessage);
					voiceChannelLastNotes.remove(note);
					break;
				default:
					voiceChannelLastNotes.remove(note);
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

		}

		private void playChannelBase(MidiQueueMessage mqm) {

			double lowVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
			double highVoiceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);

			ToneMap notesToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_NOTATE, midiStream.getStreamId()));
			ToneMap chromaToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, midiStream.getStreamId()));
			ToneMap beatToneMap = workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_BEAT, midiStream.getStreamId()));

			ToneTimeFrame notesTimeFrame = notesToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame chromaTimeFrame = chromaToneMap.getTimeFrame(mqm.sequence);
			ToneTimeFrame beatTimeFrame = beatToneMap.getTimeFrame(mqm.sequence);

			ChannelData base1Channel = channels[BASE_1_CHANNEL];

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
						} catch (InvalidMidiDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						midiMessages.add(midiMessage);
						baseChannelLastNotes.remove(note);
					}
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

		}

		private void clearChannels() {
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
			if (voiceChannelLastNotes != null) {
				voiceChannelLastNotes.clear();
			}
			if (chordChannelLastNotes != null) {
				chordChannelLastNotes.clear();
			}
			if (padChannelLastNotes != null) {
				padChannelLastNotes.clear();
			}
			if (beatChannelLastNotes != null) {
				beatChannelLastNotes.clear();
			}
			if (baseChannelLastNotes != null) {
				baseChannelLastNotes.clear();
			}
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

		private DelayQueue<MidiQueueMessage> bq;

		private String streamId;

		public MidiStream(String streamId) {
			this.streamId = streamId;
			bq = new DelayQueue<>(); // LinkedBlockingQueue<>();
			Thread.startVirtualThread(new MidiQueueConsumer(bq, this));
		}

		public void close() {
			bq.clear();

		}

		public DelayQueue<MidiQueueMessage> getBq() {
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