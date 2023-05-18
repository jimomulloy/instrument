package jomu.instrument.actuation;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import io.vertx.core.impl.ConcurrentHashSet;
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
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

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

	Set<String> deadStreams = new ConcurrentHashSet<>();

	public AudioSynthesizer buildAudioSynthesizer() {
		audioSynthesizer = new TarsosAudioSynthesizer(parameterManager);
		return this.audioSynthesizer;
	}

	public AudioSynthesizer buildResynthAudioSynthesizer() {
		resynthSynthesizer = new ResynthAudioSynthesizer(parameterManager);
		return this.resynthSynthesizer;
	}

	public MidiSynthesizer buildMidiSynthesizer() {
		LOG.severe(">>Voice buildMidiSynthesizer");
		if (!midiSynthesizer.open()) {
			LOG.severe(">>Voice Open MidiSynthesizer error");
			throw new InstrumentException(">>Voice Open MidiSynthesizer error");
		}
		return this.midiSynthesizer;
	}

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

	private void waitForPlayers() {
		LOG.severe(">>Voice wait for players");
	}

	public MidiSynthesizer getAudioSequencer() {
		return this.midiSynthesizer;
	}

	public AudioSynthesizer getAudioSynthesizer() {
		return audioSynthesizer;
	}

	public AudioSynthesizer getResynthAudioSynthesizer() {
		return resynthSynthesizer;
	}

	@Override
	public void initialise() {
		LOG.severe(">>Voice initialise");
		midiSynthesizer = buildMidiSynthesizer();
		audioSynthesizer = buildAudioSynthesizer();
		resynthSynthesizer = buildResynthAudioSynthesizer();
	}

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

	public void sendMessage(SendMessage message) {
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

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	public void writeAudio(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		audioSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);

	}

	public void writeResynthAudio(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		resynthSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);

	}

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

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	public void clear(String streamId) {
		LOG.severe(">>VOICE clear: ");
		deadStreams.add(streamId);
		resynthSynthesizer.clear(streamId);
		audioSynthesizer.clear(streamId);
		midiSynthesizer.clear(streamId);
	}

	public void reset() {
		LOG.severe(">>VOICE clear: ");
		midiSynthesizer.reset();
	}

	class SendMessage {

		public ToneTimeFrame toneTimeFrame;
		public String streamId;
		public int sequence;

		public SendMessage(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
			this.toneTimeFrame = toneTimeFrame;
			this.streamId = streamId;
			this.sequence = sequence;
		}
	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		// TODO Auto-generated method stub

	}
}
