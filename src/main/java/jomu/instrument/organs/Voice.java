package jomu.instrument.organs;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import be.tarsos.dsp.io.jvm.AudioPlayer;
import jomu.instrument.Instrument;
import jomu.instrument.world.tonemap.MidiModel;

public class Voice {

	private AudioGenerator generator;
	private MidiModel audioSequencer;
	// private AudioSynthesiser synthesiser;

	public AudioGenerator buildAudioGenerator(int sampleRate,
			int audioBufferSize) throws LineUnavailableException {
		Hearing hearing = Instrument.getInstance().getCoordinator()
				.getHearing();
		SourceDataLine audioOutput = hearing.getTarsosIO().getAudioOutput();
		generator = new AudioGenerator(audioBufferSize, 0, sampleRate,
				audioOutput);
		generator.addAudioProcessor(new AudioPlayer(
				new AudioFormat(sampleRate, 16, 1, true, false)));
		return this.generator;
	}

	public MidiModel buildAudioSequencer() {
		audioSequencer = new MidiModel();
		audioSequencer.open();
		return this.audioSequencer;
	}

	/*
	 * public AudioSynthesiser buildAudioSynthesiser(int sampleRate, int
	 * audioBufferSize) throws LineUnavailableException { synthesiser = new
	 * AudioSynthesiser(audioBufferSize, 0, sampleRate); return
	 * this.synthesiser; }
	 */

	public AudioGenerator getAudioGenerator() {
		return this.generator;
	}

	public MidiModel getAudioSequencer() {
		return this.audioSequencer;
	}

	public void initialise() {
	}

	public void start() {
		// TODO Auto-generated method stub

	}

}
