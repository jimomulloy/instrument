package jomu.instrument.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;

import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public interface AudioSynthesizer {

	/**
	 * Clear current AudioModel objects after Reset
	 */

	void clear(String streamId);

	void close(String streamId);

	void playFrameSequence(ToneTimeFrame toneTimeFrame, String streamId, int sequence);

	/**
	 * Create audio output stream from ToneMap data
	 *
	 * @param audioOutSamples
	 * @param audioOutput
	 * @return
	 */
	AudioInputStream writeStream(ToneMap toneMap, float[] audioOutSamples, SourceDataLine audioOutput);

}