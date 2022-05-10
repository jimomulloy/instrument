package jomu.instrument.cell;

import java.util.List;
import java.util.function.Consumer;

import jomu.instrument.cell.Cell.CellTypes;

public class Generator {

	public static NuCell createNuCell(CellTypes cellType) {
		switch (cellType) {
		case SOURCE:
			return createSourceTypeCell();
		case SINK:
			return createSinkTypeCell();
		case AUDIO_PITCH:
			return createAudioPitchTypeCell();
		case AUDIO_CQ:
			return createAudioCQTypeCell();
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

	private static NuCell createAudioPitchTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_PITCH);
		cell.setProcessor(getAudioPitchProcessor(cell));
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
	};

	private static NuCell createJunctionTypeCell() {
		NuCell cell = new NuCell(CellTypes.JUNCTION);
		cell.setProcessor(getJunctionProcessor(cell));
		return cell;
	};

	private static Consumer<List<NuMessage>> getAudioCQProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			// System.out.println(">>getAudioCQProcessor");
			// System.out.println(cell.toString());
			String sequence = "";
			Object output = null;
			for (NuMessage message : messages) {
				sequence = message.sequence;
				output = message.input;
				// System.out.println("CQ process message: " + message);
			}
			cell.send(sequence, output);
		};

	}

	private static Consumer<List<NuMessage>> getSinkProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			// System.out.println(">>getSinkProcessor");
			// System.out.println(cell.toString());
			for (NuMessage message : messages) {
				// System.out.println("process message: " + message + ", input: " +
				// message.input);
			}
		};
	}

	private static Consumer<List<NuMessage>> getAudioPitchProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			// System.out.println(">>getAudioPitchProcessor");
			// System.out.println(cell.toString());
			for (NuMessage message : messages) {
				// System.out.println("send message: " + message);
				cell.send(message.sequence, message.input);
			}
		};
	}

	private static Consumer<List<NuMessage>> getSourceProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			// System.out.println(">>getSourceProcessor");
			// System.out.println(cell.toString());
			for (NuMessage message : messages) {
				// System.out.println("send message: " + message);
				cell.send(message.sequence, message.input);
			}
		};
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
}
