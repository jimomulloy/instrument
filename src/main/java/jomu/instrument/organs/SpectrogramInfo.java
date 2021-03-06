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

package jomu.instrument.organs;

import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.util.fft.FFT;

public class SpectrogramInfo {
	PitchDetectionResult pitchDetectionResult;
	float[] amplitudes;
	FFT fft;

	public SpectrogramInfo(PitchDetectionResult pitchDetectionResult, float[] amplitudes, FFT fft) {
		super();
		this.pitchDetectionResult = pitchDetectionResult;
		this.amplitudes = amplitudes;
		this.fft = fft;
	}

	public PitchDetectionResult getPitchDetectionResult() {
		return pitchDetectionResult;
	}

	public float[] getAmplitudes() {
		return amplitudes;
	}

	public FFT getFft() {
		return fft;
	}

	public SpectrogramInfo clone() {
		return new SpectrogramInfo(pitchDetectionResult.clone(), amplitudes.clone(), fft);
	}
}
