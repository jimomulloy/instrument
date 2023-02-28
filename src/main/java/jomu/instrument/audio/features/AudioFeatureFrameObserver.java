package jomu.instrument.audio.features;

public interface AudioFeatureFrameObserver {

	void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame);

	void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame);

}
