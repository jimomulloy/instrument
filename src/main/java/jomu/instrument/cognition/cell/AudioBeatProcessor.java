package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.OnsetFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.monitor.Visor;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.WorldModel;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioBeatProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;
	private float tmMax = 0;

	private WorldModel worldModel;
	private Visor visor;

	public AudioBeatProcessor(NuCell cell) {
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
			System.out.println(">>AudioBeatProcessor accept: " + message + ", streamId: " + streamId);
			if (message.source.getCellType().equals(CellTypes.SOURCE)) {
				Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
				ToneMap toneMap = worldModel.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId));
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				if (afp != null) {
					AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
					OnsetFeatures osf = aff.getOnsetFeatures();
					osf.buildToneMapFrame(toneMap);
					// visor.updateToneMap(toneMap);
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