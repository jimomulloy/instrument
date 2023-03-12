package jomu.instrument.monitor;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public interface Visor {

	void startUp();
	
	void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame);

	void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame);

	void clearView();

	void showFrame(int frame);

	void updateTimeFrameView(ToneTimeFrame data);

	void updateToneMapView(ToneMap toneMap, String toneMapViewType);

	void updateToneMapView(ToneMap toneMap, ToneTimeFrame ttf, String toneMapViewType);

	void resetToneMapView();

	void updateBeatsView(ToneMap toneMap);

	void updatePercussionView(ToneMap toneMap);

	void updateChromaPreView(ToneMap toneMap, ToneTimeFrame ttf);

	void updateChromaPreView(ToneMap toneMap);

	void updateChromaPostView(ToneMap toneMap, ToneTimeFrame ttf);

	void updateChromaPostView(ToneMap toneMap);

	void updateSpectrumView(ToneTimeFrame toneTimeFrame, int windowSize);

	void audioStopped();

	void updateParameters();

	void updateStatusMessage(String string);

}