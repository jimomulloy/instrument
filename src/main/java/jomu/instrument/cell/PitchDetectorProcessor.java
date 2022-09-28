package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.model.Memory;
import jomu.instrument.model.tonemap.ToneMap;
import jomu.instrument.organs.AudioFeatureFrame;
import jomu.instrument.organs.PitchDetectorFeatures;

public class PitchDetectorProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private Memory memory;

	public PitchDetectorProcessor(NuCell cell) {
		super();
		System.out.println(">>PitchDetectorProcessor create");
		this.cell = cell;
		memory = Instrument.getInstance().getMemory();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		// System.out.println(">>getAudioCQProcessor");
		// System.out.println(cell.toString());
		String sequence = "";
		Object output = null;
		System.out.println(">>PitchDetectorProcessor accepting");
		for (NuMessage message : messages) {
			sequence = message.sequence;
			output = message.input;
			// TODO ONLY Process one message?
			if (message.input != null) {
				System.out.println(">>PitchDetectorProcessor accept: " + message);
				if (message.source.getCellType().equals(CellTypes.SOURCE)) {
					AudioFeatureFrame frame = (AudioFeatureFrame) message.input;
					PitchDetectorFeatures pdf = frame.getPitchDetectorFeatures();
					pdf.buildToneMap();
					ToneMap toneMap = pdf.getToneMap();
					System.out.println(">PitchDetectorProcessor process tonemap");
					memory.getAtlas().putToneMap(this.cell.getType(), toneMap);
					// if (toneMap != null && toneMap.getTunerModel().tune()) {
					// cqf.displayToneMap();
					// System.out.println(">>ConstantQMessageProcessor send");
					// cell.send(sequence, output);
					// }
				}
				// }
			}
		}
	}

}
