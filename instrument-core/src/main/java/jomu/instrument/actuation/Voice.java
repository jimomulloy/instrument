package jomu.instrument.actuation;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

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

	public AudioSynthesizer buildAudioSynthesizer() {
		audioSynthesizer = new TarsosAudioSynthesizer(parameterManager);
		return this.audioSynthesizer;
	}

	public AudioSynthesizer buildResynthAudioSynthesizer() {
		resynthSynthesizer = new ResynthAudioSynthesizer(parameterManager);
		return this.resynthSynthesizer;
	}

	public MidiSynthesizer buildMidiSynthesizer() {
		// midiSynthesizer = new MidiSynthesizer(workspace, parameterManager,
		// controller);
		LOG.finer(">>Voice buildMidiSynthesizer");
		if (!midiSynthesizer.open()) {
			LOG.severe(">>Voice Open MidiSynthesizer error");
			throw new InstrumentException(">>Voice Open MidiSynthesizer error");
		}
		return this.midiSynthesizer;
	}

	public void close(String streamId) {
		resynthSynthesizer.close(streamId);
		audioSynthesizer.close(streamId);
		LOG.finer(">>Voice CLOSE!!");
		midiSynthesizer.close(streamId);
		if (!parameterManager.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY)) {
			if (controller.isCountDownLatch()) {
				LOG.finer(">>Voice job close");
				controller.getCountDownLatch().countDown();
			}
		}
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
		LOG.finer(">>Voice initialise");
		midiSynthesizer = buildMidiSynthesizer();
		audioSynthesizer = buildAudioSynthesizer();
		resynthSynthesizer = buildResynthAudioSynthesizer();
	}

	public void send(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
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

	public void clear() {
		LOG.severe(">>VOICE clear: ");
		resynthSynthesizer.clear();
		audioSynthesizer.clear();
		midiSynthesizer.clear();
	}
}
