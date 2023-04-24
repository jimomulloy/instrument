package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.MFCCFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioMFCCProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioMFCCProcessor.class.getName());

	public AudioMFCCProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);

		float pdLowThreshold = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_LOW_THRESHOLD);

		LOG.finer(">>AudioMFCCProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);

		MFCCFeatures features = aff.getMFCCFeatures();

		features.buildToneMapFrame(toneMap);
		float[] spectrum = features.getSpectrum(pdLowThreshold);

		FFTSpectrum fftSpectrum = new FFTSpectrum(features.getSource().getSampleRate(),
				features.getSource().getBufferSize(), spectrum);
		features.buildToneMapFrame(toneMap);
		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
