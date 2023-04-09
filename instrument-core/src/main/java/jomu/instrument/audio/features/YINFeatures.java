package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.logging.Logger;

import be.tarsos.dsp.pitch.PitchDetectionResult;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class YINFeatures extends AudioEventFeatures<SpectrogramInfo> implements ToneMapConstants {

	private static final Logger LOG = Logger.getLogger(YINFeatures.class.getName());

	public boolean logSwitch = true;
	public int powerHigh = 100;
	public int powerLow = 0;
	private AudioFeatureFrame audioFeatureFrame;

	public float[] getSpectrum() {
		float[] spectrum = null;
		for (Entry<Double, SpectrogramInfo> entry : features.entrySet()) {
			PitchDetectionResult pdr = entry.getValue().getPitchDetectionResult();
			float[] spectralEnergy = entry.getValue().getAmplitudes();
			if (spectrum == null) {
				spectrum = new float[spectralEnergy.length];
			}
			for (int i = 0; i < spectralEnergy.length; i++) {
				if (getSource().isPowerSquared()) {
					spectrum[i] += spectralEnergy[i] * spectralEnergy[i];
				} else {
					spectrum[i] += spectralEnergy[i];
				}
			}
		}
		if (spectrum == null) {
			spectrum = new float[0];
		}
		return spectrum;
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		if (features.size() > 0) {

			float binWidth = getSource().getBinWidth();
			double timeStart = -1;
			double nextTime = -1;

			for (Entry<Double, SpectrogramInfo> column : features.entrySet()) {
				nextTime = column.getKey();
				if (timeStart == -1) {
					timeStart = nextTime;
				}
			}

			TimeSet timeSet = new TimeSet(timeStart, nextTime + binWidth, getSource().getSampleRate(),
					nextTime + binWidth - timeStart);

			// TODO !!
			int lowPitch = 36;
			int highPitch = 120;

			PitchSet pitchSet = new PitchSet(lowPitch, highPitch);

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			for (Entry<Double, SpectrogramInfo> entry : features.entrySet()) {
				PitchDetectionResult pitchDetect = entry.getValue().getPitchDetectionResult();
				float pitch = pitchDetect.getPitch();
				if (pitch > -1) {
					int index = pitchSet.getIndex(pitch);
					ttf.getElement(index).isPeak = true;
				}
			}
		} else {
			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;
			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);

			// TODO !!
			int lowPitch = 36;
			int highPitch = 120;

			PitchSet pitchSet = new PitchSet(lowPitch, highPitch);

			ToneTimeFrame ttf = new ToneTimeFrame(timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor().getTarsosFeatures().getYINSource());
		this.features = getSource().getAndClearFeatures();
	}

	@Override
	public YINSource getSource() {
		return (YINSource) source;
	}
}
