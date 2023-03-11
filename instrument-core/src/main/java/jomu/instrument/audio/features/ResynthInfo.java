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

public class ResynthInfo {
	private float[] envelopeBuffer;
	private float[] sourceBuffer;

	public ResynthInfo(float[] sourceBuffer, float[] envelopeBuffer) {
		this.sourceBuffer = sourceBuffer;
		this.envelopeBuffer = envelopeBuffer;
	}

	@Override
	public ResynthInfo clone() {
		ResynthInfo sic = new ResynthInfo(sourceBuffer.clone(), envelopeBuffer.clone());
		return sic;
	}

	public float[] getSourceBuffer() {
		return sourceBuffer;
	}

	public float[] getEnvelopeBuffer() {
		return envelopeBuffer;
	}
}
