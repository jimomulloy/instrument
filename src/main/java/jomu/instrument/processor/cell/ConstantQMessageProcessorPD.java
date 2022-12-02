package jomu.instrument.processor.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
import jomu.instrument.processor.cell.Cell.CellTypes;
import jomu.instrument.sensor.Hearing;
import jomu.instrument.workspace.WorldModel;
import jomu.instrument.workspace.tonemap.ToneMap;

public class ConstantQMessageProcessorPD implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private WorldModel worldModel;

	public ConstantQMessageProcessorPD(NuCell cell) {
		super();
		this.cell = cell;
		worldModel = Instrument.getInstance().getWorldModel();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		// System.out.println(">>getAudioCQProcessor");
		// System.out.println(cell.toString());
		int sequence;
		String streamId;
		System.out.println(">>ConstantQMessageProcessor accepting");
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>ConstantQMessageProcessor accept: " + message);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
				ConstantQFeatures cqf = aff.getConstantQFeatures();
				// cqf.buildToneMap();
				ToneMap toneMap = cqf.getToneMap(); // .clone();
				// float[] fft = toneMap.extractFFT(4096);
				// PitchDetect pd = new PitchDetect(4096, (float)
				// toneMap.getTimeFrame().getTimeSet().getSampleRate(),
				// convertDoublesToFloats(toneMap.getTimeFrame().getPitchSet().getFreqSet()));
				// pd.detect(fft);
				// toneMap.loadFFT(fft, 4096);
				// toneMap.reset();
				System.out.println(">>ConstantQMessageProcessor process tonemap");
				// worldModel.getAtlas().putToneMap(this.cell.getCellType(),
				// toneMap);
				cqf.displayToneMap();
				System.out.println(">>ConstantQMessageProcessor send");
				cell.send(streamId, sequence);
			}
		}
	}

	public static float[] convertDoublesToFloats(double[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		float[] output = new float[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = (float) input[i];
		}
		return output;
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
