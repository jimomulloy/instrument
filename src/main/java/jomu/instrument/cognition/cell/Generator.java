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
		case AUDIO_SPECTRUM:
			return createAudioSpectrumTypeCell();
		case AUDIO_SPECTRAL_PEAKS:
			return createAudioSpectralPeaksTypeCell();
		case AUDIO_CQ:
			return createAudioCQTypeCell();
		case AUDIO_ONSET:
			return createAudioOnsetTypeCell();
		case AUDIO_BEAT:
			return createAudioBeatTypeCell();
		case AUDIO_CHROMA:
			return createAudioChromaTypeCell();
		case AUDIO_INTEGRATE:
			return createAudioIntegrateTypeCell();
		case AUDIO_NOTATE:
			return createAudioNotateTypeCell();
		case JUNCTION:
			return createJunctionTypeCell();
		default:
			// TODO
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

	private static NuCell createAudioSpectrumTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_SPECTRUM);
		cell.setProcessor(getAudioSpectrumProcessor(cell));
		return cell;
	}

	private static NuCell createAudioSpectralPeaksTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		cell.setProcessor(getAudioSpectralPeaksProcessor(cell));
		return cell;
	}

	private static NuCell createAudioChromaTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_CHROMA);
		cell.setProcessor(getAudioChromaProcessor(cell));
		return cell;
	}

	private static NuCell createAudioOnsetTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_ONSET);
		cell.setProcessor(getAudioOnsetProcessor(cell));
		return cell;
	}

	private static NuCell createAudioBeatTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_BEAT);
		cell.setProcessor(getAudioBeatProcessor(cell));
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

	private static Consumer<List<NuMessage>> getAudioSpectrumProcessor(NuCell cell) {
		return new AudioSpectrumProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioSpectralPeaksProcessor(NuCell cell) {
		return new PitchDetectorProcessor(cell);
		// return new AudioSpectralPeaksProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioChromaProcessor(NuCell cell) {
		return new AudioChromaProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioOnsetProcessor(NuCell cell) {
		return new AudioOnsetProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioBeatProcessor(NuCell cell) {
		return new AudioBeatProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getAudioSinkProcessor(NuCell cell) {
		return new AudioSinkProcessor(cell);
	}

	private static Consumer<List<NuMessage>> getJunctionProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				cell.send(message);
			}
		};
	}

	private static Consumer<List<NuMessage>> getSinkProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				System.out.println(">>SinkProcessor process message: " + message);
			}
		};
	}

	private static Consumer<List<NuMessage>> getSourceProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				cell.send(message.streamId, message.sequence);
			}
		};
	}
}
