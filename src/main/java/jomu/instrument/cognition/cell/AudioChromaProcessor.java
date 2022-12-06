package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.monitor.Visor;
import jomu.instrument.workspace.WorldModel;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioChromaProcessor implements Consumer<List<NuMessage>> {

	private static final int C4_NOTE = 36;
	private NuCell cell;
	private float tmMax = 0;

	private WorldModel worldModel;
	private Visor visor;

	public AudioChromaProcessor(NuCell cell) {
		super();
		this.cell = cell;
		worldModel = Instrument.getInstance().getWorldModel();
		visor = Instrument.getInstance().getDruid().getVisor();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>AudioChromaProcessor accept: " + message
					+ ", streamId: " + streamId);
			if (message.source.getCellType().equals(CellTypes.AUDIO_CQ)) {
				ToneMap cqToneMap = worldModel.getAtlas().getToneMap(
						buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
				ToneMap chromaToneMap = worldModel.getAtlas().getToneMap(
						buildToneMapKey(this.cell.getCellType(), streamId));
				ToneTimeFrame cqTimeFrame = cqToneMap.getTimeFrame(sequence);
				chromaToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence)
						.clone().chroma(C4_NOTE, cqTimeFrame.getPitchLow(),
								cqTimeFrame.getPitchHigh()));
				visor.updateToneMap(chromaToneMap);
				cell.send(streamId, sequence);
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
