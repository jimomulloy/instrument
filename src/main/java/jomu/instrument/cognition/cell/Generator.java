package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.cognition.cell.Cell.CellTypes;

public class Generator {

	private static final Logger LOG = Logger.getLogger(Generator.class.getName());

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
		case AUDIO_YIN:
			return createAudioYINTypeCell();
		case AUDIO_TUNER_PEAKS:
			return createAudioTunerPeaksTypeCell();
		case AUDIO_SPECTRAL_PEAKS:
			return createAudioSpectralPeaksTypeCell();
		case AUDIO_CQ:
			return createAudioCQTypeCell();
		case AUDIO_CQ_ORIGIN:
			return createAudioCQOriginTypeCell();
		case AUDIO_CQ_MICRO_TONE:
			return createAudioCQMicroToneTypeCell();
		case AUDIO_BEAT:
			return createAudioBeatTypeCell();
		case AUDIO_ONSET:
			return createAudioOnsetTypeCell();
		case AUDIO_PERCUSSION:
			return createAudioPercussionTypeCell();
		case AUDIO_HPS:
			return createAudioHpsTypeCell();
		case AUDIO_PRE_CHROMA:
			return createAudioPreChromaTypeCell();
		case AUDIO_POST_CHROMA:
			return createAudioPostChromaTypeCell();
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

	private static NuCell createAudioCQOriginTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_CQ_ORIGIN);
		cell.setProcessor(getAudioCQOriginProcessor(cell));
		return cell;
	}

	private static NuCell createAudioCQMicroToneTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_CQ_MICRO_TONE);
		cell.setProcessor(getAudioCQMicroToneProcessor(cell));
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

	private static NuCell createAudioYINTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_YIN);
		cell.setProcessor(getAudioYINProcessor(cell));
		return cell;
	}

	private static NuCell createAudioSpectralPeaksTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		cell.setProcessor(getAudioSpectralPeaksProcessor(cell));
		return cell;
	}

	private static NuCell createAudioTunerPeaksTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_TUNER_PEAKS);
		cell.setProcessor(getAudioTunerPeaksProcessor(cell));
		return cell;
	}

	private static NuCell createAudioPreChromaTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_PRE_CHROMA);
		cell.setProcessor(getAudioChromaPreProcessor(cell));
		return cell;
	}

	private static NuCell createAudioPostChromaTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_POST_CHROMA);
		cell.setProcessor(getAudioChromaPostProcessor(cell));
		return cell;
	}

	private static NuCell createAudioBeatTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_BEAT);
		cell.setProcessor(getAudioBeatProcessor(cell));
		return cell;
	}

	private static NuCell createAudioOnsetTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_ONSET);
		cell.setProcessor(getAudioOnsetProcessor(cell));
		return cell;
	}

	private static NuCell createAudioPercussionTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_PERCUSSION);
		cell.setProcessor(getAudioPercussionProcessor(cell));
		return cell;
	}

	private static NuCell createAudioHpsTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_HPS);
		cell.setProcessor(getAudioHpsProcessor(cell));
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

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioCQProcessor(NuCell cell) {
		return new AudioCQProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioCQOriginProcessor(NuCell cell) {
		return new AudioCQOriginProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioCQMicroToneProcessor(NuCell cell) {
		return new AudioCQMicroToneProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioIntegrateProcessor(NuCell cell) {
		return new AudioIntegrateProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioNotateProcessor(NuCell cell) {
		return new AudioNotateProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioPitchProcessor(NuCell cell) {
		return new AudioPitchProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioYINProcessor(NuCell cell) {
		return new AudioYINProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioSpectralPeaksProcessor(NuCell cell) {
		return new AudioSpectralPeaksProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioTunerPeaksProcessor(NuCell cell) {
		return new AudioTunerPeaksProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioChromaPreProcessor(NuCell cell) {
		return new AudioChromaPreProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioChromaPostProcessor(NuCell cell) {
		return new AudioChromaPostProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioBeatProcessor(NuCell cell) {
		return new AudioBeatProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioOnsetProcessor(NuCell cell) {
		return new AudioOnsetProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioPercussionProcessor(NuCell cell) {
		return new AudioPercussionProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioHpsProcessor(NuCell cell) {
		return new AudioHpsProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getAudioSinkProcessor(NuCell cell) {
		return new AudioSinkProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getJunctionProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				cell.send(message);
			}
		};
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getSinkProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				LOG.finer(">>SinkProcessor process message: " + message);
			}
		};
	}

	private static ThrowingConsumer<List<NuMessage>, Exception> getSourceProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				cell.send(message.streamId, message.sequence);
			}
		};
	}
}
