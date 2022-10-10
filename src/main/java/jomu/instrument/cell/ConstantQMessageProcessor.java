package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.model.Memory;
import jomu.instrument.model.tonemap.PitchDetect;
import jomu.instrument.model.tonemap.ToneMap;
import jomu.instrument.organs.AudioFeatureFrame;
import jomu.instrument.organs.ConstantQFeatures;

public class ConstantQMessageProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private Memory memory;

	public ConstantQMessageProcessor(NuCell cell) {
		super();
		this.cell = cell;
		memory = Instrument.getInstance().getMemory();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		// System.out.println(">>getAudioCQProcessor");
		// System.out.println(cell.toString());
		String sequence = "";
		Object output = null;
		System.out.println(">>ConstantQMessageProcessor accepting");
		for (NuMessage message : messages) {
			sequence = message.sequence;
			output = message.input;
			// TODO ONLY Process one message?
			if (message.input != null) {
				System.out.println(">>ConstantQMessageProcessor accept: " + message);
				if (message.source.getCellType().equals(CellTypes.SOURCE)) {
					AudioFeatureFrame frame = (AudioFeatureFrame) message.input;
					ConstantQFeatures cqf = frame.getConstantQFeatures();
					cqf.buildToneMap();
					ToneMap toneMap = cqf.getToneMap(); //.clone();
					float[] fft = toneMap.extractFFT(1024);
					toneMap.loadFFT(fft, 1024);
					PitchDetect pd = new PitchDetect(1024, (float) toneMap.getFrame().getTimeSet().getSampleRate(),
							convertDoublesToFloats(toneMap.getFrame().getPitchSet().getFreqSet()));
					//pd.detect(fft);
					toneMap.loadFFT(fft, 1024);
					System.out.println(">>ConstantQMessageProcessor process tonemap");
					memory.getAtlas().putToneMap(this.cell.getType(), toneMap);
					cqf.displayToneMap();
					// if (toneMap != null && toneMap.getTunerModel().tune()) {
					// cqf.displayToneMap();
					System.out.println(">>ConstantQMessageProcessor send");
					cell.send(sequence, output);
					// }
				}
				// }
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
