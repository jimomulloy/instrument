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

package jomu.instrument.audio.features;

import be.tarsos.dsp.pitch.PitchDetectionResult;

public class SpectrogramInfo {
	float[] amplitudes;
	float[] phaseOffsets;
	PitchDetectionResult pitchDetectionResult;
	float[] frequencyEstimates;

	public SpectrogramInfo(PitchDetectionResult pitchDetectionResult, float[] amplitudes, float[] phaseOffsets,
			float[] frequencyEstimates) {
		super();
		this.pitchDetectionResult = pitchDetectionResult;
		this.amplitudes = amplitudes;
		this.phaseOffsets = phaseOffsets;
		this.frequencyEstimates = frequencyEstimates;
	}

	@Override
	public SpectrogramInfo clone() {
		return new SpectrogramInfo(pitchDetectionResult.clone(), amplitudes.clone(), phaseOffsets,
				frequencyEstimates);
	}

	public float[] getAmplitudes() {
		return amplitudes;
	}

	public float[] getfrequencyEstimates() {
		return frequencyEstimates;
	}

	public float[] getPhaseOffsets() {
		return phaseOffsets;
	}

	public PitchDetectionResult getPitchDetectionResult() {
		return pitchDetectionResult;
	}
}
