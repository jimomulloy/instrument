package jomu.instrument.actuation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;
import jomu.instrument.audio.AudioSynthesizer;
import jomu.instrument.audio.MidiSynthesizer;
import jomu.instrument.audio.ResynthAudioSynthesizer;
import jomu.instrument.audio.TarsosAudioSynthesizer;
import jomu.instrument.control.Controller;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

/**
 * The Class Voice.
 *
 * @author jim
 */
@ApplicationScoped
public class Voice implements Organ {

	/**
	 * The Class SendMessage.
	 */
	class SendMessage {

		/** The sequence. */
		public int sequence;

		/** The stream id. */
		public String streamId;

		/** The tone time frame. */
		public ToneTimeFrame toneTimeFrame;

		/**
		 * Instantiates a new send message.
		 *
		 * @param toneTimeFrame
		 *            the tone time frame
		 * @param streamId
		 *            the stream id
		 * @param sequence
		 *            the sequence
		 */
		public SendMessage(final ToneTimeFrame toneTimeFrame, final String streamId, final int sequence) {
			this.toneTimeFrame = toneTimeFrame;
			this.streamId = streamId;
			this.sequence = sequence;
		}
	}

	private static final Logger LOG = Logger.getLogger(Voice.class.getName());

	AudioSynthesizer audioSynthesizer;

	@Inject
	Controller controller;

	/** The dead streams. */
	Set<String> deadStreams = ConcurrentHashMap.newKeySet();

	@Inject
	MidiSynthesizer midiSynthesizer;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Console console;

	AudioSynthesizer resynthSynthesizer;

	ConcurrentLinkedQueue<SendMessage> smq = new ConcurrentLinkedQueue<>();

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

	private String streamPlayerId;

	private String currentStreamId;

	/**
	 * Builds the audio synthesizer.
	 *
	 * @return the audio synthesizer
	 */
	public AudioSynthesizer buildAudioSynthesizer() {
		this.audioSynthesizer = new TarsosAudioSynthesizer(this.parameterManager);
		return this.audioSynthesizer;
	}

	/**
	 * Builds the midi synthesizer.
	 *
	 * @return the midi synthesizer
	 */
	public MidiSynthesizer buildMidiSynthesizer() {
		LOG.severe(">>Voice buildMidiSynthesizer");
		if (!this.midiSynthesizer.open()) {
			LOG.severe(">>Voice Open MidiSynthesizer error");
			throw new InstrumentException(">>Voice Open MidiSynthesizer error");
		}
		return this.midiSynthesizer;
	}

	/**
	 * Builds the resynth audio synthesizer.
	 *
	 * @return the audio synthesizer
	 */
	public AudioSynthesizer buildResynthAudioSynthesizer() {
		this.resynthSynthesizer = new ResynthAudioSynthesizer(this.parameterManager);
		return this.resynthSynthesizer;
	}

	/**
	 * Clear.
	 *
	 * @param streamId
	 *            the stream id
	 */
	public void clear(final String streamId) {
		LOG.severe(">>VOICE clear: ");
		// ?? deadStreams.add(streamId);
		this.resynthSynthesizer.clear(streamId);
		this.audioSynthesizer.clear(streamId);
		this.midiSynthesizer.clear(streamId);
	}

	/**
	 * Close.
	 *
	 * @param streamId
	 *            the stream id
	 */
	public void close(final String streamId) {

		if (this.currentStreamId != null && this.currentStreamId == streamId) {
			System.out.println(">>CLOSE: " + streamId);
			if (!this.smq.isEmpty()) {
				for (final SendMessage sm : this.smq) {
					sendMessage(sm);
				}
				this.smq.clear();
			}
			this.deadStreams.remove(streamId);

			this.resynthSynthesizer.close(streamId);
			this.audioSynthesizer.close(streamId);
			LOG.severe(">>Voice CLOSE, midi running: " + this.midiSynthesizer.isSynthesizerRunning());
			this.midiSynthesizer.close(streamId);
			waitForPlayers();
			this.midiSynthesizer.reset();
			this.console.getVisor().setPlayerState(true);
			this.LOG.severe(">>Voice CLOSED, midi running: " + this.midiSynthesizer.isSynthesizerRunning() + ", "
					+ ", Frame Cache Size: " + this.workspace.getAtlas()
							.getFrameCache()
							.getSize());
			if (this.controller.isCountDownLatch()) {
				LOG.severe(">>Voice CLOSE JOB");
				this.controller.getCountDownLatch()
						.countDown();
			}
		}
	}

	/**
	 * Gets the audio sequencer.
	 *
	 * @return the audio sequencer
	 */
	public MidiSynthesizer getAudioSequencer() {
		return this.midiSynthesizer;
	}

	/**
	 * Gets the audio synthesizer.
	 *
	 * @return the audio synthesizer
	 */
	public AudioSynthesizer getAudioSynthesizer() {
		return this.audioSynthesizer;
	}

	/**
	 * Gets the resynth audio synthesizer.
	 *
	 * @return the resynth audio synthesizer
	 */
	public AudioSynthesizer getResynthAudioSynthesizer() {
		return this.resynthSynthesizer;
	}

	/**
	 * Initialise.
	 */
	@Override
	public void initialise() {
		LOG.severe(">>Voice initialise");
		this.midiSynthesizer = buildMidiSynthesizer();
		this.audioSynthesizer = buildAudioSynthesizer();
		this.resynthSynthesizer = buildResynthAudioSynthesizer();
	}

	/**
	 * Process exception.
	 *
	 * @param exception
	 *            the exception
	 * @throws InstrumentException
	 *             the instrument exception
	 */
	@Override
	public void processException(final InstrumentException exception) throws InstrumentException {
		// TODO Auto-generated method stub

	}

	/**
	 * Reset.
	 */
	public void reset() {
		LOG.severe(">>VOICE reset: ");
		// this.midiSynthesizer.reset(); // TODO ??
	}

	/**
	 * Send.
	 *
	 * @param toneTimeFrame
	 *            the tone time frame
	 * @param streamId
	 *            the stream id
	 * @param sequence
	 *            the sequence
	 * @param pause
	 *            the pause
	 */
	public void send(final ToneTimeFrame toneTimeFrame, final String streamId, final int sequence,
			final boolean pause) {

		if (this.currentStreamId != null
				&& this.currentStreamId != streamId
				&& this.currentStreamId == this.streamPlayerId) {
			System.out.println(">>D - STOP STREAM PLAY 1: " + streamId + ", " + this.streamPlayerId);
			stopStreamPlayer();
		}
		if (sequence == 10) {
			System.out.println(">>SEND: " + streamId);
		}
		this.currentStreamId = streamId;

		if (this.deadStreams.contains(streamId))
			return;
		if (pause) {
			this.smq.add(new SendMessage(toneTimeFrame, streamId, sequence));
		} else {
			if (sequence >= this.parameterManager
					.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_START_OFFSET)
					&& sequence <= this.parameterManager
							.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_END_OFFSET)) {
				if (this.parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY)) {
					writeMidi(toneTimeFrame, streamId, sequence);
				}
				if (this.parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_AUDIO_PLAY)) {
					writeAudio(toneTimeFrame, streamId, sequence);
				}
				if (this.parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_RESYNTH_PLAY)) {
					writeResynthAudio(toneTimeFrame, streamId, sequence);
				}
			}
		}
	}

	/**
	 * Send message.
	 *
	 * @param message
	 *            the message
	 */
	private void sendMessage(final SendMessage message) {
		if (this.deadStreams.contains(message.streamId))
			return;
		if (message.sequence >= this.parameterManager
				.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_START_OFFSET)
				&& message.sequence <= this.parameterManager
						.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY_END_OFFSET)) {

			if (this.parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY)) {
				writeMidi(message.toneTimeFrame, message.streamId, message.sequence);
			}
			if (this.parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_AUDIO_PLAY)) {
				writeAudio(message.toneTimeFrame, message.streamId, message.sequence);
			}
			if (this.parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_RESYNTH_PLAY)) {
				writeResynthAudio(message.toneTimeFrame, message.streamId, message.sequence);
			}
		}
	}

	public void setAudioSynthesizer(final AudioSynthesizer audioSynthesizer) {
		this.audioSynthesizer = audioSynthesizer;
	}

	public void setMidiSynthesizer(final MidiSynthesizer midiSynthesizer) {
		this.midiSynthesizer = midiSynthesizer;
	}

	public void setResynthSynthesizer(final AudioSynthesizer resynthSynthesizer) {
		this.resynthSynthesizer = resynthSynthesizer;
	}

	/**
	 * Start.
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	/**
	 * Start stream player.
	 *
	 * @param streamId
	 *            the stream id
	 * @return true, if successful
	 */
	public boolean startStreamPlayer(final String streamId, ToneMap synthToneMap) {
		if (this.streamPlayerId != null && this.streamPlayerId != streamId) {
			System.out.println(">>A - STOP STREAM PLAY 1: " + streamId);
			stopStreamPlayer();
		}
		this.streamPlayerId = streamId;
		System.out.println(">>START STREAM PLAY: " + streamId);
		do {
			int sequence = 1;
			ToneTimeFrame frame = synthToneMap.getTimeFrame(sequence);
			while (frame != null && streamId == this.streamPlayerId) {
				send(frame, streamId, sequence, false);
				sequence++;
				frame = synthToneMap.getTimeFrame(sequence);
			}
			System.out.println(">>START STREAM PLAY CLOSING: " + streamId);
			if (streamId == this.streamPlayerId) {
				System.out.println(">>C - STOP STREAM PLAY 1: " + streamId);
				close(streamId);
			}
		} while (streamId == this.streamPlayerId
				&& parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_LOOP_SAVE));
		this.streamPlayerId = null;
		return true;
	}

	/**
	 * Stop.
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	/**
	 * Stop stream player.
	 */
	public void stopStreamPlayer() {
		System.out.println(">>STOP STREAM PLAY");
		String streamId = this.streamPlayerId;
		this.streamPlayerId = null;
		if (streamId != null) {
			System.out.println(">>B CLOSE STREAM PLAY: " + streamId);
			close(streamId);
		}
	}

	/**
	 * Wait for players.
	 */
	private void waitForPlayers() {
		int counter = 600;
		while (counter > 0 && this.midiSynthesizer.isSynthesizerRunning()) {
			try {
				counter--;
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Write audio.
	 *
	 * @param toneTimeFrame
	 *            the tone time frame
	 * @param streamId
	 *            the stream id
	 * @param sequence
	 *            the sequence
	 */
	private void writeAudio(final ToneTimeFrame toneTimeFrame, final String streamId, final int sequence) {
		this.audioSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);

	}

	/**
	 * Write midi.
	 *
	 * @param toneTimeFrame
	 *            the tone time frame
	 * @param streamId
	 *            the stream id
	 * @param sequence
	 *            the sequence
	 */
	public void writeMidi(final ToneTimeFrame toneTimeFrame, final String streamId, final int sequence) {
		try {
			this.midiSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);
		} catch (final InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Write resynth audio.
	 *
	 * @param toneTimeFrame
	 *            the tone time frame
	 * @param streamId
	 *            the stream id
	 * @param sequence
	 *            the sequence
	 */
	private void writeResynthAudio(final ToneTimeFrame toneTimeFrame, final String streamId, final int sequence) {
		this.resynthSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);

	}
}
