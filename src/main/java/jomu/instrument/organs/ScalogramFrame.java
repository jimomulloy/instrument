package jomu.instrument.organs;

import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.wavelet.HaarWaveletTransform;

public class ScalogramFrame {
	float[][] dataPerScale;
	float[] durationsOfBlockPerLevel;
	float[] startFrequencyPerLevel;// cents
	float[] stopFrequencyPerLevel;// cents

	float currentMax;
	private float[] transformedData;

	public ScalogramFrame(float[] transformedData, float currentMax) {
		this.transformedData = transformedData;
		this.currentMax = currentMax;
		int levels = HaarWaveletTransform.log2(transformedData.length);
		dataPerScale = new float[levels][];
		durationsOfBlockPerLevel = new float[levels];
		startFrequencyPerLevel = new float[levels];
		stopFrequencyPerLevel = new float[levels];
		for (int i = 0; i < levels; i++) {
			int samples = HaarWaveletTransform.pow2(i);
			dataPerScale[i] = new float[samples];
			durationsOfBlockPerLevel[i] = (131072 / (float) samples) / 44100.0f;
			stopFrequencyPerLevel[i] = (float) PitchConverter
					.hertzToAbsoluteCent(44100
							/ (float) HaarWaveletTransform.pow2(levels - i));
			if (i > 0) {
				startFrequencyPerLevel[i] = stopFrequencyPerLevel[i - 1];
			}
			mra(transformedData, i, dataPerScale);
		}

	}

	@Override
	public ScalogramFrame clone() {
		ScalogramFrame cloned = new ScalogramFrame(this.transformedData,
				this.currentMax);
		return cloned;
	}

	private void mra(float[] transformedData, int level,
			float[][] dataPerScale) {
		int startIndex = transformedData.length
				/ HaarWaveletTransform.pow2(dataPerScale.length - level);
		int stopIndex = transformedData.length
				/ HaarWaveletTransform.pow2(dataPerScale.length - level - 1);

		int j = 0;
		for (int i = startIndex; i < stopIndex; i++) {
			dataPerScale[level][j] = -1 * transformedData[i];
			j++;
		}
		normalize(dataPerScale[level]);
	}

	private void normalize(float[] data) {
		for (float element : data) {
			currentMax = Math.max(Math.abs(element), currentMax);
		}
		for (int i = 0; i < data.length; i++) {
			// data[i]=data[i]/maxValue;
		}
	}
}