package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.logging.Logger;

import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class CepstrumFeatures extends AudioEventFeatures<CepstrumInfo> implements ToneMapConstants {

	private static final Logger LOG = Logger.getLogger(CepstrumFeatures.class.getName());

	public boolean logSwitch = true;
	public int powerHigh = 100;
	public int powerLow = 0;
	private AudioFeatureFrame audioFeatureFrame;

	private TimeSet timeSet;
	private PitchSet pitchSet;

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor()
				.getTarsosFeatures()
				.getCepstrumSource());
		this.features = getSource().getAndClearFeatures();
	}

	public float[] getSpectrum(double lowThreshold) {
		float[] spectrum = null;
		for (Entry<Double, CepstrumInfo> entry : features.entrySet()) {
			double[] spectralEnergy = entry.getValue()
					.getMagnitudes();
			if (spectrum == null) {
				spectrum = new float[spectralEnergy.length];
			}
			for (int i = 0; i < spectralEnergy.length; i++) {
				if (getSource().isMicroToneSwitch()) {
					if (spectrum[i] < spectralEnergy[i]) {
						spectrum[i] = (float) spectralEnergy[i];
					}
				} else {
					spectrum[i] += spectralEnergy[i];
				}
			}
		}
		if (spectrum == null) {
			spectrum = new float[0];
		}
		for (int i = 0; i < spectrum.length; i++) {
			if (spectrum[i] < lowThreshold) {
				spectrum[i] = 0;
			}
		}
		return spectrum;
	}

	public void buildToneMapFrame(ToneMap toneMap) {

		if (features.size() > 0) {

			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;
			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);
			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);

			if (features.size() > 0) {
				for (CepstrumInfo feature : features.values()) {
					for (int peak : feature.peaks) {
						if (peak == feature.getMaxACFIndex()) {
							// float frequency = feature.getLength() / peak;
							float frequency = getSource().getSampleRate() / peak;
							int tmIndex = pitchSet.getIndex(frequency);
							if (tmIndex > -1) {
								if (getSource().isMicroToneSwitch()) {
									if (ttf.getElement(tmIndex).amplitude < feature.correlations[peak]) {
										ttf.getElement(tmIndex).amplitude = feature.correlations[peak];
									}
								} else {
									ttf.getElement(tmIndex).amplitude += feature.correlations[peak];
								}
								ttf.getElement(tmIndex).isPeak = true;
							}
						}
					}
				}
				ttf.reset();
			}

		} else {
			double timeStart = this.audioFeatureFrame.getStart() / 1000.0;
			double timeEnd = this.audioFeatureFrame.getEnd() / 1000.0;
			TimeSet timeSet = new TimeSet(timeStart, timeEnd, getSource().getSampleRate(), timeEnd - timeStart);
			PitchSet pitchSet = new PitchSet();

			ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
			toneMap.addTimeFrame(ttf);
		}
	}

	@Override
	public CepstrumSource getSource() {
		return (CepstrumSource) source;
	}

}
