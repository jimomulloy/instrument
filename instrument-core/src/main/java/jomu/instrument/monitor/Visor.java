package jomu.instrument.monitor;

import jomu.instrument.InstrumentException;
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

	void updateSpectrumView(ToneTimeFrame toneTimeFrame, int windowSize);

	void audioStopped();

	void updateParameters();

	void setPlayerState(boolean enabled);

	void updateStatusMessage(String string);

	void showException(InstrumentException exception);

	void shutdown();

	void updateViewThresholds();

}