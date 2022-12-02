package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.Instrument;
import jomu.instrument.actuator.Voice;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.sensor.Hearing;
import jomu.instrument.workspace.WorldModel;
import jomu.instrument.workspace.tonemap.ToneMap;

public class AudioSinkProcessor implements Consumer<List<NuMessage>> {

	private NuCell cell;

	private float tmMax = 0;

	private WorldModel worldModel;

	public AudioSinkProcessor(NuCell cell) {
		super();
		this.cell = cell;
		worldModel = Instrument.getInstance().getWorldModel();
	}

	@Override
	public void accept(List<NuMessage> messages) {
		int sequence;
		String streamId;
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			if (message.source.getCellType().equals(CellTypes.AUDIO_NOTATE)) {
				Voice voice = Instrument.getInstance().getCoordinator().getVoice();
				ToneMap notateToneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId));
				voice.send(notateToneMap.getTimeFrame(sequence), streamId, sequence);
				Hearing hearing = Instrument.getInstance().getCoordinator().getHearing();
				AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
				if (afp == null || (afp.isClosed() && afp.isLastSequence(sequence))) {
					voice.close(streamId);
				}
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
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
