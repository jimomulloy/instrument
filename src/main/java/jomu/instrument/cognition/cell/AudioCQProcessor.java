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

	public AudioCQProcessor(NuCell cell) {
		super();
		this.cell = cell;
		workspace = Instrument.getInstance().getWorkspace();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		// System.out.println(">>getAudioCQProcessor");
		// System.out.println(cell.toString());
		int sequence;
		String streamId;

		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>ConstantQMessageProcessor accept: " + message + ", streamId: " + streamId);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
				ToneMap toneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				if (afp != null) {
					double lowThreshold = parameterManager
							.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_LOW_THRESHOLD); // 0.01
					double thresholdFactor = parameterManager
							.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_THRESHOLD_FACTOR); // 0.5F
					double signalMinimum = parameterManager
							.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_SIGNAL_MINIMUM); // 0.0000001
					double normaliseThreshold = parameterManager
							.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_CQ_NORMALISE_THRESHOLD); // 20.o

					int noiseFloorMedianFilterLenth = parameterManager
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FILTER_LENGTH); // 12;
					float noiseFloorFactor = parameterManager
							.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_NOISE_FLOOR_FACTOR); // =
																												// 100F;
																												// //
																												// 1.8F;
																												// //
																												// 2.9F;
					int numberOfSpectralPeaks = parameterManager
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_NUMBER_PEAKS); // 4;
					int minPeakSize = parameterManager
							.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_MINIMUM_PEAK_SIZE); // = 1;

					AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
					ConstantQFeatures cqf = aff.getConstantQFeatures();
					cqf.buildToneMapFrame(toneMap);
					cqf.displayToneMap();
					// toneMap.getTimeFrame().compress(10F);
					toneMap.getTimeFrame().square();
					toneMap.getTimeFrame().lowThreshold(lowThreshold, signalMinimum);
					toneMap.getTimeFrame().normaliseThreshold(normaliseThreshold, signalMinimum);

					float maxAmplitude = (float) toneMap.getTimeFrame().getMaxAmplitude();
					float minAmplitude = (float) toneMap.getTimeFrame().getMinAmplitude();
					System.out.println(">>MAX AMP PRE DB: " + maxAmplitude + ", " + tmMax);
					System.out.println(">>MIN AMP PRE DB: " + minAmplitude);
					toneMap.getTimeFrame().decibel(0.01);
					maxAmplitude = (float) toneMap.getTimeFrame().getMaxAmplitude();
					minAmplitude = (float) toneMap.getTimeFrame().getMinAmplitude();
					System.out.println(">>MAX AMP: " + maxAmplitude + ", " + tmMax);
					System.out.println(">>MIN AMP: " + minAmplitude);
					if (tmMax < maxAmplitude) {
						tmMax = maxAmplitude;
					}

					AudioTuner tuner = new AudioTuner();
					tuner.normalize(toneMap);
					double rethreshold = (thresholdFactor * (maxAmplitude - minAmplitude)) + minAmplitude;
					toneMap.getTimeFrame().lowThreshold(rethreshold, signalMinimum);

					SpectralPeakDetector spectralPeakDetector = new SpectralPeakDetector(toneMap.getTimeFrame());

					PeakInfo peakInfo = new PeakInfo(spectralPeakDetector.getMagnitudes(),
							spectralPeakDetector.getFrequencyEstimates());

					List<SpectralPeak> peaks = peakInfo.getPeakList(noiseFloorMedianFilterLenth, noiseFloorFactor,
							numberOfSpectralPeaks, minPeakSize);

					tuner.processPeaks(toneMap, peaks);

					/*
					 * FFTSpectrum fftSpectrum = toneMap.getTimeFrame() .extractFFTSpectrum(4096);
					 * // PitchDetect pd = new PitchDetect(4096, (float) //
					 * toneMap.getTimeFrame().getTimeSet().getSampleRate(), //
					 * convertDoublesToFloats(toneMap.getTimeFrame().getPitchSet (). getFreqSet()));
					 * // pd.whiten(fftSpectrum.getSpectrum()); Whitener whitener = new
					 * Whitener(fftSpectrum); whitener.whiten(); FFTSpectrum whitenedSpectrum = new
					 * FFTSpectrum( fftSpectrum.getSampleRate(), fftSpectrum.getWindowSize(),
					 * whitener.getWhitenedSpectrum()); PitchAnalyser pitchAnalyser = new
					 * PitchAnalyser(fftSpectrum, toneMap.getTimeFrame().getPitches(), 20);
					 * toneMap.getTimeFrame().loadFFTSpectrum(fftSpectrum);
					 * toneMap.getTimeFrame().deNoise(0.05);
					 */
					// cqf.displayToneMap();
					cell.send(streamId, sequence);
				}
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

	private float[] convertDoublesToFloats(double[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		float[] output = new float[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = (float) input[i];
		}
		return output;
	}

	private double[] convertFloatsToDoubles(float[] input) {
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
