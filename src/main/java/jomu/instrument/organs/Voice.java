package jomu.instrument.organs;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import jomu.instrument.audio.AudioGenerator;
import jomu.instrument.audio.AudioSynthesizer;
import jomu.instrument.audio.MidiSynthesizer;
import jomu.instrument.world.tonemap.ToneTimeFrame;

public class Voice {

	private AudioSynthesizer audioSynthesizer;
	private AudioGenerator generator;
	private MidiSynthesizer midiSynthesizer;

	/*
	 * public AudioGenerator buildAudioGenerator(int sampleRate, int
	 * audioBufferSize) throws LineUnavailableException { Hearing hearing =
	 * Instrument.getInstance().getCoordinator() .getHearing(); SourceDataLine
	 * audioOutput = hearing.getTarsosIO().getAudioOutput(); generator = new
	 * AudioGenerator(audioBufferSize, 0, sampleRate, audioOutput);
	 * generator.addAudioProcessor(new AudioPlayer( new AudioFormat(sampleRate,
	 * 16, 1, true, false))); return this.generator; }
	 */
	public AudioSynthesizer buildAudioSynthesizer() {
		audioSynthesizer = new AudioSynthesizer();
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

	public AudioGenerator getAudioGenerator() {
		return this.generator;
	}

	public MidiSynthesizer getAudioSequencer() {
		return this.midiSynthesizer;
	}

	public AudioSynthesizer getAudioSynthesizer() {
		return audioSynthesizer;
	}

	public void initialise() {
		midiSynthesizer = buildMidiSynthesizer();
		audioSynthesizer = buildAudioSynthesizer();
	}

	public void send(ToneTimeFrame toneTimeFrame, String streamId,
			int sequence) {
		System.out.println(
				">>send!!!: " + toneTimeFrame.getTimeSet().getStartTime());
		writeMidi(toneTimeFrame, streamId, sequence);
		writeAudio(toneTimeFrame, streamId, sequence);
	}

	public void start() {
		// TODO Auto-generated method stub

	}

	public void writeAudio(ToneTimeFrame toneTimeFrame, String streamId,
			int sequence) {
		audioSynthesizer.playFrameSequence(toneTimeFrame, streamId, sequence);

	}

	public void writeMidi(ToneTimeFrame toneTimeFrame, String streamId,
			int sequence) {
		try {
			midiSynthesizer.playFrameSequence(toneTimeFrame, streamId,
					sequence);
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
