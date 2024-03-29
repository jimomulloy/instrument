package jomu.instrument.audio.features;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import jomu.instrument.workspace.tonemap.ToneMapConstants;

public class ResynthFeatures extends AudioEventFeatures<ResynthInfo> implements ToneMapConstants {

	private static final Logger LOG = Logger.getLogger(ResynthFeatures.class.getName());

	private AudioFeatureFrame audioFeatureFrame;

	void initialise(AudioFeatureFrame audioFeatureFrame) {
		this.audioFeatureFrame = audioFeatureFrame;
		initialise(audioFeatureFrame.getAudioFeatureProcessor()
				.getTarsosFeatures()
				.getResynthSource());
		TreeMap<Double, ResynthInfo> newFeatures = getSource().getAndClearFeatures();
		for (Entry<Double, ResynthInfo> entry : newFeatures.entrySet()) {
			this.features.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public ResynthSource getSource() {
		return (ResynthSource) source;
	}
}
