package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.SpectralPeaksFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioSpectralPeaksProcessor extends ProcessorCommon {

	public AudioSpectralPeaksProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioSpectralPeaksProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_SPECTRAL_PEAKS, streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		if (afp != null) {
			AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
			SpectralPeaksFeatures spf = aff.getSpectralPeaksFeatures();
			spf.buildToneMapFrame(toneMap);

			if (spf.getSpectrum() != null) {
				FFTSpectrum fftSpectrum = new FFTSpectrum(spf.getSps().getSampleRate(), 1024, spf.getSpectrum());
				toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
				toneMap.getTimeFrame().deNoise(0.05);
			}
			// spf.displayToneMap();
			cell.send(streamId, sequence);
		}
	}
}
