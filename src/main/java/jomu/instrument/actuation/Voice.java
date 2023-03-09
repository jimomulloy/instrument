package jomu.instrument.actuation;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.springframework.stereotype.Component;

import jomu.instrument.Instrument;
import jomu.instrument.Organ;
import jomu.instrument.audio.AudioSynthesizer;
import jomu.instrument.audio.MidiSynthesizer;
import jomu.instrument.audio.ResynthAudioSynthesizer;
import jomu.instrument.audio.TarsosAudioSynthesizer;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

@ApplicationScoped
@Component
public class Voice implements Organ {

	private static final Logger LOG = Logger.getLogger(Voice.class.getName());

	AudioSynthesizer resynthSynthesizer;
	AudioSynthesizer audioSynthesizer;
	MidiSynthesizer midiSynthesizer;
	ParameterManager parameterManager;
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
		midiSynthesizer = new MidiSynthesizer(workspace, parameterManager);
		midiSynthesizer.open();
		return this.midiSynthesizer;
	}

	public void close(String streamId) {
		resynthSynthesizer.close(streamId);
		audioSynthesizer.close(streamId);
		midiSynthesizer.close(streamId);
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
		this.workspace = Instrument.getInstance().getWorkspace();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
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
		resynthSynthesizer.clear();
		audioSynthesizer.clear();
		midiSynthesizer.clear();
	}
}
