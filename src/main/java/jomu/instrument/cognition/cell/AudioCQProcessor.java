package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.audio.AudioTuner;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
import jomu.instrument.audio.features.PeakInfo;
import jomu.instrument.audio.features.SpectralPeakDetector;
import jomu.instrument.audio.features.SpectralPeakDetector.SpectralPeak;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioCQProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private float tmMax = 0;

	private Workspace workspace;
	private ParameterManager parameterManager;
	private Hearing hearing;

	public AudioCQProcessor(NuCell cell) {
		super();
		this.cell = cell;
		this.workspace = Instrument.getInstance().getWorkspace();
		this.hearing = Instrument.getInstance().getCoordinator().getHearing();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;

		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>ConstantQMessageProcessor accept: " + message + ", streamId: " + streamId);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				if (afp != null) {
					double lowThreshold = parameterManager
							.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);
					double thresholdFactor = parameterManager
							.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR);
					double signalMinimum = parameterManager
							.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM);
					double normaliseThreshold = parameterManager
							.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD);
					int noiseFloorMedianFilterLenth = parameterManager
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH);
					float noiseFloorFactor = parameterManager
							.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR);
					int numberOfSpectralPeaks = parameterManager
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS);
					int minPeakSize = parameterManager
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE);
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
					boolean cqSwitchTuner = parameterManager
							.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_TUNER);
					boolean cqSwitchPeaks = parameterManager
							.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PEAKS);

					AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
					ConstantQFeatures cqf = aff.getConstantQFeatures();
					cqf.buildToneMapFrame(toneMap);
					cqf.displayToneMap();
					if (cqSwitchCompress) {
						toneMap.getTimeFrame().compress(compression);
					}
					if (cqSwitchSquare) {
						toneMap.getTimeFrame().square();
					}
					if (cqSwitchLowThreshold) {
						toneMap.getTimeFrame().lowThreshold(lowThreshold, signalMinimum);
					}
					toneMap.getTimeFrame().lowThreshold(lowThreshold, signalMinimum);
					toneMap.getTimeFrame().normaliseThreshold(normaliseThreshold, signalMinimum);

					float maxAmplitude = (float) toneMap.getTimeFrame().getMaxAmplitude();
					float minAmplitude = (float) toneMap.getTimeFrame().getMinAmplitude();
					System.out.println(">>MAX AMP PRE DB: " + maxAmplitude + ", " + tmMax);
					System.out.println(">>MIN AMP PRE DB: " + minAmplitude);

					if (cqSwitchDecibel) {
						toneMap.getTimeFrame().decibel(decibelLevel);
					}

					if (cqSwitchNormalise) {
						maxAmplitude = (float) toneMap.getTimeFrame().getMaxAmplitude();
						minAmplitude = (float) toneMap.getTimeFrame().getMinAmplitude();
						System.out.println(">>MAX AMP: " + maxAmplitude + ", " + tmMax);
						System.out.println(">>MIN AMP: " + minAmplitude);
						if (tmMax < maxAmplitude) {
							tmMax = maxAmplitude;
						}
					}

					AudioTuner tuner = new AudioTuner();
					if (cqSwitchTuner) {
						tuner.normalize(toneMap);
						double rethreshold = (thresholdFactor * (maxAmplitude - minAmplitude)) + minAmplitude;
						toneMap.getTimeFrame().lowThreshold(rethreshold, signalMinimum);
					}

					if (cqSwitchPeaks) {
						SpectralPeakDetector spectralPeakDetector = new SpectralPeakDetector(toneMap.getTimeFrame());

						PeakInfo peakInfo = new PeakInfo(spectralPeakDetector.getMagnitudes(),
								spectralPeakDetector.getFrequencyEstimates());

						List<SpectralPeak> peaks = peakInfo.getPeakList(noiseFloorMedianFilterLenth, noiseFloorFactor,
								numberOfSpectralPeaks, minPeakSize);

						if (cqSwitchTuner) {
							tuner.processPeaks(toneMap, peaks);
						}
					}

					cell.send(streamId, sequence);
				}
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

}
