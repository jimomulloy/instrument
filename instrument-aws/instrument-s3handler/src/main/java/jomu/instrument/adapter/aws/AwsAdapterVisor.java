package jomu.instrument.adapter.aws;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.monitor.Visor;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

@ApplicationScoped
public class AwsAdapterVisor implements Visor {

	@Override
	public void startUp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame) {
		// TODO Auto-generated method stub

	}

	@Override
	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearView() {
		// TODO Auto-generated method stub

	}

	@Override
	public void showFrame(int frame) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTimeFrameView(ToneTimeFrame data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateToneMapView(ToneMap toneMap, String toneMapViewType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateToneMapView(ToneMap toneMap, ToneTimeFrame ttf, String toneMapViewType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBeatsView(ToneMap toneMap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePercussionView(ToneMap toneMap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateChromaPreView(ToneMap toneMap, ToneTimeFrame ttf) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateChromaPreView(ToneMap toneMap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateChromaPostView(ToneMap toneMap, ToneTimeFrame ttf) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateChromaPostView(ToneMap toneMap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSpectrumView(ToneTimeFrame toneTimeFrame, int windowSize) {
		// TODO Auto-generated method stub

	}

	@Override
	public void audioStopped() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateParameters() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateStatusMessage(String string) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBeatsView(ToneMap toneMap, ToneTimeFrame ttf) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePercussionView(ToneMap toneMap, ToneTimeFrame ttf) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateChromaSynthView(ToneMap toneMap, ToneTimeFrame ttf) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateChromaSynthView(ToneMap toneMap) {
		// TODO Auto-generated method stub

	}

}
