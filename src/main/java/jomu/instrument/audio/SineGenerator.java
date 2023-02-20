/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*  
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

package jomu.instrument.audio;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class SineGenerator implements AudioProcessor {

	private double gain;
	private double frequency;
	private double phase;

	public SineGenerator() {
		this(1.0, 440);
	}

	public SineGenerator(double gain, double frequency) {
		this.gain = gain;
		this.frequency = frequency;
		this.phase = 0;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] buffer = audioEvent.getFloatBuffer();
		double sampleRate = audioEvent.getSampleRate();
		double twoPiF = 2 * Math.PI * frequency;
		double time = 0;
		if (Math.abs(frequency) == 1569) {
			System.out.println(">>1569: " + frequency + ", " + gain + ", " + audioEvent.getTimeStamp());
		}
		if (gain > 0) {
			System.out.println(">>GAIN: " + frequency + ", " + gain + ", " + audioEvent.getTimeStamp() + ", " + phase);
		}
		for (int i = 0; i < buffer.length; i++) {
			time = i / sampleRate;
			// buffer[i] += (float) (gain * Math.sin(twoPiF * time + phase));
			if (gain > 0) {
				buffer[i] += (float) (gain * Math.sin(twoPiF * time + phase));
			}
		}
		phase = twoPiF * buffer.length / sampleRate + phase;
		return true;
	}

	public void setGain(double gain) {
		if (this.gain > 0 && gain == 0) {
			System.out.println(">>Reset gain: " + frequency + ", " + gain);
		}
		if (this.gain == 0 && gain > 0) {
			System.out.println(">>Set gain: " + frequency + ", " + gain);
		}

		this.gain = gain;
	}

	public double getGain() {
		return gain;
	}

	public double getFrequency() {
		return frequency;
	}

	public double getPhase() {
		return phase;
	}

	@Override
	public void processingFinished() {
	}
}
