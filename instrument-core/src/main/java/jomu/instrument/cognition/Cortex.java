package jomu.instrument.cognition;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.cognition.cell.Generator;
import jomu.instrument.cognition.cell.NuCell;
import jomu.instrument.cognition.cell.ProcessorExceptionHandler;
import jomu.instrument.cognition.cell.Weaver;
import jomu.instrument.control.Coordinator;

@ApplicationScoped
public class Cortex implements Organ, AudioFeatureFrameObserver, ProcessorExceptionHandler<InstrumentException> {

	private static final Logger LOG = Logger.getLogger(Cortex.class.getName());

	@Inject
	Coordinator coordinator;

	NuCell sourceAddCell;
	NuCell sourceUpdateCell;
	NuCell audioCQCell;
	NuCell audioCQOriginCell;
	NuCell audioBeatCell;
	NuCell audioOnsetCell;
	NuCell audioPercussionCell;
	NuCell audioHpsCell;
	NuCell audioPitchCell;
	NuCell audioSpectralPeaksCell;
	NuCell audioPreChromaCell;
	NuCell audioPostChromaCell;
	NuCell audioIntegrateCell;
	NuCell audioNotateCell;
	NuCell audioSinkCell;
	NuCell audioTunerPeaksCell;
	NuCell audioYINCell;
	NuCell audioSACFCell;
	NuCell audioMFCCCell;
	NuCell audioCepstrumCell;
	NuCell audioSynthesisCell;

	NuCell[] cells;

	@Override
	public void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame) {
		sourceAddCell.send(audioFeatureFrame.getAudioFeatureProcessor().getStreamId(),
				audioFeatureFrame.getFrameSequence());
	}

	@Override
	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		if (audioFeatureFrame.getConstantQFeatures().isCommitted()) {
			sourceUpdateCell.send(audioFeatureFrame.getAudioFeatureProcessor().getStreamId(),
					audioFeatureFrame.getFrameSequence());
		}
	}

	@Override
	public void initialise() {
		List<NuCell> cellList = new ArrayList<>();
		sourceAddCell = Generator.createNuCell(CellTypes.SOURCE);
		cellList.add(sourceAddCell);
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		cellList.add(sourceUpdateCell);
		audioCQCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		cellList.add(audioCQCell);
		audioCQOriginCell = Generator.createNuCell(CellTypes.AUDIO_CQ_ORIGIN);
		cellList.add(audioCQOriginCell);
		audioBeatCell = Generator.createNuCell(CellTypes.AUDIO_BEAT);
		cellList.add(audioBeatCell);
		audioOnsetCell = Generator.createNuCell(CellTypes.AUDIO_ONSET);
		cellList.add(audioOnsetCell);
		audioPercussionCell = Generator.createNuCell(CellTypes.AUDIO_PERCUSSION);
		cellList.add(audioPercussionCell);
		audioHpsCell = Generator.createNuCell(CellTypes.AUDIO_HPS);
		cellList.add(audioHpsCell);
		audioPitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		cellList.add(audioPitchCell);
		audioSpectralPeaksCell = Generator.createNuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		cellList.add(audioSpectralPeaksCell);
		audioPreChromaCell = Generator.createNuCell(CellTypes.AUDIO_PRE_CHROMA);
		cellList.add(audioPreChromaCell);
		audioPostChromaCell = Generator.createNuCell(CellTypes.AUDIO_POST_CHROMA);
		cellList.add(audioPostChromaCell);
		audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		cellList.add(audioIntegrateCell);
		audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		cellList.add(audioNotateCell);
		audioSinkCell = Generator.createNuCell(CellTypes.AUDIO_SINK);
		cellList.add(audioSinkCell);
		audioTunerPeaksCell = Generator.createNuCell(CellTypes.AUDIO_TUNER_PEAKS);
		cellList.add(audioTunerPeaksCell);
		audioYINCell = Generator.createNuCell(CellTypes.AUDIO_YIN);
		cellList.add(audioYINCell);
		audioSACFCell = Generator.createNuCell(CellTypes.AUDIO_SACF);
		cellList.add(audioSACFCell);
		audioMFCCCell = Generator.createNuCell(CellTypes.AUDIO_MFCC);
		cellList.add(audioMFCCCell);
		audioCepstrumCell = Generator.createNuCell(CellTypes.AUDIO_CEPSTRUM);
		cellList.add(audioCepstrumCell);
		audioSynthesisCell = Generator.createNuCell(CellTypes.AUDIO_SYNTHESIS);
		cellList.add(audioSynthesisCell);
		//
		cells = cellList.toArray(new NuCell[cellList.size()]);
		setProcessorExceptionHandlers();

		//
		Weaver.connect(sourceUpdateCell, audioBeatCell);
		Weaver.connect(sourceUpdateCell, audioPercussionCell);
		Weaver.connect(sourceUpdateCell, audioPitchCell);
		Weaver.connect(sourceUpdateCell, audioYINCell);
		Weaver.connect(sourceUpdateCell, audioCQCell);
		Weaver.connect(sourceUpdateCell, audioCQOriginCell);
		Weaver.connect(sourceUpdateCell, audioSpectralPeaksCell);
		Weaver.connect(sourceUpdateCell, audioSACFCell);
		Weaver.connect(sourceUpdateCell, audioCepstrumCell);
		Weaver.connect(sourceUpdateCell, audioMFCCCell);
		Weaver.connect(sourceUpdateCell, audioSinkCell);
		//
		Weaver.connect(audioCQCell, audioPreChromaCell);
		Weaver.connect(audioCQOriginCell, audioPreChromaCell);
		Weaver.connect(audioCQCell, audioHpsCell);
		Weaver.connect(audioCQOriginCell, audioHpsCell);
		Weaver.connect(audioCQCell, audioOnsetCell);
		Weaver.connect(audioCQOriginCell, audioOnsetCell);
		Weaver.connect(audioCQCell, audioTunerPeaksCell);
		//
		Weaver.connect(audioCQCell, audioIntegrateCell);
		Weaver.connect(audioOnsetCell, audioIntegrateCell);
		Weaver.connect(audioBeatCell, audioIntegrateCell);
		Weaver.connect(audioPercussionCell, audioIntegrateCell);
		Weaver.connect(audioPitchCell, audioIntegrateCell);
		Weaver.connect(audioYINCell, audioIntegrateCell);
		Weaver.connect(audioHpsCell, audioIntegrateCell);
		Weaver.connect(audioSpectralPeaksCell, audioIntegrateCell);
		Weaver.connect(audioSACFCell, audioIntegrateCell);
		Weaver.connect(audioMFCCCell, audioIntegrateCell);
		Weaver.connect(audioCepstrumCell, audioIntegrateCell);
		Weaver.connect(audioTunerPeaksCell, audioIntegrateCell);
		//
		Weaver.connect(audioIntegrateCell, audioNotateCell);
		//
		Weaver.connect(audioNotateCell, audioSynthesisCell);
		//
		Weaver.connect(audioPreChromaCell, audioPostChromaCell);
		Weaver.connect(audioPostChromaCell, audioSynthesisCell);
		//
		Weaver.connect(audioSynthesisCell, audioSinkCell);
	}

	private void setProcessorExceptionHandlers() {
		for (NuCell cell : cells) {
			cell.setProcessorExceptionHandler(this);
		}
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleException(InstrumentException exception) {
		coordinator.handleException(exception);
	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		for (NuCell cell : cells) {
			cell.clear();
		}
	}
}
