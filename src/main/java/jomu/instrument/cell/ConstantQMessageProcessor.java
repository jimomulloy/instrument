package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.organs.ConstantQFeatures;
import jomu.instrument.organs.PitchFrame;
import jomu.instrument.tonemap.ToneMap;

public class ConstantQMessageProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;

	public ConstantQMessageProcessor(NuCell cell) {
		super();
		this.cell = cell;
	}

	@Override
	public void accept(List<NuMessage> messages) {
		// System.out.println(">>getAudioCQProcessor");
		// System.out.println(cell.toString());
		String sequence = "";
		Object output = null;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			output = message.input;
			System.out.println("CQ process message: " + message);
			if (message.input != null) {
				PitchFrame frame = (PitchFrame) message.input;
				ConstantQFeatures cqf = frame.getConstantQFeatures();
				ToneMap toneMap = cqf.getToneMap();
				if (toneMap != null && toneMap.getTunerModel().tune()) {
					cell.send(sequence, output);
				}
			}
		}
	}

}
