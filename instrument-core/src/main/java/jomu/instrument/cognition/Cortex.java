package jomu.instrument.cognition;

import javax.enterprise.context.ApplicationScoped;

import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.cognition.cell.Generator;
import jomu.instrument.cognition.cell.NuCell;
import jomu.instrument.cognition.cell.Weaver;

@ApplicationScoped
public class Cortex implements Organ, AudioFeatureFrameObserver {

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
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		audioCQCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		audioCQOriginCell = Generator.createNuCell(CellTypes.AUDIO_CQ_ORIGIN);
		audioBeatCell = Generator.createNuCell(CellTypes.AUDIO_BEAT);
		audioOnsetCell = Generator.createNuCell(CellTypes.AUDIO_ONSET);
		audioPercussionCell = Generator.createNuCell(CellTypes.AUDIO_PERCUSSION);
		audioHpsCell = Generator.createNuCell(CellTypes.AUDIO_HPS);
		audioPitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		audioSpectralPeaksCell = Generator.createNuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		audioPreChromaCell = Generator.createNuCell(CellTypes.AUDIO_PRE_CHROMA);
		audioPostChromaCell = Generator.createNuCell(CellTypes.AUDIO_POST_CHROMA);
		audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		audioSinkCell = Generator.createNuCell(CellTypes.AUDIO_SINK);
		audioTunerPeaksCell = Generator.createNuCell(CellTypes.AUDIO_TUNER_PEAKS);
		audioYINCell = Generator.createNuCell(CellTypes.AUDIO_YIN);
		audioSACFCell = Generator.createNuCell(CellTypes.AUDIO_SACF);
		audioMFCCCell = Generator.createNuCell(CellTypes.AUDIO_MFCC);
		audioCepstrumCell = Generator.createNuCell(CellTypes.AUDIO_CEPSTRUM);
		audioSynthesisCell = Generator.createNuCell(CellTypes.AUDIO_SYNTHESIS);
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
}
