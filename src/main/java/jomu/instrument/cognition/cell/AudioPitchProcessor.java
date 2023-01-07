package jomu.instrument.cognition.cell;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentParameterNames;
import jomu.instrument.audio.analysis.Klapuri;
import jomu.instrument.audio.analysis.PitchDetect;
import jomu.instrument.audio.analysis.PolyphonicPitchDetection;
import jomu.instrument.audio.analysis.Whitener;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.PitchDetectorFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.FFTSpectrum;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioPitchProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private Workspace workspace;
	private Hearing hearing;
	private Console console;
	private ParameterManager parameterManager;

	public AudioPitchProcessor(NuCell cell) {
		super();
		this.cell = cell;
		this.hearing = Instrument.getInstance().getCoordinator().getHearing();
		this.console = Instrument.getInstance().getConsole();
		this.workspace = Instrument.getInstance().getWorkspace();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		for (NuMessage message : messages) {
			int sequence = message.sequence;
			String streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {

				System.out.println(">>AudioPitchProcessor accept: " + message + ", streamId: " + streamId);
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

				ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_PITCH, streamId));
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
				PitchDetectorFeatures pdf = aff.getPitchDetectorFeatures();
				pdf.buildToneMapFrame(toneMap);
				float[] spectrum = pdf.getSpectrum();
				if (spectrum != null) {
					System.out.println(">>PP TIME: " + toneMap.getTimeFrame().getStartTime());

					FFTSpectrum fftSpectrum = new FFTSpectrum(pdf.getPds().getSampleRate(),
							pdf.getPds().getBufferSize(), spectrum);

					toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);

					System.out.println(">>PP MAX AMP 1: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
							+ toneMap.getTimeFrame().getMinAmplitude());

					for (int i = 0; i < spectrum.length; i++) {
						if (spectrum[i] < pdLowThreshold) {
							spectrum[i] = 0;
						}
					}
					toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);

					System.out.println(">>PP MAX AMP 2: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
							+ toneMap.getTimeFrame().getMinAmplitude());

					// toneMap.getTimeFrame().square();
					// toneMap.getTimeFrame().lowThreshold((toneMap.getTimeFrame().getMaxAmplitude()
					// * pdLowThreshold),
					// ToneTimeFrame.AMPLITUDE_FLOOR);

					if (pdSwitchWhitener) {
						Whitener whitener = new Whitener(fftSpectrum);
						whitener.whiten();
						fftSpectrum = new FFTSpectrum(fftSpectrum.getSampleRate(), fftSpectrum.getWindowSize(),
								whitener.getWhitenedSpectrum());
						toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
						System.out.println(">>PP MAX AMP 3: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
								+ toneMap.getTimeFrame().getMinAmplitude());
					}

					if (pdSwitchKlapuri) {
						PolyphonicPitchDetection ppp = new PolyphonicPitchDetection(pdf.getPds().getSampleRate(),
								fftSpectrum.getWindowSize(), harmonics, pdLowThreshold);
						System.out.println(">>PP MAX ENTER KLAPURI!!");
						Klapuri klapuri = new Klapuri(convertFloatsToDoubles(spectrum), ppp);
						for (int i = 0; i < klapuri.processedSpectrumData.length; i++) {
							spectrum[i] = (float) klapuri.processedSpectrumData[i];
						}
						toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
						processKlapuriPeaks(fftSpectrum.getSpectrum(), new ArrayList<Double>(klapuri.f0s),
								new ArrayList<Double>(klapuri.f0saliences), toneMap.getTimeFrame().getElements());
						System.out.println(">>!!KLAPURI PEAKS : " + klapuri.f0s.size());
					} else if (pdSwitchTarsos) {
						PitchDetect pd = new PitchDetect(fftSpectrum.getWindowSize(), fftSpectrum.getSampleRate());
						// pd.whiten(fftSpectrum.getSpectrum());
						pd.detect(fftSpectrum.getSpectrum());
						toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
						System.out.println(">>PP MAX AMP 5: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
								+ toneMap.getTimeFrame().getMinAmplitude());
					} else {
						toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
					}

					System.out.println(">>PP MAX AMP 6: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
							+ toneMap.getTimeFrame().getMinAmplitude());

					// if (pdSwitchCompress) {
					// toneMap.getTimeFrame().compress(compression);
					// System.out.println(">>PP MAX AMP 2: " +
					// toneMap.getTimeFrame().getMaxAmplitude() + ", "
					// + toneMap.getTimeFrame().getMinAmplitude());
					// }

				}
				console.getVisor().updateToneMapLayer2View(toneMap);
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

	private void processKlapuriPeaks(float[] spectrumData, ArrayList<Double> f0s, ArrayList<Double> f0saliences,
			ToneMapElement[] elements) {
		for (ToneMapElement element : elements) {
			element.amplitude = ToneTimeFrame.AMPLITUDE_FLOOR;
		}
		for (int i = 0; i < f0s.size(); i++) {
			double f0 = f0s.get(i);
			double f0salience = f0saliences.get(i);
			int note = PitchSet.freqToMidiNote(f0);
			elements[note].amplitude = f0salience;
			System.out.println(">>KLAP map: " + note + ", " + elements[note].amplitude);
		}
		// Arrays.stream(elements).map(element -> element.amplitude =
		// ToneTimeFrame.AMPLITUDE_FLOOR);
		// f0s.stream().map(f0 -> PitchSet.freqToMidiNote(f0)).forEach(note ->
		// elements[note].amplitude = 1);
	}

	public static double[] convertFloatsToDoubles(float[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}
}
