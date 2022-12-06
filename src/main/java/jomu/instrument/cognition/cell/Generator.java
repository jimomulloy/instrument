package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.cognition.cell.Cell.CellTypes;

public class Generator {

	public static NuCell createNuCell(CellTypes cellType) {
		switch (cellType) {
		case SOURCE:
			return createSourceTypeCell();
		case SINK:
			return createSinkTypeCell();
		case AUDIO_SINK:
			return createAudioSinkTypeCell();
		case AUDIO_PITCH:
			return createAudioPitchTypeCell();
		case AUDIO_CQ:
			return createAudioCQTypeCell();
		case AUDIO_INTEGRATE:
			return createAudioIntegrateTypeCell();
		case AUDIO_NOTATE:
			return createAudioNotateTypeCell();
		case JUNCTION:
			return createJunctionTypeCell();
		default:
			return null;
		}
	}

	private static NuCell createAudioCQTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_CQ);
		cell.setProcessor(getAudioCQProcessor(cell));
		return cell;
	}

	private static NuCell createAudioIntegrateTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_INTEGRATE);
		cell.setProcessor(getAudioIntegrateProcessor(cell));
		return cell;
	}

	private static NuCell createAudioNotateTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_NOTATE);
		cell.setProcessor(getAudioNotateProcessor(cell));
		return cell;
	}

	private static NuCell createAudioPitchTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_PITCH);
		cell.setProcessor(getAudioPitchProcessor(cell));
		return cell;
	}

	private static NuCell createAudioSinkTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_SINK);
		cell.setProcessor(getAudioSinkProcessor(cell));
		return cell;
	}

	private static NuCell createJunctionTypeCell() {
		NuCell cell = new NuCell(CellTypes.JUNCTION);
		cell.setProcessor(getJunctionProcessor(cell));
		return cell;
	}

	private static NuCell createSinkTypeCell() {
		NuCell cell = new NuCell(CellTypes.SINK);
		cell.setProcessor(getSinkProcessor(cell));
		return cell;
	}

	private static NuCell createSourceTypeCell() {
		NuCell cell = new NuCell(CellTypes.SOURCE);
		cell.setProcessor(getSourceProcessor(cell));
		return cell;
	}

	private static Consumer<List<NuMessage>> getAudioCQProcessor(NuCell cell) {
		return new AudioCQProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioIntegrateProcessor(NuCell cell) {
		return new AudioIntegrateProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioNotateProcessor(NuCell cell) {
		return new AudioNotateProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioPitchProcessor(NuCell cell) {
		return new PitchDetectorProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioSinkProcessor(NuCell cell) {
		return new AudioSinkProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getJunctionProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			// System.out.println(">>getJunctionProcessor");
			for (NuMessage message : messages) {
				// System.out.println("send message: " + message);
				cell.send(message);
			}
		};
	}

	private static Consumer<List<NuMessage>> getSinkProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			// System.out.println(">>getSinkProcessor");
			// System.out.println(cell.toString());
			for (NuMessage message : messages) {
				System.out.println(">>SinkProcessor process message: " + message);
			}
		};
	}

	private static Consumer<List<NuMessage>> getSourceProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			// System.out.println(">>getSourceProcessor");
			// System.out.println(cell.toString());
			for (NuMessage message : messages) {
				// System.out.println("send message: " + message);
				cell.send(message.streamId, message.sequence);
			}
		};
	}
}
