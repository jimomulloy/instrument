package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.AudioTuner;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioCQProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioCQProcessor.class.getName());

	public AudioCQProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioCQProcessor accept: " + sequence + ", streamId: " + streamId);
		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));

		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
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
		boolean cqSwitchCompressUeMax = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE_MAX);
		boolean cqSwitchSquare = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SQUARE);
		boolean cqSwitchLowThreshold = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_LOW_THRESHOLD);
		boolean cqSwitchDecibel = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_DECIBEL);
		boolean cqSwitchNormalise = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE);
		boolean cqSwitchNormaliseMax = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_NORMALISE);
		boolean cqSwitchCompressMax = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_MAX);
		boolean cqSwitchCompressLog = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_COMPRESS_LOG);
		boolean cqSwitchScale = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SCALE);
		boolean cqSwitchWhiten = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN);
		boolean cqSwitchWhitenCompensate = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN_COMPENSATE);
		double cqWhitenFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_FACTOR);
		double cqWhitenThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_WHITEN_THRESHOLD);
		boolean cqSwitchPreHarmonics = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_HARMONICS);
		boolean cqSwitchPostHarmonics = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_HARMONICS);
		boolean cqSwitchPreSharpen = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_PRE_SHARPEN);
		boolean cqSwitchPostSharpen = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_POST_SHARPEN);
		boolean cqSwitchSharpenHarmonic = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_SHARPEN_HARMONIC);
		boolean cqWhiten = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SWITCH_WHITEN);
		double lowCQThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_LOW_THRESHOLD);
		double highCQThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.MONITOR_TONEMAP_VIEW_HIGH_THRESHOLD);

		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		ConstantQFeatures cqf = aff.getConstantQFeatures();
		cqf.buildToneMapFrame(toneMap);

		ToneMap cqAdaptiveWhitenControlMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType() + "_WHITENER", streamId));
		cqAdaptiveWhitenControlMap.addTimeFrame(toneMap.getTimeFrame(sequence).clone());

		LOG.finer(">>CQ TIME: " + toneMap.getTimeFrame().getStartTime() + ", "
				+ toneMap.getTimeFrame().getMaxAmplitude() + ", " + toneMap.getTimeFrame().getMinAmplitude());

		// if (cqWhiten) {
		// FFTSpectrum fftSpectrum =
		// toneMap.getTimeFrame().extractFFTSpectrum(cqf.getSource().getWindowSize());
		// Whitener whitener = new Whitener(fftSpectrum);
		// whitener.whiten();
		// fftSpectrum = new FFTSpectrum(fftSpectrum.getSampleRate(),
		// fftSpectrum.getWindowSize(),
		// whitener.getWhitenedSpectrum());
		// toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
		// }

		AudioTuner tuner = new AudioTuner();

		if (cqSwitchSharpenHarmonic) {
			if (cqSwitchPreHarmonics) {
				tuner.processOvertones(toneMap.getTimeFrame());
			}
			if (cqSwitchPreSharpen) {
				toneMap.getTimeFrame().sharpen();
			}
		} else {
			if (cqSwitchPreSharpen) {
				toneMap.getTimeFrame().sharpen();
			}
			if (cqSwitchPreHarmonics) {
				tuner.processOvertones(toneMap.getTimeFrame());
			}
		}

		if (cqSwitchCompress) {
			toneMap.getTimeFrame().compress(compression, cqSwitchCompressMax);
		}
		if (cqSwitchSquare) {
			toneMap.getTimeFrame().square();
		}

		if (cqSwitchLowThreshold) {
			toneMap.getTimeFrame().lowThreshold(lowThreshold, signalMinimum);
		}

		LOG.finer(">>CQ MAX/MIN BEFORE SCALE: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
				+ toneMap.getTimeFrame().getMinAmplitude());
		if (cqSwitchScale) {
			toneMap.getTimeFrame().scale(lowCQThreshold, highCQThreshold, cqSwitchCompressLog);
			LOG.finer(">>CQ MAX/MIN AMP AFTER SCALE: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
					+ toneMap.getTimeFrame().getMinAmplitude());
		}

		LOG.finer(">>CQ MAX/MIN BEFORE WHITEN: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
				+ toneMap.getTimeFrame().getMinAmplitude());
		if (cqSwitchWhiten) {
			toneMap.getTimeFrame().adaptiveWhiten(cqAdaptiveWhitenControlMap,
					toneMap.getPreviousTimeFrame(toneMap.getTimeFrame().getStartTime()), cqWhitenFactor,
					cqWhitenThreshold, cqSwitchWhitenCompensate);
			LOG.finer(">>CQ MAX/MIN AMP AFTER WHITEN: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
					+ toneMap.getTimeFrame().getMinAmplitude());
		}

		if (cqSwitchDecibel) {
			toneMap.getTimeFrame().decibel(decibelLevel);
		}

		if (cqSwitchSharpenHarmonic) {
			if (cqSwitchPostHarmonics) {
				tuner.processOvertones(toneMap.getTimeFrame());
			}
			if (cqSwitchPostSharpen) {
				toneMap.getTimeFrame().sharpen();
			}
		} else {
			if (cqSwitchPostSharpen) {
				toneMap.getTimeFrame().sharpen();
			}
			if (cqSwitchPostHarmonics) {
				tuner.processOvertones(toneMap.getTimeFrame());
			}
		}

		LOG.finer(">>CQ MAX/MIN AMP X: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
				+ toneMap.getTimeFrame().getMinAmplitude());

		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());

		cell.send(streamId, sequence);

	}
}
