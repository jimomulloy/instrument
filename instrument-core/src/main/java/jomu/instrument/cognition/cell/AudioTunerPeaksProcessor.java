package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.AudioTuner;
import jomu.instrument.audio.features.PeakInfo;
import jomu.instrument.audio.features.SpectralPeakDetector;
import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioTunerPeaksProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioTunerPeaksProcessor.class.getName());

	public AudioTunerPeaksProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioTunerPeaksProcessor accept: " + sequence + ", streamId: " + streamId);
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
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);

		ToneMap integrateToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE, streamId));
		ToneMap tpToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		tpToneMap.addTimeFrame(integrateToneMap.getTimeFrame(sequence).clone());

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

			tuner.flagPeaks(tpToneMap, peaks);

			if (tpSwitchTuner) {
				tuner.processPeaks(tpToneMap, peaks);
			}
		}

		tpToneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		console.getVisor().updateToneMapView(tpToneMap, this.cell.getCellType().toString());
		cell.send(streamId, sequence);
	}
}
