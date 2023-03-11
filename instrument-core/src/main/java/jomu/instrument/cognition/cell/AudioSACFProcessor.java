package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.SACFFeatures;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioSACFProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioSACFProcessor.class.getName());

	public AudioSACFProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);

		LOG.finer(">>AudioSACFProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);

		SACFFeatures features = aff.getSACFFeatures();
		features.buildToneMapFrame(toneMap);
		float[] spectrum = features.getSpectrum();

		FFTSpectrum fftSpectrum = new FFTSpectrum(features.getSource().getSampleRate(),
				features.getSource().getBufferSize(), spectrum);

		toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
