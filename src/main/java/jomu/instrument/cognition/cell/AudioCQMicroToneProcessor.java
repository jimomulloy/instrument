package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.CQMicroToneFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioCQMicroToneProcessor extends ProcessorCommon {

	public AudioCQMicroToneProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioCQMicroToneProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_MICRO_TONE, streamId));

		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		if (afp != null) {
			double lowThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);
			double signalMinimum = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM);
			double normaliseThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD);
			double decibelLevel = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_DECIBEL_LEVEL);
			float compression = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_COMPRESSION);
			boolean cqSwitchCompress = parameterManager
					.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS);
			boolean cqSwitchSquare = parameterManager
					.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE);
			boolean cqSwitchLowThreshold = parameterManager
					.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD);
			boolean cqSwitchDecibel = parameterManager
					.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL);
			boolean cqSwitchNormalise = parameterManager
					.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE);

			AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
			CQMicroToneFeatures cqf = aff.getCQMicroToneFeatures();
			cqf.buildToneMapFrame(toneMap);
			System.out.println(">>CQ TIME: " + toneMap.getTimeFrame().getStartTime());

			System.out.println(">>CQ MAX/MIN AMP 1: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
					+ toneMap.getTimeFrame().getMinAmplitude());
			if (cqSwitchCompress) {
				toneMap.getTimeFrame().compress(compression);
				System.out.println(">>CQ MAX/MIN AMP 2: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
						+ toneMap.getTimeFrame().getMinAmplitude());
			}
			if (cqSwitchSquare) {
				toneMap.getTimeFrame().square();
			}

			if (cqSwitchLowThreshold) {
				toneMap.getTimeFrame().lowThreshold(lowThreshold, signalMinimum);
			}

			if (cqSwitchDecibel) {
				toneMap.getTimeFrame().decibel(decibelLevel);
				System.out.println(">>CQ MAX/MIN AMP 3: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
						+ toneMap.getTimeFrame().getMinAmplitude());
			}

			System.out.println(">>CQ MAX/MIN AMP X: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
					+ toneMap.getTimeFrame().getMinAmplitude());

			// iss.addToneMap(toneMap);
			console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
			cell.send(streamId, sequence);
		}
	}
}
