package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.audio.AudioTuner;
import jomu.instrument.audio.features.PeakInfo;
import jomu.instrument.audio.features.SpectralPeakDetector;
import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioTunerPeaksProcessor extends ProcessorCommon {

	public AudioTunerPeaksProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioTunerPeaksProcessor accept: " + sequence + ", streamId: " + streamId);
		int noiseFloorMedianFilterLenth = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH);
		float noiseFloorFactor = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR);
		int numberOfSpectralPeaks = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS);
		int minPeakSize = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE);
		boolean tpSwitchTuner = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_TUNER);
		boolean tpSwitchPeaks = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_SWITCH_PEAKS);

		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		ToneMap tpToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		tpToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		ToneTimeFrame tpTimeFrame = tpToneMap.getTimeFrame(sequence);

		AudioTuner tuner = new AudioTuner();

		if (tpSwitchTuner) {
			tuner.processPeaks(tpToneMap);
		}

		if (tpSwitchPeaks) {
			SpectralPeakDetector spectralPeakDetector = new SpectralPeakDetector(tpToneMap.getTimeFrame());

			PeakInfo peakInfo = new PeakInfo(spectralPeakDetector.getMagnitudes(),
					spectralPeakDetector.getFrequencyEstimates());

			List<SpectralPeak> peaks = peakInfo.getPeakList(noiseFloorMedianFilterLenth, noiseFloorFactor,
					numberOfSpectralPeaks, minPeakSize);

			if (tpSwitchTuner) {
				tuner.processPeaks(tpToneMap, peaks);
			}
		}
		console.getVisor().updateToneMapView(tpToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
