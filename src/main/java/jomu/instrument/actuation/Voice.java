package jomu.instrument.actuation;

import javax.enterprise.context.ApplicationScoped;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.springframework.stereotype.Component;

import jomu.instrument.Instrument;
import jomu.instrument.Organ;
import jomu.instrument.audio.AudioSynthesizer;
import jomu.instrument.audio.MidiSynthesizer;
import jomu.instrument.audio.TarsosAudioSynthesizer;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

@ApplicationScoped
@Component
public class Voice implements Organ {

	private AudioSynthesizer audioSynthesizer;
	private MidiSynthesizer midiSynthesizer;
	private ParameterManager parameterManager;

	public AudioSynthesizer buildAudioSynthesizer() {
		audioSynthesizer = new TarsosAudioSynthesizer();
		return this.audioSynthesizer;
	}

	public MidiSynthesizer buildMidiSynthesizer() {
		midiSynthesizer = new MidiSynthesizer();
		midiSynthesizer.open();
		return this.midiSynthesizer;
	}

	public void close(String streamId) {
		audioSynthesizer.close(streamId);
		midiSynthesizer.close(streamId);
	}

	public MidiSynthesizer getAudioSequencer() {
		return this.midiSynthesizer;
	}

	public AudioSynthesizer getAudioSynthesizer() {
		return audioSynthesizer;
	}

	@Override
	public void initialise() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		midiSynthesizer = buildMidiSynthesizer();
		audioSynthesizer = buildAudioSynthesizer();
	}

	public void send(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		System.out.println(">>send!!!: " + toneTimeFrame.getTimeSet().getStartTime());
		writeMidi(toneTimeFrame, streamId, sequence);
		// writeAudio(toneTimeFrame, streamId, sequence);
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	public void writeAudio(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		audioSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);

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
}
