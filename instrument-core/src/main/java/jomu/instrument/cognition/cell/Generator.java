package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
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
		case AUDIO_PHASE:
			return createAudioPhaseTypeCell();
		case AUDIO_YIN:
			return createAudioYINTypeCell();
		case AUDIO_SACF:
			return createAudioSACFTypeCell();
		case AUDIO_MFCC:
			return createAudioMFCCTypeCell();
		case AUDIO_CEPSTRUM:
			return createAudioCepstrumTypeCell();
		case AUDIO_SYNTHESIS:
			return createAudioSyntesisTypeCell();
		case AUDIO_TUNER_PEAKS:
			return createAudioTunerPeaksTypeCell();
		case AUDIO_SPECTRAL_PEAKS:
			return createAudioSpectralPeaksTypeCell();
		case AUDIO_CQ:
			return createAudioCQTypeCell();
		case AUDIO_CQ_ORIGIN:
			return createAudioCQOriginTypeCell();
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
			throw new InstrumentException("NuCell createNuCell undefined type: " + cellType);
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

	private static NuCell createAudioPhaseTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_PHASE);
		cell.setProcessor(getAudioPhaseProcessor(cell));
		return cell;
	}

	private static NuCell createAudioYINTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_YIN);
		cell.setProcessor(getAudioYINProcessor(cell));
		return cell;
	}

	private static NuCell createAudioSACFTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_SACF);
		cell.setProcessor(getAudioSACFProcessor(cell));
		return cell;
	}

	private static NuCell createAudioMFCCTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_MFCC);
		cell.setProcessor(getAudioMFCCProcessor(cell));
		return cell;
	}

	private static NuCell createAudioCepstrumTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_CEPSTRUM);
		cell.setProcessor(getAudioCepstrumProcessor(cell));
		return cell;
	}

	private static NuCell createAudioSyntesisTypeCell() {
		NuCell cell = new NuCell(CellTypes.AUDIO_SYNTHESIS);
		cell.setProcessor(getAudioSynthesisProcessor(cell));
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

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioCQProcessor(NuCell cell) {
		return new AudioCQProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioCQOriginProcessor(NuCell cell) {
		return new AudioCQOriginProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioIntegrateProcessor(NuCell cell) {
		return new AudioIntegrateProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioNotateProcessor(NuCell cell) {
		return new AudioNotateProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioPitchProcessor(NuCell cell) {
		return new AudioPitchProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioPhaseProcessor(NuCell cell) {
		return new AudioPhaseProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioYINProcessor(NuCell cell) {
		return new AudioYINProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioSACFProcessor(NuCell cell) {
		return new AudioSACFProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioMFCCProcessor(NuCell cell) {
		return new AudioMFCCProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioCepstrumProcessor(NuCell cell) {
		return new AudioCepstrumProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioSynthesisProcessor(NuCell cell) {
		return new AudioSynthesisProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioSpectralPeaksProcessor(NuCell cell) {
		return new AudioSpectralPeaksProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioTunerPeaksProcessor(NuCell cell) {
		return new AudioTunerPeaksProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioChromaPreProcessor(NuCell cell) {
		return new AudioChromaPreProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioChromaPostProcessor(NuCell cell) {
		return new AudioChromaPostProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioBeatProcessor(NuCell cell) {
		return new AudioBeatProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioOnsetProcessor(NuCell cell) {
		return new AudioOnsetProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioPercussionProcessor(NuCell cell) {
		return new AudioPercussionProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioHpsProcessor(NuCell cell) {
		return new AudioHpsProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getAudioSinkProcessor(NuCell cell) {
		return new AudioSinkProcessor(cell);
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getJunctionProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				cell.send(message);
			}
		};
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getSinkProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				LOG.finer(">>SinkProcessor process message: " + message);
			}
		};
	}

	private static ThrowingConsumer<List<NuMessage>, InstrumentException> getSourceProcessor(NuCell cell) {
		return (List<NuMessage> messages) -> {
			for (NuMessage message : messages) {
				cell.send(message.streamId, message.sequence);
			}
		};
	}
}
