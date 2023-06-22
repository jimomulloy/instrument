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
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.Controller;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
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

	private static final Logger LOG = Logger.getLogger(Voice.class.getName());

	AudioSynthesizer resynthSynthesizer;

	AudioSynthesizer audioSynthesizer;

	@Inject
	MidiSynthesizer midiSynthesizer;

	@Inject
	ParameterManager parameterManager;

	@Inject
	Controller controller;

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

	ConcurrentLinkedQueue<SendMessage> smq = new ConcurrentLinkedQueue<>();

	/** The dead streams. */
	Set<String> deadStreams = ConcurrentHashMap.newKeySet();

	/**
	 * Builds the audio synthesizer.
	 *
	 * @return the audio synthesizer
	 */
	public AudioSynthesizer buildAudioSynthesizer() {
		audioSynthesizer = new TarsosAudioSynthesizer(parameterManager);
		return this.audioSynthesizer;
	}

	/**
	 * Builds the resynth audio synthesizer.
	 *
	 * @return the audio synthesizer
	 */
	public AudioSynthesizer buildResynthAudioSynthesizer() {
		resynthSynthesizer = new ResynthAudioSynthesizer(parameterManager);
		return this.resynthSynthesizer;
	}

	public void setResynthSynthesizer(AudioSynthesizer resynthSynthesizer) {
		this.resynthSynthesizer = resynthSynthesizer;
	}

	public void setAudioSynthesizer(AudioSynthesizer audioSynthesizer) {
		this.audioSynthesizer = audioSynthesizer;
	}

	public void setMidiSynthesizer(MidiSynthesizer midiSynthesizer) {
		this.midiSynthesizer = midiSynthesizer;
	}

	/**
	 * Builds the midi synthesizer.
	 *
	 * @return the midi synthesizer
	 */
	public MidiSynthesizer buildMidiSynthesizer() {
		LOG.severe(">>Voice buildMidiSynthesizer");
		if (!midiSynthesizer.open()) {
			LOG.severe(">>Voice Open MidiSynthesizer error");
			throw new InstrumentException(">>Voice Open MidiSynthesizer error");
		}
		return this.midiSynthesizer;
	}

	/**
	 * Close.
	 *
	 * @param streamId the stream id
	 */
	public void close(String streamId) {
		if (!smq.isEmpty()) {
			for (SendMessage sm : smq) {
				sendMessage(sm);
			}
			smq.clear();
		}
		deadStreams.remove(streamId);

		waitForPlayers();

		resynthSynthesizer.close(streamId);
		audioSynthesizer.close(streamId);
		LOG.severe(">>Voice CLOSE, midi running: " + midiSynthesizer.isSynthesizerRunning());
		midiSynthesizer.close(streamId);
		int counter = 60;
		while (counter > 0 && midiSynthesizer.isSynthesizerRunning()) {
			try {
				counter--;
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		midiSynthesizer.reset();
		LOG.severe(">>Voice CLOSED, midi running: " + midiSynthesizer.isSynthesizerRunning() + ", "
				+ ", Frame Cache Size: " + workspace.getAtlas().getFrameCache().getSize());
		if (controller.isCountDownLatch()) {
			LOG.severe(">>Voice CLOSE JOB");
			controller.getCountDownLatch().countDown();
		}
	}

	/**
	 * Wait for players.
	 */
	private void waitForPlayers() {
		LOG.severe(">>Voice wait for players");
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
		return audioSynthesizer;
	}

	/**
	 * Gets the resynth audio synthesizer.
	 *
	 * @return the resynth audio synthesizer
	 */
	public AudioSynthesizer getResynthAudioSynthesizer() {
		return resynthSynthesizer;
	}

	/**
	 * Initialise.
	 */
	@Override
	public void initialise() {
		LOG.severe(">>Voice initialise");
		midiSynthesizer = buildMidiSynthesizer();
		audioSynthesizer = buildAudioSynthesizer();
		resynthSynthesizer = buildResynthAudioSynthesizer();
	}

	/**
	 * Send.
	 *
	 * @param toneTimeFrame the tone time frame
	 * @param streamId      the stream id
	 * @param sequence      the sequence
	 * @param pause         the pause
	 */
	public void send(ToneTimeFrame toneTimeFrame, String streamId, int sequence, boolean pause) {
		if (deadStreams.contains(streamId)) {
			return;
		}
		if (pause) {
			smq.add(new SendMessage(toneTimeFrame, streamId, sequence));
		} else {
			if (parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY)) {
				writeMidi(toneTimeFrame, streamId, sequence);
			}
			if (parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_AUDIO_PLAY)) {
				writeAudio(toneTimeFrame, streamId, sequence);
			}
			if (parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_RESYNTH_PLAY)) {
				writeResynthAudio(toneTimeFrame, streamId, sequence);
			}
		}
	}

	/**
	 * Send message.
	 *
	 * @param message the message
	 */
	private void sendMessage(SendMessage message) {
		if (deadStreams.contains(message.streamId)) {
			return;
		}
		if (parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY)) {
			writeMidi(message.toneTimeFrame, message.streamId, message.sequence);
		}
		if (parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_AUDIO_PLAY)) {
			writeAudio(message.toneTimeFrame, message.streamId, message.sequence);
		}
		if (parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_RESYNTH_PLAY)) {
			writeResynthAudio(message.toneTimeFrame, message.streamId, message.sequence);
		}
	}

	/**
	 * Start.
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	/**
	 * Write audio.
	 *
	 * @param toneTimeFrame the tone time frame
	 * @param streamId      the stream id
	 * @param sequence      the sequence
	 */
	private void writeAudio(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		audioSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);

	}

	/**
	 * Write resynth audio.
	 *
	 * @param toneTimeFrame the tone time frame
	 * @param streamId      the stream id
	 * @param sequence      the sequence
	 */
	private void writeResynthAudio(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		resynthSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);

	}

	/**
	 * Write midi.
	 *
	 * @param toneTimeFrame the tone time frame
	 * @param streamId      the stream id
	 * @param sequence      the sequence
	 */
	public void writeMidi(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		try {
			midiSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Stop.
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	/**
	 * Clear.
	 *
	 * @param streamId the stream id
	 */
	public void clear(String streamId) {
		LOG.severe(">>VOICE clear: ");
		deadStreams.add(streamId);
		resynthSynthesizer.clear(streamId);
		audioSynthesizer.clear(streamId);
		midiSynthesizer.clear(streamId);
	}

	/**
	 * Reset.
	 */
	public void reset() {
		LOG.severe(">>VOICE reset: ");
		midiSynthesizer.reset();
	}

	/**
	 * The Class SendMessage.
	 */
	class SendMessage {

		/** The tone time frame. */
		public ToneTimeFrame toneTimeFrame;

		/** The stream id. */
		public String streamId;

		/** The sequence. */
		public int sequence;

		/**
		 * Instantiates a new send message.
		 *
		 * @param toneTimeFrame the tone time frame
		 * @param streamId      the stream id
		 * @param sequence      the sequence
		 */
		public SendMessage(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
			this.toneTimeFrame = toneTimeFrame;
			this.streamId = streamId;
			this.sequence = sequence;
		}
	}

	/**
	 * Process exception.
	 *
	 * @param exception the exception
	 * @throws InstrumentException the instrument exception
	 */
	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		// TODO Auto-generated method stub

	}

	/**
	 * Start stream player.
	 *
	 * @param streamId the stream id
	 * @return true, if successful
	 */
	public boolean startStreamPlayer(String streamId) {
		ToneMap synthToneMap = workspace.getAtlas()
				.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, streamId));
		LOG.severe(">>Play Stream from tm: " + synthToneMap);
		if (synthToneMap == null) {
			return false;
		}
		int sequence = 1;
		ToneTimeFrame frame = synthToneMap.getTimeFrame(sequence);
		while (frame != null) {
			send(frame, streamId, sequence, false);
			sequence++;
			frame = synthToneMap.getTimeFrame(sequence);
		}
		return true;
	}

	/**
	 * Stop stream player.
	 */
	public void stopStreamPlayer() {
		// TODO Auto-generated method stub

	}
}
