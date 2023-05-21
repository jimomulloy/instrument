package jomu.instrument.adapter.aws;

import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jomu.instrument.InstrumentException;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.monitor.Visor;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

@ApplicationScoped
@Alternative
@io.quarkus.arc.Priority(1)
public class AwsAdapterVisor implements Visor {

	private static final Logger LOG = Logger.getLogger(AwsAdapterVisor.class.getName());

	@Override
	public void startUp() {
		// TODO Auto-generated method stub
		LOG.severe(">>AwsAdapterVisor start up");

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
	public void showException(InstrumentException exception) {
		// TODO Auto-generated method stub

	}

}
