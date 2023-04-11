package jomu.instrument.monitor;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public interface Visor extends AudioFeatureFrameObserver {

	void startUp();

	void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame);

	void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame);

	void clearView();

	void showFrame(int frame);

	void updateTimeFrameView(ToneTimeFrame data);

	void updateToneMapView(ToneMap toneMap, String toneMapViewType);

	void updateToneMapView(ToneMap toneMap, ToneTimeFrame ttf, String toneMapViewType);

	void updateBeatsView(ToneMap toneMap);

	void updateBeatsView(ToneMap toneMap, ToneTimeFrame ttf);

	void updatePercussionView(ToneMap toneMap);

	void updatePercussionView(ToneMap toneMap, ToneTimeFrame ttf);

	void updateChromaPreView(ToneMap toneMap, ToneTimeFrame ttf);

	void updateChromaPreView(ToneMap toneMap);

	void updateChromaPostView(ToneMap toneMap, ToneTimeFrame ttf);

	void updateChromaPostView(ToneMap toneMap);

	void updateSpectrumView(ToneTimeFrame toneTimeFrame, int windowSize);

	void audioStopped();

	void updateParameters();

	void updateStatusMessage(String string);

}