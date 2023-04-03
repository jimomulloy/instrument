package jomu.instrument.cognition.cell;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.audio.analysis.Klapuri;
import jomu.instrument.audio.analysis.PitchDetect;
import jomu.instrument.audio.analysis.PolyphonicPitchDetection;
import jomu.instrument.audio.analysis.Whitener;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.PitchDetectorFeatures;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioPitchProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioPitchProcessor.class.getName());

	public AudioPitchProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioPitchProcessor accept: " + sequence + ", streamId: " + streamId);
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

		ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);

		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		PitchDetectorFeatures pdf = aff.getPitchDetectorFeatures();

		pdf.buildToneMapFrame(toneMap);
		float[] spectrum = pdf.getSpectrum();

		float maxs = 0;
		int maxi = 0;
		for (int i = 0; i < spectrum.length; i++) {
			if (maxs < spectrum[i]) {
				maxs = spectrum[i];
				maxi = i;
			}
		}

		FFTSpectrum fftSpectrum = new FFTSpectrum(pdf.getSource().getSampleRate(), pdf.getSource().getBufferSize(),
				spectrum);

		toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);

		if (pdSwitchWhitener) {
			Whitener whitener = new Whitener(fftSpectrum);
			whitener.whiten();
			fftSpectrum = new FFTSpectrum(fftSpectrum.getSampleRate(), fftSpectrum.getWindowSize(),
					whitener.getWhitenedSpectrum());
			toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
		}

		if (pdSwitchKlapuri) {
			PolyphonicPitchDetection ppp = new PolyphonicPitchDetection(pdf.getSource().getSampleRate(),
					fftSpectrum.getWindowSize(), harmonics, pdLowThreshold);
			Klapuri klapuri = new Klapuri(convertFloatsToDoubles(spectrum), ppp);
			for (int i = 0; i < klapuri.processedSpectrumData.length; i++) {
				spectrum[i] = (float) klapuri.processedSpectrumData[i];
			}
			toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
			processKlapuriPeaks(fftSpectrum.getSpectrum(), new ArrayList<Double>(klapuri.f0s),
					new ArrayList<Double>(klapuri.f0saliences), toneMap.getTimeFrame().getElements());
			toneMap.getTimeFrame().reset();
		} else if (pdSwitchTarsos) {
			PitchDetect pd = new PitchDetect(fftSpectrum.getWindowSize(), pdf.getSource().getSampleRate(),
					toneMap.getTimeFrame().getPitches());
			// pd.whiten(fftSpectrum.getSpectrum());
			pd.detect(fftSpectrum.getSpectrum());
			// Arrays.stream(convertFloatsToDoubles(pd.fzeros)).boxed().collect(Collectors.toList()),
			toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
			processTarsosPeaks(fftSpectrum.getSpectrum(), pd.fzeros, pd.fzeroSaliences,
					toneMap.getTimeFrame().getElements());
			toneMap.getTimeFrame().reset();
		}

		toneMap.getTimeFrame().filter(toneMapMinFrequency, toneMapMaxFrequency);

		console.getVisor().updateToneMapView(toneMap, this.cell.getCellType().toString());
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
