package jomu.instrument.organs;

public interface AudioFeatureFrameObserver {

	void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame);

	void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame);

}
