package jomu.instrument.cognition;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
		sourceAddCell = Generator.createNuCell(CellTypes.SOURCE);
		sourceAddCell.setProcessorExceptionHandler(this);
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		sourceUpdateCell.setProcessorExceptionHandler(this);
		audioCQCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		audioCQCell.setProcessorExceptionHandler(this);
		audioCQOriginCell = Generator.createNuCell(CellTypes.AUDIO_CQ_ORIGIN);
		audioCQOriginCell.setProcessorExceptionHandler(this);
		audioBeatCell = Generator.createNuCell(CellTypes.AUDIO_BEAT);
		audioBeatCell.setProcessorExceptionHandler(this);
		audioOnsetCell = Generator.createNuCell(CellTypes.AUDIO_ONSET);
		audioOnsetCell.setProcessorExceptionHandler(this);
		audioPercussionCell = Generator.createNuCell(CellTypes.AUDIO_PERCUSSION);
		audioPercussionCell.setProcessorExceptionHandler(this);
		audioHpsCell = Generator.createNuCell(CellTypes.AUDIO_HPS);
		audioHpsCell.setProcessorExceptionHandler(this);
		audioPitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		audioPitchCell.setProcessorExceptionHandler(this);
		audioSpectralPeaksCell = Generator.createNuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		audioSpectralPeaksCell.setProcessorExceptionHandler(this);
		audioPreChromaCell = Generator.createNuCell(CellTypes.AUDIO_PRE_CHROMA);
		audioPreChromaCell.setProcessorExceptionHandler(this);
		audioPostChromaCell = Generator.createNuCell(CellTypes.AUDIO_POST_CHROMA);
		audioPostChromaCell.setProcessorExceptionHandler(this);
		audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		audioIntegrateCell.setProcessorExceptionHandler(this);
		audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		audioNotateCell.setProcessorExceptionHandler(this);
		audioSinkCell = Generator.createNuCell(CellTypes.AUDIO_SINK);
		audioSinkCell.setProcessorExceptionHandler(this);
		audioTunerPeaksCell = Generator.createNuCell(CellTypes.AUDIO_TUNER_PEAKS);
		audioTunerPeaksCell.setProcessorExceptionHandler(this);
		audioYINCell = Generator.createNuCell(CellTypes.AUDIO_YIN);
		audioYINCell.setProcessorExceptionHandler(this);
		audioSACFCell = Generator.createNuCell(CellTypes.AUDIO_SACF);
		audioSACFCell.setProcessorExceptionHandler(this);
		audioMFCCCell = Generator.createNuCell(CellTypes.AUDIO_MFCC);
		audioMFCCCell.setProcessorExceptionHandler(this);
		audioCepstrumCell = Generator.createNuCell(CellTypes.AUDIO_CEPSTRUM);
		audioCepstrumCell.setProcessorExceptionHandler(this);
		audioSynthesisCell = Generator.createNuCell(CellTypes.AUDIO_SYNTHESIS);
		audioSynthesisCell.setProcessorExceptionHandler(this);
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
}
