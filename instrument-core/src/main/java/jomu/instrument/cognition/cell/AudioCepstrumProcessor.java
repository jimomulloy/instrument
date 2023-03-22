package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.CepstrumFeatures;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioCepstrumProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioCepstrumProcessor.class.getName());

	public AudioCepstrumProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);

		LOG.finer(">>AudioCepstrumProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);

		CepstrumFeatures features = aff.getCepstrumFeatures();
		features.buildToneMapFrame(toneMap);
		float[] spectrum = features.getSpectrum();

		float maxs = 0;
		int maxi = 0;
		for (int i = 0; i < spectrum.length; i++) {
			if (maxs < spectrum[i]) {
				maxs = spectrum[i];
				maxi = i;
			}
		}

		FFTSpectrum fftSpectrum = new FFTSpectrum(features.getSource().getSampleRate(),
				features.getSource().getBufferSize(), spectrum);

		toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
