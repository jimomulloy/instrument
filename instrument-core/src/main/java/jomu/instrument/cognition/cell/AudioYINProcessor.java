package jomu.instrument.cognition.cell;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.audio.analysis.Klapuri;
import jomu.instrument.audio.analysis.PitchDetect;
import jomu.instrument.audio.analysis.PolyphonicPitchDetection;
import jomu.instrument.audio.analysis.Whitener;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.YINFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.CalibrationMap;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioYINProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioYINProcessor.class.getName());

	public AudioYINProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioYINProcessor accept: " + sequence + ", streamId: " + streamId);
		int harmonics = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_HARMONICS);
		float compression = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_COMPRESSION);
		float pdLowThreshold = parameterManager
				.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_LOW_THRESHOLD);
		boolean pdSwitchCompress = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_COMPRESS);
		boolean pdSwitchWhitener = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_WHITENER);
		boolean pdSwitchKlapuri = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_KLAPURI);
		boolean pdSwitchTarsos = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PITCH_DETECT_SWITCH_TARSOS);
		double toneMapMinFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MINIMUM_FREQUENCY);
		double toneMapMaxFrequency = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_MAXIMUM_FREQUENCY);
		boolean calibrateSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_SWITCH);
		boolean calibrateForwardSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_FORWARD_SWITCH);
		double calibrateRange = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CALIBRATE_RANGE);
		double lowThreshold = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD);

		ToneMap toneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		YINFeatures features = aff.getYINFeatures();
		features.buildToneMapFrame(toneMap);
		float[] spectrum = features.getSpectrum(pdLowThreshold);

		FFTSpectrum fftSpectrum = new FFTSpectrum(features.getSource()
				.getSampleRate(),
				features.getSource()
						.getBufferSize(),
				spectrum);
		toneMap.getTimeFrame()
				.loadFFTSpectrum(fftSpectrum);

		ToneTimeFrame ttf = toneMap.getTimeFrame();

		if (pdSwitchWhitener) {
			Whitener whitener = new Whitener(fftSpectrum);
			whitener.whiten();
			fftSpectrum = new FFTSpectrum(fftSpectrum.getSampleRate(), fftSpectrum.getWindowSize(),
					whitener.getWhitenedSpectrum());
			toneMap.getTimeFrame()
					.loadFFTSpectrum(fftSpectrum);
		}

		if (pdSwitchKlapuri) {
			PolyphonicPitchDetection ppp = new PolyphonicPitchDetection(features.getSource()
					.getSampleRate(), fftSpectrum.getWindowSize(), harmonics, pdLowThreshold);
			Klapuri klapuri = new Klapuri(convertFloatsToDoubles(spectrum), ppp);
			for (int i = 0; i < klapuri.processedSpectrumData.length; i++) {
				spectrum[i] = (float) klapuri.processedSpectrumData[i];
			}
			toneMap.getTimeFrame()
					.loadFFTSpectrum(fftSpectrum);
			processKlapuriPeaks(fftSpectrum.getSpectrum(), new ArrayList<Double>(klapuri.f0s),
					new ArrayList<Double>(klapuri.f0saliences), toneMap.getTimeFrame()
							.getElements());
			toneMap.getTimeFrame()
					.reset();
		} else if (pdSwitchTarsos) {
			PitchDetect pd = new PitchDetect(fftSpectrum.getWindowSize(), features.getSource()
					.getSampleRate(),
					toneMap.getTimeFrame()
							.getPitches());
			// pd.whiten(fftSpectrum.getSpectrum());
			pd.detect(fftSpectrum.getSpectrum());
			// Arrays.stream(convertFloatsToDoubles(pd.fzeros)).boxed().collect(Collectors.toList()),
			toneMap.getTimeFrame()
					.loadFFTSpectrum(fftSpectrum);
			processTarsosPeaks(fftSpectrum.getSpectrum(), pd.fzeros, pd.fzeroSaliences, toneMap.getTimeFrame()
					.getElements());
			toneMap.getTimeFrame()
					.reset();
		}

		toneMap.getTimeFrame()
				.filter(toneMapMinFrequency, toneMapMaxFrequency);

		LOG.finer(">>YIN TIME: " + ttf.getStartTime() + ", " + ttf.getMaxAmplitude() + ", " + ttf.getMinAmplitude()
				+ ", " + ttf.getRmsPower());
		if (workspace.getAtlas()
				.hasCalibrationMap(streamId) && calibrateSwitch) {
			CalibrationMap cm = workspace.getAtlas()
					.getCalibrationMap(streamId);
			ttf.calibrate(toneMap, cm, calibrateRange, calibrateForwardSwitch, lowThreshold, false);
		}

		console.getVisor()
				.updateToneMapView(toneMap, this.cell.getCellType()
						.toString());
		cell.send(streamId, sequence);
	}

	private void processKlapuriPeaks(float[] spectrumData, List<Double> f0s, List<Double> f0saliences,
			ToneMapElement[] elements) {
		for (ToneMapElement element : elements) {
			element.amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
		}
		for (int i = 0; i < f0s.size(); i++) {
			double f0 = f0s.get(i);
			double f0salience = f0saliences.get(i);
			int note = PitchSet.freqToMidiNote(f0);
			if (note == -1) {
				note = 0;
			}
			if (note < elements.length - 1) {
				elements[note + 1].amplitude = f0salience;
			}
		}
	}

	private void processTarsosPeaks(float[] spectrumData, float[] fzeros, float[] fzeroSaliences,
			ToneMapElement[] elements) {
		for (ToneMapElement element : elements) {
			element.amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
		}
		for (int i = 0; i < fzeros.length && i < elements.length - 1; i++) {
			if (fzeros[i] > 0) {
				elements[i + 1].amplitude = fzeroSaliences[i];
			}
		}
	}
}
