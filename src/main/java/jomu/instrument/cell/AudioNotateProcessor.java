package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.organs.Hearing;
import jomu.instrument.world.WorldModel;
import jomu.instrument.world.tonemap.ToneMap;
import jomu.instrument.world.tonemap.TunerModel;

public class AudioNotateProcessor implements Consumer<List<NuMessage>> {

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
	private static double[] convertFloatsToDoubles(float[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}

	private NuCell cell;

	private WorldModel worldModel;

	private float tmMax = 0;

	public AudioNotateProcessor(NuCell cell) {
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
		System.out.println(">>AudioNotateProcessor accepting");
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>AudioNotateProcessor accept: " + message);
			if (message.source.getCellType()
					.equals(CellTypes.AUDIO_INTEGRATE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator()
						.getHearing();
				ToneMap integrateToneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE,
								streamId, sequence));
				ToneMap notateToneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(this.cell.getCellType(),
								streamId, sequence));
				notateToneMap.addTimeFrame(integrateToneMap.getTimeFrame());

				TunerModel tuner = new TunerModel();

				tuner.noteScan(notateToneMap);

				System.out.println(">>AudioNotateProcessor send");
				cell.send(streamId, sequence);
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId,
			int sequence) {
		return cellType + ":" + streamId + ":" + sequence;
	}
}
