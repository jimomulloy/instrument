package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.organs.AudioFeatureFrame;
import jomu.instrument.organs.AudioFeatureProcessor;
import jomu.instrument.organs.ConstantQFeatures;
import jomu.instrument.organs.Hearing;
import jomu.instrument.world.Memory;
import jomu.instrument.world.tonemap.PitchDetect;
import jomu.instrument.world.tonemap.ToneMap;

public class ConstantQMessageProcessorPD implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private Memory memory;

	public ConstantQMessageProcessorPD(NuCell cell) {
		super();
		this.cell = cell;
		memory = Instrument.getInstance().getMemory();
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
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor();
				AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
				ConstantQFeatures cqf = aff.getConstantQFeatures();
				cqf.buildToneMap();
				ToneMap toneMap = cqf.getToneMap(); //.clone();
				//float[] fft = toneMap.extractFFT(4096);
				//PitchDetect pd = new PitchDetect(4096, (float) toneMap.getTimeFrame().getTimeSet().getSampleRate(),
				//		convertDoublesToFloats(toneMap.getTimeFrame().getPitchSet().getFreqSet()));
				//pd.detect(fft);
				//toneMap.loadFFT(fft, 4096);
				//toneMap.reset();
				System.out.println(">>ConstantQMessageProcessor process tonemap");
				memory.getAtlas().putToneMap(this.cell.getType(), toneMap);
				cqf.displayToneMap();
				System.out.println(">>ConstantQMessageProcessor send");
				cell.send(streamId, sequence);
			}
		}
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
}
