package jomu.instrument.cognition.cell;

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
import jomu.instrument.workspace.tonemap.ToneMap;

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
								fftSpectrum.getWindowSize(), harmonics);
						System.out.println(">>PP MAX ENTER KLAPURI!!");
						Klapuri klapuri = new Klapuri(convertFloatsToDoubles(spectrum), ppp);
						System.out.println(">>!!KLAPURI PEAKS : " + klapuri.f0s.size());
						toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
						System.out.println(">>PP MAX AMP 4: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
								+ toneMap.getTimeFrame().getMinAmplitude());
					}

					if (pdSwitchTarsos) {
						PitchDetect pd = new PitchDetect(fftSpectrum.getWindowSize(), fftSpectrum.getSampleRate());
						pd.detect(fftSpectrum.getSpectrum());
						toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
						System.out.println(">>PP MAX AMP 5: " + toneMap.getTimeFrame().getMaxAmplitude() + ", "
								+ toneMap.getTimeFrame().getMinAmplitude());
					}

					toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);

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
