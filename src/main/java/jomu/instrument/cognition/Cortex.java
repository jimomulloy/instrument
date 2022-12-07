package jomu.instrument.cognition;

import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.cognition.cell.Generator;
import jomu.instrument.cognition.cell.NuCell;
import jomu.instrument.cognition.cell.Weaver;

public class Cortex implements Organ, AudioFeatureFrameObserver {

	private NuCell sourceAddCell;
	private NuCell sourceUpdateCell;

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
		NuCell audioCQCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		NuCell audioOnsetCell = Generator.createNuCell(CellTypes.AUDIO_ONSET);
		NuCell audioBeatCell = Generator.createNuCell(CellTypes.AUDIO_BEAT);
		NuCell audioSpectrumCell = Generator.createNuCell(CellTypes.AUDIO_SPECTRUM);
		// NuCell audioSpectralPeaksCell = Generator
		// .createNuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		NuCell audioChromaCell = Generator.createNuCell(CellTypes.AUDIO_CHROMA);
		NuCell audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		NuCell audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		NuCell audioSinkCell = Generator.createNuCell(CellTypes.AUDIO_SINK);
		Weaver.connect(audioCQCell, audioChromaCell);
		Weaver.connect(audioCQCell, audioIntegrateCell);
		// Weaver.connect(audioSpectralPeaksCell, audioIntegrateCell);
		Weaver.connect(audioSpectrumCell, audioIntegrateCell);
		Weaver.connect(audioChromaCell, audioIntegrateCell);
		Weaver.connect(audioIntegrateCell, audioNotateCell);
		Weaver.connect(audioNotateCell, audioSinkCell);
		Weaver.connect(sourceUpdateCell, audioCQCell);
		Weaver.connect(sourceUpdateCell, audioOnsetCell);
		Weaver.connect(sourceUpdateCell, audioBeatCell);
		Weaver.connect(sourceUpdateCell, audioSpectrumCell);
		// Weaver.connect(sourceUpdateCell, audioSpectralPeaksCell);
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
