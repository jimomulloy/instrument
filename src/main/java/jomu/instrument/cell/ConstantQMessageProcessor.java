package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.memory.Memory;
import jomu.instrument.organs.ConstantQFeatures;
import jomu.instrument.organs.PitchFrame;
import jomu.instrument.tonemap.ToneMap;

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
					PitchFrame frame = (PitchFrame) message.input;
					ConstantQFeatures cqf = frame.getConstantQFeatures();
					cqf.buildToneMap();
					ToneMap toneMap = cqf.getToneMap();
					System.out.println(">>ConstantQMessageProcessor process tonemap");
					memory.getAtlas().putToneMap(this.cell.getType(), toneMap);
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

}
