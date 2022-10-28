package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import jomu.instrument.Instrument;
import jomu.instrument.cell.Cell.CellTypes;
import jomu.instrument.organs.Voice;
import jomu.instrument.world.WorldModel;
import jomu.instrument.world.tonemap.MidiModel;
import jomu.instrument.world.tonemap.ToneMap;
import jomu.instrument.world.tonemap.ToneTimeFrame;

public class AudioSinkProcessor implements Consumer<List<NuMessage>> {

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

	public AudioSinkProcessor(NuCell cell) {
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
		System.out.println(">>AudioSinkProcessor accepting");
		for (NuMessage message : messages) {
			sequence = message.sequence;
			streamId = message.streamId;
			System.out.println(">>AudioSinkProcessor accept: " + message);
			if (message.source.getCellType().equals(CellTypes.AUDIO_NOTATE)) {
				Voice voice = Instrument.getInstance().getCoordinator()
						.getVoice();
				ToneMap notateToneMap = worldModel.getAtlas()
						.getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE,
								streamId, sequence));
				ToneTimeFrame toneTimeFrame = notateToneMap.getTimeFrame();
				/*
				 * TimeSet timeSet = toneTimeFrame.getTimeSet();
				 *
				 * AudioGenerator audioGenerator = voice.getAudioGenerator(); if
				 * (audioGenerator == null) { try { audioGenerator =
				 * voice.buildAudioGenerator( (int) timeSet.getSampleRate(),
				 * timeSet.getSampleWindow()); } catch (LineUnavailableException
				 * e) { // TODO Auto-generated catch block e.printStackTrace();
				 * } } audioGenerator.send(notateToneMap, sequence);
				 */

				MidiModel audioSequencer = voice.getAudioSequencer();
				if (audioSequencer == null) {
					audioSequencer = voice.buildAudioSequencer();
				}
				try {
					audioSequencer.writeFrameSequence(toneTimeFrame);
				} catch (InvalidMidiDataException
						| MidiUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private String buildToneMapKey(CellTypes cellType, String streamId,
			int sequence) {
		return cellType + ":" + streamId + ":" + sequence;
	}
}
