package jomu.instrument.cognition;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.stereotype.Component;

import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.cognition.cell.Generator;
import jomu.instrument.cognition.cell.NuCell;
import jomu.instrument.cognition.cell.Weaver;

@ApplicationScoped
@Component
public class Cortex implements Organ, AudioFeatureFrameObserver {

	private NuCell sourceAddCell;
	private NuCell sourceUpdateCell;
	private NuCell audioCQCell;
	private NuCell audioBeatCell;
	private NuCell audioPitchCell;
	private NuCell audioSpectralPeaksCell;
	private NuCell audioPreChromaCell;
	private NuCell audioPostChromaCell;
	private NuCell audioIntegrateCell;
	private NuCell audioNotateCell;
	private NuCell audioSinkCell;
	private NuCell audioTunerPeaksCell;

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
		audioBeatCell = Generator.createNuCell(CellTypes.AUDIO_BEAT);
		audioPitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		audioSpectralPeaksCell = Generator.createNuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		audioPreChromaCell = Generator.createNuCell(CellTypes.AUDIO_PRE_CHROMA);
		audioPostChromaCell = Generator.createNuCell(CellTypes.AUDIO_POST_CHROMA);
		audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		audioSinkCell = Generator.createNuCell(CellTypes.AUDIO_SINK);
		audioTunerPeaksCell = Generator.createNuCell(CellTypes.AUDIO_TUNER_PEAKS);
		Weaver.connect(audioCQCell, audioTunerPeaksCell);
		Weaver.connect(audioCQCell, audioPreChromaCell);
		Weaver.connect(audioPreChromaCell, audioPostChromaCell);
		Weaver.connect(audioTunerPeaksCell, audioNotateCell);
		Weaver.connect(audioPostChromaCell, audioIntegrateCell);
		Weaver.connect(audioNotateCell, audioIntegrateCell);
		Weaver.connect(audioIntegrateCell, audioSinkCell);
		Weaver.connect(audioBeatCell, audioSinkCell);
		Weaver.connect(audioPitchCell, audioSinkCell);
		Weaver.connect(audioSpectralPeaksCell, audioSinkCell);
		Weaver.connect(sourceUpdateCell, audioCQCell);
		Weaver.connect(sourceUpdateCell, audioBeatCell);
		Weaver.connect(sourceUpdateCell, audioPitchCell);
		Weaver.connect(sourceUpdateCell, audioSpectralPeaksCell);
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
