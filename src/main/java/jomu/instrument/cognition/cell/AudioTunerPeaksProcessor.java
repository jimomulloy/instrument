package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.audio.AudioTuner;
import jomu.instrument.audio.features.PeakInfo;
import jomu.instrument.audio.features.SpectralPeakDetector;
import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.perception.Hearing;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioTunerPeaksProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private float tmMax = 0;

	private Workspace workspace;
	private ParameterManager parameterManager;
	private Hearing hearing;

	private InstrumentStoreService iss;
	private Console console;

	public AudioTunerPeaksProcessor(NuCell cell) {
		super();
		this.cell = cell;
		this.workspace = Instrument.getInstance().getWorkspace();
		this.hearing = Instrument.getInstance().getCoordinator().getHearing();
		this.iss = Instrument.getInstance().getStorage().getInstrumentStoreService();
		this.console = Instrument.getInstance().getConsole();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;

		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.AUDIO_CQ)) {
				System.out.println(">>AudioTunerPeaksProcessor accept: " + message + ", streamId: " + streamId);
				double thresholdFactor = parameterManager
						.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_FACTOR);
				double signalMinimum = parameterManager
						.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TUNER_THRESHOLD_MINIMUM);
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
				tpTimeFrame.reset();

				if (tpSwitchTuner) {
					tuner.normalize(tpToneMap);
					float maxAmplitude = (float) tpTimeFrame.getMaxAmplitude();
					float minAmplitude = (float) tpTimeFrame.getMinAmplitude();
					double rethreshold = (thresholdFactor * (maxAmplitude - minAmplitude)) + minAmplitude;
					tpToneMap.getTimeFrame().lowThreshold(rethreshold, signalMinimum);
					tpToneMap.getTimeFrame().setHighThres(100);
					tpToneMap.getTimeFrame().setLowThres(10);
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
				iss.addToneMap(tpToneMap);
				console.getVisor().updateToneMapLayer2View(tpToneMap);
				cell.send(streamId, sequence);

			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

}
