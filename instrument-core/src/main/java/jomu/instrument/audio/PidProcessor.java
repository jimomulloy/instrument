package jomu.instrument.audio;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * This class processes PID control feedback on the audio signal
 *
 */
public class PidProcessor implements AudioProcessor {

	float[] samplesT1 = null;
	float[] samplesT2 = null;
	float pFactor = 0;
	float dFactor = 0;
	float iFactor = 0;

	public PidProcessor(float pFactor, float dFactor, float iFactor) {
		super();
		this.pFactor = pFactor;
		this.dFactor = dFactor;
		this.iFactor = iFactor;
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] samplesT0 = audioEvent.getFloatBuffer().clone();
		for (int n = 0; n < samplesT0.length; n++) {
			float pTerm = samplesT0[n];
			float dTerm = 0;
			float iTerm = 0;
			if (n > 0) {
				pTerm = samplesT0[n] - pFactor * samplesT0[n - 1];
				if (samplesT1 != null) {
					dTerm = dFactor * (samplesT0[n] - samplesT1[n]);
					if (samplesT2 != null) {
						iTerm = iFactor * ((samplesT0[n] - samplesT1[n]) - (samplesT1[n] - samplesT2[n]));
					}
				}
			}
			samplesT0[n] = pTerm + dTerm + iTerm;
		}

		audioEvent.setFloatBuffer(samplesT0);
		if (samplesT1 != null) {
			samplesT2 = samplesT1.clone();
		}
		samplesT1 = samplesT0.clone();
		return true;
	}

	@Override
	public void processingFinished() {

	}
}
